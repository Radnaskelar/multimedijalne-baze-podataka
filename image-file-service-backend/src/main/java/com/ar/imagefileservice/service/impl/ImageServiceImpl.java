package com.ar.imagefileservice.service.impl;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.ar.imagefileservice.domain.Image;
import com.ar.imagefileservice.dto.ImageDto;
import com.ar.imagefileservice.mapper.ImageMapper;
import com.ar.imagefileservice.repository.ImageRepository;
import com.ar.imagefileservice.service.ImageService;
import com.ar.imagefileservice.util.DominantColorExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
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
    private final ImageMapper imageMapper;
    private final DominantColorExtractor dominantColorExtractor;

    @Value("${aws.s3.bucketName}")
    private String bucketName;

    @Value("${aws.region}")
    private String region;

    @Override
    public String uploadImage(MultipartFile file, String title) {
        try {
            // Create a stable S3 key
            String key = "images/" + UUID.randomUUID() + "-" + file.getOriginalFilename();

            // Read bytes ONCE so we can use the same stream for both S3 and ImageIO
            byte[] bytes = file.getBytes();

            // Metadata prevents in-memory buffering warning and sets content-type
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(file.getContentType());
            meta.setContentLength(bytes.length);

            // Upload to S3
            ByteArrayInputStream uploadStream = new ByteArrayInputStream(bytes);
            PutObjectRequest putReq = new PutObjectRequest(bucketName, key, uploadStream, meta);
            amazonS3.putObject(putReq);

            // Analyze dominant color (read from the same bytes)
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(bytes));
            String hexColor = dominantColorExtractor.determineHexColorValue(bufferedImage);
            DominantColorExtractor.DominantColor dominantColor = dominantColorExtractor.determineDominantColor(bufferedImage);

            // Persist DB record
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
            throw new RuntimeException("Failed to upload image", e);
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

        amazonS3.deleteObject(new DeleteObjectRequest(bucketName, image.getAwsKey()));
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

        S3Object s3Object = amazonS3.getObject(bucketName, image.getAwsKey());
        try (S3ObjectInputStream stream = s3Object.getObjectContent()) {
            return StreamUtils.copyToByteArray(stream);
        } catch (IOException e) {
            throw new RuntimeException("Error while reading image data.", e);
        }
    }

    private String getImageUrl(String key) {
  return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + key;
}

}
