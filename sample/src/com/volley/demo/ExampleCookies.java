/**
 * Copyright 2013 Ognyan Bankov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.volley.demo;

import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Volley;
import com.android.volley.error.VolleyError;
import com.android.volley.request.StringRequest;
import com.android.volley.toolbox.HttpClientStack;

import org.apache.http.client.CookieStore;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;

import java.util.List;


/**
 * Demonstrates how to use cookies.
 * <p>
 * When pressing "Execute request" app executes the request, server reads the cookie (if present), increases it with one and sends its back.
 * </p>
 *
 * <p>
 * When pressing "Set cookie and execute" app sets the cookie to 41 and then executes the request.
 * </p>
 *
 * @author Ognyan Bankov
 */
public class ExampleCookies extends AppCompatActivity {
    private TextView mTvCookie;
    private RequestQueue mQueue;
    private AbstractHttpClient mHttpClient;
    private Button mBtnSetCookie;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cookie);

        // we hold a reference to the HttpClient in order to be able to get/set cookies
        mHttpClient = new DefaultHttpClient();

        mQueue = Volley.newRequestQueue(ExampleCookies.this, new HttpClientStack(mHttpClient));

        mTvCookie = findViewById(R.id.tv_cookie);
        setTvCookieText("n/a");

        Button btnRequest = findViewById(R.id.btn_execute_request);
        btnRequest.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                mQueue.add(createRequest());
            }
        });

        mBtnSetCookie = findViewById(R.id.btn_set_cookie);
        mBtnSetCookie.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CookieStore cs = mHttpClient.getCookieStore();
                BasicClientCookie c = (BasicClientCookie) getCookie(cs, "my_cookie");
                c.setValue("41");
                cs.addCookie(c);

                mQueue.add(createRequest());
            }
        });
    }


    private StringRequest createRequest() {

        return new StringRequest(Method.GET,
                "http://khs.bolyartech.com/http_cookie.php",
                createMyReqSuccessListener(),
                createMyReqErrorListener());
    }


    private Response.Listener<String> createMyReqSuccessListener() {
        return new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                CookieStore cs = mHttpClient.getCookieStore();
                BasicClientCookie c = (BasicClientCookie) getCookie(cs, "my_cookie");

                if (c != null) {
                    setTvCookieText(c.getValue());
                }
                mBtnSetCookie.setEnabled(true);
            }
        };
    }


    private Response.ErrorListener createMyReqErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                setTvCookieText(error.getMessage());
            }
        };
    }


    private void setTvCookieText(String str) {
        mTvCookie.setText(String.format(getString(R.string.act_cookie__tv_cookie), str));
    }


    public Cookie getCookie(CookieStore cs, String cookieName) {
        Cookie ret = null;

        List<Cookie> l = cs.getCookies();
        for (Cookie c : l) {
            if (c.getName().equals(cookieName)) {
                ret = c;
                break;
            }
        }

        return ret;
    }
}
