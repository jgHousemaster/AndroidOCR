package com.wt.ocr;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import com.wt.ocr.utils.Img2TxtUtil;

import java.util.Map;

public class PhotoObserverService extends Service {
    private ContentObserver observer;
    public static final String CHANNEL_ID = "sullivan_photo_notification_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        observer = new ContentObserver(new Handler()) {
            @Override
            public void onChange(boolean selfChange, Uri uri) {
                checkForNewPhoto(uri);
            }
        };
        getContentResolver().registerContentObserver(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, true, observer);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Service Running")
                .setContentText("Photo observer service is active.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();

        startForeground(1, notification);
    }

    private void checkForNewPhoto(Uri uri) {
        try (Cursor cursor = getContentResolver().query(uri, 
                new String[]{
                    MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.DATA  // 添加 DATA 列来获取文件路径
                }, 
                null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                int dataColumnIndex = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                
                if (columnIndex != -1 && dataColumnIndex != -1) {  // 确保两个列索引都有效
                    String albumName = cursor.getString(columnIndex);
                    if ("Sullivan".equals(albumName)) {
                        String imagePath = cursor.getString(dataColumnIndex);
                        if (imagePath != null) {
                            Map<String, Object> result = Img2TxtUtil.AnalyzeImage(imagePath);
                            if ((boolean)result.get("isSensitive")) {
                                showNotification(result);
                            }
                        } else {
                            Log.e("PhotoObserverService", "Image path is null");
                        }
                    }
                } else {
                    Log.e("PhotoObserverService", 
                          "Column not found: BUCKET_DISPLAY_NAME=" + columnIndex + 
                          ", DATA=" + dataColumnIndex);
                }
            }
        } catch (Exception e) {
            Log.e("PhotoObserverService", "Error checking for new photo", e);
        }
    }


    private void showNotification(Map<String, Object> result) {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("ALERT: THe photo you just took might be sensitive")
                .setContentText("Tap to view details")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setCategory(Notification.CATEGORY_ERROR)
                .setColorized(true)
                .setColor(Color.RED)
                .setDefaults(Notification.DEFAULT_ALL)
                .build();
        notificationManager.notify(1, notification);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        getContentResolver().unregisterContentObserver(observer);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
