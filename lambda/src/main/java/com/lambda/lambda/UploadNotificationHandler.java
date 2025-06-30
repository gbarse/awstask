package com.lambda.lambda;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.PublishRequest;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class UploadNotificationHandler implements RequestHandler<SQSEvent, Void> {

    private final SnsClient snsClient = SnsClient.create();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String topicArn = System.getenv("SNS_TOPIC_ARN");
    private static final String PUBLIC_HOST =
        Optional.ofNullable(System.getenv("APP_PUBLIC_HOST")).orElse("");
    @Override
    public Void handleRequest(SQSEvent event, Context context) {
        for (SQSEvent.SQSMessage message : event.getRecords()) {
            try {
                ImageMetadata metadata = objectMapper.readValue(message.getBody(), ImageMetadata.class);

                String downloadLink = "https://" + PUBLIC_HOST
        + "/download?fileName="
        + URLEncoder.encode(metadata.getName(), StandardCharsets.UTF_8);

                String msg = """
                    📷 An image has been uploaded.
                    Name: %s
                    Size: %d bytes
                    Extension: %s
                    🔗 Download it here: %s
                    """.formatted(metadata.getName(), metadata.getSize(), metadata.getExtension(), downloadLink);

                snsClient.publish(PublishRequest.builder()
                        .topicArn(topicArn)
                        .message(msg)
                        .build());

                context.getLogger().log("Published SNS message for image: " + metadata.getName());

            } catch (Exception e) {
                context.getLogger().log("Error processing message: " + e.getMessage());
            }
        }
        return null;
    }
}
