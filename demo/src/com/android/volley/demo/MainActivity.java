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

package com.android.volley.demo;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.example.volley_examples.R;

/**
 * Main activity. All demos are started from here.
 * @author Ognyan Bankov
 *
 */
public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act__main);

        Button btnSimpleRequest = (Button) findViewById(R.id.btn_simple_request);
        btnSimpleRequest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExampleSimpleRequest.class);
                startActivity(intent);
            }
        });

        Button btnParams = (Button) findViewById(R.id.btn_params);
        btnParams.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExampleParams.class);
                startActivity(intent);
            }
        });
        
        Button btnJsonRequest = (Button) findViewById(R.id.btn_json_request);
        btnJsonRequest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExampleJsonRequest.class);
                startActivity(intent);
            }
        });
        
        Button btnGsonRequest = (Button) findViewById(R.id.btn_gson_request);
        btnGsonRequest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExampleGsonRequest.class);
                startActivity(intent);
            }
        });        
        Button btnImageLoading = (Button) findViewById(R.id.btn_image_loading);
        btnImageLoading.setOnClickListener(new OnClickListener() {
           @Override
           public void onClick(View v) {
              Intent intent = new Intent(MainActivity.this, ExampleImageLoading.class);
              startActivity(intent);
           }
        });        
        Button btnCookies = (Button) findViewById(R.id.btn_cookies);
        btnCookies.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExampleCookies.class);
                startActivity(intent);
            }
        });        
        Button btnExtHttpClient = (Button) findViewById(R.id.btn_ext_http_client);
        btnExtHttpClient.setOnClickListener(new OnClickListener() {
           @Override
           public void onClick(View v) {
              Intent intent = new Intent(MainActivity.this, ExampleNewHttpClient.class);
              startActivity(intent);
           }
        });  
        Button btnSsSslHttpClient = (Button) findViewById(R.id.btn_ss_ssl_http_client);
        btnSsSslHttpClient.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExampleSsSslHttpClient.class);
                startActivity(intent);
            }
        });
        
        Button btnNetworkListView = (Button) findViewById(R.id.btn_network_list_view);
        btnNetworkListView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ExampleNetworkListView.class);
                startActivity(intent);
            }
        });  
    }
}
