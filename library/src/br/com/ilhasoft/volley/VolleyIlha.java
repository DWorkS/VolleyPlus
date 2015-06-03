package br.com.ilhasoft.volley;

import android.content.Context;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;

/**
 * Created by johndalton on 02/12/14.
 */
public class VolleyIlha {

    private static RequestQueue requestQueue;

    public static void initialize(Context context) {
        requestQueue = Volley.newRequestQueue(context);
    }

    public static RequestBuilder getRequestBuilder(String url) {
        checkAndThrowException();
        return new RequestBuilder(requestQueue, url);
    }

    private static void checkAndThrowException() {
        if(requestQueue == null) {
            throw new IllegalStateException("Before call initialize method.");
        }
    }
}
