package com.task.file.service;

import com.task.file.model.dto.FileDto;

import java.io.File;
import java.util.List;

public interface FileService {
    public FileDto saveFile(File file);
    public List<FileDto> getListFiles();
    public FileDto getFileById(Long id);
    public String deleteFile(Long id);
}
