package com.android.volley.toolbox;

import android.graphics.Bitmap;

/**
 * Simple cache adapter interface. If provided to the ImageLoader, it
 * will be used as an L1 cache before dispatch to Volley. Implementations
 * must not block. Implementation with an LruCache is recommended.
 */
public interface ImageCache {
    public Bitmap getBitmap(String url);
    public void putBitmap(String url, Bitmap bitmap);
    public void invalidateBitmap(String url);
    public void clear();
}