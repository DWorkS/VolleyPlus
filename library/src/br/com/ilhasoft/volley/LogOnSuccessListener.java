package br.com.ilhasoft.volley;

import com.android.volley.Response;
import com.android.volley.VolleyLog;

/**
 * Created by dev on 23/07/2014.
 */
public class LogOnSuccessListener<T> implements Response.Listener<T> {
    @Override
    public void onResponse(T response) {
        VolleyLog.d("onResponse \"%s\"", response);
    }
}
