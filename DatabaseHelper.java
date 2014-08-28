/*
 *  Name: DatabaseHelper.java
 *  Description: Handles initial database population and
 *      and execution of inserts and queries
 *
 */
package com.idealtechlabs.cryptogaze;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Hashtable;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.util.Log;

public class DatabaseHelper extends SQLiteOpenHelper {
	private static final String LOG_TAG          = "initial_logs";
	private Hashtable<String, Long> coinIdHash = new Hashtable<String, Long>();
	private AssetManager amngr;
	private Context context;
	
    	public DatabaseHelper(Context context) {
    		super(context, DatabaseContract.DATABASE_NAME, null, DatabaseContract.DATABASE_VERSION);
    		this.context = context;
    		amngr = context.getAssets();
    	}

    	// Method is called during creation of the database
    	@Override
    	public void onCreate(SQLiteDatabase db) {
        	try{
        		Cursor cursor = db.rawQuery("SELECT * FROM " + DatabaseContract.TableCoin.TABLE_NAME + " LIMIT 1", null);
        		if(cursor!=null) {
        			cursor.close();
        		}
        	}catch(Exception e){
    			db.execSQL(DatabaseContract.TableCoin.CREATE_TABLE);
    			db.execSQL(DatabaseContract.TableValues.CREATE_TABLE);
    			initDatabaseData(db);
    		}
    	}

    	// Method is called during an upgrade of the database
    	@Override
    	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
 
    	}
    
    	public Cursor getData(SQLiteDatabase db){

		// Define a projection that specifies which columns from the database
        	// to query
        	String[] columns = {
        		DatabaseContract.UNIQUE_ID,
        		DatabaseContract.TableCoin.COL1,
        		DatabaseContract.TableCoin.COL2,
        		DatabaseContract.TableCoin.COL3,
        	};

        	Cursor c = db.query(
        		DatabaseContract.TableCoin.TABLE_NAME,
            		columns, 
            		null,
            		null,
         		null,
            		null,
            		null
            	);
        
        	return c;
      
    	}
    
    	public Cursor fetchGroup(SQLiteDatabase db) {
        	String query = "SELECT * FROM " + DatabaseContract.TableCoin.TABLE_NAME;
        	return db.rawQuery(query , null);
    	}

 	public Cursor fetchChildren(SQLiteDatabase db, Long coinId) {
        	String query = "SELECT * FROM " + DatabaseContract.TableValues.TABLE_NAME +" WHERE "+ 
        		DatabaseContract.TableValues.COL1 +"=" + coinId;
        	return db.rawQuery(query, null);
    	}
    
    	private void insertCoinData(SQLiteDatabase db, String [] rowData) {
    	
    		// Create insert entries
        	ContentValues values = new ContentValues();
        	values.put(DatabaseContract.TableCoin.COL1, rowData[0]);
        	values.put(DatabaseContract.TableCoin.COL2, rowData[1]);
        	values.put(DatabaseContract.TableCoin.COL3, rowData[2]);
        	values.put(DatabaseContract.TableCoin.COL4, rowData[3]);
        	values.put(DatabaseContract.TableCoin.COL5, rowData[4]);
        	values.put(DatabaseContract.TableCoin.COL6, rowData[5]);
        	values.put(DatabaseContract.TableCoin.COL7, rowData[6]);

        	// Insert the new row, returning the primary key value of the new row
        	Long coinId = db.insert(
                	DatabaseContract.TableCoin.TABLE_NAME,
                 	null,
                 	values);
 
        	coinIdHash.put(rowData[1], coinId);
    	}
    
    	private void insertValueData(SQLiteDatabase db, String [] rowData) {
    		// Create insert entries
    		Long coinId = coinIdHash.get(rowData[0]);
        	ContentValues values = new ContentValues();
        
        	values.put(DatabaseContract.TableValues.COL1, coinId);
        	values.put(DatabaseContract.TableValues.COL2, rowData[1]);
        	values.put(DatabaseContract.TableValues.COL3, rowData[2]);
        	values.put(DatabaseContract.TableValues.COL4, rowData[3]);
        	values.put(DatabaseContract.TableValues.COL5, rowData[4]);
        	values.put(DatabaseContract.TableValues.COL6, rowData[5]);
        	values.put(DatabaseContract.TableValues.COL7, rowData[6]);
        

        	// Insert the new row
        	db.insert(
        		DatabaseContract.TableValues.TABLE_NAME,
                 	null,
                 	values);
    	}
    	
    	private void initDatabaseData(SQLiteDatabase db) {
 
        	BufferedReader br1 = null;
        	BufferedReader br2 = null;
        
        	try {
            		br1 = new BufferedReader(new InputStreamReader(amngr.open("database/InitialCoinData.txt")));
            		br2 = new BufferedReader(new InputStreamReader(amngr.open("database/InitialValueData.txt")));
            		String line = null;
            		db.beginTransaction();
            		while ((line = br1.readLine()) != null) {
            			String[] rowData = line.split("\\|");
            			insertCoinData(db, rowData);
            		}
            		line = null;
            		while ((line = br2.readLine()) != null) {
            			String[] rowData = line.split("\\|");
            			insertValueData(db, rowData);
            	}
            
            	db.setTransactionSuccessful();
         
        	} catch (IOException e) {
            		Log.e(LOG_TAG, "read database init file error");
        	} finally {
            		db.endTransaction();
            		if (br1 != null || br2 != null) {
                		try {
                    			if(br1 != null) { br1.close(); } else { br2.close(); }                   
                		} catch (IOException e) {
                    			Log.e(LOG_TAG, "buffer reader close error");
                		}
            		}
        	}
    	}
}
