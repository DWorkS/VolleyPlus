package com.android.volley;


import android.content.Context;

import com.android.volley.error.VolleyError;
import com.android.volley.request.GsonRequest;
import com.android.volley.toolbox.VolleyTickle;

import java.util.Map;

import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;

public class RxRequest<T> implements SingleOnSubscribe<T> {

    private RequestBuilder requestBuilder;

    private RxRequest(RequestBuilder requestBuilder) {
        this.requestBuilder = requestBuilder;
    }

    public static <T> RequestBuilder newRequest(Context context) {
        return new RequestBuilder<T>(context);
    }

    public static class RequestBuilder<T> {

        private final Context context;
        private String url;
        private Map<String, String> header;
        private Class type;
        private int method;
        private Map<String, String> param;

        private RequestBuilder(Context context) {
            this.context = context;
        }

        public RequestBuilder setRequestType(Class type) {
            this.type = type;
            return this;
        }

        public RequestBuilder setRequestMethod(int method) {
            this.method = method;
            return this;
        }

        public RequestBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        public RequestBuilder setHeader(Map<String, String> header) {
            this.header = header;
            return this;
        }

        public RequestBuilder setParam(Map<String, String> param) {
            this.param = param;
            return this;
        }

        public RxRequest<T> build() {
            return new RxRequest<>(this);
        }
    }

    @Override
    public void subscribe(SingleEmitter<T> emitter) {
        RxListener<T> rxListener = new RxListener<>(emitter, requestBuilder);
        rxListener.start();
    }

    private static class RxListener<T> implements Response.Listener<T>, Response.ErrorListener {

        private final SingleEmitter<T> emitter;
        private final GsonRequest<T> gsonRequest;
        private final Context context;

        private RxListener(SingleEmitter<T> emitter, RequestBuilder requestBuilder) {
            this.emitter = emitter;
            this.gsonRequest = new GsonRequest<>(requestBuilder.method, requestBuilder.url, requestBuilder.type, requestBuilder.header, requestBuilder.param, this, this);
            this.context = requestBuilder.context;
        }

        public void start() {
            RequestTickle requestTickle = VolleyTickle.newRequestTickle(context);
            requestTickle.add(gsonRequest);
            requestTickle.start();
        }

        @Override
        public void onResponse(T response) {
            if (emitter.isDisposed()) {
                return;
            }

            emitter.onSuccess(response);
        }

        @Override
        public void onErrorResponse(VolleyError error) {
            if (emitter.isDisposed()) {
                return;
            }

            emitter.onError(error);
        }
    }
}
