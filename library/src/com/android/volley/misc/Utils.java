/*
 * Copyright (C) 2012 The Android Open Source Project
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

package com.android.volley.misc;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.os.StrictMode;
import android.text.TextUtils;
import android.util.Log;

import org.apache.http.Header;
import org.apache.http.HttpResponse;

/**
 * Class containing some static utility methods.
 */
public class Utils {

	public static final int ANIMATION_FADE_IN_TIME = 200;
    public static final String SCHEME_FILE = ContentResolver.SCHEME_FILE;
	public static final String SCHEME_CONTENT = ContentResolver.SCHEME_CONTENT;
	public static final String SCHEME_ANDROID_RESOURCE = ContentResolver.SCHEME_ANDROID_RESOURCE;
    public static final String SCHEME_VIDEO = "video";
    public static final String SCHEME_ASSETS = "asset";

    private Utils() {
	};

	/**
	 * Interface for components that are internally scrollable left-to-right.
	 */
	public static interface HorizontallyScrollable {
		/**
		 * Return {@code true} if the component needs to receive right-to-left
		 * touch movements.
		 * 
		 * @param origX
		 *            the raw x coordinate of the initial touch
		 * @param origY
		 *            the raw y coordinate of the initial touch
		 */

		public boolean interceptMoveLeft(float origX, float origY);

		/**
		 * Return {@code true} if the component needs to receive left-to-right
		 * touch movements.
		 * 
		 * @param origX
		 *            the raw x coordinate of the initial touch
		 * @param origY
		 *            the raw y coordinate of the initial touch
		 */
		public boolean interceptMoveRight(float origX, float origY);
	}

	@TargetApi(11)
	public static void enableStrictMode() {
		if (Utils.hasGingerbread()) {
			StrictMode.ThreadPolicy.Builder threadPolicyBuilder = new StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog();
			StrictMode.VmPolicy.Builder vmPolicyBuilder = new StrictMode.VmPolicy.Builder().detectAll().penaltyLog();

			if (Utils.hasHoneycomb()) {
				threadPolicyBuilder.penaltyFlashScreen();
			}
			StrictMode.setThreadPolicy(threadPolicyBuilder.build());
			StrictMode.setVmPolicy(vmPolicyBuilder.build());
		}
	}

	public static boolean hasFroyo() {
		// Can use static final constants like FROYO, declared in later versions
		// of the OS since they are inlined at compile time. This is guaranteed
		// behavior.
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO;
	}

	public static boolean hasGingerbread() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD;
	}

	public static boolean hasGingerbreadMR1() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD_MR1;
	}

	public static boolean hasHoneycomb() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB;
	}

	public static boolean hasHoneycombMR1() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR1;
	}

	public static boolean hasJellyBean() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN;
	}

	public static boolean hasJellyBeanMR2() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2;
	}

	public static boolean hasKitKat() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
	}

	/**
	 * Check how much usable space is available at a given path.
	 * 
	 * @param path
	 *            The path to check
	 * @return The space available in bytes
	 */
	@SuppressWarnings("deprecation")
	@TargetApi(9)
	public static long getUsableSpace(File path) {
		if (Utils.hasGingerbread()) {
			return path.getUsableSpace();
		}
		final StatFs stats = new StatFs(path.getPath());
		return (long) stats.getBlockSize() * (long) stats.getAvailableBlocks();
	}

	/*
	 * private File getDiskCacheDir(Context context, String uniqueName) { final
	 * String cachePath = context.getCacheDir().getPath(); return new
	 * File(cachePath + File.separator + uniqueName); }
	 */

	/**
	 * Get a usable cache directory (external if available, internal otherwise).
	 * 
	 * @param context
	 *            The context to use
	 * @param uniqueName
	 *            A unique directory name to append to the cache dir
	 * @return The cache dir
	 */
	public static File getDiskCacheDir(Context context, String uniqueName) {
		// Check if media is mounted or storage is built-in, if so, try and use
		// external cache dir
		// otherwise use internal cache dir

		// TODO: getCacheDir() should be moved to a background thread as it
		// attempts to create the
		// directory if it does not exist (no disk access should happen on the
		// main/UI thread).
		final String cachePath;
		if (isExternalMounted() && null != getExternalCacheDir(context)) {
			cachePath = getExternalCacheDir(context).getPath();
		} else {
			cachePath = context.getCacheDir().getPath();
		}

		Log.i("Cache dir", cachePath + File.separator + uniqueName);
		return new File(cachePath + File.separator + uniqueName);
	}

	/**
	 * Get the external app cache directory.
	 * 
	 * @param context
	 *            The context to use
	 * @return The external cache dir
	 */
	private static File getExternalCacheDir(Context context) {
		// TODO: This needs to be moved to a background thread to ensure no disk
		// access on the
		// main/UI thread as unfortunately getExternalCacheDir() calls mkdirs()
		// for us (even
		// though the Volley library will later try and call mkdirs() as well
		// from a background
		// thread).
		return context.getExternalCacheDir();
	}

	@SuppressLint("NewApi")
	private static boolean isExternalMounted() {
		if (Utils.hasGingerbread()) {
			return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) || !Environment.isExternalStorageRemovable();
		}
		return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
	}

	public static final Charset US_ASCII = Charset.forName("US-ASCII");
	public static final Charset UTF_8 = Charset.forName("UTF-8");

	public static String readFully(Reader reader) throws IOException {
		try {
			StringWriter writer = new StringWriter();
			char[] buffer = new char[1024];
			int count;
			while ((count = reader.read(buffer)) != -1) {
				writer.write(buffer, 0, count);
			}
			return writer.toString();
		} finally {
			reader.close();
		}
	}

	/**
	 * Deletes the contents of {@code dir}. Throws an IOException if any file
	 * could not be deleted, or if {@code dir} is not a readable directory.
	 */
	public static void deleteContents(File dir) throws IOException {
		File[] files = dir.listFiles();
		if (files == null) {
			throw new IOException("not a readable directory: " + dir);
		}
		for (File file : files) {
			if (file.isDirectory()) {
				deleteContents(file);
			}
			if (!file.delete()) {
				throw new IOException("failed to delete file: " + file);
			}
		}
	}

	public static void closeQuietly(/* Auto */Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (RuntimeException rethrown) {
				throw rethrown;
			} catch (Exception ignored) {
			}
		}
	}
	
	public static boolean isSpecialType(String url){
		boolean isSpecial = url.startsWith(SCHEME_FILE)
		|| url.startsWith(SCHEME_VIDEO)
		|| url.startsWith(SCHEME_CONTENT)
		|| url.startsWith(SCHEME_ANDROID_RESOURCE);
		return isSpecial;
	}

    public static String getSchemeBaseUrl(String type, String url){
        return type + "://" + url;
    }

    public static String getSchemeBaseUrl(String type, int id){
        return type + "://" + id;
    }

    public static String getHeader(HttpResponse response, String key) {
        Header header = response.getFirstHeader(key);
        return header == null ? null : header.getValue();
    }

    public static boolean isSupportRange(HttpResponse response) {
        if (TextUtils.equals(getHeader(response, "Accept-Ranges"), "bytes")) {
            return true;
        }
        String value = getHeader(response, "Content-Range");
        return value != null && value.startsWith("bytes");
    }

    public static boolean isGzipContent(HttpResponse response) {
        return TextUtils.equals(getHeader(response, "Content-Encoding"), "gzip");
    }
}