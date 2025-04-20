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
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import com.google.android.gms.tasks.Tasks;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.wt.ocr.data.DatabaseHelper;
import com.wt.ocr.data.ScannedImage;
import com.wt.ocr.data.ScannedImageDAO;
import com.wt.ocr.data.ResultRecord;

import android.os.Handler;
import android.os.Looper;

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
        result.put("resultText", curString);
        result.put("sensiWordResult", sensiWordResult);
        result.put("isSensitive", isSensitive);
        return result;
    }

    public static Map<String, Object> AnalyzeAlbumNewPhotos() {
        // 只返回不在数据库中图片的信息
        Map<String, Object> result = new HashMap<>();
        boolean alertNeeded = false;
        ArrayList<String> sensitiveImageURLs = new ArrayList<>();
        ArrayList<String> keywords = new ArrayList<>();

        // 获取相册中所有图片
        List<String> images = AlbumUtil.scanAlbum(context, "Sullivan");
        // 获取未扫描的图片
        List<String> unscannedImages = getUnscannedImages(images);
        // 如果未扫描的图片为空，则返回
        if (unscannedImages.isEmpty()) {
            result.put("alertNeeded", false);
            result.put("sensitiveImageURLs", new ArrayList<>());
            result.put("keywords", new ArrayList<>());
            return result;
        }

        // 创建数据库访问对象
        DatabaseHelper dbHelper = new DatabaseHelper(context);
        ScannedImageDAO dao = new ScannedImageDAO(dbHelper);
    
        // 处理未扫描的图片
        for (String image : unscannedImages) {
            String curString = img2Text(image);
            int curSimilarity = Utils.fuzzyFindString(compareList, curString);
            String sensiWordResult = Utils.fuzzyFindStringShow(compareList, curString);
            boolean isSensitive = curSimilarity > 90;

            // 放入结果
            if (isSensitive) {
                alertNeeded = true;
                sensitiveImageURLs.add(image);
                String keyword = sensiWordResult.split(":")[0];
                keywords.add(keyword);
            }
    
            // 创建新的记录并存入数据库
            ScannedImage scannedImage = new ScannedImage(
                image,
                curString,
                sensiWordResult,
                isSensitive
            );
            dao.insert(scannedImage);
        }
        
        result.put("alertNeeded", alertNeeded);
        result.put("sensitiveImageURLs", sensitiveImageURLs);
        result.put("keywords", keywords);
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
            boolean isSensitive = curSimilarity > 70 || hasSensitiveRegex(curString);
    
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

    // 添加回调接口
    public interface ProgressCallback {
        void onProgress(int current, int total);
        void onComplete();
    }

    // 更新TestAnalyzeAlbum方法，添加回调参数
    public static void TestAnalyzeAlbum(ProgressCallback callback) {
        // 获取测试相册中的所有图片地址
        List<String> images = AlbumUtil.scanAlbum(context, "Sullivan");
        List<ResultRecord> results = new ArrayList<>();
        
        int total = images.size();
        int current = 0;
        
        for (String imagePath : images) {
            long start = System.currentTimeMillis();
            String text = "";
            String error = null;
            boolean ocrSuccess = false;
            String topKeyword = "";
            double maxSimilarity = 0;
            boolean markedSensitive = false;

            try {
                text = img2Text(imagePath); // OCR 调用
                ocrSuccess = text != null && !text.trim().isEmpty();

                if (ocrSuccess) {
                    maxSimilarity = Utils.fuzzyFindString(compareList, text);
                    topKeyword = Utils.fuzzyFindStringShow(compareList, text);
                    markedSensitive = hasSensitiveRegex(text);
                }
            } catch (Exception e) {
                error = e.getMessage();
            }

            long end = System.currentTimeMillis();
            double timeSpent = (end - start) / 1000.0;

            results.add(new ResultRecord(
                    imagePath,
                    ocrSuccess,
                    text,
                    topKeyword,
                    maxSimilarity,
                    markedSensitive,
                    timeSpent,
                    error
            ));
            
            current++;
            if (callback != null) {
                callback.onProgress(current, total);
            }
        }

        writeResultsToCSV(results, context);
        
        if (callback != null) {
            callback.onComplete();
        }
    }

    // 保持原来无参数的方法，以保持向后兼容性
    public static void TestAnalyzeAlbum() {
        TestAnalyzeAlbum(null);
    }

    private static boolean hasSensitiveRegex(String text) {
        // 检测文本中是否包含敏感信息
        if (text == null || text.isEmpty()) {
            return false;
        }

        // 清理文本，只保留大写字母、数字和空格
        String cleanedText = text.replaceAll("[^A-Z0-9\\s]", " ").trim();

        // 检测银行卡号（连续的13-19位数字）
        String[] words = cleanedText.split("\\s+");
        for (String word : words) {
            // 银行卡号通常是13-19位数字
            if (word.matches("\\d{13,19}")) {
                return true;
            }

            // 检测可能被空格分隔的银行卡号
            if (word.matches("\\d{4,6}") && cleanedText.contains(word)) {
                String surroundingText = cleanedText.substring(
                        Math.max(0, cleanedText.indexOf(word) - 30),
                        Math.min(cleanedText.length(), cleanedText.indexOf(word) + word.length() + 30)
                );

                // 计算这个区域内的数字总数
                int digitCount = 0;
                for (char c : surroundingText.toCharArray()) {
                    if (Character.isDigit(c)) {
                        digitCount++;
                    }
                }

                // 如果数字总数在银行卡号范围内
                if (digitCount >= 13 && digitCount <= 19) {
                    return true;
                }
            }
        }

        // 检测电话号码（放宽范围：连续的7-15位数字）
        for (String word : words) {
            if (word.matches("\\d{7,15}")) {
                return true;
            }
        }

        // 检测姓名+地址组合（注意：text只有大写字母）
        // 查找可能的姓名（连续2-3个单词）
        List<String> possibleNames = new ArrayList<>();
        for (int i = 0; i < words.length - 1; i++) {
            if (words[i].length() > 0 && words[i].matches("[A-Z]+")) {
                if (i + 1 < words.length && words[i + 1].length() > 0 && words[i + 1].matches("[A-Z]+")) {
                    possibleNames.add(words[i] + " " + words[i + 1]);

                    // 检查是否有第三个单词作为姓名的一部分
                    if (i + 2 < words.length && words[i + 2].length() > 0 && words[i + 2].matches("[A-Z]+")) {
                        possibleNames.add(words[i] + " " + words[i + 1] + " " + words[i + 2]);
                    }
                }
            }
        }

        // 对于每个可能的姓名，检查其后是否跟随地址信息
        for (String name : possibleNames) {
            int nameIndex = cleanedText.indexOf(name);
            if (nameIndex >= 0) {
                // 获取姓名后的文本
                String afterName = cleanedText.substring(nameIndex + name.length());
                // 检查是否包含数字（可能是门牌号）和多个单词（可能是街道名称）
                if (afterName.matches(".*\\d+.*") && afterName.split("\\s+").length >= 3) {
                    return true;
                }
            }
        }

        return false;
    }

    public static void writeResultsToCSV(List<ResultRecord> records, Context context) {
        String filename = "app_test_result_" + System.currentTimeMillis() + ".csv";
        File downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        File csvFile = new File(downloadDir, filename);
    
        try (FileWriter fw = new FileWriter(csvFile);
             BufferedWriter bw = new BufferedWriter(fw)) {
    
            bw.write("Filename,OCR_Success,Extracted_Text,Top_Keyword,Top_Similarity,Marked_Sensitive,Time_Spent_Seconds,Error_Message");
            bw.newLine();
    
            for (ResultRecord r : records) {
                bw.write(String.format(Locale.US,
                    "\"%s\",%b,\"%s\",\"%s\",%.2f,%b,%.2f,\"%s\"",
                    r.filename, r.ocrSuccess,
                    r.extractedText.replace("\"", "\"\""), // 防止引号冲突
                    r.topKeyword,
                    r.similarity,
                    r.markedSensitive,
                    r.timeSpentSeconds,
                    r.errorMessage == null ? "" : r.errorMessage.replace("\"", "\"\"")
                ));
                bw.newLine();
            }
    
            Toast.makeText(context, "CSV 导出成功: " + csvFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
    
        } catch (IOException e) {
            Toast.makeText(context, "导出失败: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
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
        
        // 设置页面分割模式
        baseApi.setPageSegMode(TessBaseAPI.PageSegMode.PSM_AUTO);
        
        // 设置OCR引擎模式
        baseApi.setVariable("tessedit_ocr_engine_mode", "1");  // 1 = 神经网络LSTM模式
        
        // 设置白名单字符（如果你知道图片中只会出现特定字符）
        // baseApi.setVariable("tessedit_char_whitelist", "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");
        
        // 其他可能的优化参数
        baseApi.setVariable("debug_file", "/dev/null");  // 禁用调试输出
        baseApi.setVariable("tessdata_dir", LANGUAGE_PATH);
    }

    public static String img2Text(String path) {
        String resultText = "";
        TextRecognizer recognizer = null;
        Bitmap originalBitmap = null;
        
        try {
            recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);

            // 1. 加载并调整图片大小
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 1; // 根据图片大小动态调整，1表示不缩放
            originalBitmap = BitmapFactory.decodeFile(path, options);
            
            if (originalBitmap == null) {
                return "无法加载图片";
            }

            InputImage image = InputImage.fromBitmap(originalBitmap, 0);
            
            // 使用同步方式处理图片，但有超时限制
            try {
                // 转换为同步调用，设置10秒超时
                Text visionText = Tasks.await(recognizer.process(image), 10, TimeUnit.SECONDS);
                resultText = visionText.getText();
            } catch (Exception e) {
                resultText = "识别失败: " + e.getMessage();
            }
        } finally {
            // 确保资源释放
            if (originalBitmap != null && !originalBitmap.isRecycled()) {
                originalBitmap.recycle();
            }
            if (recognizer != null) {
                recognizer.close();
            }
        }
        
        // 文本后处理
        resultText = resultText.replaceAll("\\s+", " ");
        resultText = resultText.trim();
        resultText = resultText.replaceAll("[^a-zA-Z0-9 ]", "");
        resultText = resultText.toUpperCase();
        
        return resultText;
    }

}
