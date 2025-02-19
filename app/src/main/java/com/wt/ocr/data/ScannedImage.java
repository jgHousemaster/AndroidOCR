package com.wt.ocr.data;

public class ScannedImage {
    private long id;                // 数据库主键 ID
    private String filename;        // 图片文件名/路径
    private String text;           // OCR 识别出的文本内容
    private String sensiWord;      // 格式: "匹配词:相似度" (例如 "PASSWORD:95")
    private boolean isSensitive;   // true: 相似度 > 90

    // 构造函数
    public ScannedImage() {
    }

    public ScannedImage(String filename, String text, String sensiWord, boolean isSensitive) {
        this.filename = filename;
        this.text = text;
        this.sensiWord = sensiWord;
        this.isSensitive = isSensitive;
    }

    // Getters and Setters
    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getSensiWord() {
        return sensiWord;
    }

    public void setSensiWord(String sensiWord) {
        this.sensiWord = sensiWord;
    }

    public boolean isSensitive() {
        return isSensitive;
    }

    public void setSensitive(boolean sensitive) {
        isSensitive = sensitive;
    }

    @Override
    public String toString() {
        return "ScannedImage{" +
                "id=" + id +
                ", filename='" + filename + '\'' +
                ", text='" + text + '\'' +
                ", sensiWord='" + sensiWord + '\'' +
                ", isSensitive=" + isSensitive +
                '}';
    }
}