VolleyPlus
==========
![VolleyPlus](https://github.com/DWorkS/VolleyPlus/raw/master/header.png)

**VolleyPlus** library Project improvements to Volley along with full image caching.

VolleyPlus involves using **RequestQueue**, **RequestTickle** and **Request**.
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
* GZipRequest
* MultiPartRequest
* SimpleMultiPartRequest
* DownloadRequest
* ImageRequest

VolleyPlus has also very powerful image caching **SimpleImageLoder**.
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



## Sample App

[![Get it on Google Play](http://www.android.com/images/brand/get_it_on_play_logo_small.png)](http://play.google.com/store/apps/details?id=com.volley.demo)


Developed By
============

* Hari Krishna Dulipudi - <hakr@dworks.in>


License
=======

    Copyright 2014 Hari Krishna Dulipudi
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

