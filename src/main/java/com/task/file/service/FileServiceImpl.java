package com.task.file.service;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.task.file.model.dto.FileDto;
import com.task.file.model.entity.FileEntity;
import com.task.file.repository.FileRepository;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileServiceImpl implements FileService{
    private final String NAME_BUCKET = "file-storage-s3";
    private final String DIRECTORY = "Document";
    FileRepository fileRepository;
    ModelMapper modelMapper;
    ResourceLoader resourceLoader;

    @Autowired
    public FileServiceImpl(FileRepository fileRepository, ModelMapper modelMapper, ResourceLoader resourceLoader) {
        this.fileRepository = fileRepository;
        this.modelMapper = modelMapper;
        this.resourceLoader = resourceLoader;
    }

    @Override
    public FileDto saveFile(MultipartFile file) {
        FileDto fileDto = new FileDto();
        fileDto.setName(file.getOriginalFilename());
        //todo переделать ссылку
        fileDto.setPathDirectory(DIRECTORY);
        fileDto.setBucket(NAME_BUCKET);
        fileDto.setContentType(file.getContentType());
        fileDto.setSize(file.getSize());

        AmazonS3 s3 = createCloudManager();
        ObjectMetadata objectMetadata = new ObjectMetadata();
        objectMetadata.setContentLength(file.getSize());
        objectMetadata.setContentType("application/octet-stream");
        if (s3.listBuckets().stream().noneMatch(bucket -> bucket.getName().equals(NAME_BUCKET)))
            s3.createBucket(NAME_BUCKET);
        String key =  UUID.randomUUID() + file.getOriginalFilename();
        fileDto.setKeyFile(key);

        try (ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes());) {
            s3.putObject(NAME_BUCKET, DIRECTORY + "/" + key, bais, objectMetadata);
        } catch (AmazonServiceException ase) {
            System.out.println("Caught an AmazonServiceException, which means your request made it "
                    + "to Amazon S3, but was rejected with an error response for some reason.");
            System.out.println("Error Message:    " + ase.getMessage());
            System.out.println("HTTP Status Code: " + ase.getStatusCode());
            System.out.println("AWS Error Code:   " + ase.getErrorCode());
            System.out.println("Error Type:       " + ase.getErrorType());
            System.out.println("Request ID:       " + ase.getRequestId());
        } catch (AmazonClientException ace) {
            System.out.println("Caught an AmazonClientException, which means the client encountered "
                    + "a serious internal problem while trying to communicate with S3, "
                    + "such as not being able to access the network.");
            System.out.println("Error Message: " + ace.getMessage());
        } catch (IOException e) {
            throw new RuntimeException("File not found " + e);
        }

        FileEntity fileEntity = modelMapper.map(fileDto, FileEntity.class);
        FileEntity savedFileEntity = fileRepository.save(fileEntity);
        return modelMapper.map(savedFileEntity, FileDto.class);

    }

    private AmazonS3 createCloudManager() {
        AWSCredentials credentials = null;
        try {
            credentials = new ProfileCredentialsProvider().getCredentials();
        } catch (Exception e) {
            throw new AmazonClientException(
                    "Cannot load the credentials from the credential profiles file",
                    e);
        }
        return AmazonS3ClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(
                        new AmazonS3ClientBuilder.EndpointConfiguration(
                                "storage.yandexcloud.net","ru-central1"
                        )
                )
                .build();
    }

    @Override
    public List<FileDto> getListFiles() {
//        ObjectListing objectListing = s3.listObjects(new ListObjectsRequest()
//                    .withBucketName(bucketName)
//                    .withPrefix("My"));
//            for (S3ObjectSummary objectSummary : objectListing.getObjectSummaries()) {
//                System.out.println(" - " + objectSummary.getKey() + "  " +
//                        "(size = " + objectSummary.getSize() + ")");
//            }
        return fileRepository.findAll().stream().map(this::mapToDto).collect(Collectors.toList());
    }

    private FileDto mapToDto(FileEntity entity) {
        return modelMapper.map(entity, FileDto.class);
    }

    @Override
    public byte[] getFileByKey(FileDto fileInfo) {
        AmazonS3 s3 = createCloudManager();
        S3Object object = s3.getObject(new GetObjectRequest(fileInfo.getBucket(), fileInfo.getPathDirectory() + "/" + fileInfo.getKeyFile()));
        try {
            //todo сделать буферное чтение (частями)
            return object.getObjectContent().readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public FileDto getFileInfoById(Long id) {
        Optional<FileEntity> optionalFileEntity = fileRepository.findById(id);
        if(optionalFileEntity.isPresent()) {
            return mapToDto(optionalFileEntity.get());
        } else {
            throw new RuntimeException();
        }

    }

    @Override
    public String deleteFile(Long id) {
        /*
         * Delete an object - Unless versioning has been turned on for your bucket,
         * there is no way to undelete an object, so use caution when deleting objects.
         */
//            System.out.println("Deleting an object\n");
//            s3.deleteObject(bucketName, key);

        /*
         * Delete a bucket - A bucket must be completely empty before it can be
         * deleted, so remember to delete any objects from your buckets before
         * you try to delete them.
         */
//            System.out.println("Deleting bucket " + bucketName + "\n");
//            s3.deleteBucket(bucketName);
//        } catch (AmazonServiceException ase) {
//            System.out.println("Caught an AmazonServiceException, which means your request made it "
//                    + "to Amazon S3, but was rejected with an error response for some reason.");
//            System.out.println("Error Message:    " + ase.getMessage());
//            System.out.println("HTTP Status Code: " + ase.getStatusCode());
//            System.out.println("AWS Error Code:   " + ase.getErrorCode());
//            System.out.println("Error Type:       " + ase.getErrorType());
//            System.out.println("Request ID:       " + ase.getRequestId());
//        } catch (AmazonClientException ace) {
//            System.out.println("Caught an AmazonClientException, which means the client encountered "
//                    + "a serious internal problem while trying to communicate with S3, "
//                    + "such as not being able to access the network.");
//            System.out.println("Error Message: " + ace.getMessage());
//        }
        return null;
    }


}
