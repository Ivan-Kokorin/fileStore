package com.task.file.controller;

import com.task.file.model.dto.FileDto;
import com.task.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/files")
public class FileStoreController {
    FileService fileService;

    @Autowired
    public FileStoreController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping
    public FileDto saveFile(@RequestParam("file") MultipartFile file) {
        return fileService.saveFile(file);
    }

    @GetMapping
    public List<FileDto> getListFiles() {
        return fileService.getListFiles();
    }

    @GetMapping
    @RequestMapping("/{id}")
    public ResponseEntity<InputStreamResource> getConcreteFile(@PathVariable String id) {
        FileDto fileInfo = fileService.getFileInfoById(Long.parseLong(id));
        byte[] bytesFile = fileService.getFileByKey(fileInfo);
        String fileName = fileInfo.getName();
        fileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + fileName)
                .body(new InputStreamResource(new ByteArrayInputStream(bytesFile)));
    }

}
