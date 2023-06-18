package com.task.file.service;

import com.task.file.controller.exception.ResourceNotFoundException;
import com.task.file.controller.exception.SaveMetadataException;
import com.task.file.model.dto.FileDisplayInfoDto;
import com.task.file.model.dto.FileDto;
import com.task.file.model.entity.FileEntity;
import com.task.file.repository.FileRepository;
import jakarta.persistence.EntityManager;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Profile("data-base")
public class FileServiceDbImpl implements FileService {
    private static final Logger LOG = LoggerFactory.getLogger(FileServiceDbImpl.class);
    private final String MESSAGE_ABOUT_DELETED = "File success deleted";
    FileRepository fileRepository;
    ModelMapper modelMapper;
    ResourceLoader resourceLoader;
    EntityManager em;

    @Autowired
    public FileServiceDbImpl(FileRepository fileRepository, ModelMapper modelMapper, ResourceLoader resourceLoader, EntityManager em) {
        this.fileRepository = fileRepository;
        this.modelMapper = modelMapper;
        this.resourceLoader = resourceLoader;
        this.em = em;
    }

    @Override
    public FileDto saveFile(MultipartFile multipartFile) {
        //extract metadata
        FileDto fileDto = new FileDto();
        fileDto.setTypeStore(TypeStore.DATA_BASE.getType());
        fileDto.setName(multipartFile.getOriginalFilename());
        fileDto.setContentType(multipartFile.getContentType());
        fileDto.setSize(multipartFile.getSize());
        try {
            fileDto.setContent(multipartFile.getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        LOG.info("Create DTO metadata: " + fileDto);
        return saveFileDataInDb(fileDto);
    }

    private FileDto saveFileDataInDb(FileDto fileDto) {
        FileEntity fileEntity = modelMapper.map(fileDto, FileEntity.class);
        FileEntity savedFileEntity = fileRepository.save(fileEntity);
        Optional<FileEntity> checkedEntity = fileRepository.findById(savedFileEntity.getId());
        if (checkedEntity.isPresent()) {
            LOG.info("Success save file in DB. File data: " + checkedEntity.get());
            return modelMapper.map(savedFileEntity, FileDto.class);
        } else {
            LOG.error("Failed to save file metadata. " + fileDto);
            throw new SaveMetadataException("Failed to save file metadata");
        }
    }

    @Override
    public List<FileDisplayInfoDto> getListFiles() {
        return em.createQuery("select new com.task.file.model.dto.FileDisplayInfoDto(f.id, f.name, f.contentType, f.size, f.createdDate, f.updatedDate) from FileEntity f where f.typeStore = 'Data base'", FileDisplayInfoDto.class)
                .getResultList();
    }

    @Override
    public ResponseEntity<InputStreamResource> findFileById(Long id) {
        FileDto fileInfo = getFileData(id);
        byte[] bytesFile = fileInfo.getContent();
        String fileName = fileInfo.getName();
        fileName = UriUtils.encode(fileName, StandardCharsets.UTF_8);
        LOG.info("Success request file with metadata: " + fileInfo);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header("Content-Disposition", "attachment; filename*=UTF-8''" + fileName)
                .body(new InputStreamResource(new ByteArrayInputStream(bytesFile)));
    }

    private FileDto getFileData(Long id) {
        Optional<FileEntity> optionalFileEntity = fileRepository.findById(id);
        if (optionalFileEntity.isPresent()) {
            LOG.info("Received data: " + optionalFileEntity.get());
            return modelMapper.map(optionalFileEntity.get(), FileDto.class);
        } else {
            LOG.error("File not found with id: " + id);
            throw new ResourceNotFoundException("File not found, sorry");
        }
    }

    @Override
    public String deleteFile(Long id) {
        Optional<FileEntity> optionalFileEntity = fileRepository.findById(id);
        if(optionalFileEntity.isPresent()) {
            fileRepository.deleteById(optionalFileEntity.get().getId());
            LOG.info("Success deleted file : " + optionalFileEntity.get());
            return MESSAGE_ABOUT_DELETED;
        } else {
            LOG.error("File data not found for deleting. Id of file: " + id);
            throw new ResourceNotFoundException("File data not found");
        }
    }
}