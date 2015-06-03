package br.com.ilhasoft.volley;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.error.AuthFailureError;
import com.android.volley.request.StringRequest;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dev on 23/07/2014.
 */
class Request extends StringRequest {

    private static final int DAY_IN_MILIS = 86400 * 1000;

    HashMap<String, String> headers = new HashMap<String, String>();
    HashMap<String, String> params = new HashMap<String, String>();

    String body = "";
    String cachekey;
    Integer fakeCacheOverride = null;
    String redirectUrl;

    public Request(int method, String url, Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
        fakeCacheOverride = DAY_IN_MILIS;
    }

    /**
     * @param name
     * @param value
     * @return the value of any previous mapping with the specified key or
     *         {@code null} if there was no such mapping.
     */
    public void putParam(String name, String value) {
        params.put(name, value);
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        HashMap<String, String> params = new HashMap<String, String>();
        params.putAll(super.getParams());
        params.putAll(this.params);
        return params;
    }

    /**
     * @param name
     * @param value
     * @return the value of any previous mapping with the specified key or
     *         {@code null} if there was no such mapping.
     */
    public String putHeader(String name, String value) {
        return headers.put(name, value);
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        HashMap<String, String> headers = new HashMap<String, String>();
        headers.putAll(super.getHeaders());
        headers.putAll(this.headers);
        return headers;
    }

    void setContentType(String contentType) {
        headers.put("Content-Type", contentType);
    }

    private static final String TAG = "Request";

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
//        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
//        return Response.success(parsed, HttpHeaderParser.parseIgnoreCacheHeaders(response));
        return Response.success(parsed, fakeCache(response));
    }

    private Cache.Entry fakeCache(NetworkResponse response) {
        Cache.Entry cache = HttpHeaderParser.parseCacheHeaders(response);
        if (invalidCacheInfoOnResponse(cache)) {
            int fakeCacheTime;
            if (fakeCacheOverride == null)
                fakeCacheTime = DAY_IN_MILIS;
            else
                fakeCacheTime = fakeCacheOverride;

            cache.softTtl = System.currentTimeMillis() + fakeCacheTime; // cache for a day
            cache.ttl = cache.softTtl;
        }
        return cache;
    }

    private static boolean invalidCacheInfoOnResponse(Cache.Entry mFakeCache) {
        return (mFakeCache.etag == null) || mFakeCache.isExpired() || mFakeCache.refreshNeeded();
    }

    @Override
    public byte[] getBody() throws AuthFailureError {
        return ((body == null) ? "" : body).getBytes();
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setRedirectUrl(String redirectUrl) {
        this.redirectUrl = redirectUrl;
    }

    @Override
    public String getCacheKey() {
        if (cachekey != null)
            return cachekey;
        return super.getCacheKey();
    }

    public void setFakeCacheOverride(int fakeCacheOverride) {
        this.fakeCacheOverride = fakeCacheOverride;
    }
}
