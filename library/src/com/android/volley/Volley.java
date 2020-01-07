package com.android.volley;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.http.AndroidHttpClient;
import android.text.TextUtils;
import android.widget.ImageView;

import com.android.volley.cache.DiskBasedCache;
import com.android.volley.cache.DiskLruBasedCache;
import com.android.volley.cache.SimpleImageLoader;
import com.android.volley.misc.NetUtils;
import com.android.volley.misc.Utils;
import com.android.volley.toolbox.BasicNetwork;
import com.android.volley.toolbox.HttpClientStack;
import com.android.volley.toolbox.HttpStack;
import com.android.volley.toolbox.HurlStack;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.VolleyTickle;

import java.io.File;
import java.util.ArrayList;

import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;

/**
 * Created by HaKr on 13/05/17.
 */

public class Volley {
    public static final String TAG = "Volley";

    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "volley";
    public static final int IMAGE_SIZE_BIG = 100;
    public static final int IMAGE_SIZE = 50;
    public static final String IMAGE_CACHE_DIR = "thumbs";

    private static Volley mVolleyHelper;

    private final Context mContext;
    protected RequestTickle mRequestTickle;
    protected RequestQueue mRequestQueue;
    protected SimpleImageLoader mImageLoader;
    private String mUrl;
    private int mPlaceHolder;
    private boolean mCrossfade;
    private String mCacheDir;


    public Volley(Context context){
        mContext = context;
    }

    public static Volley with(Context context) {
        if(null == mVolleyHelper) {
            mVolleyHelper = new Volley(context);
        }
        return mVolleyHelper;
    }

    public Volley load(String string) {
        mUrl = string;
        return this;
    }

    public Volley placeholder(int resource) {
        mPlaceHolder = resource;
        return this;
    }

    public Volley cachedir(String name) {
        mCacheDir = name;
        return this;
    }

    public Volley crossfade() {
        mCrossfade = true;
        return this;
    }

    public int getPlaceHolder() {
        return mPlaceHolder;
    }

    public String getUrl() {
        return mUrl;
    }

    public String getCacheDir() {
        return mCacheDir;
    }

    public boolean isCrossfade() {
        return mCrossfade;
    }

    public Context getContext() {
        return mContext;
    }

    public ImageLoader.ImageContainer into(ImageView view) {
        return getImageLoader().get(getUrl(), view);
    }

    public ImageLoader.ImageContainer load(ImageView view, Bitmap bitmap) {
        return getImageLoader().set(mUrl, view, bitmap);
    }

    public SimpleImageLoader getImageLoader(){
        if(null == mImageLoader){
            DiskLruBasedCache.ImageCacheParams cacheParams = new DiskLruBasedCache.ImageCacheParams(mContext,
                    TextUtils.isEmpty(getCacheDir()) ? IMAGE_CACHE_DIR :  getCacheDir());
            cacheParams.setMemCacheSizePercent(0.5f);

            ArrayList<Drawable> drawables = new ArrayList<>();
            drawables.add(ContextCompat.getDrawable(mContext,
                    mPlaceHolder == 0 ? R.drawable.empty_photo : mPlaceHolder));

            mImageLoader = new SimpleImageLoader(mContext, cacheParams);
            mImageLoader.setDefaultDrawables(drawables);
            mImageLoader.setMaxImageSize(hasMoreHeap() ? IMAGE_SIZE_BIG: IMAGE_SIZE);
            mImageLoader.setFadeInImage(mCrossfade);
            mImageLoader.setContetResolver(mContext.getContentResolver());
        }

        return mImageLoader;
    }
    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @param stack An {@link HttpStack} to use for the network, or null for default.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context, HttpStack stack) {
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        if (stack == null) {
            stack = Utils.hasHoneycomb() ?
                    new HurlStack() :
                    new HttpClientStack(AndroidHttpClient.newInstance(
                            NetUtils.getUserAgent(context)));
        }

        Network network = new BasicNetwork(stack);

        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
        queue.start();

        return queue;
    }

    public static RequestQueue newRequestQueue(Context context, HttpStack stack, String path) {
        File cacheDir = new File(context.getCacheDir(), path);
        if (stack == null) {
            stack = Utils.hasHoneycomb() ?
                    new HurlStack() :
                    new HttpClientStack(AndroidHttpClient.newInstance(
                            NetUtils.getUserAgent(context)));
        }

        Network network = new BasicNetwork(stack);

        RequestQueue queue = new RequestQueue(new DiskBasedCache(cacheDir), network);
        queue.start();

        return queue;
    }

    /**
     * Creates a default instance of the worker pool and calls {@link RequestQueue#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @return A started {@link RequestQueue} instance.
     */
    public static RequestQueue newRequestQueue(Context context) {
        return newRequestQueue(context, (HttpStack) null);
    }

    public static RequestQueue newRequestQueue(Context context, String path) {
        return newRequestQueue(context, null, path);
    }

    public RequestTickle getRequestTickle() {
        if (mRequestTickle == null) {
            mRequestTickle = VolleyTickle.newRequestTickle(mContext);
        }

        return mRequestTickle;
    }

    public RequestQueue getRequestQueue() {
        // lazy initialize the request queue, the queue instance will be
        // created when it is accessed for the first time
        if (mRequestQueue == null) {
            mRequestQueue = newRequestQueue(mContext);
        }

        return mRequestQueue;
    }

    public <T> void addToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        if(BuildConfig.DEBUG) {
            VolleyLog.d("Adding request to queue: %s", req.getUrl());
        }
        getRequestQueue().add(req);
    }

    public <T> void updateToRequestQueue(Request<T> req, String tag) {
        // set the default tag if tag is empty
        req.setTag(TextUtils.isEmpty(tag) ? TAG : tag);
        if(BuildConfig.DEBUG) {
            VolleyLog.d("Adding request to queue: %s", req.getUrl());
        }
        getRequestQueue().cancelAll(tag);
        getRequestQueue().add(req);
    }

    public <T> void addToRequestQueue(Request<T> req) {
        // set the default tag if tag is empty
        req.setTag(TAG);
        getRequestQueue().add(req);
    }

    public void cancelPendingRequests(Object tag) {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(tag);
        }
        mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return false;
            }
        });
    }

    public void cancelPendingRequests() {
        if (mRequestQueue != null) {
            mRequestQueue.cancelAll(new RequestQueue.RequestFilter() {
                @Override
                public boolean apply(Request<?> request) {
                    return true;
                }
            });
        }
    }

    public NetworkResponse startTickle(Request request){
        getRequestTickle().add(request);
        return getRequestTickle().start();
    }


    public static boolean hasMoreHeap(){
        return Runtime.getRuntime().maxMemory() > 20971520;
    }

    public static void putCache(Cache cache, String url, String data){
        long now = System.currentTimeMillis();
        Cache.Entry entry = new Cache.Entry();
        entry.serverDate = now;
        entry.softTtl = 0;
        entry.ttl= 0;
        entry.data = data.getBytes();
        ArrayMap<String, String> headers = new ArrayMap<>();
        headers.put("Content-Type", "application/json; charset=utf-8");
        entry.responseHeaders = headers;
        cache.put(url, entry);
    }
}
