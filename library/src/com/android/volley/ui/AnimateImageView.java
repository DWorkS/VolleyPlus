/**
 * Copyright (C) 2013 The Android Open Source Project
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
 *
 * Added by Vinay S Shenoy on 19/5/13
 */

package com.android.volley.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.AttributeSet;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;

import com.android.volley.R;

/**
 * Created by vinaysshenoy on 19/5/13.
 */
public class AnimateImageView extends ImageView implements Animation.AnimationListener{

    /**
     * Whether changes to this imageview
     * should be animated or not
     */
    private boolean mShouldAnimateChanges;

    /**
     * The animation when adding in the new image
     */
    private Animation mInAnimation;

    /**
     * The animation when removing the older image
     */
    private Animation mOutAnimation;

    /**
     * The different types of image resources
     * possible. Used to animate the images
     * when changing
     */
    private static interface ImageType {
        public static final int URI = 1;
        public static final int BITMAP = 2;
        public static final int DRAWABLE = 3;
        public static final int RESOURCE = 4;
    }

    private int mInImageType;

    /**
     * The Image Ids to be used when the
     * out animation ends and the fade animation begins
     */
    private Uri mInImageUri;
    private Bitmap mInBitmap;
    private Drawable mInDrawable;
    private int mInResId;

    public AnimateImageView(Context context) {
        this(context, null);
        init(context, null);
    }

    public AnimateImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
        init(context, attrs);
    }

    public AnimateImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {

        if(attrs == null) {
            initializeWithDefaults();
        } else {
            TypedArray attributes = context.obtainStyledAttributes(attrs, R.styleable.AnimateImageView);

            if(attributes == null) {
                initializeWithDefaults();
            }

            else {
                mShouldAnimateChanges = attributes.getBoolean(R.styleable.AnimateImageView_animate_changes, false);
                int inAnimationResId = attributes.getResourceId(R.styleable.AnimateImageView_in_animation, 0);
                int outAnimationResId = attributes.getResourceId(R.styleable.AnimateImageView_out_animation, 0);
                attributes.recycle();

                if(inAnimationResId == 0) {
                    mInAnimation = new AlphaAnimation(0.0f, 1.0f);
                    mInAnimation.setDuration(400);
                    mInAnimation.setFillAfter(true);
                } else {
                    mInAnimation = AnimationUtils.loadAnimation(context, inAnimationResId);
                }

                if(outAnimationResId == 0) {
                    mOutAnimation = new AlphaAnimation(1.0f, 0.0f);
                    mOutAnimation.setDuration(150);
                    mOutAnimation.setFillAfter(true);
                } else {
                    mOutAnimation = AnimationUtils.loadAnimation(context, outAnimationResId);
                }

                mInAnimation.setAnimationListener(this);
                mOutAnimation.setAnimationListener(this);
            }
        }
    }

    private void initializeWithDefaults() {

        mShouldAnimateChanges = false;
        mInAnimation = null;
        mOutAnimation = null;
    }

    /**
     * Whether any changes to this ImageView should be animated
     * @param animateChanges whether to animate the changes or not
     */
    public void setShouldAnimateChanges(boolean animateChanges) {
        mShouldAnimateChanges = animateChanges;
    }

    /**
     * The animation to use when adding a new image
     * @param inAnimation The animation to add an image
     */
    public void setInAnimation(Animation inAnimation) {
        mInAnimation = inAnimation;
    }

    /**
     * The animation to use when removing the older image
     * @param outAnimation The animation to remove an image
     */
    public void setOutAnimation(Animation outAnimation) {
        mOutAnimation = outAnimation;
    }

    @Override
    public void onAnimationStart(Animation animation) {

    }

    @Override
    public void onAnimationEnd(Animation animation) {

        if(animation == mOutAnimation) {
            switch(mInImageType) {

                case ImageType.URI: {
                    setImageURI(mInImageUri);
                    break;
                }

                case ImageType.BITMAP: {
                    setImageBitmap(mInBitmap);
                    break;
                }

                case ImageType.DRAWABLE: {
                    setImageDrawable(mInDrawable);
                    break;
                }

                case ImageType.RESOURCE: {
                    setImageResource(mInResId);
                    break;
                }

                default: {
                    throw new RuntimeException("Unknown image type");
                }
            }
            startAnimation(mInAnimation);
        }

        if(animation == mOutAnimation) {
            switch(mInImageType) {

                case ImageType.URI: {
                    mInImageUri = null;
                    break;
                }

                case ImageType.BITMAP: {
                    mInBitmap = null;
                    break;
                }

                case ImageType.DRAWABLE: {
                    mInDrawable = null;
                    break;
                }

                case ImageType.RESOURCE: {
                    mInResId = 0;
                    break;
                }

                default: {
                    throw new RuntimeException("Unknown image type");
                }
            }
            mInImageType = 0;
        }
    }

    @Override
    public void onAnimationRepeat(Animation animation) {

    }

    public void setImageResource(int resId, boolean animate) {

        if(mShouldAnimateChanges & animate) {
            mInImageType = ImageType.RESOURCE;
            mInResId = resId;
            startAnimation(mOutAnimation);
        } else {
            super.setImageResource(resId);
        }

    }

    public void setImageURI(Uri uri, boolean animate) {

        if(mShouldAnimateChanges & animate) {
            mInImageType = ImageType.URI;
            mInImageUri = uri;
            startAnimation(mOutAnimation);
        } else {
            super.setImageURI(uri);
        }
    }

    public void setImageDrawable(Drawable drawable, boolean animate) {

        if(mShouldAnimateChanges & animate) {
            mInImageType = ImageType.DRAWABLE;
            mInDrawable = drawable;
            startAnimation(mOutAnimation);
        } else {
            super.setImageDrawable(drawable);
        }
    }

    public void setImageBitmap(Bitmap bm, boolean animate) {

        if(mShouldAnimateChanges & animate) {
            mInImageType = ImageType.BITMAP;
            mInBitmap = bm;
            startAnimation(mOutAnimation);
        } else {
            super.setImageBitmap(bm);
        }
    }
}
