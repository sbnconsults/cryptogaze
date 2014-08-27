/*
 *  Name: MainActivity.java
 *  Description: main java file to be run extends ActionBarActivity
 *      The updater task which pulls data from different bid currency APIs is triggered here
 */
package com.idealtechlabs.cryptogaze;

import java.io.IOException;
import java.net.URISyntaxException;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONException;
import org.xml.sax.SAXException;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnGroupExpandListener;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.SimpleCursorTreeAdapter;
import android.widget.TextView;

public class MainActivity extends ActionBarActivity{

	private DatabaseHelper db;
	private SQLiteDatabase sldb;
	private MyExpandableListAdapter mAdapter;
	private ExpandableListView elv;
	private static final String DEBUG_TAG = "Debug";
	private int selectedGroup = -1;
    	private int previousItem = -1;
    	private MyScheduleExecutor updateTask;
	private NumberFormat nf = NumberFormat.getInstance(Locale.US);
	private static final String[] COIN_PROJECTION = new String[] {
		DatabaseContract.UNIQUE_ID,
		DatabaseContract.TableCoin.COL3,
		DatabaseContract.TableCoin.COL7
	};

	private static Context context;


	public static Resources getResourcesStatic() {
		return context.getResources();
	}
    
    	@Override
    	protected void onCreate(Bundle savedInstanceState) {
    		super.onCreate(savedInstanceState);
    		if( savedInstanceState != null ) {
    	     		selectedGroup = savedInstanceState.getInt("selectedGroup");
    		}
    	 	this.context = getApplicationContext();
    	 
        	setContentView(R.layout.activity_main);
        	db = new DatabaseHelper(this);

        	sldb = db.getReadableDatabase();
        	db.onCreate(sldb);
       
    		ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    		NetworkInfo mWifi = connManager.getNetworkInfo(connManager.getNetworkPreference());
        	ContentResolver cResolver = context.getContentResolver();
        	Map<String, String[]> newSetting = new HashMap<String,String[]> ();
		Cursor coinCursor = cResolver.query(MyContentProvider.COINS_CONTENT_URI,COIN_PROJECTION,
				DatabaseContract.TableCoin.COL3 + " != " +  DatabaseContract.TableCoin.COL7,null,null);

        	// This is where we check for currency conversion updates that need to happen
        	// incase it was not possible earlier due to lack of a working connection
		if(coinCursor != null && coinCursor.moveToFirst()){
			do {
				String coinId = coinCursor.getString(0);
				String oldCurr= coinCursor.getString(1);
				String newCurr = coinCursor.getString(2);

				String[] currVals = {oldCurr, newCurr};
				newSetting.put(coinId, currVals);
			} while (coinCursor.moveToNext());
			coinCursor.close();
    		}
		if(newSetting.size() > 0){
			try {
				Utils.updateCurrency(this, newSetting, false);
			} catch (Exception e) { e.printStackTrace(); }
		}
		
        	nf.setMaximumFractionDigits(2);
        	nf.setMinimumFractionDigits(2);
        
        	Activity actv = (Activity) this;

   
        	elv = (ExpandableListView) this.findViewById(R.id.coinExpandableListView);
        	Cursor mGroupsCursor = db.fetchGroup(sldb);
        	startManagingCursor(mGroupsCursor);
        	mGroupsCursor.moveToFirst();
		String bll = 	mGroupsCursor.getString(mGroupsCursor.getColumnIndex(DatabaseContract.TableCoin.COL1));
        	mAdapter = new MyExpandableListAdapter(mGroupsCursor, actv, this,
            		R.layout.coin_listview_each_item,
            		R.layout.value_listview_each_item, 
           		new String[] {
        			DatabaseContract.TableCoin.COL1,
        			DatabaseContract.TableCoin.COL4,
        			DatabaseContract.TableCoin.COL5,
        			DatabaseContract.UNIQUE_ID},
            		new int[] { R.id.coinName, R.id.avgValue, R.id.avgRoc, R.id.coinId },
            		new String[] { 
        			DatabaseContract.TableValues.COL2,
        			DatabaseContract.TableValues.COL5,
        			DatabaseContract.TableValues.COL6,
        			DatabaseContract.UNIQUE_ID},  
            		new int[] { R.id.source, R.id.value, R.id.roc, R.id.valueId}); 

        	elv.setAdapter(mAdapter);
        	if(selectedGroup > -1){
        		elv.expandGroup(selectedGroup);
        	}else{
        		String name = mGroupsCursor.getString(mGroupsCursor.getColumnIndex(DatabaseContract.TableCoin.COL1));
        		try {
				updateReddit(name);
        		} catch (Exception e) {
        			Log.e("REDDITs","Error getting reddit links at start");
        		}
        	}
  
        	elv.setOnGroupExpandListener(new OnGroupExpandListener() {
        		public void onGroupExpand(int groupPosition) {
                		if(groupPosition != previousItem ){
                    			elv.collapseGroup(previousItem );
                		}	
                		selectedGroup = groupPosition;
                		previousItem = groupPosition;
                
                		Cursor clicked = (Cursor) mAdapter.getGroup(groupPosition);
                		String name = clicked.getString(clicked.getColumnIndex(DatabaseContract.TableCoin.COL1));
                		try {
					updateReddit(name);
            			} catch (Exception e) {
            				Log.e("REDDITc","Error getting reddit on click");
            			}
            		}
        	}); 
        
        	elv.setOnChildClickListener(new ExpandableListView.OnChildClickListener() {
        
        		@Override
            		public boolean onChildClick(ExpandableListView parent, View v,
            		int groupPosition, int childPosition, long id) {
            			TextView sourceView = (TextView) v.findViewById(R.id.source);
            			String source = sourceView.getText().toString().toLowerCase();
            			if(source.equals("crypto-trade")){
            				source = "https://www."+ source + ".net";
            			}else{
            				source = "https://www."+ source + ".com";
            			}
            			Uri uri = Uri.parse(source);
            			Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            			startActivity(intent);
                		return true;
            		}
        	});
        	runUpdater();
    	}
    
    	@Override
    	protected void onRestoreInstanceState(Bundle state) {
        	super.onRestoreInstanceState(state);
    	}        


    	@Override
    	protected void onSaveInstanceState(Bundle state) {
		super.onSaveInstanceState(state);
		state.putInt("selectedGroup", selectedGroup);
    	}
    
	@Override
	protected void onDestroy() {
		super.onDestroy();
		if(sldb.isOpen()){
			sldb.close(); }
		stopUpdater();
	}
 
    	@Override
    	protected void onPause() {
    		super.onPause();
    		stopUpdater();
    	}
    
	@Override
	protected void onStart() {
        	super.onResume();
		if(!sldb.isOpen()){
			sldb = db.getReadableDatabase(); }
	    	
	      	runUpdater();
		if(selectedGroup > -1){
			elv.expandGroup(selectedGroup);
		}
   	}    
	
	@Override
	protected void onResume() {	
		super.onResume();
        	runUpdater();
		if(selectedGroup > -1){
			elv.expandGroup(selectedGroup);
		}
    	}
	
    	// Funciton that get the timed update going to pull current data
	private void runUpdater(){
		
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi.isConnected()) {
			//Start the updater task
			updateTask = new MyScheduleExecutor(1);		
			updateTask.scheduleWithFixedDelay(new ValueUpdaterTask(this, elv, mAdapter), 10, 10, TimeUnit.SECONDS);  
		}
	}

	private void stopUpdater(){
		if(updateTask != null){
			updateTask.shutdown(); }
	}

    	@Override
    	public boolean onCreateOptionsMenu(Menu menu) {
        	// Inflate the menu; this adds items to the action bar if it is present.
        	getMenuInflater().inflate(R.menu.main, menu);
        	return super.onCreateOptionsMenu(menu);
    	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle presses on the action bar items
	    	switch (item.getItemId()) {
	        	case R.id.action_settings:
	        		stopUpdater();
	        		startActivity(new Intent (MainActivity.this, SettingsActivity.class));
	            		return true;
	        	default:
	            		return super.onOptionsItemSelected(item);
	    	}
	}	
	
    	public void updateReddit(String name){
    		TextView feedView = (TextView) this.findViewById(R.id.feedUrl);
		Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Rupee_Foradian.ttf");
		feedView.setTypeface(font);   
    		feedView.setText("Reddit/r/"+name);
    		feedView.setVisibility(View.VISIBLE);    
    	
		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

		if (mWifi.isConnected()) {
			MyRedditRunnable myRunnable = new MyRedditRunnable(this, name);
			runOnUiThread(myRunnable); 
		}else{
        		TextView msgView = (TextView) this.findViewById(R.id.message);
    			msgView.setTypeface(font);   
        		msgView.setText("Network Unavailable");
        		msgView.setTextColor(Color.parseColor("#ffffff"));
        		msgView.setVisibility(View.VISIBLE); 
		}	
    	}
    
    	private static class MyRedditRunnable implements Runnable {
        	private String name;
        	private ActionBarActivity act;

        	MyRedditRunnable(Context context, String name) {
          		this.name = name;
          		act = (ActionBarActivity) context;
        	}

        	public void run() {
        		ListView aListView = (ListView) act.findViewById(R.id.feedListView);
        		ListAdapter adapter2;
			try {
				adapter2 = new SimpleAdapter(
					act,
					Utils.getRedditFeed(name),
					R.layout.feed_listview_each_item,
					new String[] { "headline", "postdate", "url" }, 
					new int[] { R.id.feedHeadline, R.id.feedPostDate, R.id.feedLink});

				aListView.setAdapter(adapter2);
				aListView.setOnItemClickListener(new OnItemClickListener() {
					@Override
					public void onItemClick(AdapterView<?> adapter, View view, int position, long arg){
						TextView urlView = (TextView) view.findViewById(R.id.feedLink);
						String url = urlView.getText().toString().toLowerCase();
						Uri uri = Uri.parse(url);
						Intent intent = new Intent(Intent.ACTION_VIEW, uri);
						act.startActivity(intent);
					}

				});
			} catch (Exception e) {}
        	}
    	}

   	public DatabaseHelper getdbHelper(){
	   	return db;
   	}
   
   	public SQLiteDatabase getSldb(){
		if(!sldb.isOpen()){
			sldb = db.getReadableDatabase(); }
	   	return sldb;
	}
	
	public class MyExpandableListAdapter extends SimpleCursorTreeAdapter {
		private MainActivity mAct;
		private FragmentActivity mFrg;
		private DatabaseHelper db;
		private SQLiteDatabase sldb;
		protected final HashMap<Integer, Integer> mGroupMap;
		public MyExpandableListAdapter(Cursor cursor, Context context, MainActivity mActivity, int groupLayout, 
				int childLayout, String[] groupFrom, int[] groupTo, String[] childrenFrom, 
				int[] childrenTo) {
            		super(context, cursor, groupLayout, groupFrom, groupTo,childLayout, childrenFrom, childrenTo);
            		mAct = mActivity;
            		mFrg = (FragmentActivity) mActivity;
            		mGroupMap = new HashMap<Integer, Integer>();
            		db = mAct.getdbHelper();
            		sldb = mAct.getSldb();
		}
	

		@Override
		protected Cursor getChildrenCursor(Cursor groupCursor) {
			
		     	Cursor childCursor = db.fetchChildren(sldb,groupCursor.getLong(groupCursor.getColumnIndex(DatabaseContract.UNIQUE_ID)));            
		     	mAct.startManagingCursor(childCursor);
		        childCursor.moveToFirst();
		        return childCursor;

		}
		
		public HashMap<Integer, Integer> getGroupMap() {
		    	return mGroupMap;
		}
		

		@Override
        	protected void bindGroupView(View view, Context paramContext, Cursor cursor, boolean paramBoolean) 
        	{
			if(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL1)) != null){
            			TextView coinNameView = (TextView) view.findViewById(R.id.coinName);
          			Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Rupee_Foradian.ttf");
        			coinNameView.setTypeface(font);               		           	
            			coinNameView.setText(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL1)) 
            				+ " [" + cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL2)) + "]");
            	
            			if(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL4)) != null){
            				TextView avgValView = (TextView) view.findViewById(R.id.avgValue);
            				String curr = cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL3));
            				avgValView.setTypeface(font);
            				avgValView.setText(curr + " " + Utils.currSymbol.get(curr) 
            					+ formatString(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL4))));
            			}
            
            			if(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL5)) != null){
            		
            				Button avgRocButton = (Button) view.findViewById(R.id.avgRoc);
            				String avgRocVal = cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL5));
            				avgRocVal = formatString(avgRocVal);
            				if(Float.parseFloat(avgRocVal) < 0){
            					avgRocButton.setBackgroundColor(Color.RED);
            				}else{
            					avgRocVal = " "+avgRocVal;        			
            					avgRocButton.setBackgroundColor(Color.GREEN);
            				}
            				avgRocButton.setTypeface(font);
            				avgRocButton.setText(avgRocVal + "%"); 
            			}
            	
              			if(cursor.getString(cursor.getColumnIndex(DatabaseContract.UNIQUE_ID)) != null){
            				TextView coinIdView = (TextView) view.findViewById(R.id.coinId);
            				coinIdView.setText(cursor.getString(cursor.getColumnIndex(DatabaseContract.UNIQUE_ID)));
            			}
			}
        	}

        	@Override
        	protected void bindChildView(View view, Context context, Cursor cursor, boolean paramBoolean) 
        	{      
        		if(cursor.getColumnIndex(DatabaseContract.TableValues.COL2) >= 0){
            			TextView sourceView = (TextView) view.findViewById(R.id.source);
          			Typeface font = Typeface.createFromAsset(getAssets(), "fonts/Rupee_Foradian.ttf");
        			sourceView.setTypeface(font);               		
            			sourceView.setText(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableValues.COL2)));
            	
            			if(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableValues.COL5)) != null){
            				TextView valView = (TextView) view.findViewById(R.id.value);
              				String curr = cursor.getString(cursor.getColumnIndex(DatabaseContract.TableValues.COL4));
            				valView.setTypeface(font);
            				valView.setText(Utils.currSymbol.get(curr) 
            				+ formatString(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableValues.COL5))));     		
            			}
            
            			if(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableValues.COL6)) != null){
            				TextView rocView = (TextView) view.findViewById(R.id.roc);
            				String rocVal = cursor.getString(cursor.getColumnIndex(DatabaseContract.TableValues.COL6));
            				rocVal = formatString(rocVal);
               				if(Float.parseFloat(rocVal) < 0){
            					rocView.setTextColor(Color.RED);
            				}else{
            					rocVal = " "+rocVal;
            					rocView.setTextColor(Color.GREEN);} 
            				rocView.setTypeface(font);               		
            				rocView.setText(rocVal + "%");
            			}
            	
              			if(cursor.getString(cursor.getColumnIndex(DatabaseContract.UNIQUE_ID)) != null){
            				TextView valIdView = (TextView) view.findViewById(R.id.valueId);
            				valIdView.setText(cursor.getString(cursor.getColumnIndex(DatabaseContract.UNIQUE_ID)));
            			}
        	
			}

         	} 
        
        	@Override
        	public boolean isChildSelectable(int groupPosition, int childPosition)
        	{
            		return true;
        	}
        
        	public String formatString(String number){
    			String formatted = nf.format(Double.parseDouble(String.valueOf(number)));
    			formatted = formatted.replaceAll(",","");
        		return formatted;
        	}
	} 	
   
}
