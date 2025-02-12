package com.goorm.ticker.common.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.annotation.PostConstruct;
import java.io.InputStream;

@Configuration
@Profile("!test")
public class FirebaseConfig {

    @PostConstruct
    public void initializeFirebase() {
        try {
            InputStream serviceAccount = getClass().getClassLoader().getResourceAsStream("ticker-d92f7-firebase-adminsdk-fbsvc-7b468ffbee.json");
            if (serviceAccount == null) {
                throw new RuntimeException("Firebase 서비스 계정 키 파일을 찾을 수 없습니다.");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .build();

            FirebaseApp.initializeApp(options);
        } catch (Exception e) {
            throw new RuntimeException("Firebase 초기화 중 오류 발생: " + e.getMessage(), e);
        }
    }
}