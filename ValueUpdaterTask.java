/*
 *  Name: ValueUpdaterTask.java
 *  Description: Updates the database with current quotes
 *
 */
package com.idealtechlabs.cryptogaze;

import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import javax.xml.parsers.ParserConfigurationException;

import org.json.JSONException;
import org.xml.sax.SAXException;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.support.v4.app.FragmentActivity;
import android.util.Log;
import android.widget.ExpandableListView;

import com.idealtechlabs.cryptogaze.MainActivity.MyExpandableListAdapter;

public class ValueUpdaterTask implements Runnable {
	final static DateFormat fmt = DateFormat.getTimeInstance(DateFormat.LONG);
	private ContentResolver cResolver;
	private ConnectivityManager connMgr;
	private Context context;
	private ExpandableListView elv;
	private MyExpandableListAdapter mAdapter;
	
	private static final String[] COIN_PROJECTION = new String[] {
		DatabaseContract.UNIQUE_ID,
		DatabaseContract.TableCoin.COL2,
		DatabaseContract.TableCoin.COL3,
		DatabaseContract.TableCoin.COL4,
		DatabaseContract.TableCoin.COL5,
		DatabaseContract.TableCoin.COL7
	};
	
	private static final String[] VALUES_PROJECTION = new String[] {
		DatabaseContract.UNIQUE_ID,
		DatabaseContract.TableValues.COL2,
		DatabaseContract.TableValues.COL3,
		DatabaseContract.TableValues.COL5,
		DatabaseContract.TableValues.COL6,
	};
	
	public ValueUpdaterTask(Context context, ExpandableListView elv, MyExpandableListAdapter mAdapter) {
		super();
		this.context = context;
		this.elv = elv;
		this.mAdapter = mAdapter;


		cResolver = this.context.getContentResolver();
	}

	@Override
	public void run() {
		try{
			valueUpdate();
			System.out.println("\t delayTask Execution Time: " + fmt.format(new Date()));
		}catch(Exception e){
			e.printStackTrace();
			throw new RuntimeException(e);
		}
	}

	private void valueUpdate() throws SAXException, ParserConfigurationException, JSONException, URISyntaxException{
		Cursor coinCursor = cResolver.query(MyContentProvider.COINS_CONTENT_URI,COIN_PROJECTION,null,null,null);
		
		//For each coin work out the update
		if(coinCursor.moveToFirst()){
			do {
				float newTotal = 0;
				int coinId = coinCursor.getInt(0);
				String ticker = coinCursor.getString(1);
				String coinCurr= coinCursor.getString(2);
				float coinOldAvg = coinCursor.getFloat(3);
				String setCurr= coinCursor.getString(5);
				System.out.println(coinCurr+ "  "+ setCurr);
				if(!coinCurr.equals(setCurr)){
					continue;
				}
				Cursor valueCursor = cResolver.query(MyContentProvider.VALUES_CONTENT_URI, VALUES_PROJECTION, 
						DatabaseContract.TableValues.COL1 + " = " + String.valueOf(coinId), null, null);
				
				HashMap exchange;
				try{
					exchange = Utils.getCurrExchange();
				}catch(Exception e){
					Log.e("CURR","Error getting currency from updater");
					continue;
				}
				if(valueCursor.moveToFirst()){
					do {
						int valueId = valueCursor.getInt(0);
						String source = valueCursor.getString(1);
						String api = valueCursor.getString(2);
						float sourceOldVal = valueCursor.getFloat(3);
						float newVal;
						try{
						newVal = Utils.getNewValue(api,source,ticker);
						if(!coinCurr.equals("USD")){
							newVal = Utils.convCurr(exchange, newVal, "USD", coinCurr );
						}else{
							newVal = Utils.formatCurrency(newVal);
						}
						}catch(Exception e){
							Log.e("UPDATE","Error updating value "+api);
							continue;
						}
						float valRoc = calcROC( sourceOldVal,newVal);
						newTotal += newVal;
						ContentValues valuesNew = new ContentValues();
						valuesNew.put(DatabaseContract.TableValues.COL5, newVal);
						valuesNew.put(DatabaseContract.TableValues.COL6, valRoc);
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
				float coinRoc = calcROC( coinOldAvg, newAvg);
				newAvg = Float.parseFloat(String.format("%.2f", newAvg));
				
				ContentValues coinNew = new ContentValues();
				coinNew.put(DatabaseContract.TableValues.COL5, newAvg);
				coinNew.put(DatabaseContract.TableValues.COL6, coinRoc);
				coinNew.put(DatabaseContract.TableValues.COL7, Utils.getDateTime());
				cResolver.update(Uri.withAppendedPath(MyContentProvider.COINS_CONTENT_URI,
					String.valueOf(coinId)), coinNew, null,null);
	
			} while (coinCursor.moveToNext());
			coinCursor.close();
		}
		
		cResolver.notifyChange(MyContentProvider.COINS_CONTENT_URI, null,true);
		cResolver.notifyChange(MyContentProvider.VALUES_CONTENT_URI, null,true);
		
		FragmentActivity act = (FragmentActivity) context;
		act.runOnUiThread(new Runnable() {
		     @Override
		     public void run() {
		    		mAdapter.getCursor().requery();
		     }
		});
	}

	private float calcROC(float oldVal, float newVal){
		if( oldVal == 0 || newVal == 0 ){
			return 0;
		}else{
			float ROC = ((newVal/oldVal) - 1) * 100;
			return Float.parseFloat(String.format("%.2f", ROC));
		}
	}

}