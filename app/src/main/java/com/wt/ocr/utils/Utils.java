package com.wt.ocr.utils;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.FileUtils;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.DisplayMetrics;

import com.wt.ocr.MainActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import me.xdrop.fuzzywuzzy.FuzzySearch;

/**
 * Created by Administrator on 2016/12/8.
 */

public class Utils {
    public static DisplayMetrics getScreenWH(Context context) {
        DisplayMetrics dMetrics = new DisplayMetrics();
        dMetrics = context.getResources().getDisplayMetrics();
        return dMetrics;
    }

    public static final int getWidthInPx(Context context) {
        final int width = context.getResources().getDisplayMetrics().widthPixels;
        return width;
    }

    // 获取当前时间，返回字符串
    public static String getNowTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return sdf.format(new Date());
    }

    // 读取 list.txt 文件，将字符串转换为数组并返回
    public static String[] getList(Context context) throws IOException {
        String filePath = "list.txt"; // 文件路径：app/src/main/assets/list.txt
        ArrayList<String> lines = new ArrayList<>(); // 用于存储文件内容的ArrayList

        // 安卓不允许直接读取文件，需要通过AssetManager或者 openRawResource() 方法读取
        AssetManager assetManager = context.getAssets();
        InputStream inputStream = assetManager.open(filePath);
        // 读取文件内容，并将每一行添加到ArrayList中
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        reader.close();
        inputStream.close();

        // 将ArrayList转换为数组
        String[] linesArray = lines.toArray(new String[0]);
        return linesArray;
    }

    // 传入字符串和字符串数组，查找是否有匹配（返回最大匹配词及其匹配度）
    public static String fuzzyFindStringShow(String[] list, String str) {
        int result = 0;
        String word = "";
        int cur;
        for (String s : list) {
            cur = FuzzySearch.partialRatio(str, s);
            if (cur > result) {
                result = cur;
                word = s;
            }
        }
        return word + ": " + result;
    }

    // 传入字符串和字符串数组，查找是否有匹配（仅返回数字）
    public static int fuzzyFindString(String[] list, String str) {
        int result = 0;
        int cur;
        for (String s : list) {
            cur = FuzzySearch.partialRatio(str, s);
            if (cur > result) {
                result = cur;
            }
        }
        return result;
    }

    public static void showDialog(Context context, String title, String content, String confirm) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(context);
        builder.setTitle(title);
        builder.setMessage(content);
        builder.setPositiveButton(confirm, null);
        builder.show();
    }
}
