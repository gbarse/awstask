package com.awsdemo.awstask.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageAnalyticsService {
    private final DynamoDbClient dynamo;
    private final String TABLE = "GorB-ImageAnalytics";

    public void incrementView(String imageName) {
        incrementCounter(imageName, "view_count");
    }
    public void incrementDownload(String imageName) {
        incrementCounter(imageName, "download_count");
    }

    private void incrementCounter(String imageName, String attr) {
        dynamo.updateItem(b -> b
                .tableName(TABLE)
                .key(Map.of("image_name", AttributeValue.fromS(imageName)))
                .updateExpression("ADD #c :inc")
                .expressionAttributeNames(Map.of("#c", attr))
                .expressionAttributeValues(Map.of(":inc", AttributeValue.fromN("1")))
        );
    }
}

