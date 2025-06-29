package com.awsdemo.awstask.controller;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UploadResponseDto {
    private String message;
    private String fileName;
}
