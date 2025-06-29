package com.awsdemo.awstask.controller;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageMetadataDto {
    private String name;
    private long size;
    private String extension;
    private Instant lastModified;

    public ImageMetadataDto(String fileName, Long size, LocalDateTime localDateTime, String fileExtension) {
    }
}
