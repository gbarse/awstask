package com.awsdemo.awstask.controller;

import lombok.Data;

@Data
public class EventDto {
    private String event;
    private String name;
    private long size;
    private String extension;
}

