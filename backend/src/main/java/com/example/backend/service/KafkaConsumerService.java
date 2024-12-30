package com.example.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class KafkaConsumerService {

    private final ListOperations<String, String> listOperations;
    private final ObjectMapper objectMapper;
    private final RedisTemplate<String, String> redisTemplate;

    @Autowired
    public KafkaConsumerService(RedisTemplate<String, String> redisTemplate) {
        // Redis List 연산 객체 생성
        this.listOperations = redisTemplate.opsForList();
        this.objectMapper = new ObjectMapper();
        this.redisTemplate = redisTemplate;
    }

    @KafkaListener(topics = "realtime-data", groupId = "stock-group")
    public void consume(String message) {
        try {
            // Kafka 메시지 JSON 변환
            Map<String, String> stockData = objectMapper.readValue(message, new TypeReference<Map<String, String>>() {});
            log.info("Kafka 메시지 수신: {}", stockData);

            // 데이터 유효성 검사 및 누락값 보완
            validateAndFillData(stockData);

            String stockId = stockData.get("stockId");
            String redisKey = "stock:" + stockId;

            // JSON 리스트 구조로 저장 (LIFO 방식)
            String jsonData = objectMapper.writeValueAsString(stockData);

            // 중복 데이터 확인
            if (isDuplicateData(redisKey, stockData)) {
                log.info("중복 데이터 무시: {}", stockData);
                return;
            }

            // Redis에 최신 5개의 데이터 저장
            listOperations.leftPush(redisKey, jsonData);
            listOperations.trim(redisKey, 0, 4); // 리스트 크기를 5개로 제한

            log.info("Redis에 최신 5개 데이터 저장 완료: {}", redisKey);

            // TTL 설정 (예: 1시간)
            redisTemplate.expire(redisKey, 1, java.util.concurrent.TimeUnit.HOURS);

        } catch (Exception e) {
            log.error("Kafka 메시지 처리 중 오류: ", e);
        }
    }

    /**
     * 데이터 유효성 검사 및 누락값 보완
     */
    private void validateAndFillData(Map<String, String> stockData) {
        if (!stockData.containsKey("currentPrice")) stockData.put("currentPrice", "0");
        if (!stockData.containsKey("fluctuationPrice")) stockData.put("fluctuationPrice", "0");
        if (!stockData.containsKey("fluctuationRate")) stockData.put("fluctuationRate", "0.00");
        if (!stockData.containsKey("fluctuationSign")) stockData.put("fluctuationSign", "0");
        if (!stockData.containsKey("transactionVolume")) stockData.put("transactionVolume", "0");
        if (!stockData.containsKey("tradingTime")) stockData.put("tradingTime", "000000");
    }

    /**
     * 중복 데이터 확인
     */
    private boolean isDuplicateData(String redisKey, Map<String, String> stockData) {
        String latestData = listOperations.index(redisKey, 0);
        if (latestData == null) return false;

        try {
            Map<String, String> latestDataMap = objectMapper.readValue(latestData, new TypeReference<Map<String, String>>() {});
            return stockData.equals(latestDataMap);
        } catch (Exception e) {
            log.error("중복 데이터 확인 오류: ", e);
            return false;
        }
    }
}