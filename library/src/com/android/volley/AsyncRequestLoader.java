package com.android.volley;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

import com.android.volley.error.VolleyError;
import com.android.volley.request.GsonRequest;
import com.android.volley.toolbox.VolleyTickle;
import com.google.gson.Gson;

import org.apache.http.HttpStatus;


/**
 * Created by HaKr on 12/08/15.
 */
public class AsyncRequestLoader<T> extends AsyncTaskLoader<T> {

    private GsonRequest<T> request;
    private VolleyError error;
    private T data;

    public AsyncRequestLoader(Context context, GsonRequest<T> request) {
        super(context);
        this.request = request;
    }

    @Override
    public T loadInBackground() {

        RequestTickle requestTickle = VolleyTickle.newRequestTickle(getContext());
        requestTickle.add(request);
        NetworkResponse networkResponse = requestTickle.start();

        if (networkResponse.statusCode >= HttpStatus.SC_OK
                && networkResponse.statusCode < 300) {
            data = new Gson().fromJson(VolleyTickle.parseResponse(networkResponse), request.getClazz());
        }
        error = new VolleyError(networkResponse);

        return data;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();

        if (data != null) {
            deliverResult(data);
        }

        if (data == null || takeContentChanged()) {
            forceLoad();
        }
    }

    @Override
    public void deliverResult(T data) {
        if (isReset()) {
            if (data != null) {
                releaseResources(data);
                return;
            }
        }

        T oldData = this.data;
        this.data = data;
        if (isStarted()) {
            super.deliverResult(data);
        }

        // Invalidate the old data as we don't need it any more.
        if (oldData != null && oldData != data) {
            releaseResources(oldData);
        }
    }

    @Override
    protected void onStopLoading() {
        cancelLoad();
    }

    @Override
    protected void onReset() {
        // Ensure the loader is stopped.
        onStopLoading();

        if (data != null) {
            releaseResources(data);
            data = null;
        }
    }

    @Override
    public void onCanceled(T data) {
        super.onCanceled(data);
        releaseResources(data);
        request.cancel();
    }

    private void releaseResources(T data) {

    }

    public VolleyError getError() {
        return error;
    }
}