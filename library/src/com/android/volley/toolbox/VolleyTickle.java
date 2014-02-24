/*
 * Copyright (C) 2014 Hari Krishna Dulipudi
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

package com.android.volley.toolbox;

import java.io.File;
import java.io.UnsupportedEncodingException;

import android.content.Context;
import android.net.http.AndroidHttpClient;

import com.android.volley.Network;
import com.android.volley.NetworkResponse;
import com.android.volley.RequestTickle;
import com.android.volley.cache.DiskBasedCache;
import com.android.volley.misc.NetUtils;
import com.android.volley.misc.Utils;

public class VolleyTickle {

    /** Default on-disk cache directory. */
    private static final String DEFAULT_CACHE_DIR = "volley";

    /**
     * Creates a default instance of the worker pool and calls {@link RequestTickle#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @return A started {@link RequestTickle} instance.
     */
    public static RequestTickle newRequestTickle(Context context) {
        File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
		HttpStack stack = Utils.hasHoneycomb() ?
                new HurlStack() :
                new HttpClientStack(AndroidHttpClient.newInstance(
                        NetUtils.getUserAgent(context)));
                
        Network network = new BasicNetwork(stack);
        
        RequestTickle tickle = new RequestTickle(new DiskBasedCache(cacheDir), network);

        return tickle;
    }
    
    /**
     * Creates a default instance of the worker pool and calls {@link RequestTickle#start()} on it.
     *
     * @param context A {@link Context} to use for creating the cache dir.
     * @return A started {@link RequestTickle} instance.
     */
    public static RequestTickle newRequestTickle(Context context, Network network) {
    	File cacheDir = new File(context.getCacheDir(), DEFAULT_CACHE_DIR);
        RequestTickle tickle = new RequestTickle(new DiskBasedCache(cacheDir), network);

        return tickle;
    }
    
    public static String parseResponse(NetworkResponse response){
        String parsed;
        try {
            parsed = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        }
		return parsed;
    }
}