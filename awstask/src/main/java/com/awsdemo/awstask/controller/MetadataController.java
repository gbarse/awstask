package com.awsdemo.awstask.controller;

import com.amazonaws.util.EC2MetadataUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@RestController
@Slf4j
public class MetadataController {
    @Value("${aws.sqs.queue.url}")
    private String queueUrl;
    private final S3Client s3Client;
    private final String bucketName = "gor-barseghyan-site";
    private final ImageMetadataRepository metadataRepo;
    private final SqsClient sqsClient;
    private final ImageAnalyticsService analytics;
    public MetadataController(S3Client s3Client, ImageMetadataRepository metadataRepo, SqsClient sqsClient, ImageAnalyticsService analytics) {
        this.s3Client = s3Client;
        this.metadataRepo = metadataRepo;
        this.sqsClient = sqsClient;
        this.analytics = analytics;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file) throws IOException {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(file.getOriginalFilename())
                        .contentType(file.getContentType())
                        .build(),
                RequestBody.fromBytes(file.getBytes())
        );

        ImageMetadata metadata = new ImageMetadata();
        metadata.setName(file.getOriginalFilename());
        metadata.setSize(file.getSize());
        metadata.setExtension(getFileExtension(file.getOriginalFilename()));
        metadata.setLastModified(LocalDateTime.now());

        metadataRepo.save(metadata);

        String messageBody = """
            {
              "event": "IMAGE_UPLOADED",
              "name": "%s",
              "size": %d,
              "extension": "%s"
            }
            """.formatted(metadata.getName(), metadata.getSize(), metadata.getExtension());

        sqsClient.sendMessage(SendMessageRequest.builder()
                .queueUrl(queueUrl)
                .messageBody(messageBody)
                .build());

        return ResponseEntity.ok("File uploaded, metadata saved, and message sent to SQS.");
    }

    @GetMapping("/download")
    public ResponseEntity<byte[]> downloadFile(@RequestParam String fileName) {
        log.info("Attempting to download: {}", fileName);
        analytics.incrementDownload(fileName);
        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build());

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(response.asByteArray());
    }



    @GetMapping("/metadata/{fileName}")
    public ResponseEntity<ImageMetadataDto> getMetadata(@PathVariable String fileName) {
        analytics.incrementView(fileName);
        HeadObjectResponse headObject = s3Client.headObject(HeadObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build());

        ImageMetadataDto metadata = new ImageMetadataDto();
        metadata.setName(fileName);
        metadata.setSize(headObject.contentLength());
        metadata.setExtension(fileName.contains(".") ? fileName.substring(fileName.lastIndexOf(".") + 1) : "");
        metadata.setLastModified(headObject.lastModified());

        return ResponseEntity.ok(metadata);
    }


    @GetMapping("/metadata/random")
    public ResponseEntity<ImageMetadataDto> getRandomMetadata() {
        ListObjectsV2Response listResponse = s3Client.listObjectsV2(ListObjectsV2Request.builder()
                .bucket(bucketName)
                .build());

        List<String> keys = listResponse.contents().stream().map(S3Object::key).collect(Collectors.toList());
        if (keys.isEmpty()) return ResponseEntity.notFound().build();

        String randomKey = keys.get(new Random().nextInt(keys.size()));
        return getMetadata(randomKey);
    }

    @DeleteMapping("/delete/{fileName}")
    public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(fileName)
                .build());
        return ResponseEntity.ok("Deleted: " + fileName);
    }

    private String getFileExtension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        return (dot >= 0) ? fileName.substring(dot + 1) : "unknown";
    }


//    @GetMapping("/")
//    public String getRegionAndAz() {
//            String az = EC2MetadataUtils.getAvailabilityZone();
//            String region = EC2MetadataUtils.getEC2InstanceRegion();
//            return "Region: " + region + ", AZ: " + az;
//    }

    @GetMapping("/")
    public String health() {
        return "OK !!";   // No EC2MetadataUtils!
    }


    @PostMapping("/trigger")
    public ResponseEntity<?> checkConsistency() throws JsonProcessingException {
        LambdaClient lambdaClient = LambdaClient.builder()
                .region(Region.US_EAST_2)
                .build();

        DataConsistencyRequest req = new DataConsistencyRequest();
        req.setSource("webapp");

        String json = new ObjectMapper().writeValueAsString(req);

        InvokeRequest request = InvokeRequest.builder()
                .functionName("GorB-subtask2-DataConsistencyFunction")
                .payload(SdkBytes.fromUtf8String(json))
                .build();

        InvokeResponse response = lambdaClient.invoke(request);
        String result = response.payload().asUtf8String();

        return ResponseEntity.ok(result);
    }
//    @GetMapping("/top/{n}")
//    public List<ImageAnalyticsDto> topN(@PathVariable int n) {
//        var scan = dynamo.scan(b -> b
//                .tableName(TABLE)
//                .limit(n)
//                .projectionExpression("image_name, view_count, download_count")
//                .scanIndexForward(false)      // descending if you have a GSI
//        );
//        // sort by view_count in code or add a GSI if you need server-side ordering
//    }



}

