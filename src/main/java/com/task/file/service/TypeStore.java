package com.task.file.service;

public enum TypeStore {
    DATA_BASE("Data base"),
    FILE_SYSTEM("File system"),
    S3_CLOUD("S3 cloud");
    private String type;

    TypeStore(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
