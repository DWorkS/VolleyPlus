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

import java.util.List;

import android.content.Context;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.volley.cache.SimpleImageLoader;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.ui.NetworkImageView;
import com.volley.demo.R;

public class ImageArrayAdapter extends ArrayAdapter<ImageEntry> {
	private SimpleImageLoader mImageLoader;

	public ImageArrayAdapter(Context context, int textViewResourceId, List<ImageEntry> objects, SimpleImageLoader imageLoader) {
		super(context, textViewResourceId, objects);
		mImageLoader = imageLoader;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		NetworkImageView image;
		TextView title;
		
		if (convertView == null) {
			LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = vi.inflate(R.layout.item_row, null);
		}

		image = ViewHolder.get(convertView, R.id.iv_thumb);
		title = ViewHolder.get(convertView, R.id.tv_title);

		ImageEntry entry = getItem(position);
		if (entry.getThumbnailUrl() != null) {
			image.setImageUrl(entry.getThumbnailUrl(), mImageLoader);
			image.setDefaultImageResId(R.drawable.empty_photo);
		} else {
			image.setImageResource(R.drawable.empty_photo);
		}

		title.setText(entry.getTitle());

		return convertView;
	}

	public static class ViewHolder {
		@SuppressWarnings("unchecked")
		public static <T extends View> T get(View view, int id) {
			SparseArray<View> viewHolder = (SparseArray<View>) view.getTag();
			if (viewHolder == null) {
				viewHolder = new SparseArray<View>();
				view.setTag(viewHolder);
			}
			View childView = viewHolder.get(id);
			if (childView == null) {
				childView = view.findViewById(id);
				viewHolder.put(id, childView);
			}
			return (T) childView;
		}
	}
}
