package com.wt.ocr.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Img2TxtUtil {
    private static final int REQUEST_CODE_MEDIA_PERMISSION = 101;
    private static ColorMatrix colorMatrix;
    private static TessBaseAPI baseApi = new TessBaseAPI();
    private static String LANGUAGE_PATH = "";
    private static final String LANGUAGE = "eng";
    private static String[] compareList;
    private static Context context;

    public static void init(Context ctx) {
        context = ctx;
        if (!isMediaPermissionGranted()) {
            requestMediaPermission();
        }
        initTess();
        try {
            compareList = Utils.getList(context);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> AnalyzeAlbum() {
        Map<String, Object> result = new HashMap<>();
        boolean alertNeeded = false;
        StringBuilder description = new StringBuilder();
        ArrayList<String> sensitiveImageURLs = new ArrayList<>();

        List<String> images = AlbumUtil.scanAlbum(context, "Sullivan");
        description.append("读取完成，图片数量：").append(images.size()).append("\n\n");
        description.append("读取时间：").append(Utils.getNowTime()).append("\n\n");
        
        int i = 0;
        String curString;
        int curSimilarity;

        for (String image : images) {
            curString = img2Text(image);
            curSimilarity = Utils.fuzzyFindString(compareList, curString);
            description.append("图片 ").append(i).append(" :").append(image).append(" 的识别结果:\n")
                    .append(curString).append("\n\n")
                    .append("与敏感信息词汇的最高相似度：").append(Utils.fuzzyFindStringShow(compareList, img2Text(image)))
                    .append("\n\n");
            if (curSimilarity > 90) {
                alertNeeded = true;
                sensitiveImageURLs.add(image);
            }
            i++;
        }

        result.put("alertNeeded", alertNeeded);
        result.put("description", description.toString());
        result.put("sensitiveImageURLs", sensitiveImageURLs);

        return result;
    }

    private static boolean isMediaPermissionGranted() {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) 
                == PackageManager.PERMISSION_GRANTED;
    }

    public static void requestMediaPermission() {
        ActivityCompat.requestPermissions((android.app.Activity) context, 
                new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 
                REQUEST_CODE_MEDIA_PERMISSION);
    }

    private static void initTess() {
        LANGUAGE_PATH = context.getExternalFilesDir("") + "/";
        baseApi.init(LANGUAGE_PATH, LANGUAGE);
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
    }

    private static String img2Text(String path) {
        Bitmap bitmap = convertGray(BitmapFactory.decodeFile(path));
        baseApi.setImage(bitmap);
        String result = baseApi.getUTF8Text();
        result = result.replaceAll("\\s*", "");
        result = result.replaceAll("[^a-zA-Z\\u4E00-\\u9FA5]", "");
        result = result.toUpperCase();
        return result;
    }

    private static Bitmap convertGray(Bitmap bitmap3) {
        colorMatrix = new ColorMatrix();
        colorMatrix.setSaturation(0);
        ColorMatrixColorFilter filter = new ColorMatrixColorFilter(colorMatrix);

        Paint paint = new Paint();
        paint.setColorFilter(filter);
        Bitmap result = Bitmap.createBitmap(bitmap3.getWidth(), bitmap3.getHeight(), 
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);

        canvas.drawBitmap(bitmap3, 0, 0, paint);
        return result;
    }
}
