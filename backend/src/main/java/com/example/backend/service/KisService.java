package com.example.backend.service;

import com.example.backend.dto.RankingDTO;
import com.example.backend.entity.DailyStockPrice;
import com.example.backend.entity.Popular;
import com.example.backend.entity.Stock;
import com.example.backend.repository.DailyStockPriceRepository;
import com.example.backend.repository.PopularRepository;
import com.example.backend.repository.StockRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.backend.dto.ResponseOutputDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import com.example.backend.service.KisTokenService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

//@Slf4j
@Service
public class KisService {
    @Value("${kis.api.appKey}")
    private String appkey;

    @Value("${kis.api.appSecret}")
    private String appSecret;

    //@Value("${kis.api.accessToken}")
    //private String accessToken;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Autowired
    private KafkaProducerService kafkaProducerService;
    @Autowired
    private PopularRepository popularRepository;
    @Autowired
    private StockRepository stockRepository;
    @Autowired
    private DailyStockPriceRepository dailyStockPriceRepository;

    @Autowired
    public KisService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.baseUrl("https://openapi.koreainvestment.com:9443").build();
        this.objectMapper =objectMapper;
    }
    private HttpHeaders createVolumeRankHttpHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        headers.set("appkey", appkey);
        headers.set("appSecret", appSecret);
        headers.set("tr_id", "FHPST01710000");
        headers.set("custtype", "P");
        return headers;
    }

    private Mono<List<ResponseOutputDTO>> parseFVolumeRank(String response) {
        try {
            List<ResponseOutputDTO> responseDataList = new ArrayList<>();
            JsonNode rootNode = objectMapper.readTree(response);
            JsonNode outputNode = rootNode.get("output");
            if (outputNode != null) {
                for (JsonNode node : outputNode) {
                    ResponseOutputDTO responseData = new ResponseOutputDTO();
                    responseData.setHtsKorIsnm(node.get("hts_kor_isnm").asText());
                    responseData.setMkscShrnIscd(node.get("mksc_shrn_iscd").asText());
                    responseData.setDataRank(node.get("data_rank").asInt());
                    responseData.setStckPrpr(node.get("stck_prpr").asText());
                    responseData.setPrdyVrssSign(node.get("prdy_vrss_sign").asText());
                    responseData.setPrdyVrss(node.get("prdy_vrss").asText());
                    responseData.setPrdyCtrt(node.get("prdy_ctrt").asText());
                    responseData.setAcmlVol(node.get("acml_vol").asText());
                    responseData.setPrdyVol(node.get("prdy_vol").asText());
                    responseData.setLstnStcn(node.get("lstn_stcn").asText());
                    responseData.setAvrgVol(node.get("avrg_vol").asText());
                    // responseData.setNBefrClprVrssPrprRate(node.get("n_befr_clpr_vrss_prpr_rate").asText());
                    // responseData.setVolInrt(node.get("vol_inrt").asText());
                    // responseData.setVolTnrt(node.get("vol_tnrt").asText());
                    // responseData.setNdayVolTnrt(node.get("nday_vol_tnrt").asText());
                    // responseData.setAvrgTrPbmn(node.get("avrg_tr_pbmn").asText());
                    // responseData.setTrPbmnTnrt(node.get("tr_pbmn_tnrt").asText());
                    // responseData.setNdayTrPbmnTnrt(node.get("nday_tr_pbmn_tnrt").asText());
                    // responseData.setAcmlTrPbmn(node.get("acml_tr_pbmn").asText());
                    responseDataList.add(responseData);
                }
            }
            return Mono.just(responseDataList);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
    public Mono<List<ResponseOutputDTO>> getVolumeRank(String accessToken) {
        HttpHeaders headers = createVolumeRankHttpHeaders(accessToken);

        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path("/uapi/domestic-stock/v1/quotations/volume-rank")
                        .queryParam("FID_COND_MRKT_DIV_CODE", "J")
                        .queryParam("FID_COND_SCR_DIV_CODE", "20171")
                        .queryParam("FID_INPUT_ISCD", "0002")
                        .queryParam("FID_DIV_CLS_CODE", "0")
                        .queryParam("FID_BLNG_CLS_CODE", "0")
                        .queryParam("FID_TRGT_CLS_CODE", "111111111")
                        .queryParam("FID_TRGT_EXLS_CLS_CODE", "000000")
                        .queryParam("FID_INPUT_PRICE_1", "0")
                        .queryParam("FID_INPUT_PRICE_2", "0")
                        .queryParam("FID_VOL_CNT", "0")
                        .queryParam("FID_INPUT_DATE_1", "0")
                        .build())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .retrieve()
                .bodyToMono(String.class)
                .flatMap(response -> parseFVolumeRank(response));

    }

    @Autowired
    private KisTokenService kisTokenService;

    @Scheduled(fixedRate = 10000)
    public void fetchVolumeRankPeriodically() {
        try {
            String accessToken = kisTokenService.getCachedAccessToken();
            getVolumeRank(accessToken).subscribe(response -> {
              response.forEach(dto -> {
                  try {
                      String json = objectMapper.writeValueAsString(dto);
                      kafkaProducerService.sendMessage("volume-rank-topic", json);
                  } catch (Exception e) {
                      System.err.println("Error serializing data: " + e.getMessage());
                  }
              });
          }, error -> {
              System.err.println("Error fetching volume rank: " + error.getMessage());
          });
        } catch (Exception e) {
            System.err.println("Error getting access token: " + e.getMessage());
        }
    }


    public List<RankingDTO> getPopular10() {
        List<RankingDTO> rankingDTOList = new ArrayList<>();

        List<Popular> popularList = popularRepository.findByRankingBetween(1,10);

        for (Popular popular : popularList) {

            Optional<Stock> stock = stockRepository.findByStockId(popular.getStockId());

            if (stock.isEmpty()) {
                throw new RuntimeException("Stock not found");
            }

            List<DailyStockPrice> dailyStockPrices = dailyStockPriceRepository.findByStockId(popular.getStockId());
            DailyStockPrice dailyStockPrice
             = dailyStockPrices.stream().max(Comparator.comparing(DailyStockPrice::getDate)).orElse(null);

            if (dailyStockPrice == null) {
                throw new RuntimeException("DailyStockPrice not found");
            }

            RankingDTO rankingDTO = new RankingDTO(popular.getRanking(), stock.get().getStockName(), popular.getStockId(), dailyStockPrice.getFluctuationRateDaily(), dailyStockPrice.getCntgVol());
            rankingDTOList.add(rankingDTO);
        }

        return rankingDTOList;
    }

    public List<String> getDailyDataFromAPI() {
        List<String> dataList = new ArrayList<>();
        List<Popular> popularList = popularRepository.findByRankingBetween(1,10);

        for (Popular popular : popularList) {
            String data = popular.getStockId();
            dataList.add(data);
        }
        return dataList;
    }


}
