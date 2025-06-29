package com.awsdemo.awstask.controller;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface ImageMetadataRepository extends JpaRepository<ImageMetadata, Long> {
    List<ImageMetadata> findAllByName(String name);

}

