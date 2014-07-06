package com.android.volley.misc;

import java.io.ByteArrayInputStream;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.regex.Pattern;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.net.Uri;
import android.util.Base64;
import android.util.Log;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    /** Minimum class memory class to use full-res photos */
    @SuppressWarnings("unused")
	private final static long MIN_NORMAL_CLASS = 32;
    /** Minimum class memory class to use small photos */
    @SuppressWarnings("unused")
	private final static long MIN_SMALL_CLASS = 24;

    private static final String BASE64_URI_PREFIX = "base64,";
    private static final Pattern BASE64_IMAGE_URI_PATTERN = Pattern.compile("^(?:.*;)?base64,.*");
    
    /**
     * Returns the largest power-of-two divisor for use in downscaling a bitmap
     * that will not result in the scaling past the desired dimensions.
     *
     * @param actualWidth Actual width of the bitmap
     * @param actualHeight Actual height of the bitmap
     * @param desiredWidth Desired width of the bitmap
     * @param desiredHeight Desired height of the bitmap
     */
    // Visible for testing.
    public static int findBestSampleSize(
            int actualWidth, int actualHeight, int desiredWidth, int desiredHeight) {
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
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     *
     * @param fileDescriptor The file descriptor to read from
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decodeSampledBitmapFromDescriptor(
            FileDescriptor fileDescriptor, int reqWidth, int reqHeight) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }
    
    /**
     * Decode and sample down a bitmap from a file input stream to the requested width and height.
     *
     * @param fileDescriptor The file descriptor to read from
     * @return A bitmap sampled down from the original with the same aspect ratio and dimensions
     *         that are equal to or greater than the requested width and height
     */
    public static Bitmap decodeSampledBitmapFromDescriptor(FileDescriptor fileDescriptor) {

        // First decode with inJustDecodeBounds=true to check dimensions
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);

        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false;

        return BitmapFactory.decodeFileDescriptor(fileDescriptor, null, options);
    }
    
    /**
     * Calculate an inSampleSize for use in a {@link BitmapFactory.Options} object when decoding
     * bitmaps using the decode* methods from {@link BitmapFactory}. This implementation calculates
     * the closest inSampleSize that is a power of 2 and will result in the final decoded bitmap
     * having a width and height equal to or larger than the requested width and height.
     *
     * @param options An options object with out* params already populated (run through a decode*
     *            method with inJustDecodeBounds==true
     * @param reqWidth The requested width of the resulting bitmap
     * @param reqHeight The requested height of the resulting bitmap
     * @return The value to be used for inSampleSize
     */
    public static int calculateInSampleSize(BitmapFactory.Options options,
            int reqWidth, int reqHeight) {
        // Raw height and width of image
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {

            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
            // height and width larger than the requested height and width.
            while ((halfHeight / inSampleSize) > reqHeight
                    && (halfWidth / inSampleSize) > reqWidth) {
                inSampleSize *= 2;
            }

            // This offers some additional logic in case the image has a strange
            // aspect ratio. For example, a panorama may have a much larger
            // width than height. In these cases the total pixels might still
            // end up being too large to fit comfortably in memory, so we should
            // be more aggressive with sample down the image (=larger inSampleSize).

            long totalPixels = width * height / inSampleSize;

            // Anything more than 2x the requested pixels we'll sample down further
            final long totalReqPixelsCap = reqWidth * reqHeight * 2;

            while (totalPixels > totalReqPixelsCap) {
                inSampleSize *= 2;
                totalPixels /= 2;
            }
        }
        return inSampleSize;
    }
    

    /**
     * @return true if the MimeType type is image
     */
    public static boolean isImageMimeType(String mimeType) {
        return mimeType != null && mimeType.startsWith("image/");
    }

    /**
     * Create a bitmap from a local URI
     *
     * @param resolver The ContentResolver
     * @param uri      The local URI
     * @param maxSize  The maximum size (either width or height)
     * @return The new bitmap or null
     */
    public static Bitmap decodeStream(final ContentResolver resolver, final Uri uri,
            final int maxSize) {
        Bitmap result = null;
        final InputStreamFactory factory = createInputStreamFactory(resolver, uri);
        try {
            final Point bounds = getImageBounds(factory);
            if (bounds == null) {
                return result;
            }

            final BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = Math.max(bounds.x / maxSize, bounds.y / maxSize);
            result = decodeStream(factory, null, opts);
            return result;

        } catch (FileNotFoundException exception) {
            // Do nothing - the photo will appear to be missing
        } catch (IOException exception) {
        } catch (IllegalArgumentException exception) {
            // Do nothing - the photo will appear to be missing
        } catch (SecurityException exception) {
        }
        return result;
    }
    
    /**
     * Create a bitmap from a local URI
     *
     * @param resolver The ContentResolver
     * @param uri      The local URI
     * @param maxSize  The maximum size (either width or height)
     * @return The new bitmap or null
     */
    public static Bitmap decodeStream(final ContentResolver resolver, final Uri uri,
    		BitmapFactory.Options opts) {
        Bitmap result = null;
        final InputStreamFactory factory = createInputStreamFactory(resolver, uri);
        try {
            result = decodeStream(factory, null, opts);
            return result;

        } catch (FileNotFoundException exception) {
            // Do nothing - the photo will appear to be missing
        } catch (IllegalArgumentException exception) {
            // Do nothing - the photo will appear to be missing
        } catch (SecurityException exception) {
        }
        return result;
    }
    
    /**
     * Wrapper around {@link BitmapFactory#decodeStream(InputStream, Rect,
     * BitmapFactory.Options)} that returns {@code null} on {@link
     * OutOfMemoryError}.
     *
     * @param is The input stream that holds the raw data to be decoded into a
     *           bitmap.
     * @param outPadding If not null, return the padding rect for the bitmap if
     *                   it exists, otherwise set padding to [-1,-1,-1,-1]. If
     *                   no bitmap is returned (null) then padding is
     *                   unchanged.
     * @param opts null-ok; Options that control downsampling and whether the
     *             image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     *         decoded, or, if opts is non-null, if opts requested only the
     *         size be returned (in opts.outWidth and opts.outHeight)
     */
    public static Bitmap decodeStream(InputStream is, Rect outPadding, BitmapFactory.Options opts) {
        try {
            return BitmapFactory.decodeStream(is, outPadding, opts);
        } catch (OutOfMemoryError oome) {
            Log.e(TAG, "ImageUtils#decodeStream(InputStream, Rect, Options) threw an OOME", oome);
            return null;
        }
    }
    
    /**
     * Wrapper around {@link BitmapFactory#decodeStream(InputStream, Rect,
     * BitmapFactory.Options)} that returns {@code null} on {@link
     * OutOfMemoryError}.
     *
     * @param factory    Used to create input streams that holds the raw data to be decoded into a
     *                   bitmap.
     * @param outPadding If not null, return the padding rect for the bitmap if
     *                   it exists, otherwise set padding to [-1,-1,-1,-1]. If
     *                   no bitmap is returned (null) then padding is
     *                   unchanged.
     * @param opts       null-ok; Options that control downsampling and whether the
     *                   image should be completely decoded, or just is size returned.
     * @return The decoded bitmap, or null if the image data could not be
     * decoded, or, if opts is non-null, if opts requested only the
     * size be returned (in opts.outWidth and opts.outHeight)
     */
    public static Bitmap decodeStream(final InputStreamFactory factory, final Rect outPadding,
            final BitmapFactory.Options opts) throws FileNotFoundException {
        InputStream is = null;
        try {
            // Determine the orientation for this image
            is = factory.createInputStream();
            final int orientation = Exif.getOrientation(is, -1);
            is.close();

            // Decode the bitmap
            is = factory.createInputStream();
            final Bitmap originalBitmap = BitmapFactory.decodeStream(is, outPadding, opts);

            if (is != null && originalBitmap == null && !opts.inJustDecodeBounds) {
                Log.w(TAG, "ImageUtils#decodeStream(InputStream, Rect, Options): "
                        + "Image bytes cannot be decoded into a Bitmap");
                throw new UnsupportedOperationException(
                        "Image bytes cannot be decoded into a Bitmap.");
            }

            // Rotate the Bitmap based on the orientation
            if (originalBitmap != null && orientation != 0) {
                final Matrix matrix = new Matrix();
                matrix.postRotate(orientation);
                return Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.getWidth(),
                        originalBitmap.getHeight(), matrix, true);
            }
            return originalBitmap;
        } catch (OutOfMemoryError oome) {
            Log.e(TAG, "ImageUtils#decodeStream(InputStream, Rect, Options) threw an OOME", oome);
            return null;
        } catch (IOException ioe) {
            Log.e(TAG, "ImageUtils#decodeStream(InputStream, Rect, Options) threw an IOE", ioe);
            return null;
        } finally {
            if (is != null) {
                try {
                    is.close();
                } catch (IOException e) {
                    // Do nothing
                }
            }
        }
    }

    /**
     * Gets the image bounds
     *
     * @param factory Used to create the InputStream.
     *
     * @return The image bounds
     */
    public static Point getImageBounds(final InputStreamFactory factory)
            throws IOException {
        final BitmapFactory.Options opts = new BitmapFactory.Options();
        opts.inJustDecodeBounds = true;
        decodeStream(factory, null, opts);

        return new Point(opts.outWidth, opts.outHeight);
    }
    

    public static InputStreamFactory createInputStreamFactory(final ContentResolver resolver,
            final Uri uri) {
        final String scheme = uri.getScheme();
        if ("data".equals(scheme)) {
            return new DataInputStreamFactory(resolver, uri);
        }
        return new BaseInputStreamFactory(resolver, uri);
    }

    /**
     * Utility class for when an InputStream needs to be read multiple times. For example, one pass
     * may load EXIF orientation, and the second pass may do the actual Bitmap decode.
     */
    public interface InputStreamFactory {

        /**
         * Create a new InputStream. The caller of this method must be able to read the input
         * stream starting from the beginning.
         * @return
         */
        InputStream createInputStream() throws FileNotFoundException;
    }

    private static class BaseInputStreamFactory implements InputStreamFactory {
        protected final ContentResolver mResolver;
        protected final Uri mUri;

        public BaseInputStreamFactory(final ContentResolver resolver, final Uri uri) {
            mResolver = resolver;
            mUri = uri;
        }

        @Override
        public InputStream createInputStream() throws FileNotFoundException {
            return mResolver.openInputStream(mUri);
        }
    }

    private static class DataInputStreamFactory extends BaseInputStreamFactory {
        private byte[] mData;

        public DataInputStreamFactory(final ContentResolver resolver, final Uri uri) {
            super(resolver, uri);
        }

        @Override
        public InputStream createInputStream() throws FileNotFoundException {
            if (mData == null) {
                mData = parseDataUri(mUri);
                if (mData == null) {
                    return super.createInputStream();
                }
            }
            return new ByteArrayInputStream(mData);
        }

        private byte[] parseDataUri(final Uri uri) {
            final String ssp = uri.getSchemeSpecificPart();
            try {
                if (ssp.startsWith(BASE64_URI_PREFIX)) {
                    final String base64 = ssp.substring(BASE64_URI_PREFIX.length());
                    return Base64.decode(base64, Base64.URL_SAFE);
                } else if (BASE64_IMAGE_URI_PATTERN.matcher(ssp).matches()){
                    final String base64 = ssp.substring(
                            ssp.indexOf(BASE64_URI_PREFIX) + BASE64_URI_PREFIX.length());
                    return Base64.decode(base64, Base64.DEFAULT);
                } else {
                    return null;
                }
            } catch (IllegalArgumentException ex) {
                Log.e(TAG, "Mailformed data URI: " + ex);
                return null;
            }
        }
    }
}