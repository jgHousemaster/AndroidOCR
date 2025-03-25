package com.wt.ocr;

import android.content.Intent;
import android.os.Bundle;
import androidx.databinding.DataBindingUtil;
import com.wt.ocr.databinding.ActivitySettingsBinding;

public class SettingsActivity extends BaseActivity {

    private ActivitySettingsBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = DataBindingUtil.setContentView(this, R.layout.activity_settings);
        
        // 设置底部导航栏
        binding.bottomNavigation.setSelectedItemId(R.id.navigation_settings); // 设置当前选中项
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    Intent intent = new Intent(this, MainActivity.class);
                    // 不使用动画过渡
                    intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                    startActivity(intent);
                    // 不使用动画过渡
                    overridePendingTransition(0, 0);
                    return true;
                case R.id.navigation_settings:
                    // 已经在设置页面，不需要操作
                    return true;
            }
            return false;
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
} 