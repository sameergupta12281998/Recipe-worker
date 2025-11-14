package com.example.recipe_worker.dto;

public class ImageUploadDto {
    // filename optional (helps when persisting)
    private String filename;

    // MIME type (image/jpeg, image/png, etc.)
    private String mimeType;

    // Base64 encoded bytes (or null if file-based upload)
    private String dataBase64;

    public ImageUploadDto() {}

    public ImageUploadDto(String filename, String mimeType, String dataBase64) {
        this.filename = filename;
        this.mimeType = mimeType;
        this.dataBase64 = dataBase64;
    }

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getMimeType() { return mimeType; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public String getDataBase64() { return dataBase64; }
    public void setDataBase64(String dataBase64) { this.dataBase64 = dataBase64; }
}
