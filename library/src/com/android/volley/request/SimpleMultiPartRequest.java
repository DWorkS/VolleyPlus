package com.android.volley.request;

import java.io.UnsupportedEncodingException;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;

/**
 * A Simple request for making a Multi Part request whose response is retrieve as String
 * 
 * @param <T> Response expected
 */
public class SimpleMultiPartRequest extends MultiPartRequest<String> {

	private Listener<String> mListener;

	public SimpleMultiPartRequest(String url, Listener<String> listener, ErrorListener errorlistener) {
		super(Method.POST, url, listener, errorlistener);
		mListener = listener;
	}

	
	public SimpleMultiPartRequest(int method, String url, Listener<String> listener, ErrorListener errorlistener) {
		super(method, url, listener, errorlistener);
		mListener = listener;
	}


    @Override
    protected void deliverResponse(String response) {
    	if(null != mListener){
    		mListener.onResponse(response);
    	}
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }
}