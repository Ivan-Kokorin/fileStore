package com.task.file.service;

import com.task.file.model.dto.FileDisplayInfoDto;
import com.task.file.model.dto.FileDto;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileService {
    public FileDto saveFile(MultipartFile file);
    public List<FileDisplayInfoDto> getListFiles();
    public ResponseEntity<InputStreamResource> findFileById(Long id);
    public String deleteFile(Long id);

}
