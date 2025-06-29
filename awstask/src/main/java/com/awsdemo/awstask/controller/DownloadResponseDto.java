package com.awsdemo.awstask.controller;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DownloadResponseDto {
    private String fileName;
    private byte[] content;
    private String contentType;
}
