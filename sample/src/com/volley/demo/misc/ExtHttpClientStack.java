/**
 * Copyright 2013 Ognyan Bankov
 * Portions copyright 2011 The Android Open Source Project
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

package com.volley.demo.misc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.message.BasicStatusLine;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.error.AuthFailureError;
import com.android.volley.toolbox.HttpStack;

public class ExtHttpClientStack implements HttpStack {
	protected final HttpClient mClient;

	private final static String HEADER_CONTENT_TYPE = "Content-Type";

	public ExtHttpClientStack(HttpClient client) {
		mClient = client;
	}

	private static void addHeaders(HttpUriRequest httpRequest, Map<String, String> headers) {
		for (String key : headers.keySet()) {
			httpRequest.setHeader(key, headers.get(key));
		}
	}

	@SuppressWarnings("unused")
	private static List<NameValuePair> getPostParameterPairs(Map<String, String> postParams) {
		List<NameValuePair> result = new ArrayList<NameValuePair>(postParams.size());
		for (String key : postParams.keySet()) {
			result.add(new BasicNameValuePair(key, postParams.get(key)));
		}
		return result;
	}

	@Override
	public org.apache.http.HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws IOException, AuthFailureError {

		HttpUriRequest httpRequest = createHttpRequest(request, additionalHeaders);
		addHeaders(httpRequest, additionalHeaders);
		addHeaders(httpRequest, request.getHeaders());
		onPrepareRequest(httpRequest);
		HttpParams httpParams = httpRequest.getParams();
		int timeoutMs = request.getTimeoutMs();
		// TODO: Reevaluate this connection timeout based on more wide-scale
		// data collection and possibly different for wifi vs. 3G.
		HttpConnectionParams.setConnectionTimeout(httpParams, 5000);
		HttpConnectionParams.setSoTimeout(httpParams, timeoutMs);

		HttpResponse resp = mClient.execute(httpRequest);

		return convertResponseNewToOld(resp);
	}

	private org.apache.http.HttpResponse convertResponseNewToOld(HttpResponse resp) throws IllegalStateException, IOException {

		ProtocolVersion protocolVersion = new ProtocolVersion(resp.getProtocolVersion().getProtocol(), resp.getProtocolVersion().getMajor(), resp.getProtocolVersion().getMinor());

		StatusLine responseStatus = new BasicStatusLine(protocolVersion, resp.getStatusLine().getStatusCode(), resp.getStatusLine().getReasonPhrase());

		BasicHttpResponse response = new BasicHttpResponse(responseStatus);
		org.apache.http.HttpEntity ent = convertEntityNewToOld(resp.getEntity());
		response.setEntity(ent);

		for (Header h : resp.getAllHeaders()) {
			org.apache.http.Header header = convertheaderNewToOld(h);
			response.addHeader(header);
		}

		return response;
	}

	private org.apache.http.HttpEntity convertEntityNewToOld(HttpEntity ent) throws IllegalStateException, IOException {

		BasicHttpEntity ret = new BasicHttpEntity();
		if (ent != null) {
			ret.setContent(ent.getContent());
			ret.setContentLength(ent.getContentLength());
			Header h;
			h = ent.getContentEncoding();
			if (h != null) {
				ret.setContentEncoding(convertheaderNewToOld(h));
			}
			h = ent.getContentType();
			if (h != null) {
				ret.setContentType(convertheaderNewToOld(h));
			}
		}

		return ret;
	}

	private org.apache.http.Header convertheaderNewToOld(Header header) {
		org.apache.http.Header ret = new BasicHeader(header.getName(), header.getValue());
		return ret;
	}

	/**
	 * Creates the appropriate subclass of HttpUriRequest for passed in request.
	 */
	@SuppressWarnings("deprecation")
	/* protected */static HttpUriRequest createHttpRequest(Request<?> request, Map<String, String> additionalHeaders) throws AuthFailureError {
		switch (request.getMethod()) {
		case Method.DEPRECATED_GET_OR_POST: {
			// This is the deprecated way that needs to be handled for backwards
			// compatibility.
			// If the request's post body is null, then the assumption is that
			// the request is
			// GET. Otherwise, it is assumed that the request is a POST.
			byte[] postBody = request.getPostBody();
			if (postBody != null) {
				HttpPost postRequest = new HttpPost(request.getUrl());
				postRequest.addHeader(HEADER_CONTENT_TYPE, request.getPostBodyContentType());
				HttpEntity entity;
				entity = new ByteArrayEntity(postBody);
				postRequest.setEntity(entity);
				return postRequest;
			} else {
				return new HttpGet(request.getUrl());
			}
		}
		case Method.GET:
			return new HttpGet(request.getUrl());
		case Method.DELETE:
			return new HttpDelete(request.getUrl());
		case Method.POST: {
			HttpPost postRequest = new HttpPost(request.getUrl());
			postRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
			setEntityIfNonEmptyBody(postRequest, request);
			return postRequest;
		}
		case Method.PUT: {
			HttpPut putRequest = new HttpPut(request.getUrl());
			putRequest.addHeader(HEADER_CONTENT_TYPE, request.getBodyContentType());
			setEntityIfNonEmptyBody(putRequest, request);
			return putRequest;
		}
		default:
			throw new IllegalStateException("Unknown request method.");
		}
	}

	private static void setEntityIfNonEmptyBody(HttpEntityEnclosingRequestBase httpRequest, Request<?> request) throws AuthFailureError {
		byte[] body = request.getBody();
		if (body != null) {
			HttpEntity entity = new ByteArrayEntity(body);
			httpRequest.setEntity(entity);
		}
	}

	/**
	 * Called before the request is executed using the underlying HttpClient.
	 * 
	 * <p>
	 * Overwrite in subclasses to augment the request.
	 * </p>
	 */
	protected void onPrepareRequest(HttpUriRequest request) throws IOException {
		// Nothing.
	}
}
