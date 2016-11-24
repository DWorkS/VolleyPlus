package com.android.volley.request;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.Response.ProgressListener;
import com.android.volley.error.AuthFailureError;
import com.android.volley.misc.MultiPartParam;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.android.volley.misc.MultipartUtils.BINARY;
import static com.android.volley.misc.MultipartUtils.BOUNDARY_PREFIX;
import static com.android.volley.misc.MultipartUtils.COLON_SPACE;
import static com.android.volley.misc.MultipartUtils.CONTENT_TYPE_MULTIPART;
import static com.android.volley.misc.MultipartUtils.CONTENT_TYPE_OCTET_STREAM;
import static com.android.volley.misc.MultipartUtils.CRLF;
import static com.android.volley.misc.MultipartUtils.FILENAME;
import static com.android.volley.misc.MultipartUtils.FORM_DATA;
import static com.android.volley.misc.MultipartUtils.HEADER_CONTENT_DISPOSITION;
import static com.android.volley.misc.MultipartUtils.HEADER_CONTENT_TRANSFER_ENCODING;
import static com.android.volley.misc.MultipartUtils.HEADER_CONTENT_TYPE;
import static com.android.volley.misc.MultipartUtils.SEMICOLON_SPACE;
import static com.android.volley.misc.MultipartUtils.getContentLengthForMultipartRequest;

/**
 * A request for making a Multi Part request
 *
 * @param <T> Response expected
 */
public abstract class MultiPartRequest<T> extends Request<T> implements ProgressListener{

	private static final String PROTOCOL_CHARSET = "utf-8";
	private int curTime;
	private String boundaryPrefixed;
	private Listener<T> mListener;
	private ProgressListener mProgressListener;
	private Map<String, MultiPartParam> mMultipartParams = null;
	private Map<String, String> mFileUploads = null;
	public static final int TIMEOUT_MS = 30000;
	private boolean isFixedStreamingMode;

	/**
	 * Creates a new request with the given method.
	 *
	 * @param method the request {@link Method} to use
	 * @param url URL to fetch the string at
	 * @param listener Listener to receive the String response
	 * @param errorListener Error listener, or null to ignore errors
	 */
	public MultiPartRequest(int method, String url, Listener<T> listener, ErrorListener errorListener) {

		super(method, url, Priority.NORMAL, errorListener, new DefaultRetryPolicy(TIMEOUT_MS, DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
				                                                                         DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
		mListener = listener;
		mMultipartParams = new HashMap<String, MultiPartParam>();
		mFileUploads = new HashMap<String, String>();

		curTime = (int) (System.currentTimeMillis() / 1000);
		boundaryPrefixed = BOUNDARY_PREFIX + curTime;
	}


	/**
	 * Get the protocol charset
	 */
	public String getProtocolCharset() {
		return PROTOCOL_CHARSET;
	}

	/**
	 * Get the Content Length
	 */
	public int getContentLength() {
		return getContentLengthForMultipartRequest(getBoundryPrefixed(), getMultipartParams(), getFilesToUpload());
	}

	@Override
	public String getBodyContentType() {
		return String.format(CONTENT_TYPE_MULTIPART, getProtocolCharset(), getBoundry());
	}

	@Override
	abstract protected Response<T> parseNetworkResponse(NetworkResponse response);

	@Override
	protected void deliverResponse(T response) {
		if(null != mListener){
			mListener.onResponse(response);
		}
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
	public void onProgress(long transferredBytes, long totalSize) {
		if(null != mProgressListener){
			mProgressListener.onProgress(transferredBytes, totalSize);
		}
	}

	public boolean isFixedStreamingMode() {
		return isFixedStreamingMode;
	}

	public void setFixedStreamingMode(boolean isFixedStreamingMode) {
		this.isFixedStreamingMode = isFixedStreamingMode;
	}

	/**
	 * Get the boundry prefixed
	 */
	public String getBoundryPrefixed() {
		return boundaryPrefixed;
	}

	/**
	 * Get the boundry
	 */
	public int getBoundry() {
		return curTime;
	}


	/**
	 * Add a parameter to be sent in the multipart request
	 *
	 * @param name The name of the paramter
	 * @param contentType The content type of the paramter
	 * @param value the value of the paramter
	 * @return The Multipart request for chaining calls
	 */
	public MultiPartRequest<T> addMultipartParam(String name, String contentType, String value) {
		mMultipartParams.put(name, new MultiPartParam(contentType, value));
		return this;
	}

	/**
	 * Add a string parameter to be sent in the multipart request
	 *
	 * @param name The name of the paramter
	 * @param value the value of the paramter
	 * @return The Multipart request for chaining calls
	 */
	public MultiPartRequest<T> addStringParam(String name, String value) {
		mMultipartParams.put(name, new MultiPartParam("text/plain", value));
		return this;
	}

	/**
	 * Add a file to be uploaded in the multipart request
	 *
	 * @param name The name of the file key
	 * @param filePath The path to the file. This file MUST exist.
	 * @return The Multipart request for chaining method calls
	 */
	public MultiPartRequest<T> addFile(String name, String filePath) {

		mFileUploads.put(name, filePath);
		return this;
	}

	/**
	 * Get all the multipart params for this request
	 *
	 * @return A map of all the multipart params NOT including the file uploads
	 */
	public Map<String, MultiPartParam> getMultipartParams() {
		return mMultipartParams;
	}

	/**
	 * Get all the files to be uploaded for this request
	 *
	 * @return A map of all the files to be uploaded for this request
	 */
	public Map<String, String> getFilesToUpload() {
		return mFileUploads;
	}

	@Override
	public byte[] getBody() throws AuthFailureError {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream dos = new DataOutputStream(bos);

		try {
			buildParts(dos);
			return bos.toByteArray();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return super.getBody();
	}

	private void buildParts(DataOutputStream dos) throws IOException {
		Map<String, MultiPartParam> multipartParams = getMultipartParams();
		Map<String, String> filesToUpload = getFilesToUpload();

		for (Map.Entry<String, MultiPartParam> multipartParam : multipartParams.entrySet()) {
			MultiPartParam param = multipartParam.getValue();
			buildStringPart(dos, multipartParam.getKey(), param);
		}

		for (Map.Entry<String, String> fileToUpload : filesToUpload.entrySet()) {

			File file = new File(fileToUpload.getValue());

			if (!file.exists()) {
				throw new IOException(String.format("File not found: %s", file.getAbsolutePath()));
			} else if (file.isDirectory()) {
				throw new IOException(String.format("File is a directory: %s", file.getAbsolutePath()));
			}
			buildDataPart(dos, fileToUpload.getKey(), file);
		}

		// close multipart form data after text and file data
		dos.writeBytes(getBoundryPrefixed() + BOUNDARY_PREFIX);
		dos.writeBytes(CRLF);
	}

	private void buildStringPart(DataOutputStream dataOutputStream, String key, MultiPartParam param) throws IOException {

		dataOutputStream.writeBytes(getBoundryPrefixed());
		dataOutputStream.writeBytes(CRLF);
		dataOutputStream.writeBytes(String.format(HEADER_CONTENT_DISPOSITION + COLON_SPACE + FORM_DATA, key));
		dataOutputStream.writeBytes(CRLF);
		dataOutputStream.writeBytes(HEADER_CONTENT_TYPE + COLON_SPACE + param.contentType);
		dataOutputStream.writeBytes(CRLF);
		dataOutputStream.writeBytes(CRLF);
		dataOutputStream.writeBytes(param.value);
		dataOutputStream.writeBytes(CRLF);
	}

	private void buildDataPart(DataOutputStream dataOutputStream, String key, File file) throws IOException {

		dataOutputStream.writeBytes(getBoundryPrefixed());
		dataOutputStream.writeBytes(CRLF);
		dataOutputStream.writeBytes(String.format(HEADER_CONTENT_DISPOSITION + COLON_SPACE + FORM_DATA + SEMICOLON_SPACE + FILENAME, key, file.getName()));
		dataOutputStream.writeBytes(CRLF);
		dataOutputStream.writeBytes(HEADER_CONTENT_TYPE + COLON_SPACE + CONTENT_TYPE_OCTET_STREAM);
		dataOutputStream.writeBytes(CRLF);
		dataOutputStream.writeBytes(HEADER_CONTENT_TRANSFER_ENCODING + COLON_SPACE + BINARY);
		dataOutputStream.writeBytes(CRLF);
		dataOutputStream.writeBytes(CRLF);

		FileInputStream fileInputStream = new FileInputStream(file);
		int bytesAvailable = fileInputStream.available();

		int maxBufferSize = 1024 * 1024;
		int bufferSize = Math.min(bytesAvailable, maxBufferSize);
		byte[] buffer = new byte[bufferSize];

		int bytesRead = fileInputStream.read(buffer, 0, bufferSize);

		while (bytesRead > 0) {
			dataOutputStream.write(buffer, 0, bufferSize);
			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
		}


		dataOutputStream.writeBytes(CRLF);
	}

}
