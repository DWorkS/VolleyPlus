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

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.net.http.AndroidHttpClient;
import android.os.Build;
import android.support.v4.app.FragmentActivity;
import android.widget.ImageView;

import com.android.volley.Cache;
import com.android.volley.Network;
import com.android.volley.RequestQueue;
import com.android.volley.cache.DiskLruBasedCache.ImageCacheParams;
import com.android.volley.error.VolleyError;
import com.android.volley.misc.NetUtils;
import com.android.volley.misc.Utils;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageCache;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.ui.PhotoView;

import java.util.ArrayList;

/**
 * A class that wraps up remote image loading requests using the Volley library combined with a
 * memory cache. An single instance of this class should be created once when your Activity or
 * Fragment is created, then use {@link #get(String, android.widget.ImageView)} or one of
 * the variations to queue the image to be fetched and loaded from the network. Loading images
 * in a {@link android.widget.ListView} or {@link android.widget.GridView} is also supported but
 * you must store the {@link com.android.volley.Request} in your ViewHolder type class and pass it
 * into loadImage to ensure the request is canceled as views are recycled.
 */
public class SimpleImageLoader extends ImageLoader {
    private static final ColorDrawable transparentDrawable = new ColorDrawable(
            android.R.color.transparent);
    private static final int HALF_FADE_IN_TIME = Utils.ANIMATION_FADE_IN_TIME / 2;
    protected static final String CACHE_DIR = "images";

    private ArrayList<Drawable> mPlaceHolderDrawables;
    private boolean mFadeInImage = true;
    private int mMaxImageHeight = 0;
    private int mMaxImageWidth = 0;

    /**
     * {@inheritDoc}
     */
    public SimpleImageLoader(RequestQueue queue) {
        this(queue, BitmapCache.getInstance(null));
    }

    /**
     * {@inheritDoc}
     */
    public SimpleImageLoader(RequestQueue queue, ImageCache imageCache) {
        this(queue, imageCache, null);
    }

    /**
     * {@inheritDoc}
     */
    public SimpleImageLoader(RequestQueue queue, ImageCache imageCache, Resources resources) {
        super(queue, imageCache, resources);
    }

    /**
     * Creates an ImageLoader with Bitmap memory cache.
     * @param activity
     */
    public SimpleImageLoader(FragmentActivity activity) {
        super(newRequestQueue(activity, null),
                BitmapImageCache.getInstance(activity.getSupportFragmentManager()),
                activity.getResources());
    }
    
    /**
     * Creates an ImageLoader with Bitmap memory cache.
     * @param activity
     * @param imageCacheParams
     */
    public SimpleImageLoader(FragmentActivity activity, ImageCacheParams imageCacheParams) {
        super(newRequestQueue(activity, imageCacheParams),
                BitmapImageCache.getInstance(activity.getSupportFragmentManager(), imageCacheParams),
                activity.getResources());
    }

    /**
     * Creates an ImageLoader with Bitmap memory cache.
     * @param context
     */
    public SimpleImageLoader(Context context) {
        super(newRequestQueue(context, null), BitmapImageCache.getInstance(null),
                context.getResources());
    }

    /**
     * Creates an ImageLoader with Bitmap memory cache.
     * @param context
     * @param imageCacheParams
     */
    public SimpleImageLoader(Context context, ImageCacheParams imageCacheParams) {
        super(newRequestQueue(context, imageCacheParams),
                BitmapImageCache.getInstance(null, imageCacheParams),
                context.getResources());
    }

    /**
     * Starts processing requests on the {@link RequestQueue}.
     */
    public void startProcessingQueue() {
        getRequestQueue().start();
    }

    /**
     * Stops processing requests on the {@link RequestQueue}.
     */
    public void stopProcessingQueue() {
        getRequestQueue().stop();
    }
    
    /**
     * Clears {@link Cache}.
     */
    public void clearCache() {
    	getCache().clear();
	}
    
    /**
     * Flushed {@link Cache} and clears {@link com.android.volley.toolbox.ImageCache}.
     */
    public void flushCache() {
    	getImageCache().clear();
    	getCache().flush();
	}
    
    /**
     * Closes {@link Cache}.
     */
    public void closeCache() {
    	getCache().close();
	}
    
    public boolean isCached(String key) {
    	return getCache().get(key) != null;
	}

    @Deprecated
    public void invalidate(String key) {
        final String cacheKey = getCacheKey(key, mMaxImageWidth, mMaxImageHeight);
    	getImageCache().invalidateBitmap(cacheKey);
    	getCache().invalidate(key, true);
	}

    public void invalidate(String key, ImageView view) {
        final String cacheKey = getCacheKey(key, mMaxImageWidth, mMaxImageHeight, view.getScaleType());
        getImageCache().invalidateBitmap(cacheKey);
        getCache().invalidate(key, true);
        //default cache
        invalidate(key);
    }

	public SimpleImageLoader setFadeInImage(boolean fadeInImage) {
        mFadeInImage = fadeInImage;
        return this;
    }

    public SimpleImageLoader setMaxImageSize(int maxImageWidth, int maxImageHeight) {
        mMaxImageWidth = maxImageWidth;
        mMaxImageHeight = maxImageHeight;
        return this;
    }

    /**
     * A default placeholder image while the image is being fetched and loaded.
     * @param defaultPlaceHolderResId
     * @return
     */
    public  SimpleImageLoader setDefaultDrawable(int defaultPlaceHolderResId){
        mPlaceHolderDrawables = new ArrayList<Drawable>(1);
        mPlaceHolderDrawables.add(defaultPlaceHolderResId == -1 ?
                null : getResources().getDrawable(defaultPlaceHolderResId));
        return this;
    }

    /**
     * A default placeholder image while the image is being fetched and loaded.
     * @param placeHolderDrawables
     * @return
     */
    public  SimpleImageLoader setDefaultDrawables(ArrayList<Drawable> placeHolderDrawables){
        mPlaceHolderDrawables = placeHolderDrawables;
        return this;
    }

    public SimpleImageLoader setMaxImageSize(int maxImageSize) {
        return setMaxImageSize(maxImageSize, maxImageSize);
    }
    
    public int getMaxImageWidth() {
        return mMaxImageWidth;
    }

    public int getMaxImageHeight() {
        return mMaxImageHeight;
    }
    
    //Get 
    public ImageContainer get(String requestUrl, ImageView imageView) {
        return get(requestUrl, imageView, 0);
    }
    
    public ImageContainer get(String requestUrl, ImageView imageView, int maxImageWidth, int maxImageHeight) {
        return get(requestUrl, imageView, mPlaceHolderDrawables != null ? mPlaceHolderDrawables.get(0) : null, maxImageWidth, maxImageHeight);
    }

    public ImageContainer get(String requestUrl, ImageView imageView, int placeHolderIndex) {
        return get(requestUrl, imageView, 
        		mPlaceHolderDrawables != null ? mPlaceHolderDrawables.get(placeHolderIndex) : null,
                mMaxImageWidth, mMaxImageHeight);
    }

    public ImageContainer get(String requestUrl, ImageView imageView, Drawable placeHolder) {
        return get(requestUrl, imageView, placeHolder, mMaxImageWidth, mMaxImageHeight);
    }

    public ImageContainer get(String requestUrl, ImageView imageView, Drawable placeHolder,
            int maxWidth, int maxHeight) {

        // Find any old image load request pending on this ImageView (in case this view was
        // recycled)
        ImageContainer imageContainer = imageView.getTag() != null &&
                imageView.getTag() instanceof ImageContainer ?
                (ImageContainer) imageView.getTag() : null;

        // Find image url from prior request
        String recycledImageUrl = imageContainer != null ? imageContainer.getRequestUrl() : null;

        // If the new requestUrl is null or the new requestUrl is different to the previous
        // recycled requestUrl
        if (requestUrl == null || !requestUrl.equals(recycledImageUrl)) {
            if (imageContainer != null) {
                // Cancel previous image request
                imageContainer.cancelRequest();
                imageView.setTag(null);
            }
            if (requestUrl != null) {
                // Queue new request to fetch image
                imageContainer = get(requestUrl,
                        getImageListener(getResources(), imageView, placeHolder, mFadeInImage),
                        maxWidth, maxHeight, imageView.getScaleType());
                // Store request in ImageView tag
                imageView.setTag(imageContainer);
            } else {
            	if(!(imageView instanceof PhotoView)){
            		imageView.setImageDrawable(placeHolder);
            	}
                imageView.setTag(null);
            }
        }

        return imageContainer;
    }
    
    //Set
    public ImageContainer set(String requestUrl, ImageView imageView, Bitmap bitmap) {
        return set(requestUrl, imageView, 0, bitmap);
    }

    public ImageContainer set(String requestUrl, ImageView imageView, int placeHolderIndex, Bitmap bitmap) {
        return set(requestUrl, imageView, 
        		mPlaceHolderDrawables != null ? mPlaceHolderDrawables.get(placeHolderIndex) : null,
                mMaxImageWidth, mMaxImageHeight, bitmap);
    }

    public ImageContainer set(String requestUrl, ImageView imageView, Drawable placeHolder, Bitmap bitmap) {
        return set(requestUrl, imageView, placeHolder, mMaxImageWidth, mMaxImageHeight, bitmap);
    }

    public ImageContainer set(String requestUrl, ImageView imageView, Drawable placeHolder,
            int maxWidth, int maxHeight, Bitmap bitmap) {

        // Find any old image load request pending on this ImageView (in case this view was
        // recycled)
        ImageContainer imageContainer = imageView.getTag() != null &&
                imageView.getTag() instanceof ImageContainer ?
                (ImageContainer) imageView.getTag() : null;

        // Find image url from prior request
        //String recycledImageUrl = imageContainer != null ? imageContainer.getRequestUrl() : null;

        if (imageContainer != null) {
            // Cancel previous image request
            imageContainer.cancelRequest();
            imageView.setTag(null);
        }
        if (requestUrl != null) {
            // Queue new request to fetch image
            imageContainer = set(requestUrl,
                    getImageListener(getResources(), imageView, placeHolder, mFadeInImage),
                    maxWidth, maxHeight, imageView.getScaleType(), bitmap);
            // Store request in ImageView tag
            imageView.setTag(imageContainer);
        } else {
        	if(!(imageView instanceof PhotoView)){
        		imageView.setImageDrawable(placeHolder);
        	}
            imageView.setTag(null);
        }

        return imageContainer;
    }
    

    public static ImageListener getImageListener(final Resources resources,
            final ImageView imageView, final Drawable placeHolder, final boolean fadeInImage) {
        return new ImageListener() {
            @Override
            public void onResponse(ImageContainer response, boolean isImmediate) {
                imageView.setTag(null);
                if (response.getBitmap() != null) {
                	
                	if(imageView instanceof PhotoView){
                        setPhotoImageBitmap((PhotoView) imageView, response.getBitmap(), resources,
                                fadeInImage && !isImmediate);		
                	}
                	else{
                        setImageBitmap(imageView, response.getBitmap(), resources,
                                fadeInImage && !isImmediate);	
                	}
                } else {
                	if(!(imageView instanceof PhotoView)){
                		imageView.setImageDrawable(placeHolder);
                	}
                }
            }

            @Override
            public void onErrorResponse(VolleyError volleyError) {
            }
        };
    }

    private static RequestQueue newRequestQueue(Context context, ImageCacheParams imageCacheParams) {

        Network network = new BasicNetwork(
                Utils.hasHoneycomb() ?
                        new HurlStack() :
                        new HttpClientStack(AndroidHttpClient.newInstance(
                                NetUtils.getUserAgent(context))));

        Cache cache; 
        if(null != imageCacheParams){
            cache = new DiskLruBasedCache(imageCacheParams);	
        }
        else{
        	cache = new DiskLruBasedCache(Utils.getDiskCacheDir(context, CACHE_DIR));
        }
        RequestQueue queue = new RequestQueue(cache, network);
        queue.start();
        return queue;
    }

    /**
     * Sets a {@link android.graphics.Bitmap} to an {@link android.widget.ImageView} using a
     * fade-in animation. If there is a {@link android.graphics.drawable.Drawable} already set on
     * the ImageView then use that as the image to fade from. Otherwise fade in from a transparent
     * Drawable.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private static void setImageBitmap(final ImageView imageView, final Bitmap bitmap,
            Resources resources, boolean fadeIn) {

        // If we're fading in and on HC MR1+
        if (fadeIn && Utils.hasHoneycombMR1()) {
            // Use ViewPropertyAnimator to run a simple fade in + fade out animation to update the
            // ImageView
            imageView.animate()
                    .scaleY(0.95f)
                    .scaleX(0.95f)
                    .alpha(0f)
                    .setDuration(imageView.getDrawable() == null ? 0 : HALF_FADE_IN_TIME)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            imageView.setImageBitmap(bitmap);
                            imageView.animate()
                                    .alpha(1f)
                                    .scaleY(1f)
                                    .scaleX(1f)
                                    .setDuration(HALF_FADE_IN_TIME)
                                    .setListener(null);
                        }
                    });
        } else if (fadeIn) {
            // Otherwise use a TransitionDrawable to fade in
            Drawable initialDrawable;
            if (imageView.getDrawable() != null) {
                initialDrawable = imageView.getDrawable();
            } else {
                initialDrawable = transparentDrawable;
            }
            BitmapDrawable bitmapDrawable = new BitmapDrawable(resources, bitmap);
            // Use TransitionDrawable to fade in
            final TransitionDrawable td =
                    new TransitionDrawable(new Drawable[] {
                            initialDrawable,
                            bitmapDrawable
                    });
            imageView.setImageDrawable(td);
            td.startTransition(Utils.ANIMATION_FADE_IN_TIME);
        } else {
            // No fade in, just set bitmap directly
            imageView.setImageBitmap(bitmap);
        }
    }
    
    /**
     * Sets a {@link android.graphics.Bitmap} to an {@link android.widget.ImageView} using a
     * fade-in animation. If there is a {@link android.graphics.drawable.Drawable} already set on
     * the ImageView then use that as the image to fade from. Otherwise fade in from a transparent
     * Drawable.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    private static void setPhotoImageBitmap(final PhotoView imageView, final Bitmap bitmap,
            Resources resources, boolean fadeIn) {
        // If we're fading in and on HC MR1+
        if (fadeIn && Utils.hasHoneycombMR1()) {
            // Use ViewPropertyAnimator to run a simple fade in + fade out animation to update the
            // ImageView
            imageView.animate()
                    .scaleY(0.95f)
                    .scaleX(0.95f)
                    .alpha(0f)
                    .setDuration(imageView.getDrawable() == null ? 0 : HALF_FADE_IN_TIME)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            imageView.bindPhoto(bitmap);
                            imageView.animate()
                                    .alpha(1f)
                                    .scaleY(1f)
                                    .scaleX(1f)
                                    .setDuration(HALF_FADE_IN_TIME)
                                    .setListener(null);
                        }
                    });
        } else if (fadeIn) {
            // Otherwise use a TransitionDrawable to fade in
            Drawable initialDrawable;
            if (imageView.getDrawable() != null) {
                initialDrawable = imageView.getDrawable();
            } else {
                initialDrawable = transparentDrawable;
            }
            BitmapDrawable bitmapDrawable = new BitmapDrawable(resources, bitmap);
            // Use TransitionDrawable to fade in
            final TransitionDrawable td =
                    new TransitionDrawable(new Drawable[] {
                            initialDrawable,
                            bitmapDrawable
                    });
            imageView.bindDrawable(td);
            td.startTransition(Utils.ANIMATION_FADE_IN_TIME);
        } else {
            // No fade in, just set bitmap directly
            imageView.bindPhoto(bitmap);
        }
    }

    /**
     * Interface an activity can implement to provide an ImageLoader to its children fragments.
     */
    public interface ImageLoaderProvider {
        public SimpleImageLoader getImageLoaderInstance();
    }
}