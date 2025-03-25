package com.wt.ocr.fragment;

import android.os.Bundle;
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

public class SettingsFragment extends Fragment {

    private TextView longTextView;
    private Button clearDatabaseButton;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // 获取长文本视图
        longTextView = view.findViewById(R.id.longTextView);
        
        // 获取清空数据库按钮
        clearDatabaseButton = view.findViewById(R.id.clearDatabaseButton);
        
        // 设置按钮点击事件
        clearDatabaseButton.setOnClickListener(v -> clearDatabase());
        
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
} 