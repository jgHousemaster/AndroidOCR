package com.wt.ocr.fragment;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.wt.ocr.R;
import com.wt.ocr.data.DatabaseHelper;
import com.wt.ocr.data.ScannedImageDAO;
import com.wt.ocr.utils.Img2TxtUtil;

public class SettingsFragment extends Fragment {

    private TextView longTextView;
    private Button clearDatabaseButton;
    private Button testButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // 获取长文本视图
        longTextView = view.findViewById(R.id.longTextView);
        
        // 获取清空数据库按钮，测试按钮
        clearDatabaseButton = view.findViewById(R.id.clearDatabaseButton);
        testButton = view.findViewById(R.id.testButton);
        // 设置按钮点击事件
        clearDatabaseButton.setOnClickListener(v -> clearDatabase());
        testButton.setOnClickListener(v -> testAnalyzeAlbum());
        return view;
    }
    
    /**
     * Clear all data from the database
     */
    private void clearDatabase() {
        try {
            // Get database helper and DAO
            DatabaseHelper dbHelper = new DatabaseHelper(getContext());
            ScannedImageDAO dao = new ScannedImageDAO(dbHelper);
            
            // Delete all records
            boolean success = dao.deleteAll();
            
            // Show success or failure message
            if (success) {
                Toast.makeText(getContext(), "Database cleared successfully", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "Failed to clear database", Toast.LENGTH_SHORT).show();
            }
            
        } catch (Exception e) {
            // Show error message
            Toast.makeText(getContext(), 
                    "Error clearing database: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void testAnalyzeAlbum() {
        // 创建并显示进度对话框
        ProgressDialog progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("正在分析图片...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        // 在后台线程执行耗时操作
        new Thread(() -> {
            Img2TxtUtil.TestAnalyzeAlbum(new Img2TxtUtil.ProgressCallback() {
                @Override
                public void onProgress(int current, int total) {
                    // 在UI线程更新进度条
                    new Handler(Looper.getMainLooper()).post(() -> {
                        progressDialog.setMax(total);
                        progressDialog.setProgress(current);
                    });
                }
                
                @Override
                public void onComplete() {
                    // 完成后关闭进度对话框
                    new Handler(Looper.getMainLooper()).post(() -> {
                        progressDialog.dismiss();
                        Toast.makeText(getContext(), "分析完成", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        }).start();
    }
} 