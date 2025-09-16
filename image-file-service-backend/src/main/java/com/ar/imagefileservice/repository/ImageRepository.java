package com.ar.imagefileservice.repository;

import com.ar.imagefileservice.domain.Image;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

    List<Image> findByDominantColorIgnoreCase(String dominantColor);

}
