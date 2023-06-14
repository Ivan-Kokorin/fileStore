package com.task.file.service;

import com.task.file.model.dto.FileDto;
import com.task.file.repository.FileRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Service
public class FileServiceImpl implements FileService{
    FileRepository fileRepository;
    ModelMapper modelMapper;
    @Autowired
    public FileServiceImpl(FileRepository fileRepository, ModelMapper modelMapper) {
        this.fileRepository = fileRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public FileDto saveFile(File file) {
        return null;
    }

    @Override
    public List<FileDto> getListFiles() {
        return null;
    }

    @Override
    public FileDto getFileById(Long id) {
        return null;
    }

    @Override
    public String deleteFile(Long id) {
        return null;
    }
}
