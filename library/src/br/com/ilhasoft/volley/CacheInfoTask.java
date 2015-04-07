package br.com.ilhasoft.volley;

import android.os.AsyncTask;

import com.android.volley.Cache;

/**
 * Created by johndalton on 24/01/15.
 */
public class CacheInfoTask extends AsyncTask<String, Void, Cache.Entry> {

    private Cache cache;
    private RequestBuilder.OnCacheInfoResponseListener onCacheInfoResponseListener;

    public CacheInfoTask(Cache cache, RequestBuilder.OnCacheInfoResponseListener onCacheInfoResponseListener) {
        this.cache = cache;
        this.onCacheInfoResponseListener = onCacheInfoResponseListener;
    }

    @Override
    protected Cache.Entry doInBackground(String... params) {
        if(params == null || params.length == 0) return null;

        String cacheKey = params[0];
        return cache.get(cacheKey);
    }

    @Override
    protected void onPostExecute(Cache.Entry entry) {
        super.onPostExecute(entry);
        if(onCacheInfoResponseListener != null) {
            onCacheInfoResponseListener.onCacheInfoResponse(entry);
        }
    }
}
