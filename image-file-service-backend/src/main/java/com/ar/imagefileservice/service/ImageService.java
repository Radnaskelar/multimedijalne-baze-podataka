package com.ar.imagefileservice.service;

import com.ar.imagefileservice.dto.ImageDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ImageService {

    String uploadImage(MultipartFile file, String title);

    List<ImageDto> getAllImages();


    void deleteImage(Long id);

    List<ImageDto> searchImagesByColor(String color);

    byte[] getImageDataById(Long imageId);

}
