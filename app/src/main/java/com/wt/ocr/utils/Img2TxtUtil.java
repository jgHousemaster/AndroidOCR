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
import java.util.HashSet;
import java.util.Set;
import com.wt.ocr.data.DatabaseHelper;
import com.wt.ocr.data.ScannedImage;
import com.wt.ocr.data.ScannedImageDAO;

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

    public static Map<String, Object> AnalyzeImage(String path) {
        Map<String, Object> result = new HashMap<>();
        String curString = img2Text(path);
        int curSimilarity = Utils.fuzzyFindString(compareList, curString);
        String sensiWordResult = Utils.fuzzyFindStringShow(compareList, curString);
        boolean isSensitive = curSimilarity > 90;
        result.put("sensiWordResult", sensiWordResult);
        result.put("isSensitive", isSensitive);
        return result;
    }

    public static Map<String, Object> AnalyzeAlbum() {
        Map<String, Object> result = new HashMap<>();
        boolean alertNeeded = false;
        StringBuilder description = new StringBuilder();
        ArrayList<String> sensitiveImageURLs = new ArrayList<>();
    
        // 获取相册中所有图片
        List<String> images = AlbumUtil.scanAlbum(context, "Sullivan");
        // 获取未扫描的图片
        List<String> unscannedImages = getUnscannedImages(images);
        
        description.append("读取完成，相册总图片数量：").append(images.size()).append("\n");
        description.append("新增未扫描图片数量：").append(unscannedImages.size()).append("\n");
        description.append("读取时间：").append(Utils.getNowTime()).append("\n\n");
    
        // 创建数据库访问对象
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        ScannedImageDAO dao = new ScannedImageDAO(dbHelper);
    
        // 处理未扫描的图片
        for (String image : unscannedImages) {
            String curString = img2Text(image);
            int curSimilarity = Utils.fuzzyFindString(compareList, curString);
            String sensiWordResult = Utils.fuzzyFindStringShow(compareList, curString);
            boolean isSensitive = curSimilarity > 90;
    
            // 创建新的记录并存入数据库
            ScannedImage scannedImage = new ScannedImage(
                image,
                curString,
                sensiWordResult,
                isSensitive
            );
            dao.insert(scannedImage);
        }
    
        // 从数据库读取所有记录并生成报告
        List<ScannedImage> allScannedImages = dao.getAll();
        int index = 0;
        for (ScannedImage image : allScannedImages) {
            description.append("图片 ").append(index++).append(" :").append(image.getFilename())
                    .append(" 的识别结果:\n")
                    .append(image.getText()).append("\n")
                    .append("与敏感信息词汇的最高相似度：").append(image.getSensiWord())
                    .append("\n\n");
    
            if (image.isSensitive()) {
                alertNeeded = true;
                sensitiveImageURLs.add(image.getFilename());
            }
        }
    
        result.put("alertNeeded", alertNeeded);
        result.put("description", description.toString());
        result.put("sensitiveImageURLs", sensitiveImageURLs);
    
        return result;
    }

    private static List<String> getUnscannedImages(List<String> images) {
        // 获取未扫描的图片，并更新数据库

        List<String> unscannedImages = new ArrayList<>();
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        ScannedImageDAO dao = new ScannedImageDAO(dbHelper);
        
        // 获取数据库中所有记录
        List<ScannedImage> dbImages = dao.getAll();
        
        // 创建一个 HashSet 来存储数据库中的文件名，提高查询效率
        Set<String> dbFilenames = new HashSet<>();
        for (ScannedImage dbImage : dbImages) {
            dbFilenames.add(dbImage.getFilename());
        }
        
        // 检查数据库记录是否在当前相册中存在
        Set<String> currentImages = new HashSet<>(images);
        for (ScannedImage dbImage : dbImages) {
            if (!currentImages.contains(dbImage.getFilename())) {
                // 数据库中有记录但相册中已不存在，删除该记录
                dao.delete(dbImage.getId());
            }
        }
        
        // 检查相册中的图片是否在数据库中存在
        for (String image : images) {
            if (!dbFilenames.contains(image)) {
                // 相册中有但数据库中没有的图片，添加到未扫描列表
                unscannedImages.add(image);
            }
        }
        
        return unscannedImages;
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
