package com.wt.ocr.fragment;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.loader.content.CursorLoader;

import com.wt.ocr.R;
import com.wt.ocr.data.DatabaseHelper;
import com.wt.ocr.data.ScannedImageDAO;
import com.wt.ocr.utils.Img2TxtUtil;
import android.app.Activity;

import java.util.Map;

public class SettingsFragment extends Fragment {

    private TextView longTextView;
    private Button clearDatabaseButton;
    private Button testButton;
    private Button singleTestButton;
    private TextView singleTestResult;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // 获取长文本视图
        longTextView = view.findViewById(R.id.longTextView);
        singleTestResult = view.findViewById(R.id.singleTestResult);
        
        // 获取清空数据库按钮，测试按钮
        clearDatabaseButton = view.findViewById(R.id.clearDatabaseButton);
        testButton = view.findViewById(R.id.testButton);
        singleTestButton = view.findViewById(R.id.singleTestButton);
        // 设置按钮点击事件
        clearDatabaseButton.setOnClickListener(v -> clearDatabase());
        testButton.setOnClickListener(v -> testAnalyzeAlbum());
        singleTestButton.setOnClickListener(v -> singleTest());
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

    private void singleTest() {
        // 创建Intent来打开图片选择器
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, 1001); // 1001是请求码，用于在onActivityResult中识别请求
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1001 && resultCode == Activity.RESULT_OK && data != null) {
            Uri selectedImageUri = data.getData();
            if (selectedImageUri != null) {
                try {
                    // 获取图片的实际路径
                    String imagePath = getRealPathFromURI(selectedImageUri);
                    if (imagePath != null) {
                        
                        // 在后台线程执行OCR操作
                        new Thread(() -> {
                            try {
                                Map<String, Object> result = Img2TxtUtil.AnalyzeImage(imagePath);
                                String resultText = (String) result.get("resultText");
                                String sensiWordResult = (String) result.get("sensiWordResult");
                                boolean isSensitive = (boolean) result.get("isSensitive");
                                String result_text =
                                        resultText + "\n" + sensiWordResult + "\n" + isSensitive;
                                // 在主线程更新UI
                                getActivity().runOnUiThread(() -> {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "OCR Success", Toast.LENGTH_SHORT).show();
                                        singleTestResult.setText(result_text);
                                    }
                                });
                            } catch (Exception e) {
                                getActivity().runOnUiThread(() -> {
                                    if (getContext() != null) {
                                        Toast.makeText(getContext(), "分析图片时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }).start();
                        
                    } else {
                        Toast.makeText(getContext(), "无法获取图片路径", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(getContext(), "分析图片时出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
    
    // 从URI获取实际文件路径的辅助方法
    private String getRealPathFromURI(Uri contentUri) {
        String[] proj = {MediaStore.Images.Media.DATA};
        CursorLoader loader = new CursorLoader(getContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        if (cursor != null) {
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String result = cursor.getString(column_index);
            cursor.close();
            return result;
        }
        return null;
    }
} 