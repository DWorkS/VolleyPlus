VolleyPlus
==========

<a href="https://www.buymeacoffee.com/1hakr" target="_blank"><img src="https://www.buymeacoffee.com/assets/img/custom_images/yellow_img.png" alt="Buy Me A Coffee" height='50' style='border:0px;height:50px;' ></a>

![VolleyPlus](https://github.com/DWorkS/VolleyPlus/raw/master/header.png)

**VolleyPlus** library Project improvements to Volley along with full image caching.It involves using **RequestQueue**, **RequestTickle** and **Request**.
* `RequestQueue` - Dispatch Queue which takes a Request and executes in a worker thread or if cache found its takes from cache and responds back to the UI main thread.
* `RequestTickle` - A single class which takes a Request and executes in same thread or if cache found its takes from cache and responds back to the same thread. Mainly useful in sync operations where you want to perform operations sequentially.
* `Request` - All network(HTTP) requests are created from this class. It takes main parameters required for a HTTP request like
	* METHOD Type - GET, POST, PUT, DELETE, HEAD, OPTIONS, TRACE, PATCH
	* URL 
	* Headers
	* Parameters
	* Retry Policy
	* Request Priority (RequestQueue)
	* Request data (HTTP Body)
	* Request Cancellation
	* Response Caching
	* Successful Response Listener
	* Error Listener  
	* Progress Listener (for few Request types)

**VolleyPlus** Provides variety of implementations of **Request**.    
* StringRequest
* JsonRequest
* JsonObjectRequest
* JsonArrayRequest
* GsonRequest
* GZipRequest
* MultiPartRequest/SimpleMultiPartRequest
```
    SimpleMultipartRequest request = new SimpleMultipartRequest(Method.POST, apiUrl, mListener, mErrorListener);
    request.addFile("photo", imagePath);
    request.addMultipartParam("body", "text/plain", "some text");

    RequestQueue mRequestQueue = Volley.newRequestQueue(getApplicationContext());
    mRequestQueue.add(request);
    mRequestQueue.start();
```
* DownloadRequest
* ImageRequest

**VolleyPlus** has also very powerful image caching **SimpleImageLoder**.
* **DiskLruBasedCache** based on **DiskLruCache** for Level2 (L2) cache
* Supports `NewtworkImageView` usage with `SimpleImageLoader`
* Can also update the cache
* Provides option to flush, close and clear the cache in both L1 and L2 cache
* Supported types of Image Caching
    *   Network Caching
    *   Resource Caching
    *   File Caching
    *   Video Caching
    *   Content URI Caching


## Usage
**RequestQueue**
```
RequestQueue mRequestQueue = Volley.newRequestQueue(getApplicationContext());

StringRequest stringRequest = new StringRequest(Request.Method.GET, url, new Response.Listener<String>() {
    @Override
    public void onResponse(String response) {
    	....
    }
}, new Response.ErrorListener() {
    @Override
    public void onErrorResponse(VolleyError error) {
    	....
    }
});

mRequestQueue.add(stringRequest);
```

**RequestTickle**
```
RequestTickle mRequestTickle = VolleyTickle.newRequestTickle(getApplicationContext());

StringRequest stringRequest = new StringRequest(Request.Method.GET, url, null, null);
mRequestTickle.add(stringRequest);
NetworkResponse response = mRequestTickle.start();

if (response.statusCode == 200) {
	String data = VolleyTickle.parseResponse(response);
	....
}
else{
	....
}

```

**SimpleImageLoader**
```
ImageCacheParams cacheParams = new ImageCacheParams(getApplicationContext(), "CacheDirectory");
cacheParams.setMemCacheSizePercent(0.5f);

SimpleImageLoader mImageFetcher = new SimpleImageLoader(getApplicationContext(), R.drawable.holder_image, cacheParams);
mImageFetcher.setMaxImageSize(300);
....

mImageFetcher.get(url, image_view);

OR

network_image_view.setImageUrl(url, mImageFetcher);
network_image_view.setDefaultImageResId(R.drawable.holder_image);

```

## Quick Start

Volley is available as an AAR, so you just need to add the following dependency to your `build.gradle`.
```
buildscript {
    repositories {
        jcenter()
    }
}
...

dependencies {
     compile 'dev.dworks.libs:volleyplus:+'
}
...
```

## Sample App

[![Get it on Google Play](http://www.android.com/images/brand/get_it_on_play_logo_small.png)](http://play.google.com/store/apps/details?id=com.volley.demo)


Developed By
============

* Hari Krishna Dulipudi - <hakr@dworks.in>


License
=======

    Copyright 2017 Hari Krishna Dulipudi
    Copyright (C) 2011 The Android Open Source Project

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.

