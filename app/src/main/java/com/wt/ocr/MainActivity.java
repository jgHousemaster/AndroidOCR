package com.wt.ocr;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.databinding.DataBindingUtil;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.wt.ocr.databinding.ActivityMainBinding;
import com.wt.ocr.utils.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

// 获取权限，复制 assets 中的文件，跳转到拍照页面

public class MainActivity extends BaseActivity implements View.OnClickListener {

    private static final int PERMISSIONS_REQUEST_CAMERA = 454;

    static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
    static final String PERMISSION_WRITE_STORAGE = Manifest.permission.WRITE_EXTERNAL_STORAGE;

    private static final int REQUEST_CODE_MEDIA_PERMISSION = 101;

    private FirebaseAnalytics mFirebaseAnalytics;

    private ActivityMainBinding mBinding;

    // 图像识别
    private ColorMatrix colorMatrix;
    private TessBaseAPI baseApi = new TessBaseAPI();
    private static String LANGUAGE_PATH = "";
    private static final String LANGUAGE = "eng";//chi_sim | eng

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_main);
//        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
//        mBinding.btnCamera.setOnClickListener(this);
        mBinding.btnStart.setOnClickListener(this);

        // 检查读取权限
        if (!isMediaPermissionGranted()) {
            requestMediaPermission();
        }

        initTess();

        new Thread(new Runnable() {
            @Override
            public void run() {
                deepFile("tessdata");
            }
        }).start();
    }

    public void onClick(View view) {
        if (view.getId() == R.id.btn_start) {
            Toast.makeText(this, "正在读取 Sullivan 相册中的图片", Toast.LENGTH_SHORT).show();
            List<String> images = AlbumScanner.scanAlbum(this, "Sullivan");
            String result = "读取完成，图片数量：" + images.size() + "\n\n";
            result += "读取时间：" + Utils.getNowTime() + "\n\n";
            int i = 0;
            for (String image : images) {
                result += "图片 " + i + " :" + img2Text(image);
                i++;
            }
            mBinding.tvResult.setText(result);
        }
//            checkSelfPermission();
        }
//        if (view.getId() == R.id.btn_camera) {
//            // 检查权限，权限通过则跳转到拍照页面
//            checkSelfPermission();
//            //google firebase 分析，貌似没用
//            Bundle bundle = new Bundle();
//            bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "main");
//            bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "拍照");
//            bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Action");
//            mFirebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
//        }
//    }

    /**
     * 将assets中的文件复制出
     *
     * @param path
     */
    public void deepFile(String path) {
        String newPath = getExternalFilesDir(null) + "/";
        try {
            String str[] = getAssets().list(path);
            if (str.length > 0) {//如果是目录
                File file = new File(newPath + path);
                file.mkdirs();
                for (String string : str) {
                    path = path + "/" + string;
                    deepFile(path);
                    path = path.substring(0, path.lastIndexOf('/'));//回到原来的path
                }
            } else {//如果是文件
                InputStream is = getAssets().open(path);
                FileOutputStream fos = new FileOutputStream(new File(newPath + path));
                byte[] buffer = new byte[1024];
                while (true) {
                    int len = is.read(buffer);
                    if (len == -1) {
                        break;
                    }
                    fos.write(buffer, 0, len);
                }
                is.close();
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 在这里处理权限请求的结果
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_MEDIA_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                mBinding.tvResult.setText("权限已开启，3");
            } else {
                // 权限拒绝，弹出提示
                Toast.makeText(this, "请开启权限", Toast.LENGTH_SHORT).show();
                requestMediaPermission();
            }
        }
//        if (requestCode == PERMISSIONS_REQUEST_CAMERA) {
//            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
////                mBinding.tvResult.setText("权限已开启，1");
////                openAlum();
//                // 权限通过，跳转到拍照页面
////                Intent intent = new Intent(this, TakePhoteActivity.class);
////                startActivity(intent);
//            } else {
//                // 权限拒绝，弹出提示
//                Toast.makeText(this, "请开启权限", Toast.LENGTH_SHORT).show();
//            }
//        }
    }

    /**
     * 检查权限
     */
//    void checkSelfPermission() {
//        if (ContextCompat.checkSelfPermission(this, PERMISSION_CAMERA) != PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(this, PERMISSION_WRITE_STORAGE) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{PERMISSION_CAMERA, PERMISSION_WRITE_STORAGE}, PERMISSIONS_REQUEST_CAMERA);
//        } else {
////            Intent intent = new Intent(this, TakePhoteActivity.class);
////            startActivity(intent);
//        }
//    }

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

    // Android 13 之后，不再支持 Manifest.permission.READ_EXTERNAL_STORAGE 权限，需要使用 READ_MEDIA_IMAGES 权限

    // 检查读取相册权限
    private boolean isMediaPermissionGranted() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED;
    }

    // 请求读取相册权限
    private void requestMediaPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_MEDIA_IMAGES}, REQUEST_CODE_MEDIA_PERMISSION);
    }

    private String img2Text(String path) {

        // 识别图片中的文字
        Bitmap bitmap = convertGray(BitmapFactory.decodeFile(path));

        baseApi.setImage(bitmap);
        String result = baseApi.getUTF8Text();
//        baseApi.recycle();

        return path + " 的识别结果:\n" + result + "\n\n";
    }

    private Bitmap convertGray(Bitmap bitmap3) {
        colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);

        Paint paint = new Paint();
        paint.setColorFilter(filter);
        Bitmap result = Bitmap.createBitmap(bitmap3.getWidth(), bitmap3.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        canvas.drawBitmap(bitmap3, 0, 0, paint);
        return result;
    }

    private void initTess() {
        LANGUAGE_PATH = getExternalFilesDir("") + "/";
        //字典库
        baseApi.init(LANGUAGE_PATH, LANGUAGE);
        //设置设别模式
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
    }

}
