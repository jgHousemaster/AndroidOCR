package com.wt.ocr;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.databinding.DataBindingUtil;

import com.wt.ocr.databinding.ActivityCutoutPhoteBinding;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

// 裁剪图片，保存到 cache，跳转到 ShowCropperedActivity

/**
 * 裁剪
 */
public class CutOutPhotoActivity extends BaseActivity {

    private ActivityCutoutPhoteBinding mBinding;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_cutout_phote);
        // 获取图片路径，保存在 path 中
        String path = getIntent().getStringExtra("path");
        // 用 bitmap 对象加载图片
        Bitmap bitmap = BitmapFactory.decodeFile(path);

        // 在屏幕上显示图片
        mBinding.cropImageView.setImageBitmap(bitmap);

        mBinding.btnClosecropper.setOnClickListener(view -> finish());

        // 点击对勾按钮，获取裁剪后的图片
        mBinding.btnStartcropper.setOnClickListener(view -> {
            // 获取裁剪框内的图片
            Bitmap cropperBitmap = mBinding.cropImageView.getCroppedImage();
            // 图像名称
            String path1 = getCacheDir().getPath();
            // 保存图片至 resultPath
            String resultPath = saveImage(path1, cropperBitmap);

            // 跳转到 ShowCropperedActivity，携带图片路径、宽度和高度
            Intent intent = new Intent(CutOutPhotoActivity.this, ShowCropperedActivity.class);
            intent.putExtra("path", resultPath);
            intent.putExtra("width", cropperBitmap.getWidth());
            intent.putExtra("height", cropperBitmap.getHeight());
            startActivity(intent);
            // 回收图片
            cropperBitmap.recycle();
            finish();
        });
    }

    /**
     * 存储图像
     */
    private String saveImage(String path, Bitmap source) {
        // 这个方法用于保存图片，并返回图片路径
        OutputStream outputStream = null;
        File file;
        try {
            File dir = new File(path);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // 以当前时间戳命名
            file = new File(path, System.currentTimeMillis() + ".jpg");
            if (file.createNewFile()) {
                outputStream = new FileOutputStream(file);
                if (source != null) {
                    source.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                }
            }
        } catch (IOException e) {
            return "";
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable t) {
                }
            }
        }

        return file.getPath();
    }
}
