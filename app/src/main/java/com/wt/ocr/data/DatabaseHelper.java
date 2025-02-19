package com.wt.ocr.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ocr.db";
    private static final int DATABASE_VERSION = 1;

    // 表名和列名常量
    public static final String TABLE_NAME = "scanned_images";
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_FILENAME = "filename";
    public static final String COLUMN_TEXT = "text";
    public static final String COLUMN_SENSI_WORD = "sensi_word";
    public static final String COLUMN_IS_SENSITIVE = "is_sensitive";

    private static final String SQL_CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_FILENAME + " TEXT NOT NULL, " +
                    COLUMN_TEXT + " TEXT, " +
                    COLUMN_SENSI_WORD + " TEXT, " +
                    COLUMN_IS_SENSITIVE + " INTEGER NOT NULL DEFAULT 0)";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }
}
