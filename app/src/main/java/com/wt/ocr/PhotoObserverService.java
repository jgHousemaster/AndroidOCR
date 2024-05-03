package com.wt.ocr;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;

import androidx.core.app.NotificationCompat;

public class PhotoObserverService extends Service {
    private ContentObserver observer;
    public static final String CHANNEL_ID = "sullivan_photo_notification_channel";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("ning", "Service_onCreate");
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
        Log.d("ning", "我在执行");
        try (Cursor cursor = getContentResolver().query(uri, new String[]{MediaStore.Images.Media.BUCKET_DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                if (columnIndex != -1) {  // 确保获取到有效的列索引
                    String albumName = cursor.getString(columnIndex);
                    if ("Sullivan".equals(albumName)) {
                        showNotification();
                    }
                } else {
                    Log.e("PhotoObserverService", "Column not found: " + MediaStore.Images.Media.BUCKET_DISPLAY_NAME);
                }
            }
        } catch (Exception e) {
            Log.e("PhotoObserverService", "Error checking for new photo", e);
        }
    }


    private void showNotification() {
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("New Photo Added")
                .setContentText("A new photo was added to album Sullivan.")
                .setSmallIcon(R.mipmap.ic_launcher)
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
