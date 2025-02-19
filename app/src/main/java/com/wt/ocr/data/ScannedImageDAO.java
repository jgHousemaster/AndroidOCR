package com.wt.ocr.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.List;

public class ScannedImageDAO {
    private DatabaseHelper dbHelper;

    public ScannedImageDAO(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    // 插入新记录
    public long insert(ScannedImage image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_FILENAME, image.getFilename());
        values.put(DatabaseHelper.COLUMN_TEXT, image.getText());
        values.put(DatabaseHelper.COLUMN_SENSI_WORD, image.getSensiWord());
        values.put(DatabaseHelper.COLUMN_IS_SENSITIVE, image.isSensitive() ? 1 : 0);
        return db.insert(DatabaseHelper.TABLE_NAME, null, values);
    }

    // 查询所有记录
    public List<ScannedImage> getAll() {
        List<ScannedImage> imageList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + DatabaseHelper.TABLE_NAME;
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                ScannedImage image = new ScannedImage();
                image.setId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                image.setFilename(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FILENAME)));
                image.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT)));
                image.setSensiWord(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SENSI_WORD)));
                image.setSensitive(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_SENSITIVE)) == 1);
                imageList.add(image);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return imageList;
    }

    // 根据ID查询单条记录
    public ScannedImage getById(long id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME,
                null,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)},
                null, null, null);

        ScannedImage image = null;
        if (cursor.moveToFirst()) {
            image = new ScannedImage();
            image.setId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
            image.setFilename(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FILENAME)));
            image.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT)));
            image.setSensiWord(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SENSI_WORD)));
            image.setSensitive(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_SENSITIVE)) == 1);
        }
        cursor.close();
        return image;
    }

    // 更新记录
    public int update(ScannedImage image) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_FILENAME, image.getFilename());
        values.put(DatabaseHelper.COLUMN_TEXT, image.getText());
        values.put(DatabaseHelper.COLUMN_SENSI_WORD, image.getSensiWord());
        values.put(DatabaseHelper.COLUMN_IS_SENSITIVE, image.isSensitive() ? 1 : 0);
        return db.update(DatabaseHelper.TABLE_NAME, values,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(image.getId())});
    }

    // 删除记录
    public void delete(long id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_NAME,
                DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    // 查询敏感图片
    public List<ScannedImage> getSensitiveImages() {
        List<ScannedImage> imageList = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query(DatabaseHelper.TABLE_NAME,
                null,
                DatabaseHelper.COLUMN_IS_SENSITIVE + " = 1",
                null, null, null, null);

        if (cursor.moveToFirst()) {
            do {
                ScannedImage image = new ScannedImage();
                image.setId(cursor.getLong(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
                image.setFilename(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_FILENAME)));
                image.setText(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEXT)));
                image.setSensiWord(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_SENSI_WORD)));
                image.setSensitive(true);
                imageList.add(image);
            } while (cursor.moveToNext());
        }
        cursor.close();
        return imageList;
    }
    
}