package com.volley.demo;

import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity implements OnItemClickListener {

	private ListView mList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		setContentView(R.layout.activity_main);
		mList = (ListView)findViewById(android.R.id.list);
		mList.setOnItemClickListener(this);
		mList.setAdapter(getSampleAdapter());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_about:
			Intent aboutIntent = new Intent(getApplicationContext(), AboutActivity.class);
			startActivity(aboutIntent);
			break;
		}
		return super.onOptionsItemSelected(item);
	}
	
	private ListAdapter getSampleAdapter() {
		ArrayList<ActivityInfo> items = new ArrayList<ActivityInfo>();
		final String thisClazzName = getClass().getName();

		try {
			PackageInfo pInfo = getPackageManager().getPackageInfo(
					getPackageName(), PackageManager.GET_ACTIVITIES);
			ActivityInfo[] aInfos = pInfo.activities;

			for (ActivityInfo aInfo : aInfos) {
				if (!thisClazzName.equals(aInfo.name) 
						&& !aInfo.name.endsWith("AboutActivity")
						&& !aInfo.name.endsWith("ImageDetailActivity")
						&& !aInfo.name.endsWith("AdActivity")) {
					items.add(aInfo);
				}
			}
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		return new SampleAdapter(this, items);
	}

	private static class SampleAdapter extends BaseAdapter {

		private final ArrayList<ActivityInfo> mItems;

		private final LayoutInflater mInflater;

		public SampleAdapter(Context context, ArrayList<ActivityInfo> activities) {
			mItems = activities;
			mInflater = LayoutInflater.from(context);
		}

		@Override
		public int getCount() {
			return mItems.size();
		}

		@Override
		public ActivityInfo getItem(int position) {
			return mItems.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			TextView tv = (TextView) convertView;
			if (tv == null) {
				tv = (TextView) mInflater.inflate(
						android.R.layout.simple_list_item_1, parent, false);
			}
			ActivityInfo item = getItem(position);
			if (!TextUtils.isEmpty(item.nonLocalizedLabel)) {
				tv.setText(item.nonLocalizedLabel);
			} else {
				tv.setText(item.labelRes);
			}
			return tv;
		}
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ActivityInfo info = (ActivityInfo) parent.getItemAtPosition(position);
		Intent intent = new Intent();
		intent.setComponent(new ComponentName(this, info.name));
		startActivity(intent);		
	}
}