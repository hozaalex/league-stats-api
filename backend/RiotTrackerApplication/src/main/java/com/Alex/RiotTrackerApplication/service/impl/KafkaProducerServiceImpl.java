package com.Alex.RiotTrackerApplication.service.impl;

import com.Alex.RiotTrackerApplication.model.dto.SummonerKafkaRequest;
import com.Alex.RiotTrackerApplication.service.KafkaProducerService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

@Service
public class KafkaProducerServiceImpl implements KafkaProducerService {

    private static final Logger log = Logger.getLogger(KafkaProducerServiceImpl.class.getName());


    private KafkaTemplate<String, Object> kafkaTemplate;
    public KafkaProducerServiceImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Value("${riot.kafka.topics.summoner-requests}")
    private String summonerRequestsTopic;


    public String sendSummonerRequest(String gameName, String tagLine, String region, String userIp) {
        String requestId = UUID.randomUUID().toString();

        SummonerKafkaRequest request = SummonerKafkaRequest.builder()
                .requestId(requestId)
                .gameName(gameName)
                .tagLine(tagLine)
                .region(region)
                .userIp(userIp)
                .timestamp(System.currentTimeMillis())
                .build();


        String messageKey = gameName + "#" + tagLine + "#" + region;

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(summonerRequestsTopic, messageKey, request);


        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info(String.format(
                        "Sent summoner request [%s] for %s#%s to topic [%s] partition [%d] offset [%d]",
                        requestId,
                        gameName,
                        tagLine,
                        summonerRequestsTopic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset()
                ));
            } else {
                log.severe(String.format(
                        "Failed to send summoner request [%s]: %s",
                        requestId,
                        ex.getMessage()
                ));
            }
        });

        return requestId;
    }

    public void sendToDeadLetterQueue(String dlqTopic, Object originalMessage, String errorMessage) {
        try {
            Map<String, Object> dlqMessage = new HashMap<>();
            dlqMessage.put("originalMessage", originalMessage);
            dlqMessage.put("errorMessage", errorMessage);
            dlqMessage.put("failedAt", System.currentTimeMillis());
            dlqMessage.put("timestamp", java.time.Instant.now().toString());

            kafkaTemplate.send(dlqTopic, dlqMessage);

            log.warning(String.format(
                    "Sent failed message to DLQ [%s]: %s",
                    dlqTopic,
                    errorMessage
            ));
        } catch (Exception e) {
            log.severe(String.format(
                    "Failed to send message to DLQ [%s]: %s",
                    dlqTopic,
                    e.getMessage()
            ));

        }
    }
}
