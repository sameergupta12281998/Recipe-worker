package com.example.recipe_worker.dto;

import java.util.List;
import java.util.Map;

public class PagedResponse<T> {
    private Map<String, Object> meta;
    private List<T> data;

    public PagedResponse() {}

    public PagedResponse(Map<String,Object> meta, List<T> data) {
        this.meta = meta;
        this.data = data;
    }

    public Map<String, Object> getMeta() { return meta; }
    public void setMeta(Map<String, Object> meta) { this.meta = meta; }
    public List<T> getData() { return data; }
    public void setData(List<T> data) { this.data = data; }
}
