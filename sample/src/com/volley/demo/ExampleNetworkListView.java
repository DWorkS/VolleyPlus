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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ListView;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.error.VolleyError;
import com.android.volley.request.JsonObjectRequest;
import com.volley.demo.util.ImageArrayAdapter;
import com.volley.demo.util.ImageEntry;
import com.volley.demo.util.MyVolley;


/**
 * Demonstrates: 1. ListView which is populated by HTTP paginated requests; 2. Usage of NetworkImageView; 
 * 3. "Endless" ListView pagination with read-ahead
 * 
 * Please note that for production environment you will need to add functionality like handling rotation, 
 * showing/hiding (indeterminate) progress indicator while loading, indicating that there are no more records, etc...
 *   
 * @author Ognyan Bankov (ognyan.bankov@bulpros.com)
 *
 */
public class ExampleNetworkListView extends ActionBarActivity {
    private static final int RESULTS_PAGE_SIZE = 20;

    private ListView mLvPicasa;
    private boolean mHasData = false;
    private boolean mInError = false;
    private ArrayList<ImageEntry> mEntries = new ArrayList<ImageEntry>();
    private ImageArrayAdapter mAdapter;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_network_list_view);

        mLvPicasa = (ListView) findViewById(R.id.lv_picasa);
        mAdapter = new ImageArrayAdapter(this, 0, mEntries, MyVolley.getImageLoader());
        mLvPicasa.setAdapter(mAdapter);
        mLvPicasa.setOnScrollListener(new EndlessScrollListener());
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!mHasData && !mInError) {
            loadPage();
        }
    }


    private void loadPage() {
        RequestQueue queue = MyVolley.getRequestQueue();

        int startIndex = 1 + mEntries.size();
        JsonObjectRequest myReq = new JsonObjectRequest(Method.GET,
                                                "https://picasaweb.google.com/data/feed/api/all?q=kitten&max-results="
                                                        +
                                                        RESULTS_PAGE_SIZE
                                                        +
                                                        "&thumbsize=160&alt=json"
                                                        + "&start-index="
                                                        + startIndex,
                                                        null,
                                                createMyReqSuccessListener(),
                                                createMyReqErrorListener());

        queue.add(myReq);
    }


    private Response.Listener<JSONObject> createMyReqSuccessListener() {
        return new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    JSONObject feed = response.getJSONObject("feed");
                    JSONArray entries = feed.getJSONArray("entry");
                    JSONObject entry;
                    for (int i = 0; i < entries.length(); i++) {
                        entry = entries.getJSONObject(i);
                        
                        String url = null;
                        
                        JSONObject media = entry.getJSONObject("media$group");
                        if (media != null && media.has("media$thumbnail")) {
                            JSONArray thumbs = media.getJSONArray("media$thumbnail");
                            if (thumbs != null && thumbs.length() > 0) {
                                url = thumbs.getJSONObject(0).getString("url");
                            }
                        }
                        
                        mEntries.add(new ImageEntry(entry.getJSONObject("title").getString("$t"), url));
                    }
                    mAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    showErrorDialog();
                }
            }
        };
    }


    private Response.ErrorListener createMyReqErrorListener() {
        return new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                showErrorDialog();
            }
        };
    }


    private void showErrorDialog() {
        mInError = true;
        
        AlertDialog.Builder b = new AlertDialog.Builder(ExampleNetworkListView.this);
        b.setMessage("Error occured");
        b.show();
    }
    
    
    /**
     * Detects when user is close to the end of the current page and starts loading the next page
     * so the user will not have to wait (that much) for the next entries.
     * 
     * @author Ognyan Bankov (ognyan.bankov@bulpros.com)
     */
    public class EndlessScrollListener implements OnScrollListener {
        // how many entries earlier to start loading next page
        private int visibleThreshold = 5;
        private int currentPage = 0;
        private int previousTotal = 0;
        private boolean loading = true;

        public EndlessScrollListener() {
        }
        public EndlessScrollListener(int visibleThreshold) {
            this.visibleThreshold = visibleThreshold;
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                int visibleItemCount, int totalItemCount) {
            if (loading) {
                if (totalItemCount > previousTotal) {
                    loading = false;
                    previousTotal = totalItemCount;
                    currentPage++;
                }
            }
            if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
                // I load the next page of gigs using a background task,
                // but you can call any function here.
                loadPage();
                loading = true;
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
            
        }
        
        
        public int getCurrentPage() {
            return currentPage;
        }
    }
}
