package br.com.ilhasoft.volley;

import android.os.Handler;

import com.android.volley.Response;

/**
 * Created by dev on 24/07/2014.
 */
public class ObjectRequest<T> extends Request {

    Response.Listener<T> listener;
    Converter<T> converter;

    ObjectRequest(int method, String url, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(method, url, null, errorListener);
        this.listener = listener;
        throw new UnsupportedOperationException("Not implemented yet.");
    }

    @Override
    protected void deliverResponse(final String response) {
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                listener.onResponse(converter.fromJson(response));
            }
        });
    }

    public static interface Converter<T> {
        String toJson(T object);

        T fromJson(String json);
    }
}
