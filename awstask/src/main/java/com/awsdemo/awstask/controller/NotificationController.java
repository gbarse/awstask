package com.awsdemo.awstask.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.sns.SnsClient;
import software.amazon.awssdk.services.sns.model.SnsException;
import software.amazon.awssdk.services.sns.model.SubscribeRequest;
import software.amazon.awssdk.services.sns.model.SubscribeResponse;
import software.amazon.awssdk.services.sns.model.UnsubscribeRequest;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final SnsClient snsClient;

    @Value("${aws.sns.topic.arn}")
    private String topicArn;

    @GetMapping("/subscribe")
    public String subscribeForm() {
        return "subscribe.html";
    }

    @GetMapping("/unsubscribe")
    public String unsubscribeForm() {
        return "unsubscribe.html";
    }

    @PostMapping("/subscribe")
    public ResponseEntity<String> subscribe(@RequestParam String email) throws SnsException {
        var existing = snsClient.listSubscriptionsByTopic(r -> r.topicArn(topicArn)).subscriptions().stream()
                .filter(sub -> email.equals(sub.endpoint()))
                .findFirst();

        if (existing.isPresent()) {
            return ResponseEntity.ok("Already subscribed: " + email);
        }

        SubscribeResponse response = snsClient.subscribe(SubscribeRequest.builder()
                .protocol("email")
                .endpoint(email)
                .returnSubscriptionArn(true)
                .topicArn(topicArn)
                .build());

        return ResponseEntity.ok("Confirmation email sent to " + email);
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<String> unsubscribe(@RequestParam String email) {
        var subscription = snsClient.listSubscriptionsByTopic(r -> r.topicArn(topicArn)).subscriptions().stream()
                .filter(sub -> email.equals(sub.endpoint()))
                .findFirst();

        if (subscription.isEmpty()) {
            return ResponseEntity.badRequest().body("No active subscription found for: " + email);
        }

        snsClient.unsubscribe(UnsubscribeRequest.builder()
                .subscriptionArn(subscription.get().subscriptionArn())
                .build());

        return ResponseEntity.ok("Unsubscribed " + email);
    }
}
