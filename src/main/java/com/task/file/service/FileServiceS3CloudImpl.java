package com.task.file.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


@Service
@Profile("cloud")
public class FileServiceS3CloudImpl implements FileService {
    @Value("${storage.s3-cloud.bucket}")
    private String nameBucket;
    @Value("${storage.s3-cloud.directory}")
    private String directory;
    private static final Logger LOG = LoggerFactory.getLogger(FileServiceS3CloudImpl.class);
    private final String MESSAGE_ABOUT_DELETED = "File success deleted";
    FileRepository fileRepository;
    ModelMapper modelMapper;
    EntityManager em;

    public FileServiceS3CloudImpl(FileRepository fileRepository, ModelMapper modelMapper, EntityManager em) {
        this.fileRepository = fileRepository;
        this.modelMapper = modelMapper;
        this.em = em;
    }

    @Override
    public FileDto saveFile(MultipartFile multipartFile) {
        //extract metadata
        FileDto fileDto = new FileDto();
        fileDto.setTypeStore(TypeStore.S3_CLOUD.getType());
        fileDto.setName(multipartFile.getOriginalFilename());
        fileDto.setPath(nameBucket + "&" + directory);
        fileDto.setContentType(multipartFile.getContentType());
        fileDto.setSize(multipartFile.getSize());
        String key = UUID.randomUUID() + fileDto.getName();
        fileDto.setKeyFile(key);
        LOG.info("Create DTO metadata: " + fileDto);

        saveFileInS3Cloud(fileDto, multipartFile);

        return saveFileMetadataInDb(fileDto);
    }

    private void saveFileInS3Cloud(FileDto fileDto, MultipartFile multipartFile) {
        AmazonS3 s3 = createCloudManager();
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(fileDto.getSize());
        objectMetadata.setContentType("application/octet-stream");
        try (ByteArrayInputStream bais = new ByteArrayInputStream(multipartFile.getBytes())) {
            if (s3.listBuckets().stream().noneMatch(bucket -> bucket.getName().equals(nameBucket)))
                s3.createBucket(nameBucket);
            s3.putObject(nameBucket, directory + "/" + fileDto.getKeyFile(), bais, objectMetadata);
            LOG.info("Success save file in s3 cloud. File name in cloud: " + fileDto.getKeyFile());
        } catch (AmazonServiceException ase) {
            String messageError = createMessageServiceException(ase);
            LOG.error(messageError);
            throw new SavedFileException(messageError);
        } catch (AmazonClientException ace) {
            String messageError = createMessageClientS3Exception(ace);
            LOG.error(messageError);
            throw new SavedFileException(messageError);
        } catch (IOException e) {
            LOG.error("File not found " + e);
            throw new SavedFileException("File not found " + e);
        }
    }

    private String createMessageServiceException(AmazonServiceException ase) {
        return "Caught an AmazonServiceException, which means your request made it "
                + "to Amazon S3, but was rejected with an error response for some reason."
                + "Error Message:    " + ase.getMessage()
                + "HTTP Status Code: " + ase.getStatusCode()
                + "AWS Error Code:   " + ase.getErrorCode()
                + "Error Type:       " + ase.getErrorType()
                + "Request ID:       " + ase.getRequestId();
    }

    private String createMessageClientS3Exception(AmazonClientException ace) {
        return "Caught an AmazonClientException, which means the client encountered "
                + "a serious internal problem while trying to communicate with S3, "
                + "such as not being able to access the network."
                + "Error Message: " + ace.getMessage();
    }

    private FileDto saveFileMetadataInDb(FileDto fileDto) {
        //сохранить файл
        //проверить наличие в бд
        //если нет, то удалить из облака
        FileEntity fileEntity = modelMapper.map(fileDto, FileEntity.class);
        FileEntity savedFileEntity = fileRepository.save(fileEntity);
        Optional<FileEntity> checkedEntity = fileRepository.findById(savedFileEntity.getId());
        if (checkedEntity.isPresent()) {
            LOG.info("Success save file metadata: " + fileEntity);
            return modelMapper.map(savedFileEntity, FileDto.class);
        } else {
            deleteFileFromS3Cloud(fileDto);
            LOG.error("Failed to save file metadata");
            throw new SaveMetadataException("Failed to save file metadata");
        }
    }

    private AmazonS3 createCloudManager() {
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            LOG.error("Cannot load the credentials from the credential profiles file. " + e.getMessage());
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file",
                    e);
        }
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(
                        new AmazonS3ClientBuilder.EndpointConfiguration(
                                "storage.yandexcloud.net", "ru-central1"
                        )
                )
                .build();
    }

    @Override
    public List<FileDisplayInfoDto> getListFiles() {
        return em.createQuery("select new com.task.file.model.dto.FileDisplayInfoDto(f.id, f.name, f.contentType, f.size, f.createdDate, f.updatedDate) from FileEntity f where f.typeStore = 'S3 cloud'", FileDisplayInfoDto.class)
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
        AmazonS3 s3 = createCloudManager();
        String[] splitPath = fileInfo.getPath().split("&");
        String bucket = splitPath[0];
        String directoryPath = splitPath[1];
        S3Object object = s3.getObject(new GetObjectRequest(bucket, directoryPath + "/" + fileInfo.getKeyFile()));
        try (InputStream in = object.getObjectContent()) {
            return in.readAllBytes();
        } catch (IOException e) {
            LOG.error("Error read file" + e.getMessage());
            throw new ReadInputStreamException("Error read file", e);
        }
    }

    @Override
    public String deleteFile(Long id) {
        Optional<FileEntity> optionalFileEntity = fileRepository.findById(id);
        if (optionalFileEntity.isPresent()) {
            deleteFileFromS3Cloud(modelMapper.map(optionalFileEntity.get(), FileDto.class));
            fileRepository.deleteById(optionalFileEntity.get().getId());
            return MESSAGE_ABOUT_DELETED;
        } else {
            LOG.error("File metadata not found for deleting. Id of file: " + id);
            throw new ResourceNotFoundException("File metadata not found");
        }
    }

    private void deleteFileFromS3Cloud(FileDto fileDto) {
        try {
            AmazonS3 s3 = createCloudManager();
            String[] pathSplit = fileDto.getPath().split("&");
            String bucket = pathSplit[0];
            String directory = pathSplit[1];
            DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucket, directory + "/" + fileDto.getKeyFile());
            s3.deleteObject(deleteObjectRequest);
            LOG.info("Success deleted file: " + fileDto);
        } catch (AmazonServiceException ase) {
            String messageError = createMessageServiceException(ase);
            LOG.error(messageError);
            throw new SavedFileException(messageError);
        } catch (AmazonClientException ace) {
            String messageError = createMessageClientS3Exception(ace);
            LOG.error(messageError);
            throw new SavedFileException(messageError);
        }
    }
}