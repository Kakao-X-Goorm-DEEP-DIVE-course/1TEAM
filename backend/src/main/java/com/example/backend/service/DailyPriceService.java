package com.example.backend.service;

import com.example.backend.dto.DailyPriceStockNameDTO;
import com.example.backend.entity.DailyStockPrice;
import com.example.backend.entity.Stock;
import com.example.backend.repository.DailyStockPriceRepository;
import com.example.backend.repository.StockRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import lombok.extern.slf4j.Slf4j;
import com.example.backend.dto.DailyPriceDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
public class DailyPriceService {

    private final RestTemplate restTemplate;
    private DailyStockPrice dailyStockPrice;
    @Autowired
    private DailyStockPriceRepository dailyStockPriceRepository;

    @Autowired
    private StockRepository stockRepository;


    @Value("${kis.api.appKey}")
    private String appKey;

    @Value("${kis.api.appSecret}")
    private String appSecret;

    @Value("${kis.api.baseUrl}")
    private String baseUrl;

    //@Value("${kis.api.accessToken}")
    //private String token;

    private final String DAILY_PATH = "/uapi/domestic-stock/v1/quotations/inquire-daily-price";

    public DailyPriceService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<DailyPriceDTO> postDailyPrice(String stockCode, String token) throws Exception {
        String url = baseUrl + DAILY_PATH;

        // HTTP Header 설정
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("appkey", appKey);
        headers.set("appsecret", appSecret);
        headers.set("authorization", "Bearer " + token);
        headers.set("tr_id", "FHKST01010400");

        // Query Parameters 설정
        HttpEntity<String> entity = new HttpEntity<>(null, headers);
        ResponseEntity<String> response;

        try {
            response = restTemplate.exchange(
                    url + "?FID_COND_MRKT_DIV_CODE=J&FID_INPUT_ISCD=" + stockCode +
                            "&FID_PERIOD_DIV_CODE=D&FID_ORG_ADJ_PRC=0",
                    HttpMethod.GET,
                    entity,
                    String.class
            );
        } catch (Exception e) {
            log.error("API 호출 중 오류 발생", e);
            throw new RuntimeException("API 호출 실패", e);
        }

        if (response.getStatusCode() != HttpStatus.OK) {
            throw new RuntimeException("API 호출 실패: " + response.getStatusCode());
        }

        // JSON 파싱 및 DTO 변환
        ObjectMapper mapper = new ObjectMapper();
        JsonNode rootNode = mapper.readTree(response.getBody());
        JsonNode outputArray = rootNode.path("output");

        List<DailyPriceDTO> priceList = new ArrayList<>();
        for (JsonNode item : outputArray) {
            DailyPriceDTO dto = new DailyPriceDTO(
                    stockCode,                   // Stock ID
                    item.path("stck_bsop_date").asInt(), // 날짜
                    item.path("stck_hgpr").asInt(),      // 고가
                    item.path("stck_lwpr").asInt(),      // 저가
                    item.path("stck_clpr").asInt(),      // 종가
                    item.path("stck_oprc").asInt(),      // 시가
                    (float) item.path("prdy_ctrt").asDouble(),      // 등락율
                    item.path("acml_vol").asInt()        // 일거래량
            );

            priceList.add(dto);
        }

        return priceList;
    }

    @Transactional
    public void saveList(List<DailyPriceDTO> dtoList) throws Exception {

        for (DailyPriceDTO dto : dtoList) {
            DailyStockPrice existingPrice = dailyStockPriceRepository.findByStockIdAndDate(dto.getStockId(), dto.getDate());

            if (existingPrice == null) {
                dailyStockPrice = new DailyStockPrice();

                dailyStockPrice.setStockId(dto.getStockId());
                dailyStockPrice.setDate(dto.getDate());
                dailyStockPrice.setFluctuationRateDaily(dto.getChangeRate());
                dailyStockPrice.setCntgVol(dto.getVolume());
                dailyStockPrice.setOpeningPrice(dto.getOpen());
                dailyStockPrice.setClosingPrice(dto.getClose());
                dailyStockPrice.setHighPrice(dto.getHigh());
                dailyStockPrice.setLowPrice(dto.getLow());

                try {
                    dailyStockPriceRepository.save(dailyStockPrice);
                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }
            }
        }
    }

    public List<DailyPriceStockNameDTO> getDailyPrice(String stockCode) throws Exception {
        List<DailyPriceStockNameDTO> dailyPriceList = getDailyPriceFromDB(stockCode);

        return dailyPriceList;
    }

    @Transactional
    public List<DailyPriceStockNameDTO> getDailyPriceFromDB(String stockCode) throws Exception {
        List<DailyStockPrice> dailyPriceList = dailyStockPriceRepository.findByStockId(stockCode);

        List<DailyPriceStockNameDTO> dailyPriceDTOList = new ArrayList<>();

        Optional<Stock> stock = stockRepository.findByStockId(stockCode);

        if (stock.isEmpty()) {
            throw new IllegalArgumentException("StockId not found in Stock DB");
        }

        for (DailyStockPrice dailyStockPrice : dailyPriceList) {
            DailyPriceStockNameDTO dailyPriceStockNameDTO = new DailyPriceStockNameDTO();


            dailyPriceStockNameDTO.setStockName(stock.get().getStockName());
            // DailyStockPrice의 데이터를 DailyPriceDTO로 매핑
            dailyPriceStockNameDTO.setStockId(dailyStockPrice.getStockId());
            dailyPriceStockNameDTO.setDate(dailyStockPrice.getDate());
            dailyPriceStockNameDTO.setHigh(dailyStockPrice.getHighPrice());
            dailyPriceStockNameDTO.setLow(dailyStockPrice.getLowPrice());;
            dailyPriceStockNameDTO.setOpen(dailyStockPrice.getOpeningPrice());
            dailyPriceStockNameDTO.setClose(dailyStockPrice.getClosingPrice());
            dailyPriceStockNameDTO.setVolume(dailyStockPrice.getCntgVol());
            dailyPriceStockNameDTO.setChangeRate(dailyStockPrice.getFluctuationRateDaily());

            // DTO를 리스트에 추가
            dailyPriceDTOList.add(dailyPriceStockNameDTO);
        }

        return dailyPriceDTOList;

    }

}