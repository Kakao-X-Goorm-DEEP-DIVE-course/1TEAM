package com.example.backend.websocket;

import com.example.backend.service.KafkaProducerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okhttp3.Response;
import okhttp3.RequestBody;
import org.json.JSONObject;

@Slf4j
@Service
public class KisWebSocketClient {
    private final String WS_URL = "ws://ops.koreainvestment.com:31000/tryitout/H0STCNT0";
    private final KafkaProducerService kafkaProducerService;

    // kafka producer 추가
    @Autowired
    public KisWebSocketClient(KafkaProducerService kafkaProducerService) {
        this.kafkaProducerService = kafkaProducerService;
    }

    public void connect(String approvalKeyJson) {
        JSONObject jsonObject = new JSONObject(approvalKeyJson);
        String approvalKey = jsonObject.getString("approval_key");
        OkHttpClient client = new OkHttpClient();


        Request request = new Request.Builder()
                .url(WS_URL)
                .addHeader("approval_key", approvalKey)            // 헤더에 approval_key 추가
                .addHeader("custtype", "P")                                  // 헤더에 custtype 추가
                .addHeader("tr_type", "1")
                .addHeader("content-type", "utf-8")
                .build();

        WebSocket webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("웹소켓 연결 성공");
                String requestData = String.format(
                        "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"1\",\"content-type\":\"utf-8\"},\"body\":{\"input\":{\"tr_id\":\"H0STCNT0\",\"tr_key\":\"005930\"}}}",
                        approvalKey
                );
                webSocket.send(requestData);
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    log.info("원본 데이터: {}", text);

                    // 데이터가 |로 구분되어 있으므로 분리
                    String[] data = text.split("\\|");

                    // STCK_PRPR(현재가)와 STCK_CNTG_HOUR(체결시간) 추출
                    if (data.length > 2) {
                        String currentPrice = data[0];  // STCK_PRPR
                        String tradingTime = data[1];   // STCK_CNTG_HOUR

                        log.info("현재가(STCK_PRPR): {}, 체결시간(STCK_CNTG_HOUR): {}",
                                currentPrice, tradingTime);

                        // Kafka로 메시지 전송
                        String kafkaMessage = String.format(
                                "{\"currentPrice\": \"%s\", \"tradingTime\": \"%s\"}",
                                currentPrice, tradingTime
                        );

                        String topic = "realtime-data";
                        kafkaProducerService.sendMessage(topic, kafkaMessage);
                    }

                } catch (Exception e) {
                    log.error("데이터 처리 중 오류: ", e);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                log.error("웹소켓 에러: ", t);
            }
        });
    }
}


