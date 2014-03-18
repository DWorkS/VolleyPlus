/**
 * Copyright 2013 Ognyan Bankov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.volley.demo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.volley.demo.util.MyVolley;

/**
 * Demonstrates how to execute "plain" HTTP request, i.e. non-JSON.
 * 
 * @author Ognyan Bankov
 *
 */
public class ExampleSimpleRequest extends ActionBarActivity {
    private TextView mTvResult;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_simple_request);

        mTvResult = (TextView) findViewById(R.id.tv_result);

        Button btnSimpleRequest = (Button) findViewById(R.id.btn_simple_request);
        btnSimpleRequest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                RequestQueue queue = MyVolley.getRequestQueue();
                StringRequest myReq = new StringRequest(Method.GET, 
                                                        "http://www.google.com/",
                                                        createMyReqSuccessListener(),
                                                        createMyReqErrorListener());

                queue.add(myReq);
            }
        });
    }


    private Response.Listener<String> createMyReqSuccessListener() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                mTvResult.setText(response);
            }
        };
    }


    private Response.ErrorListener createMyReqErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                mTvResult.setText(error.getMessage());
            }
        };
    }
}
