package com.task.file.service;

import com.task.file.model.dto.FileDto;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    public FileDto saveFile(MultipartFile file);
    public List<FileDto> getListFiles();
    public byte[] getFileByKey(FileDto fileInfo);
    FileDto getFileInfoById(Long id);
    public String deleteFile(Long id);

}
