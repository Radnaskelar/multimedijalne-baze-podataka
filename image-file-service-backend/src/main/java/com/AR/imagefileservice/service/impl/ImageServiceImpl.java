package com.ar.imagefileservice.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.ar.imagefileservice.config.AwsS3Config;
import com.ar.imagefileservice.domain.Image;
import com.ar.imagefileservice.dto.ImageDto;
import com.ar.imagefileservice.mapper.ImageMapper;
import com.ar.imagefileservice.repository.ImageRepository;
import com.ar.imagefileservice.service.ImageService;
import com.ar.imagefileservice.util.DominantColorExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageServiceImpl implements ImageService {

    private final AmazonS3 amazonS3;
    private final ImageRepository imageRepository;
    private final AwsS3Config awsS3Config;
    private final ImageMapper imageMapper;
    private final DominantColorExtractor dominantColorExtractor;

    @Override
    public String uploadImage(MultipartFile file, String title) {
        try {
            String key = "images/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
            String bucketName = awsS3Config.getBucketName();

            byte[] bytes = file.getBytes();

            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(bytes.length);

            ByteArrayInputStream uploadStream = new ByteArrayInputStream(bytes);
            PutObjectRequest put = new PutObjectRequest(bucketName, key, uploadStream, metadata);
            amazonS3.putObject(put);

            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
            String hexColor = dominantColorExtractor.determineHexColorValue(bufferedImage);
            DominantColorExtractor.DominantColor dominantColor =
                    dominantColorExtractor.determineDominantColor(bufferedImage);

            Image image = Image.builder()
                    .title(title)
                    .awsKey(key)
                    .imageUrl(getImageUrl(key))
                    .hexValue(hexColor)
                    .dominantColor(dominantColor.name())
                    .build();

            imageRepository.save(image);
            return key;
        } catch (IOException e) {
            throw new RuntimeException("Error uploading image.", e);
        }
    }

    @Override
    public List<ImageDto> getAllImages() {
        return imageMapper.imagesToImageDTOs(imageRepository.findAll());
    }

    @Override
    public void deleteImage(Long id) {
        Image image = imageRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        amazonS3.deleteObject(new DeleteObjectRequest(awsS3Config.getBucketName(), image.getAwsKey()));
        imageRepository.delete(image);
    }

    @Override
    public List<ImageDto> searchImagesByColor(String color) {
        return imageMapper.imagesToImageDTOs(imageRepository.findByDominantColorIgnoreCase(color));
    }

    @Override
    public byte[] getImageDataById(Long imageId) {
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new RuntimeException("Image not found"));

        S3Object s3Object = amazonS3.getObject(awsS3Config.getBucketName(), image.getAwsKey());
        try (S3ObjectInputStream stream = s3Object.getObjectContent()) {
            return StreamUtils.copyToByteArray(stream);
        } catch (IOException e) {
            throw new RuntimeException("Error while reading image data.", e);
        }
    }

    private String getImageUrl(String key) {
        // For this to be viewable directly in the browser, the S3 object must be publicly readable
        return "https://" + awsS3Config.getBucketName() + ".s3.amazonaws.com/" + key;
    }
}
