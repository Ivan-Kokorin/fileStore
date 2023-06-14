package com.task.file.controller;

import com.task.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/files")
public class FileStoreController {
    FileService fileService;

    @Autowired
    public FileStoreController(FileService fileService) {
        this.fileService = fileService;
    }
}
