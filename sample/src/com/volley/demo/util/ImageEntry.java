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

package com.volley.demo.util;

import com.volley.demo.ExampleNetworkListView;

/**
 * Holds the data for Picasa photo that is used in the
 * {@link ExampleNetworkListView}
 * 
 * @author Ognyan Bankov (ognyan.bankov@bulpros.com)
 * 
 */
public class ImageEntry {
	private String mTitle;
	private String mThumbnailUrl;

	public ImageEntry(String title, String thumbnailUrl) {
		super();
		mTitle = title;
		mThumbnailUrl = thumbnailUrl;
	}

	public String getTitle() {
		return mTitle;
	}

	public String getThumbnailUrl() {
		return mThumbnailUrl;
	}
}
