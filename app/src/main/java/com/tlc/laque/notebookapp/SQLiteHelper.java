package com.tlc.laque.notebookapp;


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class SQLiteHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    public static final String DATABASE_NAME = "SQLiteDatabase.db";
    public static final String tableName = "tableWords";
    public static final String COLUMN_ID = "idColumn";
    public static final String ORIGINAL_WORD = "originalWord";
    public static final String TRANSLATED_WORD = "translatedWord";
    public static final String FIRST_LETTER = "firstLetter";

    public SQLiteHelper(Context context) {

        super(context, DATABASE_NAME, null, DATABASE_VERSION);

        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("create table " + tableName + " ( " + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," + ORIGINAL_WORD + " TEXT, " + TRANSLATED_WORD + " TEXT, " + FIRST_LETTER + " TEXT);");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + tableName);
            onCreate(db);
        }



}


