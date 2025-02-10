package com.goorm.ticker.notification.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.springframework.stereotype.Service;

@Service
public class FCMService {
    public void sentNotification(String topic, String title, String body) {
        Message message = Message.builder()
                .putData("title", title)
                .putData("body", body)
                .setTopic(topic)
                .build();

        try {
            String response = FirebaseMessaging.getInstance().send(message);
            System.out.println("Successfully sent notification: " + response);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}