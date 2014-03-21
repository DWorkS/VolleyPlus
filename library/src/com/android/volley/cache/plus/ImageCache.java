package com.android.volley.cache.plus;

import android.graphics.drawable.BitmapDrawable;

/**
 * Simple cache adapter interface. If provided to the ImageLoader, it
 * will be used as an L1 cache before dispatch to Volley. Implementations
 * must not block. Implementation with an LruCache is recommended.
 */
public interface ImageCache {
    public BitmapDrawable getBitmap(String url);
    public void putBitmap(String url, BitmapDrawable bitmap);
    public void invalidateBitmap(String url);
    public void clear();
}