package com.example.recipe_worker.dto;

public class ImageInfoDto {
    private String id;        
    private String filename;
    private String url;         // URL or path to fetch the image (thumbs)
    private String sizeLabel;   // e.g. "1024", "512" etc.

    public ImageInfoDto() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getSizeLabel() { return sizeLabel; }
    public void setSizeLabel(String sizeLabel) { this.sizeLabel = sizeLabel; }
}
