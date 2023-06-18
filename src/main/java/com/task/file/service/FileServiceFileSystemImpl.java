package com.task.file.service;

import com.task.file.controller.exception.ReadInputStreamException;
import com.task.file.controller.exception.ResourceNotFoundException;
import com.task.file.controller.exception.SavedFileException;
import com.task.file.controller.exception.SaveMetadataException;
import com.task.file.model.dto.FileDisplayInfoDto;
import com.task.file.model.dto.FileDto;
import com.task.file.model.entity.FileEntity;
import com.task.file.repository.FileRepository;
import jakarta.persistence.EntityManager;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Profile("file-sys")
public class FileServiceFileSystemImpl implements FileService {
    @Value("${storage.file-system.path}")
    private String path;
    private static final Logger LOG = LoggerFactory.getLogger(FileServiceFileSystemImpl.class);
    private final String MESSAGE_ABOUT_DELETED = "File success deleted";
    FileRepository fileRepository;
    ModelMapper modelMapper;
    EntityManager em;

    public FileServiceFileSystemImpl(FileRepository fileRepository, ModelMapper modelMapper, EntityManager em) {
        this.fileRepository = fileRepository;
        this.modelMapper = modelMapper;
        this.em = em;
    }

    @Override
    public FileDto saveFile(MultipartFile multipartFile) {
        //extract metadata
        FileDto fileDto = new FileDto();
        fileDto.setName(multipartFile.getOriginalFilename());
        fileDto.setPath(path);
        fileDto.setTypeStore(TypeStore.FILE_SYSTEM.getType());
        fileDto.setContentType(multipartFile.getContentType());
        fileDto.setSize(multipartFile.getSize());
        String key = UUID.randomUUID() + fileDto.getName();
        fileDto.setKeyFile(key);
        LOG.info("Create DTO metadata: " + fileDto);
        saveFileInFileSystem(fileDto, multipartFile);
        return saveFileMetadataInDb(fileDto);
    }

    private void saveFileInFileSystem(FileDto fileDto, MultipartFile multipartFile) {
        File fileForWrite = new File(fileDto.getPath() + File.separator + fileDto.getKeyFile());
        try (FileOutputStream out = new FileOutputStream(fileForWrite);) {
            out.write(multipartFile.getBytes());
            LOG.info("Success save file in file system. Path to file: " + fileDto.getPath() + fileDto.getKeyFile());
        } catch (IOException e) {
            LOG.error("File not found " + e);
            throw new SavedFileException("File not found " + e);
        }
    }
    private FileDto saveFileMetadataInDb(FileDto fileDto) {
        //сохранить файл
        //проверить наличие в бд
        //если нет, то удалить
        FileEntity fileEntity = modelMapper.map(fileDto, FileEntity.class);
        FileEntity savedFileEntity = fileRepository.save(fileEntity);
        Optional<FileEntity> checkedEntity = fileRepository.findById(savedFileEntity.getId());
        if (checkedEntity.isPresent()) {
            LOG.info("Success save file in DB. File data: " + checkedEntity.get());
            return modelMapper.map(savedFileEntity, FileDto.class);
        } else {
            deleteFileFromFileSystem(fileDto);
            throw new SaveMetadataException("Failed to save file metadata");
        }
    }
    @Override
    public List<FileDisplayInfoDto> getListFiles() {
        return em.createQuery("select new com.task.file.model.dto.FileDisplayInfoDto(f.id, f.name, f.contentType, f.size, f.createdDate, f.updatedDate) from FileEntity f where f.typeStore = 'File system'", FileDisplayInfoDto.class)
                .getResultList();
    }

    @Override
    public ResponseEntity<InputStreamResource> findFileById(Long id) {
        FileDto fileInfo = getFileMetadata(id);
        byte[] bytesFile = getFileByKey(fileInfo);
        String fileName = fileInfo.getName();
        fileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
        LOG.info("Success request file with metadata: " + fileInfo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + fileName)
                .body(new InputStreamResource(new ByteArrayInputStream(bytesFile)));
    }

    private FileDto getFileMetadata(Long id) {
        Optional<FileEntity> optionalFileEntity = fileRepository.findById(id);
        if (optionalFileEntity.isPresent()) {
            LOG.info("Received data: " + optionalFileEntity.get());
            return modelMapper.map(optionalFileEntity.get(), FileDto.class);
        } else {
            LOG.error("File not found with id: " + id);
            throw new ResourceNotFoundException("File not found, sorry");
        }
    }

    private byte[] getFileByKey(FileDto fileInfo) {
        File fileSource = new File(fileInfo.getPath() + File.separator + fileInfo.getKeyFile());
        try (InputStream in = new FileInputStream(fileSource)) {
            return in.readAllBytes();
        } catch (IOException e) {
            LOG.error("Error read file" + e.getMessage());
            throw new ReadInputStreamException("Error read file", e);
        }
    }

    @Override
    public String deleteFile(Long id) {
        Optional<FileEntity> optionalFileEntity = fileRepository.findById(id);
        if(optionalFileEntity.isPresent()) {
            deleteFileFromFileSystem(modelMapper.map(optionalFileEntity.get(), FileDto.class));
            fileRepository.deleteById(optionalFileEntity.get().getId());
            return MESSAGE_ABOUT_DELETED;
        } else {
            LOG.error("File metadata not found for deleting. Id of file: " + id);
            throw new ResourceNotFoundException("File metadata not found");
        }
    }

    private void deleteFileFromFileSystem(FileDto fileDto) {
        Path path = Path.of(fileDto.getPath() + File.separator + fileDto.getKeyFile());
        try {
            boolean deleteStatus = Files.deleteIfExists(path);
            if (deleteStatus) {
                LOG.info("Success deleted file: " + fileDto);
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
            throw new RuntimeException(e);
        }
    }
}