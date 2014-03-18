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

package com.volley.demo;

import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import com.android.volley.toolbox.ImageLoader;
import com.volley.demo.util.MyVolley;

/**
 * Demonstrates how to execute request that loads an image and sets it to <code>ImageView</code>
 * @author Ognyan Bankov
 *
 */
public class ExampleImageLoading extends ActionBarActivity {
   private ImageView mImageView;
   
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_image_loading);
      
      mImageView = (ImageView) findViewById(R.id.iv_image);
      
      
      Button btnImageLoadingRequest = (Button) findViewById(R.id.btn_image_loading);
      btnImageLoadingRequest.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
             ImageLoader imageLoader = MyVolley.getImageLoader();
             imageLoader.get("https://lh3.googleusercontent.com/-LMUs793rAL4/SUQczGj6CBI/AAAAAAAAJqs/NLBzZMDMhS4/s720/P7300049aasd.JPG", 
                            ImageLoader.getImageListener(mImageView, 
                                                          R.drawable.empty_photo, 
                                                          R.drawable.error_image));
          }
      });
      
      Button btnImageLoadingErrorRequest = (Button) findViewById(R.id.btn_image_loading_error);
      btnImageLoadingErrorRequest.setOnClickListener(new OnClickListener() {
          @Override
          public void onClick(View v) {
             ImageLoader imageLoader = MyVolley.getImageLoader();
             imageLoader.get("https://lh3.googleusercontent.com/-LMUs793rAL4/SUQczGj6CBI/AAAAAAAAJqs/NLBzZMDMhS4a/s720/P7300049.JPG", 
                            ImageLoader.getImageListener(mImageView, 
                                                          R.drawable.empty_photo, 
                                                          R.drawable.error_image));
          }
      });
   }
}
