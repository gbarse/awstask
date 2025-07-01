package com.awsdemo.awstask.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ImageAnalyticsService {

    private final DynamoDbClient dynamo;
    private static final String TABLE = "GorB-ImageAnalytics";

    public void incrementView(String imageName)     { incrementCounter(imageName, "view_count"); }
    public void incrementDownload(String imageName) { incrementCounter(imageName, "download_count"); }

    private void incrementCounter(String imageName, String counterAttr) {

        long now = Instant.now().getEpochSecond();  

        dynamo.updateItem(b -> b
                .tableName(TABLE)
                .key(Map.of("image_id", AttributeValue.fromS(imageName)))


                .updateExpression("SET last_activity = :ts ADD #ctr :inc")
                .expressionAttributeNames(Map.of("#ctr", counterAttr))
                .expressionAttributeValues(Map.of(
                        ":inc", AttributeValue.fromN("1"),
                        ":ts",  AttributeValue.fromN(Long.toString(now))
                ))
        );
    }
}
