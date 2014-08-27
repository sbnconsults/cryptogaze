/*
 *  Name: DatabaseContract.java
 *  Description: Contract class for database holding bit currency data
 *
 */
package com.idealtechlabs.cryptogaze;

import android.provider.BaseColumns;

public final class DatabaseContract {

    public static final  int    DATABASE_VERSION   = 1;
    public static final  String DATABASE_NAME      = "Coin.db";
    public static final  String UNIQUE_ID		   = "_id";
    private static final String STEXT_TYPE          = " VARCHAR(15)";
    private static final String MTEXT_TYPE          = " VARCHAR(100)";
    private static final String DEC_TYPE          = " DECIMAL";
    private static final String DATE_TYPE         = " DATETIME";
    private static final String COMMA_SEP          = ",";
  

    // To prevent someone from accidentally instantiating the contract class,
    // give it an empty constructor.
    public DatabaseContract() {}

    public static abstract class TableCoin implements BaseColumns {
        public static final String TABLE_NAME       = "coin";
        public static final String COL1 = "name";
        public static final String COL2 = "ticker";
        public static final String COL3 = "currency";
        public static final String COL4 = "value";
        public static final String COL5 = "roc";
        public static final String COL6 = "timestamp";
        public static final String COL7 = "currencySet";
        
        public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME + " (" +
                UNIQUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                COL1 + STEXT_TYPE + COMMA_SEP +
                COL2 + STEXT_TYPE + COMMA_SEP +
                COL3 + DEC_TYPE + COMMA_SEP +
                COL4 + DEC_TYPE + COMMA_SEP +
                COL5 + DEC_TYPE + COMMA_SEP +
                COL6 + DATE_TYPE + COMMA_SEP +
                COL7 + DEC_TYPE + ")";
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
    
    public static abstract class TableValues implements BaseColumns {
        public static final String TABLE_NAME       = "coinValues";
        public static final String COL1 = "coin_id";
        public static final String COL2 = "source";
        public static final String COL3 = "api";
        public static final String COL4 = "currency";
        public static final String COL5 = "value";
        public static final String COL6 = "roc";
        public static final String COL7 = "timestamp";       

        public static final String CREATE_TABLE = "CREATE TABLE IF NOT EXISTS " +
                TABLE_NAME + " (" +
                UNIQUE_ID + " INTEGER PRIMARY KEY AUTOINCREMENT" + COMMA_SEP +
                COL1 + " INTEGER" + COMMA_SEP +
                COL2 + STEXT_TYPE +COMMA_SEP +
                COL3 + MTEXT_TYPE + COMMA_SEP +
                COL4 + DEC_TYPE + COMMA_SEP +
                COL5 + DEC_TYPE + COMMA_SEP +
                COL6 + DEC_TYPE + COMMA_SEP +
                COL7 + DATE_TYPE + ")";
        		
        public static final String DELETE_TABLE = "DROP TABLE IF EXISTS " + TABLE_NAME;
    }
}
