package com.task.file.controller;

import com.task.file.controller.exception.ReadInputStreamException;
import com.task.file.controller.exception.ResourceNotFoundException;
import com.task.file.controller.exception.SavedFileException;
import com.task.file.controller.exception.SaveMetadataException;
import com.task.file.model.dto.FileDisplayInfoDto;
import com.task.file.model.dto.FileDto;
import com.task.file.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<FileDto> saveFile(@RequestParam("file") MultipartFile file) {
        return new ResponseEntity<>(fileService.saveFile(file), HttpStatus.CREATED);
    }

    @GetMapping
    public List<FileDisplayInfoDto> getListFiles() {
        return fileService.getListFiles();
    }

    @GetMapping
    @RequestMapping("/{id}")
    public ResponseEntity<InputStreamResource> getConcreteFile(@PathVariable String id) {
        return fileService.findFileById(Long.parseLong(id));
    }

    @DeleteMapping
    @RequestMapping("/delete/{id}")
    public Response deleteFile(@PathVariable String id) {
        return new Response(fileService.deleteFile(Long.parseLong(id)));
    }

    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    @ExceptionHandler(ResourceNotFoundException.class)
    public Response handleResourceException(ResourceNotFoundException e) {
        return new Response(e.getMessage());
    }

    @ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(ReadInputStreamException.class)
    public Response handleReadFileException(ReadInputStreamException e) {
        return new Response(e.getMessage());
    }

    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(SavedFileException.class)
    public Response handleS3CloudException(SavedFileException e) {
        return new Response(e.getMessage());
    }

    @ResponseStatus(value = HttpStatus.SERVICE_UNAVAILABLE)
    @ExceptionHandler(SaveMetadataException.class)
    public Response handleSaveMetadataException(SaveMetadataException e) {
        return new Response(e.getMessage());
    }
}
