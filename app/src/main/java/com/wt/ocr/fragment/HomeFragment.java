package com.wt.ocr.fragment;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.wt.ocr.R;
import com.wt.ocr.adapter.ScanResultAdapter;
import com.wt.ocr.data.DatabaseHelper;
import com.wt.ocr.data.ScannedImageDAO;
import com.wt.ocr.utils.Img2TxtUtil;
import com.wt.ocr.utils.Utils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class HomeFragment extends Fragment {

    private ScanResultAdapter adapter;
    private LinearLayout problematicPhotosContainer;
    private LinearLayout newProblematicPhotosContainer;
    private LinearLayout resultsContainer;
    private LinearLayout autoCheckResultsContainer;
    private TextView autoCheckTextView;
    private ImageView checkMark;
    private ImageView eyeMark;
    private Button scanButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // 初始化视图
        resultsContainer = view.findViewById(R.id.resultsContainer);
        problematicPhotosContainer = view.findViewById(R.id.problematicPhotosContainer);
        newProblematicPhotosContainer = view.findViewById(R.id.newProblematicPhotosContainer);
        autoCheckResultsContainer = view.findViewById(R.id.autoCheckResultsContainer);
        autoCheckTextView = view.findViewById(R.id.autoCheckTextView);
        checkMark = view.findViewById(R.id.checkMark);
        eyeMark = view.findViewById(R.id.eyeMark);
        scanButton = view.findViewById(R.id.scanButton);
        
        // 设置 RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.resultsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ScanResultAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        
        // 设置按钮点击事件
        scanButton.setOnClickListener(v -> startScan());
        
        // 自动检查新照片
        new Thread(() -> {
            Map<String, Object> analysisResult = Img2TxtUtil.AnalyzeAlbumNewPhotos();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> updateAutoCheckUI(analysisResult));
            }
        }).start();
        
        return view;
    }
    
    private void startScan() {
        // 扫描所有照片
        Toast.makeText(getContext(), "run scanning...", Toast.LENGTH_SHORT).show();
        new Thread(() -> {
            Map<String, Object> analysisResult = Img2TxtUtil.AnalyzeAlbum();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> updateUI(analysisResult));
            }
        }).start();
    }
    
    private void updateUI(Map<String, Object> result) {
        // 点击扫描按钮后，显示扫描结果

        resultsContainer.setVisibility(View.VISIBLE);
        
        // 更新扫描结果
        DatabaseHelper dbHelper = new DatabaseHelper(getContext());
        ScannedImageDAO dao = new ScannedImageDAO(dbHelper);
        adapter.updateResults(dao.getAll());

        // 显示问题照片
        if (result.containsKey("alertNeeded") && (Boolean) result.get("alertNeeded")) {
            ArrayList<String> sensitiveImages = (ArrayList<String>) result.get("sensitiveImageURLs");
            showProblematicPhotos(sensitiveImages);
        }
    }
    
    private void updateAutoCheckUI(Map<String, Object> result) {
        if (result == null) return;
        
        boolean hasNewSensitivePhotos = result.containsKey("alertNeeded") && (Boolean) result.get("alertNeeded");
        
        if (hasNewSensitivePhotos) {
            // 正确处理keywords ArrayList
            ArrayList<String> keywordsList = (ArrayList<String>) result.get("keywords");
            String keywordsString = "";
            // 对关键词列表进行去重处理
            if (keywordsList != null && !keywordsList.isEmpty()) {
                Set<String> uniqueKeywords = new HashSet<>(keywordsList);
                keywordsList = new ArrayList<>(uniqueKeywords);
                keywordsString = TextUtils.join(", ", keywordsList);
            }
            
            autoCheckTextView.setText("Potential sensitive photos detected since last scan, KEY WORDS: " + keywordsString);
            eyeMark.setVisibility(View.VISIBLE);
            checkMark.setVisibility(View.GONE);
            
            // 显示敏感照片
            if (result.containsKey("sensitiveImageURLs")) {
                ArrayList<String> sensitiveImages = (ArrayList<String>) result.get("sensitiveImageURLs");
                if (sensitiveImages != null && !sensitiveImages.isEmpty()) {
                    autoCheckResultsContainer.setVisibility(View.VISIBLE);
                    showNewProblematicPhotos(sensitiveImages);
                }
            }
        } else {
            autoCheckTextView.setText("No new sensitive photos detected. Good job!");
            checkMark.setVisibility(View.VISIBLE);
            eyeMark.setVisibility(View.GONE);
            autoCheckResultsContainer.setVisibility(View.GONE);
        }
    }

    private void showNewProblematicPhotos(List<String> photoUrls) {
        newProblematicPhotosContainer.removeAllViews();
        for (String url : photoUrls) {
            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(new LinearLayout.LayoutParams(
                    dpToPx(100), dpToPx(100)));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(dpToPx(4), 0, dpToPx(4), 0);
            
            // 使用 Glide 加载本地文件
            File imageFile = new File(url);
            Glide.with(this)
                    .load(imageFile)
                    .into(imageView);
            
            // 点击直接打开相册
            imageView.setOnClickListener(v -> openImageInGallery(url));
            newProblematicPhotosContainer.addView(imageView);
        }
    }
    
    
    private void showProblematicPhotos(List<String> photoUrls) {
        problematicPhotosContainer.removeAllViews();
        
        for (String url : photoUrls) {
            ImageView imageView = new ImageView(getContext());
            imageView.setLayoutParams(new LinearLayout.LayoutParams(
                    dpToPx(100), dpToPx(100)));
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            imageView.setPadding(dpToPx(4), 0, dpToPx(4), 0);
            
            // 使用 Glide 加载本地文件
            File imageFile = new File(url);
            Glide.with(this)
                    .load(imageFile)
                    .into(imageView);
            
            // 点击直接打开相册
            imageView.setOnClickListener(v -> openImageInGallery(url));
            problematicPhotosContainer.addView(imageView);
        }
    }
    
    private void openImageInGallery(String imageUrl) {
        File file = new File(imageUrl);
        Uri photoURI = FileProvider.getUriForFile(getContext(),
                getContext().getPackageName() + ".fileprovider",
                file);

        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(photoURI, "image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        
        try {
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "无法打开相册: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
    
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
} 