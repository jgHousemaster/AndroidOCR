package com.wt.ocr;

import static com.wt.ocr.App.CHANNEL_ID;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.wt.ocr.databinding.ActivityMainBinding;
import com.wt.ocr.utils.Img2TxtUtil;
import com.wt.ocr.utils.Utils;
import com.wt.ocr.adapter.ScanResultAdapter;
import com.wt.ocr.data.ScannedImage;
import com.wt.ocr.data.ScannedImageDAO;
import com.wt.ocr.data.DatabaseHelper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import me.xdrop.fuzzywuzzy.FuzzySearch;
import com.bumptech.glide.Glide;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.app.AlertDialog;
import android.net.Uri;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import com.wt.ocr.fragment.HomeFragment;
import com.wt.ocr.fragment.SettingsFragment;

// 获取权限，复制 assets 中的文件，跳转到拍照页面

public class MainActivity extends BaseActivity {

    private static final int PERMISSIONS_REQUEST_CAMERA = 454;

    static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final int REQUEST_CODE_MEDIA_PERMISSION = 101;

    private FirebaseAnalytics mFirebaseAnalytics;

    private ActivityMainBinding binding;
    private ScanResultAdapter adapter;

    // 图像识别
    private ColorMatrix colorMatrix;
    private TessBaseAPI baseApi = new TessBaseAPI();
    private static String LANGUAGE_PATH = "";
    private static final String LANGUAGE = "eng";//chi_sim | eng
    private String[] compareList;
    // 是否需要弹窗
    private boolean alertNeeded = false;

    private HomeFragment homeFragment;
    private SettingsFragment settingsFragment;
    private Fragment activeFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main);
        
        // 初始化 OCR
        Img2TxtUtil.init(this);

        // 启动服务
        startPhotoObserverService();
        
        // 初始化 Fragment
        setupFragments();
        
        // 设置底部导航栏
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    switchToFragment(homeFragment);
                    return true;
                case R.id.navigation_settings:
                    switchToFragment(settingsFragment);
                    return true;
            }
            return false;
        });
        
        // 默认选中主页
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_home);
    }
    
    private void setupFragments() {
        homeFragment = new HomeFragment();
        settingsFragment = new SettingsFragment();
        
        // 初始添加两个 Fragment，但只显示 homeFragment
        getSupportFragmentManager().beginTransaction()
                .add(R.id.fragment_container, homeFragment)
                .add(R.id.fragment_container, settingsFragment)
                .hide(settingsFragment)
                .commit();
        
        activeFragment = homeFragment;
    }
    
    private void switchToFragment(Fragment fragment) {
        if (fragment != activeFragment) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.setCustomAnimations(
                    android.R.anim.fade_in,
                    android.R.anim.fade_out);
            transaction.hide(activeFragment).show(fragment).commit();
            activeFragment = fragment;
        }
    }

    private void startPhotoObserverService() {
        Intent serviceIntent = new Intent(this, PhotoObserverService.class);
        startService(serviceIntent);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_MEDIA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "权限已开启", Toast.LENGTH_SHORT).show();
            } else {
                // 权限拒绝，弹出提示
                Toast.makeText(this, "请开启权限", Toast.LENGTH_SHORT).show();
                Img2TxtUtil.requestMediaPermission();
            }
        }
    }

    void openAlum() {
        // 扫描相册，提取所有图片
        Intent intent = new Intent();
        // 设置文件类型为图片（MIME 类型）
        intent.setType("image/*");
        // 打开相册选择界面
        intent.setAction(Intent.ACTION_GET_CONTENT);
        // 启动 intent 并要求返回结果，结果将在 onActivityResult() 方法中处理
        startActivityForResult(intent, 1);
    }

}
