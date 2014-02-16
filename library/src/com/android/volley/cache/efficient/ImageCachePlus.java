package com.android.volley.cache.efficient;

import android.graphics.drawable.BitmapDrawable;

/**
 * Simple cache adapter interface. If provided to the ImageLoader, it
 * will be used as an L1 cache before dispatch to Volley. Implementations
 * must not block. Implementation with an LruCache is recommended.
 */
public interface ImageCachePlus {
    public BitmapDrawable getBitmap(String data);
    public void putBitmap(String data, BitmapDrawable bitmap);
}