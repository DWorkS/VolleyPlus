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

import java.io.ByteArrayOutputStream;
import java.util.Map;

import org.apache.http.impl.cookie.DateParseException;
import org.apache.http.impl.cookie.DateUtils;
import org.apache.http.protocol.HTTP;

import android.graphics.Bitmap;
import android.support.v4.util.ArrayMap;

import com.android.volley.Cache;
import com.android.volley.NetworkResponse;

/**
 * Utility methods for parsing HTTP headers.
 */
public class HttpHeaderParser {

    /**
     * Extracts a {@link Cache.Entry} from a {@link NetworkResponse}.
     *
     * @param response The network response to parse headers from
     * @return a cache entry for the given response, or null if the response is not cacheable.
     */
    public static Cache.Entry parseCacheHeaders(NetworkResponse response) {
        long now = System.currentTimeMillis();
    	
        long serverDate = 0;
        long lastModified = 0;
        long serverExpires = 0;
        long softExpire = 0;
        long finalExpire = 0;
        long maxAge = 0;
        long staleWhileRevalidate = 0;
        boolean hasCacheControl = false;
        Map<String, String> headers = null;
        String serverEtag = null;
        
        if(null != response){
	        headers = response.headers;
	
	        String headerValue;
	
	        headerValue = headers.get("Date");
	        if (headerValue != null) {
	            serverDate = parseDateAsEpoch(headerValue);
	        }
	
	        headerValue = headers.get("Cache-Control");
	        if (headerValue != null) {
	            hasCacheControl = true;
	            String[] tokens = headerValue.split(",");
	            for (int i = 0; i < tokens.length; i++) {
	                String token = tokens[i].trim();
                    if (token.equals("no-cache") || token.equals("no-store")) {
                        hasCacheControl = false;
                    } else if (token.startsWith("max-age=")) {
                        hasCacheControl = true;
                        try {
                            maxAge = Long.parseLong(token.substring(8));
                        } catch (Exception e) {
                        }
                    } else if (token.startsWith("stale-while-revalidate=")) {
                        try {
                            staleWhileRevalidate = Long.parseLong(token.substring(23));
                        } catch (Exception e) {
                        }
                    } else if (token.equals("must-revalidate") || token.equals("proxy-revalidate")) {
                        maxAge = 0;
                    }
                }
	        }
	
	        headerValue = headers.get("Expires");
	        if (headerValue != null) {
	            serverExpires = parseDateAsEpoch(headerValue);
	        }

            headerValue = headers.get("Last-Modified");
            if (headerValue != null) {
                lastModified = parseDateAsEpoch(headerValue);
            }

	        serverEtag = headers.get("ETag");
	
	        // Cache-Control takes precedence over an Expires header, even if both exist and Expires
	        // is more restrictive.
	        if (hasCacheControl) {
	            softExpire = now + maxAge * 1000;
                finalExpire = softExpire + staleWhileRevalidate * 1000;
	        } else if (serverDate > 0 && serverExpires >= serverDate) {
	            // Default semantic for Expire header in HTTP specification is softExpire.
	            softExpire = now + (serverExpires - serverDate);
                finalExpire = softExpire;
	        }
        }
        
        Cache.Entry entry = new Cache.Entry();
        entry.data = response == null ? null : response.data;
        entry.etag = serverEtag;
        entry.softTtl = softExpire;
        entry.ttl = finalExpire;
        entry.serverDate = serverDate;
        entry.lastModified = lastModified;
        entry.responseHeaders = headers;

        return entry;
    }

    /**
     * Extracts a {@link Cache.Entry} from a {@link NetworkResponse}.
     * Cache-control headers are ignored. SoftTtl == 3 mins, ttl == 24 hours.
     * @param bitmap The network response to parse headers from
     * @return a cache entry for the given response, or null if the response is not cacheable.
     */
    public static Cache.Entry parseBitmapCacheHeaders(Bitmap bitmap) {
    	NetworkResponse response = null;
    	if(null != bitmap){
	    	ByteArrayOutputStream stream = new ByteArrayOutputStream();
	    	bitmap.compress(Bitmap.CompressFormat.JPEG, 70, stream);
	    	byte[] byteArray = stream.toByteArray();
	    	response = new NetworkResponse(byteArray);
    	}
        return parseCacheHeaders(response);
    }
    
   /**
    * Extracts a {@link Cache.Entry} from a {@link NetworkResponse}.
    * Cache-control headers are ignored. SoftTtl == 3 mins, ttl == 24 hours.
    * @param response The network response to parse headers from
    * @return a cache entry for the given response, or null if the response is not cacheable.
    */
   public static Cache.Entry parseIgnoreCacheHeaders(NetworkResponse response) {

       long now = System.currentTimeMillis();
       Map<String, String> headers = response.headers;
       long serverDate = 0;
       String serverEtag = null;
       String headerValue;

       headerValue = headers.get("Date");
       if (headerValue != null) {
           serverDate = parseDateAsEpoch(headerValue);
       }

       serverEtag = headers.get("ETag");

       final long cacheHitButRefreshed = 3 * 60 * 1000; // in 3 minutes cache will be hit, but also refreshed on background
       final long cacheExpired = 24 * 60 * 60 * 1000; // in 24 hours this cache entry expires completely
       final long softExpire = now  + cacheHitButRefreshed;
       final long ttl = now + cacheExpired;

       Cache.Entry entry = new Cache.Entry();
       entry.data = response.data;
       entry.etag = serverEtag;
       entry.softTtl = softExpire;
       entry.ttl = ttl;
       entry.serverDate = serverDate;
       entry.responseHeaders = headers;

       return entry;
   }

    /**
     * Extracts a {@link Cache.Entry} from a {@link NetworkResponse}.
     * Cache-control headers are ignored. SoftTtl == 3 mins, ttl == 24 hours.
     * @param response The network response to parse headers from
     * @param soft_expire The soft expire duration in milli seconds
     * @param expire The full expire duration in milli seconds
     * @return a cache entry for the given response, or null if the response is not cacheable.
     */
    public static Cache.Entry parseIgnoreCacheHeaders(NetworkResponse response, long soft_expire, long expire) {

        long now = System.currentTimeMillis();
        Map<String, String> headers = response.headers;
        long serverDate = 0;
        String serverEtag = null;
        String headerValue;

        headerValue = headers.get("Date");
        if (headerValue != null) {
            serverDate = parseDateAsEpoch(headerValue);
        }

        serverEtag = headers.get("ETag");

        final long cacheHitButRefreshed = soft_expire; // in this duration cache will be hit, but also refreshed on background
        final long cacheExpired = expire; // in this duration this cache entry expires completely
        final long softExpire = now  + cacheHitButRefreshed;
        final long ttl = now + cacheExpired;

        Cache.Entry entry = new Cache.Entry();
        entry.data = response.data;
        entry.etag = serverEtag;
        entry.softTtl = softExpire;
        entry.ttl = ttl;
        entry.serverDate = serverDate;
        entry.responseHeaders = headers;

        return entry;
    }

    /**
     * Parse date in RFC1123 format, and return its value as epoch
     */
    public static long parseDateAsEpoch(String dateStr) {
        try {
            // Parse date in RFC1123 format if this header contains one
            return DateUtils.parseDate(dateStr).getTime();
        } catch (DateParseException e) {
            // Date in invalid format, fallback to 0
            return 0;
        }
    }

    /**
     * Retrieve a charset from headers
     *
     * @param headers An {@link java.util.Map} of headers
     * @param defaultCharset Charset to return if none can be found
     * @return Returns the charset specified in the Content-Type of this header,
     * or the defaultCharset if none can be found.
     */
    public static String parseCharset(Map<String, String> headers, String defaultCharset) {
        String contentType = headers.get(HTTP.CONTENT_TYPE);
        if (contentType != null) {
            String[] params = contentType.split(";");
            for (int i = 1; i < params.length; i++) {
                String[] pair = params[i].trim().split("=");
                if (pair.length == 2) {
                    if (pair[0].equals("charset")) {
                        return pair[1];
                    }
                }
            }
        }
        return defaultCharset;
    }
    /**
     * Returns the charset specified in the Content-Type of this header,
     * or the HTTP default (ISO-8859-1) if none can be found.
     */
    public static String parseCharset(Map<String, String> headers) {
        return parseCharset(headers, HTTP.DEFAULT_CONTENT_CHARSET);
    }
}