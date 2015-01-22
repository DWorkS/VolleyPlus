package br.com.ilhasoft.volley;

import com.android.volley.Response;

/**
 * Created by dev on 28/07/2014.
 */
public class ResponseWaiter {

    private RequestBuilder builder;
    private Response.Listener<String> previousResponseListener;
    private String[] result = new String[1];

    public static String buildRequestAndWaitResponse(RequestBuilder builder){
        return new ResponseWaiter(builder).buildAndWait();
    }

    private ResponseWaiter(RequestBuilder builder) {
        this.builder = builder;
    }

    private String buildAndWait(){
        previousResponseListener = builder.responseListener;
        builder.responseListener = basicResponseListener;
        builder.addToRequestQueue();
        waitUntilTimeOut();
        return result[0];
    }

    private void waitUntilTimeOut() {
        long maxTime = System.currentTimeMillis() + builder.retryTimeout;
        while (System.currentTimeMillis() < maxTime){
            if (result[0] != null && !result[0].isEmpty())
                break;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private Response.Listener<String> basicResponseListener = new Response.Listener<String>() {
        @Override
        public void onResponse(String response) {
            result[0] = response;
            if (previousResponseListener != null)
                previousResponseListener.onResponse(response);
        }
    };
}