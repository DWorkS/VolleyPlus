/*
 * Copyright (C) 2011 The Android Open Source Project
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

package com.android.volley.request;

import android.text.TextUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.Response.ProgressListener;
import com.android.volley.toolbox.HttpHeaderParser;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * A canned request for retrieving the response body at a given URL as a String.
 */
public class DownloadRequest extends Request<String> implements ProgressListener {
    private final Listener<String> mListener;
    private final String mDownloadPath;
    private ProgressListener mProgressListener;

    /**
     * Creates a new request with the given method.
     *
     * @param url URL to fetch the string at
     * @param download_path path to save the file to
     * @param listener Listener to receive the String response
     * @param errorListener Error listener, or null to ignore errors
     */
    public DownloadRequest(String url, String download_path, Listener<String> listener,
            ErrorListener errorListener) {
        super(Method.GET, url, errorListener);
        mDownloadPath = download_path;
        mListener = listener;
    }

    /**
     * Set listener for tracking download progress
     *
     * @param listener
     */
    public void setOnProgressListener(ProgressListener listener){
    	mProgressListener = listener;
    }

    @Override
    protected void deliverResponse(String response) {
    	if(null != mListener){
    		mListener.onResponse(response);
    	}
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String parsed = null;
        try {
            byte[] data = response.data;
		    //convert array of bytes into file
		    FileOutputStream fileOuputStream = new FileOutputStream(mDownloadPath); 
		    fileOuputStream.write(data);
		    fileOuputStream.close();
		    parsed = mDownloadPath;
        } catch (UnsupportedEncodingException e) {
            parsed = new String(response.data);
        } catch (FileNotFoundException e) {
			e.printStackTrace();
		}  catch (IOException e) {
			e.printStackTrace();
		} finally{
			if(TextUtils.isEmpty(parsed)){
				parsed = "";
			}
		}
        
        return Response.success(parsed, HttpHeaderParser.parseCacheHeaders(response));
    }

	@Override
	public void onProgress(long transferredBytes, long totalSize) {
		if(null != mProgressListener){
			mProgressListener.onProgress(transferredBytes, totalSize);
		}
	}
}