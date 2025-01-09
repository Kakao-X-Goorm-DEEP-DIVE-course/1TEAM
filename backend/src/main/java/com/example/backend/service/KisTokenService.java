package com.example.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import okhttp3.*;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import java.time.Instant;


@Slf4j
@Service
public class KisTokenService {
    @Value("${kis.api.appKey}")
    private String appKey;

    @Value("${kis.api.appSecret}")
    private String appSecret;

    @Value("${kis.api.baseUrl}")
    private String baseUrl;

    private final String TOKEN_PATH = "/oauth2/tokenP";
    
    private String cachedToken;
    private Instant tokenExpirationTime;

    public String getAccessToken() throws Exception {
        if (isTokenValid()) {
            return cachedToken;
        }
        
        OkHttpClient client = new OkHttpClient().newBuilder().build();
        
        String jsonBody = String.format(
            "{\"grant_type\":\"client_credentials\",\"appkey\":\"%s\",\"appsecret\":\"%s\"}",
            appKey, appSecret
        );
        
        log.info("Request URL: {}", baseUrl + TOKEN_PATH);
        log.info("Request Body: {}", jsonBody);
        
        RequestBody body = RequestBody.create(
            MediaType.parse("application/json"), 
            jsonBody
        );
        
        Request request = new Request.Builder()
                .url(baseUrl + TOKEN_PATH)
                .post(body)
                .addHeader("content-type", "application/json")
                .build();
                
        try (Response response = client.newCall(request).execute()) {
            String responseBody = response.body().string();
            log.info("Response Code: {}", response.code());
            log.info("Response Body: {}", responseBody);
            
            if (!response.isSuccessful()) {
                throw new RuntimeException("API 호출 실패: " + response.code() + ", Body: " + responseBody);
            }
            
            cachedToken = responseBody.split("\"access_token\":\"")[1].split("\"")[0];
            tokenExpirationTime = Instant.now().plusSeconds(86400); // 토큰 유효 기간을 24시간으로 설정
            return cachedToken;
        } catch (Exception e) {
            log.error("토큰 발급 중 오류 발생", e);
            throw e;
        }
    }

    public String getCachedAccessToken() throws Exception {
        if (isTokenValid()) {
            return cachedToken;
        } else {
            return getAccessToken(); // 토큰이 만료되었거나 없으면 새로 발급
        }
    }

    private boolean isTokenValid() {
        return cachedToken != null && Instant.now().isBefore(tokenExpirationTime);
    }
}
