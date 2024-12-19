package com.example.backend.websocket;

import lombok.extern.slf4j.Slf4j;
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
    private final String WS_URL = "ws://ops.koreainvestment.com:21000/tryitout/H0STCNT0";
    
    private WebSocket webSocket;
    private String approvalKey;

    //웹소켓 연결 코드(제일 먼저 실행)
    public void connect(String approvalKeyJson) {
        JSONObject jsonObject = new JSONObject(approvalKeyJson);
        this.approvalKey = jsonObject.getString("approval_key");
        OkHttpClient client = new OkHttpClient();
        
        
        Request request = new Request.Builder()
            .url(WS_URL)
            .addHeader("approval_key", approvalKey)            
            .addHeader("custtype", "P")                                  
            .addHeader("tr_type", "1") 
            .addHeader("content-type", "utf-8")
            .build();
            
        this.webSocket = client.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                log.info("웹소켓 연결 성공");
                String[] subscriptionlist = {"K1", "Q1"}; //구독할 리스트
                subscribeStocks(subscriptionlist);
            }
            
            @Override
            public void onMessage(WebSocket webSocket, String text) {
                try {
                    log.info("원본 데이터: {}", text);
                    
                    // 데이터가 |로 구분되어 있으므로 분리
                    String[] data = text.split("\\|");

                    if (data.length > 3) {
                        String text_encryption = data[0];
                        String text_tr_id = data[1];
                        int text_data_number = Integer.parseInt(data[2]);
                        String text_response_messages = data[3];

                        String[] responsedata = text_response_messages.split("\\^");
                        
                        for (int i = 0; i < text_data_number; i++) {
                            String MKSC_SHRN_ISCD = responsedata[i * 46];
                            String STCK_CNTG_HOUR = responsedata[i * 46 + 1];
                            String STCK_PRPR = responsedata[i * 46 + 2];
                            String PRDY_VRSS_SIGN = responsedata[i * 46 + 3];
                            String PRDY_VRSS = responsedata[i * 46 + 4];
                            String PRDY_CTRT = responsedata[i * 46 + 5];
                            String WGHN_AVRG_STCK_PRC = responsedata[i * 46 + 6];
                            String STCK_OPRC = responsedata[i * 46 + 7];
                            String STCK_HGPR = responsedata[i * 46 + 8];
                            String STCK_LWPR = responsedata[i * 46 + 9];
                            String ASKP1 = responsedata[i * 46 + 10];
                            String BIDP1 = responsedata[i * 46 + 11];
                            String CNTG_VOL = responsedata[i * 46 + 12];
                            String ACML_VOL = responsedata[i * 46 + 13];
                            String ACML_TR_PBMN = responsedata[i * 46 + 14];
                            String SELN_CNTG_CSNU = responsedata[i * 46 + 15];
                            String SHNU_CNTG_CSNU = responsedata[i * 46 + 16];
                            String NTBY_CNTG_CSNU = responsedata[i * 46 + 17];
                            String CTTR = responsedata[i * 46 + 18];
                            String SELN_CNTG_SMTN = responsedata[i * 46 + 19];
                            String SHNU_CNTG_SMTN = responsedata[i * 46 + 20];
                            String CCLD_DVSN = responsedata[i * 46 + 21];
                            String SHNU_RATE = responsedata[i * 46 + 22];
                            String PRDY_VOL_VRSS_ACML_VOL_RATE = responsedata[i * 46 + 23];
                            String OPRC_HOUR = responsedata[i * 46 + 24];
                            String OPRC_VRSS_PRPR_SIGN = responsedata[i * 46 + 25];
                            String OPRC_VRSS_PRPR = responsedata[i * 46 + 26];
                            String HGPR_HOUR = responsedata[i * 46 + 27];
                            String HGPR_VRSS_PRPR_SIGN = responsedata[i * 46 + 28];
                            String HGPR_VRSS_PRPR = responsedata[i * 46 + 29];
                            String LWPR_HOUR = responsedata[i * 46 + 30];
                            String LWPR_VRSS_PRPR_SIGN = responsedata[i * 46 + 31];
                            String LWPR_VRSS_PRPR = responsedata[i * 46 + 32];
                            String BSOP_DATE = responsedata[i * 46 + 33];
                            String NEW_MKOP_CLS_CODE = responsedata[i * 46 + 34];
                            String TRHT_YN = responsedata[i * 46 + 35];
                            String ASKP_RSQN1 = responsedata[i * 46 + 36];
                            String BIDP_RSQN1 = responsedata[i * 46 + 37];
                            String TOTAL_ASKP_RSQN = responsedata[i * 46 + 38];
                            String TOTAL_BIDP_RSQN = responsedata[i * 46 + 39];
                            String VOL_TNRT = responsedata[i * 46 + 40];
                            String PRDY_SMNS_HOUR_ACML_VOL = responsedata[i * 46 + 41];
                            String PRDY_SMNS_HOUR_ACML_VOL_RATE = responsedata[i * 46 + 42];
                            String HOUR_CLS_CODE = responsedata[i * 46 + 43];
                            String MRKT_TRTM_CLS_CODE = responsedata[i * 46 + 44];
                            String VI_STND_PRC = responsedata[i * 46 + 45];

                            // 여기에서 각 데이터를 처리하거나 저장하는 로직을 추가할 수 있습니다.
                            // 예: 데이터베이스에 저장하거나 화면에 표시하는 등의 작업
                            log.info("현재가(STCK_PRPR): {}, 체결시간(STCK_CNTG_HOUR): {}", 
                                STCK_PRPR, STCK_CNTG_HOUR);
                            
                            if (MKSC_SHRN_ISCD.equals("K1")) {
                                log.info("코스피 지수: {}, 시간: {}", STCK_PRPR, STCK_CNTG_HOUR);
                            } else if (MKSC_SHRN_ISCD.equals("Q1")) {
                                log.info("코스닥 지수: {}, 시간: {}", STCK_PRPR, STCK_CNTG_HOUR);
                            }
                            

                        }
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

    //종목 코드로 구독 요청 stockCodes 종목 코드
    public void subscribeStocks(String[] stockCodes) { //리스트형식으로 받음
        if (webSocket == null) {
            log.error("WebSocket이 연결되지 않았습니다.");
            return;
        }

        String requestTemplate = "{\"header\":{\"approval_key\":\"%s\",\"custtype\":\"P\",\"tr_type\":\"1\",\"content-type\":\"utf-8\"},\"body\":{\"input\":{\"tr_id\":\"H0STCNT0\",\"tr_key\":\"%s\"}}}";

        for (String stockCode : stockCodes) {
            String request = String.format(requestTemplate, approvalKey, stockCode);
            webSocket.send(request);
            log.info("종목 코드 {} 구독 요청 전송", stockCode);
        }
    }
}


