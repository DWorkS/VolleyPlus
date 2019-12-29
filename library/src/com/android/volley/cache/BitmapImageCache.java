/*
 * Copyright 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.volley.cache;

import java.io.File;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.StatFs;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.util.LruCache;

import com.android.volley.VolleyLog;
import com.android.volley.cache.DiskLruBasedCache.ImageCacheParams;
import com.android.volley.misc.Utils;
import com.android.volley.toolbox.ImageCache;

/**
 * This class holds our bitmap caches (memory and disk).
 */
public class BitmapImageCache implements ImageCache {
    private static final String TAG = "BitmapImageCache";

    // Default memory cache size as a percent of device memory class
    private static final float DEFAULT_MEM_CACHE_PERCENT = 0.25f;

    private LruCache<String, Bitmap> mMemoryCache;
    
    /**
     * Don't instantiate this class directly, use
     * {@link #getInstance(FragmentManager, float)}.
     * @param memCacheSize Memory cache size in KB.
     */
    public BitmapImageCache(int memCacheSize) {
        init(memCacheSize);
    }

    /**
     * Find and return an existing BitmapCache stored in a {@link RetainFragment}, if not found a
     * new one is created using the supplied params and saved to a {@link RetainFragment}.
     *
     * @param fragmentManager The fragment manager to use when dealing with the retained fragment.
     * @param fragmentTag The tag of the retained fragment (should be unique for each memory cache
     *                    that needs to be retained).
     * @param memCacheSize Memory cache size in KB.
     */
    public static BitmapImageCache getInstance(FragmentManager fragmentManager, String fragmentTag,
            int memCacheSize) {
        BitmapImageCache bitmapImageCache = null;
        RetainFragment mRetainFragment = null;

        if (fragmentManager != null) {
            // Search for, or create an instance of the non-UI RetainFragment
            mRetainFragment = getRetainFragment(fragmentManager, fragmentTag);

            // See if we already have a BitmapCache stored in RetainFragment
            bitmapImageCache = (BitmapImageCache) mRetainFragment.getObject();
        }

        // No existing BitmapCache, create one and store it in RetainFragment
        if (bitmapImageCache == null) {
            bitmapImageCache = new BitmapImageCache(memCacheSize);
            if (mRetainFragment != null) {
                mRetainFragment.setObject(bitmapImageCache);
            }
        }
        return bitmapImageCache;
    }

    public static BitmapImageCache getInstance(FragmentManager fragmentManager, int memCacheSize) {
        return getInstance(fragmentManager, TAG, memCacheSize);
    }

    public static BitmapImageCache getInstance(FragmentManager fragmentManager, float memCachePercent) {
        return getInstance(fragmentManager, calculateMemCacheSize(memCachePercent));
    }

    public static BitmapImageCache getInstance(FragmentManager fragmentManger) {
        return getInstance(fragmentManger, DEFAULT_MEM_CACHE_PERCENT);
    }

    public static BitmapImageCache getInstance(FragmentManager fragmentManger, ImageCacheParams imageCacheParams) {
        return getInstance(fragmentManger, imageCacheParams != null ? imageCacheParams.memCacheSize : calculateMemCacheSize(DEFAULT_MEM_CACHE_PERCENT));
    }
    /**
     * Initialize the cache.
     */
    private void init(int memCacheSize) {
        // Set up memory cache
    	VolleyLog.d(TAG, "Memory cache created (size = " + memCacheSize + "KB)");
        mMemoryCache = new LruCache<String, Bitmap>(memCacheSize) {
            /**
             * Measure item size in kilobytes rather than units which is more practical
             * for a bitmap cache
             */
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                final int bitmapSize = getBitmapSize(bitmap) / 1024;
                return bitmapSize == 0 ? 1 : bitmapSize;
            }
            
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
            	super.entryRemoved(evicted, key, oldValue, newValue);
            	VolleyLog.d(TAG, "Memory cache entry removed - " + key);
            }
        };
    }

    /**
     * Adds a bitmap to both memory and disk cache.
     * @param data Unique identifier for the bitmap to store
     * @param bitmap The bitmap to store
     */
    public void addBitmapToCache(String data, Bitmap bitmap) {
        if (data == null || bitmap == null) {
            return;
        }

        synchronized (mMemoryCache) {
            // Add to memory cache
            //if (mMemoryCache.get(data) == null) {
            	VolleyLog.d(TAG, "Memory cache put - " + data);
                mMemoryCache.put(data, bitmap);
            //}
        }
    }

    /**
     * Get from memory cache.
     *
     * @param data Unique identifier for which item to get
     * @return The bitmap if found in cache, null otherwise
     */
    public Bitmap getBitmapFromMemCache(String data) {
        if (data != null) {
            synchronized (mMemoryCache) {
                final Bitmap memBitmap = mMemoryCache.get(data);
                if (memBitmap != null) {
                	VolleyLog.d(TAG, "Memory cache hit - " + data);
                    return memBitmap;
                }
            }
            VolleyLog.d(TAG, "Memory cache miss - " + data);
        }
        return null;
    }

    /**
     * Clears the memory cache.
     */
    public void clearCache() {
        if (mMemoryCache != null) {
            mMemoryCache.evictAll();
            VolleyLog.d(TAG, "Memory cache cleared");
        }
    }

    /**
     * Sets the memory cache size based on a percentage of the max available VM memory.
     * Eg. setting percent to 0.2 would set the memory cache to one fifth of the available
     * memory. Throws {@link IllegalArgumentException} if percent is < 0.05 or > .8.
     * memCacheSize is stored in kilobytes instead of bytes as this will eventually be passed
     * to construct a LruCache which takes an int in its constructor.
     *
     * This value should be chosen carefully based on a number of factors
     * Refer to the corresponding Android Training class for more discussion:
     * http://developer.android.com/training/displaying-bitmaps/
     *
     * @param percent Percent of memory class to use to size memory cache
     * @return Memory cache size in KB
     */
    public static int calculateMemCacheSize(float percent) {
        if (percent < 0.05f || percent > 0.8f) {
            throw new IllegalArgumentException("setMemCacheSizePercent - percent must be "
                    + "between 0.05 and 0.8 (inclusive)");
        }
        return Math.round(percent * Runtime.getRuntime().maxMemory() / 1024);
    }

    /**
     * Get the size in bytes of a bitmap.
     */
    @TargetApi(19)
    public static int getBitmapSize(Bitmap bitmap) {
        // From KitKat onward use getAllocationByteCount() as allocated bytes can potentially be
        // larger than bitmap byte count.
        if (Utils.hasKitKat()) {
            return bitmap.getAllocationByteCount();
        }

        if (Utils.hasHoneycombMR1()) {
            return bitmap.getByteCount();
        }

        // Pre HC-MR1
        return bitmap.getRowBytes() * bitmap.getHeight();
    }
    
    /**
     * Get the size in bytes of a bitmap in a BitmapDrawable. Note that from Android 4.4 (KitKat)
     * onward this returns the allocated memory size of the bitmap which can be larger than the
     * actual bitmap data byte count (in the case it was re-used).
     *
     * @param value
     * @return size in bytes
     */
    public static int getBitmapSize(BitmapDrawable value) {
    	Bitmap bitmap = value.getBitmap();
    	return getBitmapSize(bitmap);
    }
    
    /**
     * Check how much usable space is available at a given path.
     *
     * @param path The path to check
     * @return The space available in bytes
     */
    @SuppressWarnings("deprecation")
	@TargetApi(9)
    public static long getUsableSpace(File path) {
        if (Utils.hasGingerbread()) {
            return path.getUsableSpace();
        }
        final StatFs stats = new StatFs(path.getPath());
        return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
    }

    /**
     * Locate an existing instance of this Fragment or if not found, create and
     * add it using FragmentManager.
     *
     * @param fm The FragmentManager manager to use.
     * @param fragmentTag The tag of the retained fragment (should be unique for each memory
     *                    cache that needs to be retained).
     * @return The existing instance of the Fragment or the new instance if just
     *         created.
     */
    private static RetainFragment getRetainFragment(FragmentManager fm, String fragmentTag) {
        // Check to see if we have retained the worker fragment.
        RetainFragment mRetainFragment = (RetainFragment) fm.findFragmentByTag(fragmentTag);

        // If not retained (or first time running), we need to create and add it.
        if (mRetainFragment == null) {
            mRetainFragment = new RetainFragment();
            fm.beginTransaction().add(mRetainFragment, fragmentTag).commitAllowingStateLoss();
        }

        return mRetainFragment;
    }

    @Override
    public Bitmap getBitmap(String key) {
        return getBitmapFromMemCache(key);
    }

    @Override
    public void putBitmap(String key, Bitmap bitmap) {
        addBitmapToCache(key, bitmap);
    }

	@Override
	public void invalidateBitmap(String url) {
        if (url == null) {
            return;
        }

        synchronized (mMemoryCache) {
            // Add to memory cache
            //if (mMemoryCache.get(data) == null) {
            	VolleyLog.d(TAG, "Memory cache remove - " + url);
                mMemoryCache.remove(url);
            //}
        }
	}

	@Override
	public void clear() {
		clearCache();
	}
	
    /**
     * A simple non-UI Fragment that stores a single Object and is retained over configuration
     * changes. It will be used to retain the BitmapCache object.
     */
    public static class RetainFragment extends Fragment {
        private Object mObject;

        /**
         * Empty constructor as per the Fragment documentation
         */
        public RetainFragment() {}

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Make sure this Fragment is retained over a configuration change
            setRetainInstance(true);
        }

        /**
         * Store a single object in this Fragment.
         *
         * @param object The object to store
         */
        public void setObject(Object object) {
            mObject = object;
        }

        /**
         * Get the stored object.
         *
         * @return The stored object
         */
        public Object getObject() {
            return mObject;
        }
    }
}