//package com.awsdemo.awstask.controller;
//
//import com.amazonaws.util.EC2MetadataUtils;
//import com.fasterxml.jackson.core.JsonProcessingException;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.scheduling.annotation.Scheduled;
//import org.springframework.stereotype.Component;
//import software.amazon.awssdk.services.sns.SnsClient;
//import software.amazon.awssdk.services.sns.model.PublishRequest;
//import software.amazon.awssdk.services.sqs.SqsClient;
//import software.amazon.awssdk.services.sqs.model.*;
//
//import java.net.URLEncoder;
//import java.nio.charset.StandardCharsets;
//import java.util.List;
//
//@Component
//@Slf4j
//@RequiredArgsConstructor
//public class SqsToSnsScheduler {
//
//    private final SqsClient sqsClient;
//    private final SnsClient snsClient;
//    private final ImageMetadataRepository metadataRepo;
//
//    @Value("${aws.sqs.queue.url}")
//    private String queueUrl;
//
//    @Value("${aws.sns.topic.arn}")
//    private String topicArn;
//
//    private final ObjectMapper objectMapper = new ObjectMapper();
//
//    @Scheduled(fixedDelay = 10000)
//    public void processMessages() {
//        log.info("🔄 Starting scheduled SQS polling...");
//
//        ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
//                .queueUrl(queueUrl)
//                .maxNumberOfMessages(10)
//                .waitTimeSeconds(5)
//                .build());
//
//        for (Message message : response.messages()) {
//            try {
//                log.info("📩 SQS Message Body: {}", message.body());
//
//                EventDto wrapper = objectMapper.readValue(message.body(), EventDto.class);
//                String fileName = wrapper.getName();
//
//                log.info("📂 Processing file: {}", fileName);
//
//                List<ImageMetadata> matches = metadataRepo.findAllByName(fileName);
//
//                if (matches.isEmpty()) {
//                    log.warn("No metadata found for: {}", fileName);
//                } else {
//                    ImageMetadata meta = matches.get(0);
//                    String publicIp = EC2MetadataUtils.getData("/latest/meta-data/public-ipv4");
//                    String encodedFileName = URLEncoder.encode(meta.getName(), StandardCharsets.UTF_8);
//                    String downloadUrl = "http://" + publicIp + "/download?fileName=" + encodedFileName;
//
//                    String msg = """
// An image has been uploaded.
//Name: %s
//Size: %d bytes
//Extension: %s
// Download it here: %s
//""".formatted(meta.getName(), meta.getSize(), meta.getExtension(), downloadUrl);
//
//
//                    log.info(" Publishing SNS message for file: {}", meta.getName());
//                    snsClient.publish(PublishRequest.builder()
//                            .topicArn(topicArn)
//                            .message(msg)
//                            .build());
//                }
//
//                sqsClient.deleteMessage(DeleteMessageRequest.builder()
//                        .queueUrl(queueUrl)
//                        .receiptHandle(message.receiptHandle())
//                        .build());
//
//            } catch (JsonProcessingException e) {
//                log.error(" Failed to deserialize SQS message", e);
//            } catch (Exception e) {
//                log.error("Unexpected error while processing message", e);
//            }
//        }
//    }
//}
