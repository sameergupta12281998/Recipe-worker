package com.example.recipe_worker.service;

public interface FileStorageService {

    String store(byte[] data, String filename);
}

