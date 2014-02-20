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

package com.android.volley.cache.efficient;

import java.io.File;
import java.io.FileNotFoundException;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyLog;
import com.android.volley.error.ParseError;
import com.android.volley.misc.Utils;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.ui.RecyclingBitmapDrawable;

/**
 * A canned request for getting an image at a given URL and calling back with a
 * decoded Bitmap.
 */
public class ImageRequestPlus extends Request<BitmapDrawable> {
	/** Socket timeout in milliseconds for image requests */
	private static final int IMAGE_TIMEOUT_MS = 1000;

	/** Default number of retries for image requests */
	private static final int IMAGE_MAX_RETRIES = 2;

	/** Default backoff multiplier for image requests */
	private static final float IMAGE_BACKOFF_MULT = 2f;

	private static final boolean PREFER_QUALITY_OVER_SPEED = false;

	private final Response.Listener<BitmapDrawable> mListener;
	private final Config mDecodeConfig;
	private final int mMaxWidth;
	private final int mMaxHeight;

	private Resources mResources;

	/**
	 * Decoding lock so that we don't decode more than one image at a time (to
	 * avoid OOM's)
	 */
	private static final Object sDecodeLock = new Object();

	/**
	 * Creates a new image request, decoding to a maximum specified width and
	 * height. If both width and height are zero, the image will be decoded to
	 * its natural size. If one of the two is nonzero, that dimension will be
	 * clamped and the other one will be set to preserve the image's aspect
	 * ratio. If both width and height are nonzero, the image will be decoded to
	 * be fit in the rectangle of dimensions width x height while keeping its
	 * aspect ratio.
	 * 
	 * @param url
	 *            URL of the image
	 * @param resources
	 *            {@link Resources} reference for parsing resource URIs. Can be
	 *            <code>null</code> if you don't need to load resource uris
	 * @param listener
	 *            Listener to receive the decoded bitmap
	 * @param maxWidth
	 *            Maximum width to decode this bitmap to, or zero for none
	 * @param maxHeight
	 *            Maximum height to decode this bitmap to, or zero for none
	 * @param decodeConfig
	 *            Format to decode the bitmap to
	 * @param errorListener
	 *            Error listener, or null to ignore errors
	 */
	public ImageRequestPlus(String url, Resources resources, Response.Listener<BitmapDrawable> listener, int maxWidth, int maxHeight, Config decodeConfig, Response.ErrorListener errorListener) {
		super(Method.GET, url, errorListener);
		setRetryPolicy(new DefaultRetryPolicy(IMAGE_TIMEOUT_MS, IMAGE_MAX_RETRIES, IMAGE_BACKOFF_MULT));

		mResources = resources;
		mListener = listener;
		mDecodeConfig = decodeConfig;
		mMaxWidth = maxWidth;
		mMaxHeight = maxHeight;
	}

	@Override
	public Priority getPriority() {
		return Priority.LOW;
	}

	/**
	 * Scales one side of a rectangle to fit aspect ratio.
	 * 
	 * @param maxPrimary
	 *            Maximum size of the primary dimension (i.e. width for max
	 *            width), or zero to maintain aspect ratio with secondary
	 *            dimension
	 * @param maxSecondary
	 *            Maximum size of the secondary dimension, or zero to maintain
	 *            aspect ratio with primary dimension
	 * @param actualPrimary
	 *            Actual size of the primary dimension
	 * @param actualSecondary
	 *            Actual size of the secondary dimension
	 */
	private static int getResizedDimension(int maxPrimary, int maxSecondary, int actualPrimary, int actualSecondary) {
		// If no dominant value at all, just return the actual.
		if (maxPrimary == 0 && maxSecondary == 0) {
			return actualPrimary;
		}

		// If primary is unspecified, scale primary to match secondary's scaling
		// ratio.
		if (maxPrimary == 0) {
			double ratio = (double) maxSecondary / (double) actualSecondary;
			return (int) (actualPrimary * ratio);
		}

		if (maxSecondary == 0) {
			return maxPrimary;
		}

		double ratio = (double) actualSecondary / (double) actualPrimary;
		int resized = maxPrimary;
		if (resized * ratio > maxSecondary) {
			resized = (int) (maxSecondary / ratio);
		}
		return resized;
	}

	@Override
	protected Response<BitmapDrawable> parseNetworkResponse(NetworkResponse response) {
		// Serialize all decode on a global lock to reduce concurrent heap
		// usage.
		synchronized (sDecodeLock) {
			try {
				if (getUrl().startsWith("file:")) {
					return doFileParse();
				} else if (getUrl().startsWith("android.resource:")) {
					return doResourceParse();
				} else {
					return doParse(response);
				}
			} catch (OutOfMemoryError e) {
				VolleyLog.e("Caught OOM for %d byte image, url=%s", response.data.length, getUrl());
				return Response.error(new ParseError(e));
			}
		}
	}

	/**
	 * The real guts of parseNetworkResponse. Broken out for readability.
	 * 
	 * This version is for reading a Bitmap from file
	 */
	private Response<BitmapDrawable> doFileParse() {

		final String requestUrl = getUrl();
		// Remove the 'file://' prefix
		File bitmapFile = new File(requestUrl.substring(7, requestUrl.length()));

		if (!bitmapFile.exists() || !bitmapFile.isFile()) {
			return Response.error(new ParseError(new FileNotFoundException(String.format("File not found: %s", bitmapFile.getAbsolutePath()))));
		}

		BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
		decodeOptions.inInputShareable = true;
		decodeOptions.inPurgeable = true;
		decodeOptions.inPreferredConfig = mDecodeConfig;
		Bitmap bitmap = null;
		if (mMaxWidth == 0 && mMaxHeight == 0) {

			bitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), decodeOptions);
			addMarker("read-full-size-image-from-file");
		} else {
			// If we have to resize this image, first get the natural bounds.
			decodeOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), decodeOptions);
			int actualWidth = decodeOptions.outWidth;
			int actualHeight = decodeOptions.outHeight;

			// Then compute the dimensions we would ideally like to decode to.
			int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight, actualWidth, actualHeight);
			int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth, actualHeight, actualWidth);

			// Decode to the nearest power of two scaling factor.
			decodeOptions.inJustDecodeBounds = false;
			decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
			Bitmap tempBitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath(), decodeOptions);
			addMarker(String.format("read-from-file-scaled-times-%d", decodeOptions.inSampleSize));
			// If necessary, scale down to the maximal acceptable size.
			if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
				bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
				tempBitmap.recycle();
				addMarker("scaling-read-from-file-bitmap");
			} else {
				bitmap = tempBitmap;
			}

		}

		if (bitmap == null) {
			return Response.error(new ParseError());
		} else {
			BitmapDrawable drawable;
			if (Utils.hasHoneycomb()) {
				// Running on Honeycomb or newer, so wrap in a standard
				// BitmapDrawable
				drawable = new BitmapDrawable(mResources, bitmap);
			} else {
				// Running on Gingerbread or older, so wrap in a
				// RecyclingBitmapDrawable
				// which will recycle automagically
				drawable = new RecyclingBitmapDrawable(mResources, bitmap);
			}
			return Response.success(drawable, null);
		}
	}

	/**
	 * The real guts of parseNetworkResponse. Broken out for readability.
	 * 
	 * This version is for reading a Bitmap from resource
	 */
	private Response<BitmapDrawable> doResourceParse() {

		if (mResources == null) {
			return Response.error(new ParseError());// "Resources instance is null"));
		}
		final String requestUrl = getUrl();
		final int resourceId = Integer.valueOf(Uri.parse(requestUrl).getLastPathSegment());

		BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
		decodeOptions.inInputShareable = true;
		decodeOptions.inPurgeable = true;
		decodeOptions.inPreferredConfig = mDecodeConfig;
		Bitmap bitmap = null;
		if (mMaxWidth == 0 && mMaxHeight == 0) {

			bitmap = BitmapFactory.decodeResource(mResources, resourceId, decodeOptions);
			addMarker("read-full-size-image-from-resource");
		} else {
			// If we have to resize this image, first get the natural bounds.
			decodeOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeResource(mResources, resourceId, decodeOptions);
			int actualWidth = decodeOptions.outWidth;
			int actualHeight = decodeOptions.outHeight;

			// Then compute the dimensions we would ideally like to decode to.
			int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight, actualWidth, actualHeight);
			int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth, actualHeight, actualWidth);

			// Decode to the nearest power of two scaling factor.
			decodeOptions.inJustDecodeBounds = false;

			decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
			Bitmap tempBitmap = BitmapFactory.decodeResource(mResources, resourceId, decodeOptions);
			addMarker(String.format("read-from-resource-scaled-times-%d", decodeOptions.inSampleSize));
			// If necessary, scale down to the maximal acceptable size.
			if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
				bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
				tempBitmap.recycle();
				addMarker("scaling-read-from-resource-bitmap");
			} else {
				bitmap = tempBitmap;
			}

		}

		if (bitmap == null) {
			return Response.error(new ParseError());
		} else {
			BitmapDrawable drawable;
			if (Utils.hasHoneycomb()) {
				// Running on Honeycomb or newer, so wrap in a standard
				// BitmapDrawable
				drawable = new BitmapDrawable(mResources, bitmap);
			} else {
				// Running on Gingerbread or older, so wrap in a
				// RecyclingBitmapDrawable
				// which will recycle automagically
				drawable = new RecyclingBitmapDrawable(mResources, bitmap);
			}
			return Response.success(drawable, null);
		}
	}

	/**
	 * The real guts of parseNetworkResponse. Broken out for readability.
	 */
	@TargetApi(Build.VERSION_CODES.GINGERBREAD_MR1)
	private Response<BitmapDrawable> doParse(NetworkResponse response) {
		byte[] data = response.data;
		BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
		Bitmap bitmap = null;
		if (mMaxWidth == 0 && mMaxHeight == 0) {
			decodeOptions.inPreferredConfig = mDecodeConfig;
			bitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
		} else {
			// If we have to resize this image, first get the natural bounds.
			decodeOptions.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);
			int actualWidth = decodeOptions.outWidth;
			int actualHeight = decodeOptions.outHeight;

			// Then compute the dimensions we would ideally like to decode to.
			int desiredWidth = getResizedDimension(mMaxWidth, mMaxHeight, actualWidth, actualHeight);
			int desiredHeight = getResizedDimension(mMaxHeight, mMaxWidth, actualHeight, actualWidth);

			// Decode to the nearest power of two scaling factor.
			decodeOptions.inJustDecodeBounds = false;
			// TODO(ficus): Do we need this or is it okay since API 8 doesn't
			// support it?
			if (Utils.hasGingerbreadMR1()) {
				decodeOptions.inPreferQualityOverSpeed = PREFER_QUALITY_OVER_SPEED;
			}

			decodeOptions.inSampleSize = findBestSampleSize(actualWidth, actualHeight, desiredWidth, desiredHeight);
			Bitmap tempBitmap = BitmapFactory.decodeByteArray(data, 0, data.length, decodeOptions);

			// If necessary, scale down to the maximal acceptable size.
			if (tempBitmap != null && (tempBitmap.getWidth() > desiredWidth || tempBitmap.getHeight() > desiredHeight)) {
				bitmap = Bitmap.createScaledBitmap(tempBitmap, desiredWidth, desiredHeight, true);
				tempBitmap.recycle();
			} else {
				bitmap = tempBitmap;
			}
		}

		if (bitmap == null) {
			return Response.error(new ParseError());
		} else {
			BitmapDrawable drawable;
			if (Utils.hasHoneycomb()) {
				// Running on Honeycomb or newer, so wrap in a standard
				// BitmapDrawable
				drawable = new BitmapDrawable(mResources, bitmap);
			} else {
				// Running on Gingerbread or older, so wrap in a
				// RecyclingBitmapDrawable
				// which will recycle automagically
				drawable = new RecyclingBitmapDrawable(mResources, bitmap);
			}
			return Response.success(drawable, HttpHeaderParser.parseIgnoreCacheHeaders(response));
		}
	}

	@Override
	protected void deliverResponse(BitmapDrawable response) {
		mListener.onResponse(response);
	}

	/**
	 * Returns the largest power-of-two divisor for use in downscaling a bitmap
	 * that will not result in the scaling past the desired dimensions.
	 * 
	 * @param actualWidth
	 *            Actual width of the bitmap
	 * @param actualHeight
	 *            Actual height of the bitmap
	 * @param desiredWidth
	 *            Desired width of the bitmap
	 * @param desiredHeight
	 *            Desired height of the bitmap
	 */
	// Visible for testing.
	static int findBestSampleSize(int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
		double wr = (double) actualWidth / desiredWidth;
		double hr = (double) actualHeight / desiredHeight;
		double ratio = Math.min(wr, hr);
		float n = 1.0f;
		while ((n * 2) <= ratio) {
			n *= 2;
		}

		return (int) n;
	}

	/**
	 * Calculate the closest inSampleSize that is a power of 2 and will result
	 * in the final decoded bitmap having a width and height equal to or larger
	 * than the requested width and height.
	 * 
	 * @param actualHeight
	 *            Actual height of the bitmap
	 * @param actualWidth
	 *            Actual width of the bitmap
	 * @param reqWidth
	 *            The requested width of the resulting bitmap
	 * @param reqHeight
	 *            The requested height of the resulting bitmap
	 * @return The value to be used for inSampleSize
	 */
	public static int findSampleSize(int actualHeight, int actualWidth, int reqWidth, int reqHeight) {
		// Raw height and width of image
		int inSampleSize = 1;

		if (actualHeight > reqHeight || actualWidth > reqWidth) {

			final int halfHeight = actualHeight / 2;
			final int halfWidth = actualWidth / 2;

			// Calculate the largest inSampleSize value that is a power of 2 and
			// keeps both
			// height and width larger than the requested height and width.
			while ((halfHeight / inSampleSize) > reqHeight && (halfWidth / inSampleSize) > reqWidth) {
				inSampleSize *= 2;
			}

			// This offers some additional logic in case the image has a strange
			// aspect ratio. For example, a panorama may have a much larger
			// width than height. In these cases the total pixels might still
			// end up being too large to fit comfortably in memory, so we should
			// be more aggressive with sample down the image (=larger
			// inSampleSize).

			long totalPixels = actualWidth * actualHeight / inSampleSize;

			// Anything more than 2x the requested pixels we'll sample down
			// further
			final long totalReqPixelsCap = reqWidth * reqHeight * 2;

			while (totalPixels > totalReqPixelsCap) {
				inSampleSize *= 2;
				totalPixels /= 2;
			}
		}
		return inSampleSize;
	}

	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private static void addInBitmapOptions(BitmapFactory.Options options, EfficientImageCache cache) {
		// inBitmap only works with mutable bitmaps so force the decoder to
		// return mutable bitmaps.
		options.inMutable = true;

		if (cache != null) {
			// Try and find a bitmap to use for inBitmap
			Bitmap inBitmap = cache.getBitmapFromReusableSet(options);

			if (inBitmap != null) {
				options.inBitmap = inBitmap;
			}
		}
	}

}
