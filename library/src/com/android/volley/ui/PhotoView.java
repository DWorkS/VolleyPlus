/*
 * Copyright (C) 2011 Google Inc.
 * Licensed to The Android Open Source Project.
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

package com.android.volley.ui;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v4.view.ScaleGestureDetectorCompat;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.GestureDetector.OnDoubleTapListener;
import android.view.GestureDetector.OnGestureListener;
import android.view.ViewGroup.LayoutParams;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.ViewConfiguration;

import com.android.volley.R;
import com.android.volley.error.VolleyError;
import com.android.volley.misc.Utils.HorizontallyScrollable;
import com.android.volley.toolbox.ImageLoader.ImageContainer;
import com.android.volley.toolbox.ImageLoader.ImageListener;

/**
 * Layout for the photo list view header.
 */
public class PhotoView extends NetworkImageView implements OnGestureListener,
        OnDoubleTapListener, ScaleGestureDetector.OnScaleGestureListener,
        HorizontallyScrollable {
    /** Zoom animation duration; in milliseconds */
    private final static long ZOOM_ANIMATION_DURATION = 300L;
    /** Rotate animation duration; in milliseconds */
    private final static long ROTATE_ANIMATION_DURATION = 500L;
    /** Snap animation duration; in milliseconds */
    private static final long SNAP_DURATION = 100L;
    /** Amount of time to wait before starting snap animation; in milliseconds */
    private static final long SNAP_DELAY = 250L;
    /** By how much to scale the image when double click occurs */
    private final static float DOUBLE_TAP_SCALE_FACTOR = 1.5f;
    /** Amount of translation needed before starting a snap animation */
    private final static float SNAP_THRESHOLD = 20.0f;
    /** The width & height of the bitmap returned by {@link #getCroppedPhoto()} */
    private final static float CROPPED_SIZE = 256.0f;

    /**
     * Touch slop used to determine if this double tap is valid for starting a scale or should be
     * ignored.
     */
    private static int sTouchSlopSquare;

    /** If {@code true}, the static values have been initialized */
    private static boolean sInitialized;

    // Various dimensions
    /** Width & height of the crop region */
    private static int sCropSize;

    // Bitmaps
    /** Video icon */
    private static Bitmap sVideoImage;
    /** Video icon */
    private static Bitmap sVideoNotReadyImage;

    // Paints
    /** Paint to partially dim the photo during crop */
    private static Paint sCropDimPaint;
    /** Paint to highlight the cropped portion of the photo */
    private static Paint sCropPaint;

    /** The photo to display */
    private Drawable mDrawable;
    /** The matrix used for drawing; this may be {@code null} */
    private Matrix mDrawMatrix;
    /** A matrix to apply the scaling of the photo */
    private Matrix mMatrix = new Matrix();
    /** The original matrix for this image; used to reset any transformations applied by the user */
    private Matrix mOriginalMatrix = new Matrix();

    /** The fixed height of this view. If {@code -1}, calculate the height */
    private int mFixedHeight = -1;
    /** When {@code true}, the view has been laid out */
    private boolean mHaveLayout;
    /** Whether or not the photo is full-screen */
    private boolean mFullScreen;
    /** Whether or not this is a still image of a video */
    private byte[] mVideoBlob;
    /** Whether or not this is a still image of a video */
    private boolean mVideoReady;

    /** Whether or not crop is allowed */
    private boolean mAllowCrop;
    /** The crop region */
    private Rect mCropRect = new Rect();
    /** Actual crop size; may differ from {@link #sCropSize} if the screen is smaller */
    private int mCropSize;
    /** The maximum amount of scaling to apply to images */
    private float mMaxInitialScaleFactor = 2;

    /** Gesture detector */
    private GestureDetectorCompat mGestureDetector;
    /** Gesture detector that detects pinch gestures */
    private ScaleGestureDetector mScaleGetureDetector;
    /** An external click listener */
    private OnClickListener mExternalClickListener;
    /** When {@code true}, allows gestures to scale / pan the image */
    private boolean mTransformsEnabled;

    // To support zooming
    /** When {@code true}, a double tap scales the image by {@link #DOUBLE_TAP_SCALE_FACTOR} */
    private boolean mDoubleTapToZoomEnabled = true;
    /** When {@code true}, prevents scale end gesture from falsely triggering a double click. */
    private boolean mDoubleTapDebounce;
    /** When {@code false}, event is a scale gesture. Otherwise, event is a double touch. */
    private boolean mIsDoubleTouch;
    /** Runnable that scales the image */
    private ScaleRunnable mScaleRunnable;
    /** Minimum scale the image can have. */
    private float mMinScale;
    /** Maximum scale to limit scaling to, 0 means no limit. */
    private float mMaxScale;

    // To support translation [i.e. panning]
    /** Runnable that can move the image */
    private TranslateRunnable mTranslateRunnable;
    private SnapRunnable mSnapRunnable;

    // To support rotation
    /** The rotate runnable used to animate rotations of the image */
    private RotateRunnable mRotateRunnable;
    /** The current rotation amount, in degrees */
    private float mRotation;

    // Convenience fields
    // These are declared here not because they are important properties of the view. Rather, we
    // declare them here to avoid object allocation during critical graphics operations; such as
    // layout or drawing.
    /** Source (i.e. the photo size) bounds */
    private RectF mTempSrc = new RectF();
    /** Destination (i.e. the display) bounds. The image is scaled to this size. */
    private RectF mTempDst = new RectF();
    /** Rectangle to handle translations */
    private RectF mTranslateRect = new RectF();
    /** Array to store a copy of the matrix values */
    private float[] mValues = new float[9];

    /**
     * Track whether a double tap event occurred.
     */
    private boolean mDoubleTapOccurred;

    /**
     * X and Y coordinates for the current down event. Since mDoubleTapOccurred only contains the
     * information that there was a double tap event, use these to get the secondary tap
     * information to determine if a user has moved beyond touch slop.
     */
    private float mDownFocusX;
    private float mDownFocusY;

    /**
     * Whether the QuickSale gesture is enabled.
     */
    private boolean mQuickScaleEnabled;

    public PhotoView(Context context) {
        super(context);
        initialize();
    }

    public PhotoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initialize();
    }

    public PhotoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initialize();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mScaleGetureDetector == null || mGestureDetector == null) {
            // We're being destroyed; ignore any touch events
            return true;
        }

        mScaleGetureDetector.onTouchEvent(event);
        mGestureDetector.onTouchEvent(event);
        final int action = event.getAction();

        switch (action) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (!mTranslateRunnable.mRunning) {
                    snap();
                }
                break;
        }

        return true;
    }

    @Override
    public boolean onDoubleTap(MotionEvent e) {
        mDoubleTapOccurred = true;
        if (!mQuickScaleEnabled) {
            return scale(e);
        }
        return false;
    }

    @Override
    public boolean onDoubleTapEvent(MotionEvent e) {
        final int action = e.getAction();
        boolean handled = false;

        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (mQuickScaleEnabled) {
                    mDownFocusX = e.getX();
                    mDownFocusY = e.getY();
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mQuickScaleEnabled) {
                    handled = scale(e);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mQuickScaleEnabled && mDoubleTapOccurred) {
                    final int deltaX = (int) (e.getX() - mDownFocusX);
                    final int deltaY = (int) (e.getY() - mDownFocusY);
                    int distance = (deltaX * deltaX) + (deltaY * deltaY);
                    if (distance > sTouchSlopSquare) {
                        mDoubleTapOccurred = false;
                    }
                }
                break;

        }
        return handled;
    }

    private boolean scale(MotionEvent e) {
        boolean handled = false;
        if (mDoubleTapToZoomEnabled && mTransformsEnabled && mDoubleTapOccurred) {
            if (!mDoubleTapDebounce) {
                float currentScale = getScale();
                float targetScale = currentScale * DOUBLE_TAP_SCALE_FACTOR;

                // Ensure the target scale is within our bounds
                targetScale = Math.max(mMinScale, targetScale);
                targetScale = Math.min(mMaxScale, targetScale);

                mScaleRunnable.start(currentScale, targetScale, e.getX(), e.getY());
                handled = true;
            }
            mDoubleTapDebounce = false;
        }
        mDoubleTapOccurred = false;
        return handled;
    }

    @Override
    public boolean onSingleTapConfirmed(MotionEvent e) {
        if (mExternalClickListener != null && !mIsDoubleTouch) {
            mExternalClickListener.onClick(this);
        }
        mIsDoubleTouch = false;
        return true;
    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {
    }

    @Override
    public void onShowPress(MotionEvent e) {
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if (mTransformsEnabled) {
            translate(-distanceX, -distanceY);
        }
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        if (mTransformsEnabled) {
            mTranslateRunnable.stop();
            mSnapRunnable.stop();
        }
        return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if (mTransformsEnabled) {
            mTranslateRunnable.start(velocityX, velocityY);
        }
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        if (mTransformsEnabled) {
            mIsDoubleTouch = false;
            float currentScale = getScale();
            float newScale = currentScale * detector.getScaleFactor();
            scale(newScale, detector.getFocusX(), detector.getFocusY());
        }
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        if (mTransformsEnabled) {
            mScaleRunnable.stop();
            mIsDoubleTouch = true;
        }
        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (mTransformsEnabled && mIsDoubleTouch) {
            mDoubleTapDebounce = true;
            resetTransformations();
        }
    }

    @Override
    public void setOnClickListener(OnClickListener listener) {
        mExternalClickListener = listener;
    }

    @Override
    public boolean interceptMoveLeft(float origX, float origY) {
        if (!mTransformsEnabled) {
            // Allow intercept if we're not in transform mode
            return false;
        } else if (mTranslateRunnable.mRunning) {
            // Don't allow touch intercept until we've stopped flinging
            return true;
        } else {
            mMatrix.getValues(mValues);
            mTranslateRect.set(mTempSrc);
            mMatrix.mapRect(mTranslateRect);

            final float viewWidth = getWidth();
            final float transX = mValues[Matrix.MTRANS_X];
            final float drawWidth = mTranslateRect.right - mTranslateRect.left;

            if (!mTransformsEnabled || drawWidth <= viewWidth) {
                // Allow intercept if not in transform mode or the image is smaller than the view
                return false;
            } else if (transX == 0) {
                // We're at the left-side of the image; allow intercepting movements to the right
                return false;
            } else if (viewWidth >= drawWidth + transX) {
                // We're at the right-side of the image; allow intercepting movements to the left
                return true;
            } else {
                // We're in the middle of the image; don't allow touch intercept
                return true;
            }
        }
    }

    @Override
    public boolean interceptMoveRight(float origX, float origY) {
        if (!mTransformsEnabled) {
            // Allow intercept if we're not in transform mode
            return false;
        } else if (mTranslateRunnable.mRunning) {
            // Don't allow touch intercept until we've stopped flinging
            return true;
        } else {
            mMatrix.getValues(mValues);
            mTranslateRect.set(mTempSrc);
            mMatrix.mapRect(mTranslateRect);

            final float viewWidth = getWidth();
            final float transX = mValues[Matrix.MTRANS_X];
            final float drawWidth = mTranslateRect.right - mTranslateRect.left;

            if (!mTransformsEnabled || drawWidth <= viewWidth) {
                // Allow intercept if not in transform mode or the image is smaller than the view
                return false;
            } else if (transX == 0) {
                // We're at the left-side of the image; allow intercepting movements to the right
                return true;
            } else if (viewWidth >= drawWidth + transX) {
                // We're at the right-side of the image; allow intercepting movements to the left
                return false;
            } else {
                // We're in the middle of the image; don't allow touch intercept
                return true;
            }
        }
    }

    /**
     * Free all resources held by this view.
     * The view is on its way to be collected and will not be reused.
     */
    public void clear() {
        mGestureDetector = null;
        mScaleGetureDetector = null;
        mDrawable = null;
        mScaleRunnable.stop();
        mScaleRunnable = null;
        mTranslateRunnable.stop();
        mTranslateRunnable = null;
        mSnapRunnable.stop();
        mSnapRunnable = null;
        mRotateRunnable.stop();
        mRotateRunnable = null;
        setOnClickListener(null);
        mExternalClickListener = null;
        mDoubleTapOccurred = false;
    }

    public void bindResource(int resourceId){
    	bindDrawable(getResources().getDrawable(resourceId));	
    }
    
    public void bindDrawable(Drawable drawable) {
        boolean changed = false;
        if (drawable != null && drawable != mDrawable) {
            // Clear previous state.
            if (mDrawable != null) {
                mDrawable.setCallback(null);
            }

            mDrawable = drawable;

            // Reset mMinScale to ensure the bounds / matrix are recalculated
            mMinScale = 0f;

            // Set a callback?
            mDrawable.setCallback(this);

            changed = true;
        }

        configureBounds(changed);
        invalidate();
    }

    /**
     * Binds a bitmap to the view.
     *
     * @param photoBitmap the bitmap to bind.
     */
    public void bindPhoto(Bitmap photoBitmap) {
        boolean currentDrawableIsBitmapDrawable = mDrawable instanceof BitmapDrawable;
        boolean changed = !(currentDrawableIsBitmapDrawable);
        if (mDrawable != null && currentDrawableIsBitmapDrawable) {
            final Bitmap drawableBitmap = ((BitmapDrawable) mDrawable).getBitmap();
            if (photoBitmap == drawableBitmap) {
                // setting the same bitmap; do nothing
                return;
            }

            changed = photoBitmap != null &&
                    (mDrawable.getIntrinsicWidth() != photoBitmap.getWidth() ||
                    mDrawable.getIntrinsicHeight() != photoBitmap.getHeight());

            // Reset mMinScale to ensure the bounds / matrix are recalculated
            mMinScale = 0f;
            mDrawable = null;
        }

        if (mDrawable == null && photoBitmap != null) {
            mDrawable = new BitmapDrawable(getResources(), photoBitmap);
        }

        configureBounds(changed);
        invalidate();
    }

    /**
     * Returns the bound photo data if set. Otherwise, {@code null}.
     */
    public Bitmap getPhoto() {
        if (mDrawable != null && mDrawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) mDrawable).getBitmap();
        }
        return null;
    }

    /**
     * Returns the bound drawable. May be {@code null} if no drawable is bound.
     */
    public Drawable getDrawable() {
        return mDrawable;
    }

    /**
     * Gets video data associated with this item. Returns {@code null} if this is not a video.
     */
    public byte[] getVideoData() {
        return mVideoBlob;
    }

    /**
     * Returns {@code true} if the photo represents a video. Otherwise, {@code false}.
     */
    public boolean isVideo() {
        return mVideoBlob != null;
    }

    /**
     * Returns {@code true} if the video is ready to play. Otherwise, {@code false}.
     */
    public boolean isVideoReady() {
        return mVideoBlob != null && mVideoReady;
    }

    /**
     * Returns {@code true} if a photo has been bound. Otherwise, {@code false}.
     */
    public boolean isPhotoBound() {
        return mDrawable != null;
    }

    /**
     * Hides the photo info portion of the header. As a side effect, this automatically enables
     * or disables image transformations [eg zoom, pan, etc...] depending upon the value of
     * fullScreen. If this is not desirable, enable / disable image transformations manually.
     */
    public void setFullScreen(boolean fullScreen, boolean animate) {
        if (fullScreen != mFullScreen) {
            mFullScreen = fullScreen;
            requestLayout();
            invalidate();
        }
    }

    /**
     * Enable or disable cropping of the displayed image. Cropping can only be enabled
     * <em>before</em> the view has been laid out. Additionally, once cropping has been
     * enabled, it cannot be disabled.
     */
    public void enableAllowCrop(boolean allowCrop) {
        if (allowCrop && mHaveLayout) {
            throw new IllegalArgumentException("Cannot set crop after view has been laid out");
        }
        if (!allowCrop && mAllowCrop) {
            throw new IllegalArgumentException("Cannot unset crop mode");
        }
        mAllowCrop = allowCrop;
    }

    /**
     * Gets a bitmap of the cropped region. If cropping is not enabled, returns {@code null}.
     */
    public Bitmap getCroppedPhoto() {
        if (!mAllowCrop) {
            return null;
        }

        final Bitmap croppedBitmap = Bitmap.createBitmap(
                (int) CROPPED_SIZE, (int) CROPPED_SIZE, Bitmap.Config.ARGB_8888);
        final Canvas croppedCanvas = new Canvas(croppedBitmap);

        // scale for the final dimensions
        final int cropWidth = mCropRect.right - mCropRect.left;
        final float scaleWidth = CROPPED_SIZE / cropWidth;
        final float scaleHeight = CROPPED_SIZE / cropWidth;

        // translate to the origin & scale
        final Matrix matrix = new Matrix(mDrawMatrix);
        matrix.postTranslate(-mCropRect.left, -mCropRect.top);
        matrix.postScale(scaleWidth, scaleHeight);

        // draw the photo
        if (mDrawable != null) {
            croppedCanvas.concat(matrix);
            mDrawable.draw(croppedCanvas);
        }
        return croppedBitmap;
    }

    /**
     * Resets the image transformation to its original value.
     */
    public void resetTransformations() {
        // snap transformations; we don't animate
        mMatrix.set(mOriginalMatrix);

        // Invalidate the view because if you move off this PhotoView
        // to another one and come back, you want it to draw from scratch
        // in case you were zoomed in or translated (since those settings
        // are not preserved and probably shouldn't be).
        invalidate();
    }

    /**
     * Rotates the image 90 degrees, clockwise.
     */
    public void rotateClockwise() {
        rotate(90, true);
    }

    /**
     * Rotates the image 90 degrees, counter clockwise.
     */
    public void rotateCounterClockwise() {
        rotate(-90, true);
    }
    
    /**
     * Rotates the image 90 degrees, counter clockwise.
     */
    public void rotateTo(float degree) {
        rotate(degree % 360, true);
    }
    
    /**
     * Rotates the image 90 degrees, counter clockwise.
     */
    public void rotateTo(float degree, boolean animate) {
        rotate(degree % 360, animate);
    }
    
    /**
     * Get Rotation of the image.
     */
    public float getRotationDegree() {
        return mRotation % 360;
    }
    
    public void setRotationDegree(float degree) {
        mRotation = degree % 360;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // draw the photo
        if (mDrawable != null) {
            int saveCount = canvas.getSaveCount();
            canvas.save();

            if (mDrawMatrix != null) {
                canvas.concat(mDrawMatrix);
            }
            mDrawable.draw(canvas);

            canvas.restoreToCount(saveCount);

            if (mVideoBlob != null) {
                final Bitmap videoImage = (mVideoReady ? sVideoImage : sVideoNotReadyImage);
                final int drawLeft = (getWidth() - videoImage.getWidth()) / 2;
                final int drawTop = (getHeight() - videoImage.getHeight()) / 2;
                canvas.drawBitmap(videoImage, drawLeft, drawTop, null);
            }

            // Extract the drawable's bounds (in our own copy, to not alter the image)
            mTranslateRect.set(mDrawable.getBounds());
            if (mDrawMatrix != null) {
                mDrawMatrix.mapRect(mTranslateRect);
            }

            if (mAllowCrop) {
                int previousSaveCount = canvas.getSaveCount();
                canvas.drawRect(0, 0, getWidth(), getHeight(), sCropDimPaint);
                canvas.save();
                canvas.clipRect(mCropRect);

                if (mDrawMatrix != null) {
                    canvas.concat(mDrawMatrix);
                }

                mDrawable.draw(canvas);
                canvas.restoreToCount(previousSaveCount);
                canvas.drawRect(mCropRect, sCropPaint);
            }
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mHaveLayout = true;
        final int layoutWidth = getWidth();
        final int layoutHeight = getHeight();

        if (mAllowCrop) {
            mCropSize = Math.min(sCropSize, Math.min(layoutWidth, layoutHeight));
            final int cropLeft = (layoutWidth - mCropSize) / 2;
            final int cropTop = (layoutHeight - mCropSize) / 2;
            final int cropRight = cropLeft + mCropSize;
            final int cropBottom =  cropTop + mCropSize;

            // Create a crop region overlay. We need a separate canvas to be able to "punch
            // a hole" through to the underlying image.
            mCropRect.set(cropLeft, cropTop, cropRight, cropBottom);
        }
        configureBounds(changed);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mFixedHeight != -1) {
            super.onMeasure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(mFixedHeight,
                    MeasureSpec.AT_MOST));
            setMeasuredDimension(getMeasuredWidth(), mFixedHeight);
        } else {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        }
    }

    @Override
    public boolean verifyDrawable(Drawable drawable) {
        return mDrawable == drawable || super.verifyDrawable(drawable);
    }

    @Override
    /**
     * {@inheritDoc}
     */
    public void invalidateDrawable(Drawable drawable) {
        // Only invalidate this view if the passed in drawable is displayed within this view. If
        // another drawable is passed in, have the parent view handle invalidation.
        if (mDrawable == drawable) {
            invalidate();
        } else {
            super.invalidateDrawable(drawable);
        }
    }

    /**
     * Forces a fixed height for this view.
     *
     * @param fixedHeight The height. If {@code -1}, use the measured height.
     */
    public void setFixedHeight(int fixedHeight) {
        final boolean adjustBounds = (fixedHeight != mFixedHeight);
        mFixedHeight = fixedHeight;
        setMeasuredDimension(getMeasuredWidth(), mFixedHeight);
        if (adjustBounds) {
            configureBounds(true);
            requestLayout();
        }
    }

    /**
     * Enable or disable image transformations. When transformations are enabled, this view
     * consumes all touch events.
     */
    public void enableImageTransforms(boolean enable) {
        mTransformsEnabled = enable;
        if (!mTransformsEnabled) {
            resetTransformations();
        }
    }
    
    public boolean isImageTransformsEnabled() {
        return mTransformsEnabled;
    }
    
    /**
     * Configures the bounds of the photo. The photo will always be scaled to fit center.
     */
    private void configureBounds(boolean changed) {
        if (mDrawable == null || !mHaveLayout) {
            return;
        }
        final int dwidth = mDrawable.getIntrinsicWidth();
        final int dheight = mDrawable.getIntrinsicHeight();

        final int vwidth = getWidth();
        final int vheight = getHeight();

        final boolean fits = (dwidth < 0 || vwidth == dwidth) &&
                (dheight < 0 || vheight == dheight);

        // We need to do the scaling ourself, so have the drawable use its native size.
        mDrawable.setBounds(0, 0, dwidth, dheight);

        // Create a matrix with the proper transforms
        if (changed || (mMinScale == 0 && mDrawable != null && mHaveLayout)) {
            generateMatrix();
            generateScale();
        }

        mMatrix.postRotate(getRotationDegree(), getWidth() / 2, getHeight() / 2);
        if (fits || mMatrix.isIdentity()) {
            // The bitmap fits exactly, no transform needed.
            mDrawMatrix = null;
        } else {
            mDrawMatrix = mMatrix;
        }
    }

    /**
     * Generates the initial transformation matrix for drawing. Additionally, it sets the
     * minimum and maximum scale values.
     */
    private void generateMatrix() {
        final int dwidth = mDrawable.getIntrinsicWidth();
        final int dheight = mDrawable.getIntrinsicHeight();

        final int vwidth = mAllowCrop ? sCropSize : getWidth();
        final int vheight = mAllowCrop ? sCropSize : getHeight();

        final boolean fits = (dwidth < 0 || vwidth == dwidth) &&
                (dheight < 0 || vheight == dheight);

        if (fits && !mAllowCrop) {
            mMatrix.reset();
        } else {
            // Generate the required transforms for the photo
            mTempSrc.set(0, 0, dwidth, dheight);
            if (mAllowCrop) {
                mTempDst.set(mCropRect);
            } else {
                mTempDst.set(0, 0, vwidth, vheight);
            }
            RectF scaledDestination = new RectF(
                    (vwidth / 2) - (dwidth * mMaxInitialScaleFactor / 2),
                    (vheight / 2) - (dheight * mMaxInitialScaleFactor / 2),
                    (vwidth / 2) + (dwidth * mMaxInitialScaleFactor / 2),
                    (vheight / 2) + (dheight * mMaxInitialScaleFactor / 2));
            if(mTempDst.contains(scaledDestination)) {
                mMatrix.setRectToRect(mTempSrc, scaledDestination, Matrix.ScaleToFit.CENTER);
            } else {
                mMatrix.setRectToRect(mTempSrc, mTempDst, Matrix.ScaleToFit.CENTER);
            }
        }
        mOriginalMatrix.set(mMatrix);
    }

    private void generateScale() {
        final int dwidth = mDrawable.getIntrinsicWidth();
        final int dheight = mDrawable.getIntrinsicHeight();

        final int vwidth = mAllowCrop ? getCropSize() : getWidth();
        final int vheight = mAllowCrop ? getCropSize() : getHeight();

        if (dwidth < vwidth && dheight < vheight && !mAllowCrop) {
            mMinScale = 1.0f;
        } else {
            mMinScale = getScale();
        }
        mMaxScale = Math.max(mMinScale * 8, 8);
    }

    /**
     * @return the size of the crop regions
     */
    private int getCropSize() {
        return mCropSize > 0 ? mCropSize : sCropSize;
    }

    /**
     * Returns the currently applied scale factor for the image.
     * <p>
     * NOTE: This method overwrites any values stored in {@link #mValues}.
     */
    private float getScale() {
        return (float)Math.sqrt((float) Math.pow(getValue(mMatrix, Matrix.MSCALE_X), 2) + (float) Math.pow(getValue(mMatrix, Matrix.MSKEW_Y), 2));
    }
    

    /**
     * Helper method that 'unpacks' a Matrix and returns the required value
     *
     * @param matrix     - Matrix to unpack
     * @param whichValue - Which value from Matrix.M* to return
     * @return float - returned value
     */
    private float getValue(Matrix matrix, int whichValue) {
        matrix.getValues(mValues);
        return mValues[whichValue];
    }

    /**
     * Scales the image while keeping the aspect ratio.
     *
     * The given scale is capped so that the resulting scale of the image always remains
     * between {@link #mMinScale} and {@link #mMaxScale}.
     *
     * The scaled image is never allowed to be outside of the viewable area. If the image
     * is smaller than the viewable area, it will be centered.
     *
     * @param newScale the new scale
     * @param centerX the center horizontal point around which to scale
     * @param centerY the center vertical point around which to scale
     */
    private void scale(float newScale, float centerX, float centerY) {
        // rotate back to the original orientation
        mMatrix.postRotate(-mRotation, getWidth() / 2, getHeight() / 2);

        // ensure that mMixScale <= newScale <= mMaxScale
        newScale = Math.max(newScale, mMinScale);
        newScale = Math.min(newScale, mMaxScale);

        float currentScale = getScale();
        float factor = newScale / currentScale;

        // apply the scale factor
        mMatrix.postScale(factor, factor, centerX, centerY);

        // ensure the image is within the view bounds
        snap();

        // re-apply any rotation
        mMatrix.postRotate(mRotation, getWidth() / 2, getHeight() / 2);

        invalidate();
    }

    /**
     * Translates the image.
     *
     * This method will not allow the image to be translated outside of the visible area.
     *
     * @param tx how many pixels to translate horizontally
     * @param ty how many pixels to translate vertically
     * @return {@code true} if the translation was applied as specified. Otherwise, {@code false}
     *      if the translation was modified.
     */
    private boolean translate(float tx, float ty) {
        mTranslateRect.set(mTempSrc);
        mMatrix.mapRect(mTranslateRect);

        final float maxLeft = mAllowCrop ? mCropRect.left : 0.0f;
        final float maxRight = mAllowCrop ? mCropRect.right : getWidth();
        float l = mTranslateRect.left;
        float r = mTranslateRect.right;

        final float translateX;
        if (mAllowCrop) {
            // If we're cropping, allow the image to scroll off the edge of the screen
            translateX = Math.max(maxLeft - mTranslateRect.right,
                    Math.min(maxRight - mTranslateRect.left, tx));
        } else {
            // Otherwise, ensure the image never leaves the screen
            if (r - l < maxRight - maxLeft) {
                translateX = maxLeft + ((maxRight - maxLeft) - (r + l)) / 2;
            } else {
                translateX = Math.max(maxRight - r, Math.min(maxLeft - l, tx));
            }
        }

        float maxTop = mAllowCrop ? mCropRect.top: 0.0f;
        float maxBottom = mAllowCrop ? mCropRect.bottom : getHeight();
        float t = mTranslateRect.top;
        float b = mTranslateRect.bottom;

        final float translateY;

        if (mAllowCrop) {
            // If we're cropping, allow the image to scroll off the edge of the screen
            translateY = Math.max(maxTop - mTranslateRect.bottom,
                    Math.min(maxBottom - mTranslateRect.top, ty));
        } else {
            // Otherwise, ensure the image never leaves the screen
            if (b - t < maxBottom - maxTop) {
                translateY = maxTop + ((maxBottom - maxTop) - (b + t)) / 2;
            } else {
                translateY = Math.max(maxBottom - b, Math.min(maxTop - t, ty));
            }
        }

        // Do the translation
        mMatrix.postTranslate(translateX, translateY);
        invalidate();

        return (translateX == tx) && (translateY == ty);
    }

    /**
     * Snaps the image so it touches all edges of the view.
     */
    private void snap() {
        mTranslateRect.set(mTempSrc);
        mMatrix.mapRect(mTranslateRect);

        // Determine how much to snap in the horizontal direction [if any]
        float maxLeft = mAllowCrop ? mCropRect.left : 0.0f;
        float maxRight = mAllowCrop ? mCropRect.right : getWidth();
        float l = mTranslateRect.left;
        float r = mTranslateRect.right;

        final float translateX;
        if (r - l < maxRight - maxLeft) {
            // Image is narrower than view; translate to the center of the view
            translateX = maxLeft + ((maxRight - maxLeft) - (r + l)) / 2;
        } else if (l > maxLeft) {
            // Image is off right-edge of screen; bring it into view
            translateX = maxLeft - l;
        } else if (r < maxRight) {
            // Image is off left-edge of screen; bring it into view
            translateX = maxRight - r;
        } else {
            translateX = 0.0f;
        }

        // Determine how much to snap in the vertical direction [if any]
        float maxTop = mAllowCrop ? mCropRect.top : 0.0f;
        float maxBottom = mAllowCrop ? mCropRect.bottom : getHeight();
        float t = mTranslateRect.top;
        float b = mTranslateRect.bottom;

        final float translateY;
        if (b - t < maxBottom - maxTop) {
            // Image is shorter than view; translate to the bottom edge of the view
            translateY = maxTop + ((maxBottom - maxTop) - (b + t)) / 2;
        } else if (t > maxTop) {
            // Image is off bottom-edge of screen; bring it into view
            translateY = maxTop - t;
        } else if (b < maxBottom) {
            // Image is off top-edge of screen; bring it into view
            translateY = maxBottom - b;
        } else {
            translateY = 0.0f;
        }

        if (Math.abs(translateX) > SNAP_THRESHOLD || Math.abs(translateY) > SNAP_THRESHOLD) {
            mSnapRunnable.start(translateX, translateY);
        } else {
            mMatrix.postTranslate(translateX, translateY);
            invalidate();
        }
    }

    /**
     * Rotates the image, either instantly or gradually
     *
     * @param degrees how many degrees to rotate the image, positive rotates clockwise
     * @param animate if {@code true}, animate during the rotation. Otherwise, snap rotate.
     */
    private void rotate(float degrees, boolean animate) {
        if (animate) {
            mRotateRunnable.start(degrees);
        } else {
            mRotation += degrees;
            mMatrix.postRotate(degrees, getWidth() / 2, getHeight() / 2);
            invalidate();
        }
    }

    /**
     * Initializes the header and any static values
     */
    private void initialize() {
        Context context = getContext();

        if (!sInitialized) {
            sInitialized = true;

            Resources resources = context.getApplicationContext().getResources();

            sCropSize = resources.getDimensionPixelSize(R.dimen.photo_crop_width);

            sCropDimPaint = new Paint();
            sCropDimPaint.setAntiAlias(true);
            sCropDimPaint.setColor(resources.getColor(R.color.photo_crop_dim_color));
            sCropDimPaint.setStyle(Style.FILL);

            sCropPaint = new Paint();
            sCropPaint.setAntiAlias(true);
            sCropPaint.setColor(resources.getColor(R.color.photo_crop_highlight_color));
            sCropPaint.setStyle(Style.STROKE);
            sCropPaint.setStrokeWidth(resources.getDimension(R.dimen.photo_crop_stroke_width));

            final ViewConfiguration configuration = ViewConfiguration.get(context);
            final int touchSlop = configuration.getScaledTouchSlop();
            sTouchSlopSquare = touchSlop * touchSlop;
        }

        mGestureDetector = new GestureDetectorCompat(context, this, null);
        mScaleGetureDetector = new ScaleGestureDetector(context, this);
        mQuickScaleEnabled = ScaleGestureDetectorCompat.isQuickScaleEnabled(mScaleGetureDetector);
        mScaleRunnable = new ScaleRunnable(this);
        mTranslateRunnable = new TranslateRunnable(this);
        mSnapRunnable = new SnapRunnable(this);
        mRotateRunnable = new RotateRunnable(this);
    }

    /**
     * Runnable that animates an image scale operation.
     */
    private static class ScaleRunnable implements Runnable {

        private final PhotoView mHeader;

        private float mCenterX;
        private float mCenterY;

        private boolean mZoomingIn;

        private float mTargetScale;
        private float mStartScale;
        private float mVelocity;
        private long mStartTime;

        private boolean mRunning;
        private boolean mStop;

        public ScaleRunnable(PhotoView header) {
            mHeader = header;
        }

        /**
         * Starts the animation. There is no target scale bounds check.
         */
        public boolean start(float startScale, float targetScale, float centerX, float centerY) {
            if (mRunning) {
                return false;
            }

            mCenterX = centerX;
            mCenterY = centerY;

            // Ensure the target scale is within the min/max bounds
            mTargetScale = targetScale;
            mStartTime = System.currentTimeMillis();
            mStartScale = startScale;
            mZoomingIn = mTargetScale > mStartScale;
            mVelocity = (mTargetScale - mStartScale) / ZOOM_ANIMATION_DURATION;
            mRunning = true;
            mStop = false;
            mHeader.post(this);
            return true;
        }

        /**
         * Stops the animation in place. It does not snap the image to its final zoom.
         */
        public void stop() {
            mRunning = false;
            mStop = true;
        }

        @Override
        public void run() {
            if (mStop) {
                return;
            }

            // Scale
            long now = System.currentTimeMillis();
            long ellapsed = now - mStartTime;
            float newScale = (mStartScale + mVelocity * ellapsed);
            mHeader.scale(newScale, mCenterX, mCenterY);

            // Stop when done
            if (newScale == mTargetScale || (mZoomingIn == (newScale > mTargetScale))) {
                mHeader.scale(mTargetScale, mCenterX, mCenterY);
                stop();
            }

            if (!mStop) {
                mHeader.post(this);
            }
        }
    }

    /**
     * Runnable that animates an image translation operation.
     */
    private static class TranslateRunnable implements Runnable {

        private static final float DECELERATION_RATE = 1000f;
        private static final long NEVER = -1L;

        private final PhotoView mHeader;

        private float mVelocityX;
        private float mVelocityY;

        private long mLastRunTime;
        private boolean mRunning;
        private boolean mStop;

        public TranslateRunnable(PhotoView header) {
            mLastRunTime = NEVER;
            mHeader = header;
        }

        /**
         * Starts the animation.
         */
        public boolean start(float velocityX, float velocityY) {
            if (mRunning) {
                return false;
            }
            mLastRunTime = NEVER;
            mVelocityX = velocityX;
            mVelocityY = velocityY;
            mStop = false;
            mRunning = true;
            mHeader.post(this);
            return true;
        }

        /**
         * Stops the animation in place. It does not snap the image to its final translation.
         */
        public void stop() {
            mRunning = false;
            mStop = true;
        }

        @Override
        public void run() {
            // See if we were told to stop:
            if (mStop) {
                return;
            }

            // Translate according to current velocities and time delta:
            long now = System.currentTimeMillis();
            float delta = (mLastRunTime != NEVER) ? (now - mLastRunTime) / 1000f : 0f;
            final boolean didTranslate = mHeader.translate(mVelocityX * delta, mVelocityY * delta);
            mLastRunTime = now;
            // Slow down:
            float slowDown = DECELERATION_RATE * delta;
            if (mVelocityX > 0f) {
                mVelocityX -= slowDown;
                if (mVelocityX < 0f) {
                    mVelocityX = 0f;
                }
            } else {
                mVelocityX += slowDown;
                if (mVelocityX > 0f) {
                    mVelocityX = 0f;
                }
            }
            if (mVelocityY > 0f) {
                mVelocityY -= slowDown;
                if (mVelocityY < 0f) {
                    mVelocityY = 0f;
                }
            } else {
                mVelocityY += slowDown;
                if (mVelocityY > 0f) {
                    mVelocityY = 0f;
                }
            }

            // Stop when done
            if ((mVelocityX == 0f && mVelocityY == 0f) || !didTranslate) {
                stop();
                mHeader.snap();
            }

            // See if we need to continue flinging:
            if (mStop) {
                return;
            }
            mHeader.post(this);
        }
    }

    /**
     * Runnable that animates an image translation operation.
     */
    private static class SnapRunnable implements Runnable {

        private static final long NEVER = -1L;

        private final PhotoView mHeader;

        private float mTranslateX;
        private float mTranslateY;

        private long mStartRunTime;
        private boolean mRunning;
        private boolean mStop;

        public SnapRunnable(PhotoView header) {
            mStartRunTime = NEVER;
            mHeader = header;
        }

        /**
         * Starts the animation.
         */
        public boolean start(float translateX, float translateY) {
            if (mRunning) {
                return false;
            }
            mStartRunTime = NEVER;
            mTranslateX = translateX;
            mTranslateY = translateY;
            mStop = false;
            mRunning = true;
            mHeader.postDelayed(this, SNAP_DELAY);
            return true;
        }

        /**
         * Stops the animation in place. It does not snap the image to its final translation.
         */
        public void stop() {
            mRunning = false;
            mStop = true;
        }

        @Override
        public void run() {
            // See if we were told to stop:
            if (mStop) {
                return;
            }

            // Translate according to current velocities and time delta:
            long now = System.currentTimeMillis();
            float delta = (mStartRunTime != NEVER) ? (now - mStartRunTime) : 0f;

            if (mStartRunTime == NEVER) {
                mStartRunTime = now;
            }

            float transX;
            float transY;
            if (delta >= SNAP_DURATION) {
                transX = mTranslateX;
                transY = mTranslateY;
            } else {
                transX = (mTranslateX / (SNAP_DURATION - delta)) * 10f;
                transY = (mTranslateY / (SNAP_DURATION - delta)) * 10f;
                if (Math.abs(transX) > Math.abs(mTranslateX) || transX == Float.NaN) {
                    transX = mTranslateX;
                }
                if (Math.abs(transY) > Math.abs(mTranslateY) || transY == Float.NaN) {
                    transY = mTranslateY;
                }
            }

            mHeader.translate(transX, transY);
            mTranslateX -= transX;
            mTranslateY -= transY;

            if (mTranslateX == 0 && mTranslateY == 0) {
                stop();
            }

            // See if we need to continue flinging:
            if (mStop) {
                return;
            }
            mHeader.post(this);
        }
    }

    /**
     * Runnable that animates an image rotation operation.
     */
    private static class RotateRunnable implements Runnable {

        private static final long NEVER = -1L;

        private final PhotoView mHeader;

        private float mTargetRotation;
        private float mAppliedRotation;
        private float mVelocity;
        private long mLastRuntime;

        private boolean mRunning;
        private boolean mStop;

        public RotateRunnable(PhotoView header) {
            mHeader = header;
        }

        /**
         * Starts the animation.
         */
        public void start(float rotation) {
            if (mRunning) {
                return;
            }

            mTargetRotation = rotation;
            mVelocity = mTargetRotation / ROTATE_ANIMATION_DURATION;
            mAppliedRotation = 0f;
            mLastRuntime = NEVER;
            mStop = false;
            mRunning = true;
            mHeader.post(this);
        }

        /**
         * Stops the animation in place. It does not snap the image to its final rotation.
         */
        public void stop() {
            mRunning = false;
            mStop = true;
        }

        @Override
        public void run() {
            if (mStop) {
                return;
            }

            if (mAppliedRotation != mTargetRotation) {
                long now = System.currentTimeMillis();
                long delta = mLastRuntime != NEVER ? now - mLastRuntime : 0L;
                float rotationAmount = mVelocity * delta;
                if (mAppliedRotation < mTargetRotation
                        && mAppliedRotation + rotationAmount > mTargetRotation
                        || mAppliedRotation > mTargetRotation
                        && mAppliedRotation + rotationAmount < mTargetRotation) {
                    rotationAmount = mTargetRotation - mAppliedRotation;
                }
                mHeader.rotate(rotationAmount, false);
                mAppliedRotation += rotationAmount;
                if (mAppliedRotation == mTargetRotation) {
                    stop();
                }
                mLastRuntime = now;
            }

            if (mStop) {
                return;
            }
            mHeader.post(this);
        }
    }

    public void setMaxInitialScale(float f) {
        mMaxInitialScaleFactor = f;
    }

    /**
     * Loads the image for the view if it isn't already loaded.
     * @param isInLayoutPass True if this was invoked from a layout pass, false otherwise.
     */    
    @Override
    public void loadImageIfNecessary(final boolean isInLayoutPass) {
        int width = getWidth();
        int height = getHeight();

        boolean wrapWidth = false, wrapHeight = false;
        if (getLayoutParams() != null) {
            wrapWidth = getLayoutParams().width == LayoutParams.WRAP_CONTENT;
            wrapHeight = getLayoutParams().height == LayoutParams.WRAP_CONTENT;
        }
        // if the view's bounds aren't known yet, and this is not a wrap-content/wrap-content
        // view, hold off on loading the image.
        boolean isFullyWrapContent = wrapWidth && wrapHeight;
        if (width == 0 && height == 0 && !isFullyWrapContent) {
            return;
        }

        // if the URL to be loaded in this view is empty, cancel any old requests and clear the
        // currently loaded image.
        if (TextUtils.isEmpty(mUrl)) {
            if (mImageContainer != null) {
                mImageContainer.cancelRequest();
                mImageContainer = null;
            }
            setDefaultImageOrNull();
            return;
        }

        // if there was an old request in this view, check if it needs to be canceled.
        if (mImageContainer != null && mImageContainer.getRequestUrl() != null) {
            if (mImageContainer.getRequestUrl().equals(mUrl)) {
                // if the request is from the same URL, return.
                return;
            } else {
                // if there is a pre-existing request, cancel it if it's fetching a different URL.
                mImageContainer.cancelRequest();
                setDefaultImageOrNull();
            }
        }
        
        // Calculate the max image width / height to use while ignoring WRAP_CONTENT dimens.
        int maxWidth = wrapWidth ? 0 : width;
        int maxHeight = wrapHeight ? 0 : height;
        
        // The pre-existing content of this view didn't match the current URL. Load the new image
        // from the network.
        ImageContainer newContainer = mImageLoader.get(mUrl,
                new ImageListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (mErrorImageId != 0) {
                        	bindResource(mErrorImageId);
                        }
                    }

                    @Override
                    public void onResponse(final ImageContainer response, boolean isImmediate) {
                        // If this was an immediate response that was delivered inside of a layout
                        // pass do not set the image immediately as it will trigger a requestLayout
                        // inside of a layout. Instead, defer setting the image by posting back to
                        // the main thread.
                        if (isImmediate && isInLayoutPass) {
                            post(new Runnable() {
                                @Override
                                public void run() {
                                    onResponse(response, false);
                                }
                            });
                            return;
                        }

                        if (response.getBitmap() != null) {
                            bindPhoto(response.getBitmap());
                        	if(null != getImageListener()){
                        		getImageListener().onResponse(response.getBitmap());
                        	}
                        } else if (mDefaultImageId != 0) {
                        	bindResource(mDefaultImageId);
                        }
                    }
        		}, maxWidth, maxHeight);

        // update the ImageContainer to be the new bitmap container.
        mImageContainer = newContainer;
    }
    
    @Override
    protected void setDefaultImageOrNull() {
		if (mDefaultImageId != 0) {
			bindResource(mDefaultImageId);
		} else {
			bindPhoto(null);
		}
    }

    public int getActualHeight(){
        if (mDrawable != null) {
            return mDrawable.getIntrinsicHeight();
        }
        return 0;
    }
    
    public int getActualWidth(){
        if (mDrawable != null) {
            return mDrawable.getIntrinsicWidth();
        }
        return 0;
    }
}