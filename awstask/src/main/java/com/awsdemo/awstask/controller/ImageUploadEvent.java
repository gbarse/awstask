package com.awsdemo.awstask.controller;

import lombok.Data;

@Data
public class ImageUploadEvent {
    private String name;
    private long size;
    private String extension;

}

