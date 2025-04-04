package com.wt.ocr.data;

public class ResultRecord {
    // 在自动化测试中用于保存每一条测试结果
    public String filename;
    public boolean ocrSuccess;
    public String extractedText;
    public String topKeyword;
    public double similarity;
    public boolean markedSensitive;
    public double timeSpentSeconds;
    public String errorMessage;

    public ResultRecord(String filename, boolean ocrSuccess, String extractedText,
                        String topKeyword, double similarity, boolean markedSensitive,
                        double timeSpentSeconds, String errorMessage) {
        this.filename = filename;
        this.ocrSuccess = ocrSuccess;
        this.extractedText = extractedText;
        this.topKeyword = topKeyword;
        this.similarity = similarity;
        this.markedSensitive = markedSensitive;
        this.timeSpentSeconds = timeSpentSeconds;
        this.errorMessage = errorMessage;
    }
}
