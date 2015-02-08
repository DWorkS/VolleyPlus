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

package com.android.volley.toolbox;

import static com.android.volley.misc.MultipartUtils.*;

import android.text.TextUtils;

import com.android.volley.Request;
import com.android.volley.Request.Method;
import com.android.volley.Response.ProgressListener;
import com.android.volley.error.AuthFailureError;
import com.android.volley.request.MultiPartRequest;
import com.android.volley.request.MultiPartRequest.MultiPartParam;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolVersion;
import org.apache.http.StatusLine;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.message.BasicStatusLine;

import java.io.BufferedInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;

/**
 * An {@link HttpStack} based on {@link HttpURLConnection}.
 */
public class HurlStack implements HttpStack {

	private UrlRewriter mUrlRewriter;
	private final SSLSocketFactory mSslSocketFactory;
	private String mUserAgent;

	/**
	 * An interface for transforming URLs before use.
	 */
	public interface UrlRewriter {
		/**
		 * Returns a URL to use instead of the provided one, or null to indicate
		 * this URL should not be used at all.
		 */
		public String rewriteUrl(String originalUrl);
	}

	public HurlStack() {
		this(null);
	}

	/**
	 * @param urlRewriter
	 *            Rewriter to use for request URLs
	 */
	public HurlStack(UrlRewriter urlRewriter) {
		this(urlRewriter, null);
	}

	/**
	 * @param urlRewriter
	 *            Rewriter to use for request URLs
	 * @param sslSocketFactory
	 *            SSL factory to use for HTTPS connections
	 */
	public HurlStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory) {
		mUrlRewriter = urlRewriter;
		mSslSocketFactory = sslSocketFactory;
	}

	/**
	 * @param urlRewriter
	 *            Rewriter to use for request URLs
	 * @param sslSocketFactory
	 *            SSL factory to use for HTTPS connections
	 * @param userAgent
	 *            User Agent for HTTPS connections
	 */
	public HurlStack(UrlRewriter urlRewriter, SSLSocketFactory sslSocketFactory, String userAgent) {

		mUrlRewriter = urlRewriter;
		mSslSocketFactory = sslSocketFactory;
		mUserAgent = userAgent;
	}

	@Override
	public HttpResponse performRequest(Request<?> request, Map<String, String> additionalHeaders) throws AuthFailureError, IOException {
		String url = request.getUrl();
		HashMap<String, String> map = new HashMap<String, String>();
		map.putAll(request.getHeaders());
		map.putAll(additionalHeaders);
		if (mUrlRewriter != null) {
			String rewritten = mUrlRewriter.rewriteUrl(url);
			if (rewritten == null) {
				throw new IOException("URL blocked by rewriter: " + url);
			}
			url = rewritten;
		}
		URL parsedUrl = new URL(url);
		HttpURLConnection connection = openConnection(parsedUrl, request);

		if (!TextUtils.isEmpty(mUserAgent)) {
			connection.setRequestProperty(HEADER_USER_AGENT, mUserAgent);
		}

		for (String headerName : map.keySet()) {
			connection.addRequestProperty(headerName, map.get(headerName));
		}
		if (request instanceof MultiPartRequest) {
			setConnectionParametersForMultipartRequest(connection, request);
		} else {
			setConnectionParametersForRequest(connection, request);
		}

		// Initialize HttpResponse with data from the HttpURLConnection.
		ProtocolVersion protocolVersion = new ProtocolVersion("HTTP", 1, 1);
		int responseCode = connection.getResponseCode();
		if (responseCode == -1) {
			// -1 is returned by getResponseCode() if the response code could
			// not be retrieved.
			// Signal to the caller that something was wrong with the
			// connection.
			throw new IOException("Could not retrieve response code from HttpUrlConnection.");
		}
		StatusLine responseStatus = new BasicStatusLine(protocolVersion, connection.getResponseCode(), connection.getResponseMessage());
		BasicHttpResponse response = new BasicHttpResponse(responseStatus);
		response.setEntity(entityFromConnection(connection));
		for (Entry<String, List<String>> header : connection.getHeaderFields().entrySet()) {
			if (header.getKey() != null) {
				Header h = new BasicHeader(header.getKey(), header.getValue().get(0));
				response.addHeader(h);
			}
		}
		return response;
	}

	/**
	 * Perform a multipart request on a connection
	 * 
	 * @param connection
	 *            The Connection to perform the multi part request
	 * @param request
	 *            The params to add to the Multi Part request
	 *            The files to upload
	 * @throws ProtocolException
	 */
	private static void setConnectionParametersForMultipartRequest(HttpURLConnection connection, Request<?> request) throws IOException,
			ProtocolException {

		final String charset = ((MultiPartRequest<?>) request).getProtocolCharset();
		final int curTime = (int) (System.currentTimeMillis() / 1000);
		final String boundary = BOUNDARY_PREFIX + curTime;
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setRequestProperty(HEADER_CONTENT_TYPE, String.format(CONTENT_TYPE_MULTIPART, charset, curTime));
		
		Map<String, MultiPartParam> multipartParams = ((MultiPartRequest<?>) request).getMultipartParams();
		Map<String, String> filesToUpload = ((MultiPartRequest<?>) request).getFilesToUpload();
		
		if (((MultiPartRequest<?>) request).isFixedStreamingMode()) {
			int contentLength = getContentLengthForMultipartRequest(boundary, multipartParams, filesToUpload);
			
			connection.setFixedLengthStreamingMode(contentLength);
		} else {
			connection.setChunkedStreamingMode(0);
		}
		// Modified end

        ProgressListener progressListener;
        progressListener = (ProgressListener) request;

		PrintWriter writer = null;
		try {
			OutputStream out = connection.getOutputStream();
			writer = new PrintWriter(new OutputStreamWriter(out, charset), true);

			for (String key : multipartParams.keySet()) {
				MultiPartParam param = multipartParams.get(key);

				writer.append(boundary).append(CRLF).append(String.format(HEADER_CONTENT_DISPOSITION + COLON_SPACE + FORM_DATA, key)).append(CRLF)
						.append(HEADER_CONTENT_TYPE + COLON_SPACE + param.contentType).append(CRLF).append(CRLF).append(param.value).append(CRLF)
						.flush();
			}

			for (String key : filesToUpload.keySet()) {

				File file = new File(filesToUpload.get(key));

				if (!file.exists()) {
					throw new IOException(String.format("File not found: %s", file.getAbsolutePath()));
				}

				if (file.isDirectory()) {
					throw new IOException(String.format("File is a directory: %s", file.getAbsolutePath()));
				}

				writer.append(boundary)
						.append(CRLF)
						.append(String.format(HEADER_CONTENT_DISPOSITION + COLON_SPACE + FORM_DATA + SEMICOLON_SPACE + FILENAME, key, file.getName()))
						.append(CRLF).append(HEADER_CONTENT_TYPE + COLON_SPACE + CONTENT_TYPE_OCTET_STREAM).append(CRLF)
						.append(HEADER_CONTENT_TRANSFER_ENCODING + COLON_SPACE + BINARY).append(CRLF).append(CRLF).flush();

				BufferedInputStream input = null;
				try {
					FileInputStream fis = new FileInputStream(file);
                    int transferredBytes = 0;
                    int totalSize = (int) file.length();
					input = new BufferedInputStream(fis);
					int bufferLength = 0;

					byte[] buffer = new byte[1024];
					while ((bufferLength = input.read(buffer)) > 0) {
						out.write(buffer, 0, bufferLength);
                        transferredBytes += bufferLength;
                        progressListener.onProgress(transferredBytes, totalSize);
					}
					out.flush(); // Important! Output cannot be closed. Close of
									// writer will close
									// output as well.
				} finally {
					if (input != null)
						try {
							input.close();
						} catch (IOException ex) {
							ex.printStackTrace();
						}
				}
				writer.append(CRLF).flush(); // CRLF is important! It indicates
												// end of binary
												// boundary.
			}

			// End of multipart/form-data.
			writer.append(boundary + BOUNDARY_PREFIX).append(CRLF).flush();

		} catch (Exception e) {
			e.printStackTrace();

		} finally {
			if (writer != null) {
				writer.close();
			}
		}
	}

	/**
	 * Initializes an {@link HttpEntity} from the given
	 * {@link HttpURLConnection}.
	 * 
	 * @param connection
	 * @return an HttpEntity populated with data from <code>connection</code>.
	 */
	private static HttpEntity entityFromConnection(HttpURLConnection connection) {
		BasicHttpEntity entity = new BasicHttpEntity();
		InputStream inputStream;
		try {
			inputStream = connection.getInputStream();
		} catch (IOException ioe) {
			inputStream = connection.getErrorStream();
		}
		entity.setContent(inputStream);
		entity.setContentLength(connection.getContentLength());
		entity.setContentEncoding(connection.getContentEncoding());
		entity.setContentType(connection.getContentType());
		return entity;
	}

	/**
	 * Create an {@link HttpURLConnection} for the specified {@code url}.
	 */
	protected HttpURLConnection createConnection(URL url) throws IOException {
		return (HttpURLConnection) url.openConnection();
	}

	/**
	 * Opens an {@link HttpURLConnection} with parameters.
	 * 
	 * @param url
	 * @return an open connection
	 * @throws IOException
	 */
	private HttpURLConnection openConnection(URL url, Request<?> request) throws IOException {
		HttpURLConnection connection = createConnection(url);

		int timeoutMs = request.getTimeoutMs();
		connection.setConnectTimeout(timeoutMs);
		connection.setReadTimeout(timeoutMs);
		connection.setUseCaches(false);
		connection.setDoInput(true);

		// use caller-provided custom SslSocketFactory, if any, for HTTPS
		if ("https".equals(url.getProtocol()) && mSslSocketFactory != null) {
			((HttpsURLConnection) connection).setSSLSocketFactory(mSslSocketFactory);
		}

		return connection;
	}

	@SuppressWarnings("deprecation")
	/* package */static void setConnectionParametersForRequest(HttpURLConnection connection, Request<?> request) throws IOException, AuthFailureError {
		switch (request.getMethod()) {
		case Method.DEPRECATED_GET_OR_POST:
			// This is the deprecated way that needs to be handled for backwards
			// compatibility.
			// If the request's post body is null, then the assumption is that
			// the request is
			// GET. Otherwise, it is assumed that the request is a POST.
			byte[] postBody = request.getPostBody();
			if (postBody != null) {
				// Prepare output. There is no need to set Content-Length
				// explicitly,
				// since this is handled by HttpURLConnection using the size of
				// the prepared
				// output stream.
				connection.setDoOutput(true);
				connection.setRequestMethod("POST");
				connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getPostBodyContentType());
				DataOutputStream out = new DataOutputStream(connection.getOutputStream());
				out.write(postBody);
				out.close();
			}
			break;
		case Method.GET:
			// Not necessary to set the request method because connection
			// defaults to GET but
			// being explicit here.
			connection.setRequestMethod("GET");
			break;
		case Method.DELETE:
			connection.setRequestMethod("DELETE");
			break;
		case Method.POST:
			connection.setRequestMethod("POST");
			addBodyIfExists(connection, request);
			break;
		case Method.PUT:
			connection.setRequestMethod("PUT");
			addBodyIfExists(connection, request);
			break;
		case Method.HEAD:
			connection.setRequestMethod("HEAD");
			break;
		case Method.OPTIONS:
			connection.setRequestMethod("OPTIONS");
			break;
		case Method.TRACE:
			connection.setRequestMethod("TRACE");
			break;
		case Method.PATCH:
			// connection.setRequestMethod("PATCH");
			// If server doesnt support patch uncomment this
			connection.setRequestMethod("POST");
			connection.setRequestProperty("X-HTTP-Method-Override", "PATCH");
			addBodyIfExists(connection, request);
			break;
		default:
			throw new IllegalStateException("Unknown method type.");
		}
	}

	private static void addBodyIfExists(HttpURLConnection connection, Request<?> request) throws IOException, AuthFailureError {
		byte[] body = request.getBody();
		if (body != null) {
			ProgressListener progressListener = null;
			if (request instanceof ProgressListener) {
				progressListener = (ProgressListener) request;
			}
			connection.setDoOutput(true);
			connection.addRequestProperty(HEADER_CONTENT_TYPE, request.getBodyContentType());
			DataOutputStream out = new DataOutputStream(connection.getOutputStream());

			if (progressListener != null) {
				int transferredBytes = 0;
				int totalSize = body.length;
				int offset = 0;
				int chunkSize = Math.min(2048, Math.max(totalSize - offset, 0));
				while (chunkSize > 0 && offset + chunkSize <= totalSize) {
					out.write(body, offset, chunkSize);
					transferredBytes += chunkSize;
					progressListener.onProgress(transferredBytes, totalSize);
					offset += chunkSize;
					chunkSize = Math.min(chunkSize, Math.max(totalSize - offset, 0));
				}
			}
			else{
				out.write(body);
			}

			out.close();
		}
	}
}
