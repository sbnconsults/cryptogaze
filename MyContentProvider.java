/*
 *  Name: MyContentProvider.java
 *  Description: handles delete, query and update (insert not used)
 *
 */
package com.idealtechlabs.cryptogaze;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;


public class MyContentProvider extends ContentProvider {
	private DatabaseHelper dbh;
    	private static final String AUTHORITY = "com.idealtechlabs.cryptogaze.MyContentProvider";
    	public static final int COIN = 100;
	public static final int COIN_ID = 110;
    	public static final int VALUE = 200;
    	public static final int VALUE_ID = 210;
    	private static final String DEBUG = "test";
     
    	public static final Uri COINS_CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + DatabaseContract.TableCoin.TABLE_NAME);
    
    	public static final Uri VALUES_CONTENT_URI = Uri.parse("content://" + AUTHORITY
            + "/" + DatabaseContract.TableValues.TABLE_NAME);
     
    	public static final String CONTENT_ITEM_TYPE = ContentResolver.CURSOR_ITEM_BASE_TYPE
            + "/mt-tutorial";
    	public static final String CONTENT_TYPE = ContentResolver.CURSOR_DIR_BASE_TYPE
            + "/mt-tutorial";
 
    	@Override
    	public boolean onCreate() {
        	dbh = new DatabaseHelper(getContext());
        	return true;
    	}
    
    	@Override
    	public Cursor query(Uri uri, String[] projection, String selection,
            String[] selectionArgs, String sortOrder) {
        	SQLiteQueryBuilder queryBuilder = new SQLiteQueryBuilder();
        
        	int uriType = sURIMatcher.match(uri);
        
        	String tableName = (uriType == COIN_ID || uriType == COIN) ? 
        		DatabaseContract.TableCoin.TABLE_NAME : DatabaseContract.TableValues.TABLE_NAME;

        	switch (uriType) {
        		case COIN_ID:
        		case VALUE_ID:
        			queryBuilder.setTables(tableName);
        			queryBuilder.appendWhere(DatabaseContract.UNIQUE_ID + "=" + uri.getLastPathSegment());
        			break;
        		case COIN:
        		case VALUE:
        			queryBuilder.setTables(tableName);
        			break;
			 default:
            			throw new IllegalArgumentException("Unknown URI");
        	}
        
  
        	Cursor cursor = queryBuilder.query(dbh.getReadableDatabase(),
                	projection, selection, selectionArgs, null, null, sortOrder);
        	return cursor;
    	}
    
    	@Override
    	public int delete(Uri uri, String selection, String[] selectionArgs) {
        	int uriType = sURIMatcher.match(uri);
        	SQLiteDatabase sldb = dbh.getWritableDatabase();
        	int rowsAffected = 0;
        	String tableName = (uriType == COIN_ID || uriType == COIN) ? 
        		DatabaseContract.TableCoin.TABLE_NAME : DatabaseContract.TableValues.TABLE_NAME;
        	switch (uriType) {
        		case COIN:
        		case VALUE:
            			rowsAffected = sldb.delete(tableName,selection, selectionArgs);
            			break;
        		case COIN_ID:
        		case VALUE_ID:
            			String id = uri.getLastPathSegment();
            			rowsAffected = sldb.delete(tableName, DatabaseContract.UNIQUE_ID + "=" + id +
            				(!TextUtils.isEmpty(selection) ? " AND (" + selection + ')' : ""), selectionArgs);
            			break;
        		default:
            			throw new IllegalArgumentException("Unknown or Invalid URI " + uri);
        	}
        	return rowsAffected;
    	}
    
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
        	int uriType = sURIMatcher.match(uri);
        	SQLiteDatabase sldb = dbh.getWritableDatabase();
        	int rowsAffected = 0;
        	String tableName = (uriType == COIN_ID || uriType == COIN) ? 
        		DatabaseContract.TableCoin.TABLE_NAME : DatabaseContract.TableValues.TABLE_NAME;
        	switch (uriType) {
        		case COIN:
        		case VALUE:
        			rowsAffected = sldb.update(tableName, values, selection, selectionArgs);
        			break;
        		case COIN_ID:
        		case VALUE_ID:
            			String id = uri.getLastPathSegment();
 
            			rowsAffected = sldb.update(tableName, values,
            				DatabaseContract.UNIQUE_ID + "=" + id +(!TextUtils.isEmpty(selection) ? " AND (" 
            				+ selection + ')' : ""), selectionArgs);
  
            			break;
        		default:
            			throw new IllegalArgumentException("Unknown or Invalid URI " + rowsAffected);
        	}

        	return rowsAffected;
	}

    	private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    	static {
        	sURIMatcher.addURI(AUTHORITY, DatabaseContract.TableCoin.TABLE_NAME, COIN);
        	sURIMatcher.addURI(AUTHORITY, DatabaseContract.TableCoin.TABLE_NAME + "/#", COIN_ID);
        	sURIMatcher.addURI(AUTHORITY, DatabaseContract.TableValues.TABLE_NAME, VALUE);
        	sURIMatcher.addURI(AUTHORITY, DatabaseContract.TableValues.TABLE_NAME + "/#", VALUE_ID);
    	}
    
	 @Override
	public String getType(Uri uri) {
		// TODO Auto-generated method stub
		return null;
	}
}
    
  
