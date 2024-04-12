package com.wt.ocr;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class AlbumScanner {
    private static final String TAG = "AlbumScanner";

    // 扫描指定名称的相册并获取其中的所有图片
    private List<String> scanAlbum(Context context, String albumName) {
        List<String> albumImages = new ArrayList<>();

        // 查询图片信息的 Uri
        Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

        // 查询的列
        String[] projection = {MediaStore.Images.Media.DATA};

        // 查询条件
        String selection = MediaStore.Images.Media.BUCKET_DISPLAY_NAME + " = ?";
        String[] selectionArgs = new String[]{albumName};

        // 排序方式
        String sortOrder = MediaStore.Images.Media.DATE_MODIFIED + " DESC";

        // 执行查询
        ContentResolver contentResolver = context.getContentResolver();
        Cursor cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder);

        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    // 获取图片路径
                    String imagePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));
                    albumImages.add(imagePath);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error scanning album: " + e.getMessage());
            } finally {
                cursor.close();
            }
        }

        return albumImages;
    }
}
