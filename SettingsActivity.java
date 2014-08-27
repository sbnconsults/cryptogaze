/*
 *  Name: SettingsActivity.java
 *  Description: Activity for setting currency preferences
 * 		If (internet connection is obtained)
 * 			the current exchange rate converts old currency values
 *		else
 *			an updater task runs everytime making sure values get updated
 *
 */
package com.idealtechlabs.cryptogaze;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;


public class SettingsActivity extends ActionBarActivity {
	SettingsAdapter lAdapter;
	Map<String, String[]> newSetting = new HashMap<String,String[]> ();
	String[] currArray;
	LayoutInflater inflater;
	private ContentResolver cResolver;
	private SQLiteDatabase sldb;

	
	private static final String[] VALUES_PROJECTION = new String[] {
		DatabaseContract.UNIQUE_ID,
		DatabaseContract.TableValues.COL5,
	};
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
        inflater = (LayoutInflater) this.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        cResolver = getApplicationContext().getContentResolver();

		setContentView(R.layout.activity_settings);

		currArray = getResources().getStringArray(R.array.currency_array);
		DatabaseHelper db = new DatabaseHelper(this);
		sldb = db.getReadableDatabase();

		Cursor lCursor = db.fetchGroup(sldb);
		ListView la = (ListView) this.findViewById(R.id.settingsList);
		lAdapter = new SettingsAdapter(this,
			R.layout.setting_each_item,
			lCursor,
			new String[] {DatabaseContract.TableCoin.COL1 },
			new int[] {R.id.coinToSet},
		1);
		la.setAdapter(lAdapter);

	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.settings, menu);
        return super.onCreateOptionsMenu(menu);
    }
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle presses on the action bar items
	    switch (item.getItemId()) {
	        case R.id.action_settings_done:
			try {
				Utils.updateCurrency(this, newSetting, true);
		    	finish();
		    	sldb.close();
			} catch (SAXException e) {
				e.printStackTrace();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}	

	// Function that handles the currency conversion
	private void updateCurrency() throws SAXException, ParserConfigurationException, URISyntaxException{
		HashMap exchange = null;
    	for (Map.Entry<String, String[]> entry : newSetting.entrySet()){
    		   
    		String coinId = entry.getKey();
    		String[] values = entry.getValue();
    		String oldCurr = values[0];
    		String newCurr = values[1];
    		
    		if(oldCurr.equals(newCurr)){
    			continue;
    		}

    		ConnectivityManager connManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
    		NetworkInfo mWifi = connManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);

    		if (mWifi.isConnected()) {
    			if(exchange == null){
    				exchange = Utils.getCurrExchange(); }
    		
    			float newTotal = 0;
    			Cursor valueCursor = cResolver.query(MyContentProvider.VALUES_CONTENT_URI, VALUES_PROJECTION, 
    				DatabaseContract.TableValues.COL1 + " = " + coinId, null, null);
    		
    			if(valueCursor.moveToFirst()){
    				do {
    					int valueId = valueCursor.getInt(0);
    					float oldVal = valueCursor.getFloat(1);
    					float newVal;
					
    					try{
    						newVal = Utils.convCurr(exchange, oldVal, oldCurr, newCurr );
    					}
    					catch(Exception e){
    						continue;
    					}
    					newTotal += newVal;
    					ContentValues valuesNew = new ContentValues();
    					valuesNew.put(DatabaseContract.TableValues.COL4, newCurr);
    					valuesNew.put(DatabaseContract.TableValues.COL5, newVal);
    					valuesNew.put(DatabaseContract.TableValues.COL7, Utils.getDateTime());
    					cResolver.update(Uri.withAppendedPath(MyContentProvider.VALUES_CONTENT_URI,
							String.valueOf(valueId)), valuesNew, null,null);

					} while (valueCursor.moveToNext());
				} 
				valueCursor.close();
				float newAvg = 0;
				if(valueCursor.getCount() != 0){
					newAvg = newTotal / valueCursor.getCount();
				}
				newAvg = Float.parseFloat(String.format("%.2f", newAvg));
				
				ContentValues coinNew = new ContentValues();
				coinNew.put(DatabaseContract.TableCoin.COL3, newCurr);
				coinNew.put(DatabaseContract.TableCoin.COL4, newAvg);
				coinNew.put(DatabaseContract.TableCoin.COL6, Utils.getDateTime());
				coinNew.put(DatabaseContract.TableCoin.COL7, newCurr);
				cResolver.update(Uri.withAppendedPath(MyContentProvider.COINS_CONTENT_URI,
					String.valueOf(coinId)), coinNew, null,null);
    		}else{
				ContentValues coinNew = new ContentValues();
				coinNew.put(DatabaseContract.TableCoin.COL7, newCurr);				
				cResolver.update(Uri.withAppendedPath(MyContentProvider.COINS_CONTENT_URI,
						String.valueOf(coinId)), coinNew, null,null);   			
    		}
	
    	}
    	finish();
    	sldb.close();
	}

	public class Viewholder{
	    View rowView;
	    TextView coin;
	    Spinner spinner;
	    TextView coinId;
	    String oldVal;
	}
	
	public class SettingsAdapter extends SimpleCursorAdapter{
		private Context context;
		Cursor cursor;
		final Map positions =new HashMap<String, String >();
		public SettingsAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags){
		    super(context, layout, c, from, to, flags);
		    this.context = context;
		    this.cursor = c;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent){
			
			Viewholder v;
			if (convertView == null) {
			    // create a new holder for the new item view
				convertView = inflater.inflate(R.layout.setting_each_item, parent,false);
			    v = new Viewholder();
			    
			    // populate the new holder's fields
			    v.coin = (TextView) convertView.findViewById(R.id.coinToSet);
			    v.spinner = (Spinner) convertView.findViewById(R.id.crncySpinner);
			    v.coinId = (TextView) convertView.findViewById(R.id.coinToSetId);
		        ArrayAdapter<CharSequence> adapter=ArrayAdapter.createFromResource(context,R.array.currency_array,android.R.layout.simple_spinner_item);
		        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		        v.spinner.setAdapter(adapter);
		        
		        v.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener()
		        {
		            @Override
		            public void onItemSelected(AdapterView<?> parent, View view,int pos, long id)
		            {
		            	View pp =  (View) parent.getParent();
		            	Viewholder ppvh = (Viewholder) pp.getTag();
		            	
		            	if(ppvh != null){
		            		String coinId = (String) ppvh.coinId.getText();
		            		String[] currVals = {ppvh.oldVal,currArray[(int) id]};
		            		newSetting.put(coinId, currVals);
		            	}

		            }

		            @Override
		            
		            
		            public void onNothingSelected(AdapterView<?> parent) {
		                // Another interface callback
		            } 
		        });

				cursor.moveToPosition(position);
	    		v.coin.setText(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL1))
	    				+ " [" + cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL2)) + "]");
	    		v.coinId.setText(cursor.getString(cursor.getColumnIndex(DatabaseContract.UNIQUE_ID)));
	    	    v.oldVal = cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL7));
		        for (int i = 0; i < currArray.length; i++) {
		            int r = currArray[i].indexOf(cursor.getString(cursor.getColumnIndex(DatabaseContract.TableCoin.COL7)));
		            if(r >= 0) {
		            	v.spinner.setSelection(i);
		            }
		        }
		        convertView.setTag(v);
			} else {
			    v = (Viewholder) convertView.getTag();
			}

			return convertView;

		} 
		

	}
}
