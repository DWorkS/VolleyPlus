package com.android.volley.cache;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Build;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageCache;
import com.android.volley.toolbox.ImageLoader;


/**
 * Basic implementation of a Bitmap LRU cache to use
 * with {@link ImageLoader#ImageLoader(com.android.volley.RequestQueue)}
 *
 * Added by Vinay S Shenoy on 19/5/13
 */
public class LruImageCache implements ImageCache {

    private LruCache<String, Bitmap> mLruCache;

    public LruImageCache() {

        final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
        final int cacheSize = maxMemory / 8;

        mLruCache = new LruCache<String, Bitmap>(cacheSize) {
            @SuppressLint("NewApi")
			@Override
            protected int sizeOf(String key, Bitmap bitmap) {
                // The cache size will be measured in kilobytes rather than
                // number of items.
            	
            	if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1) {
            		return bitmap.getByteCount() / 1024;
            	}
            	
            	else {
            		return (bitmap.getRowBytes() * bitmap.getHeight()) / 1024;
            	}
            }
        };

    }

    @Override
    public Bitmap getBitmap(String key) {
        return mLruCache.get(key);
    }

    @Override
    public void putBitmap(String key, Bitmap bitmap) {
        //if(mLruCache.get(key) == null) {
            mLruCache.put(key, bitmap);
        //}
    }

	@Override
	public void invalidateBitmap(String url) {
		mLruCache.remove(url);
	}

	@Override
	public void clear() {
		mLruCache.evictAll();
	}
}