package com.Alex.RiotTrackerApplication.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${riot.kafka.topics.summoner-requests:summoner-requests}")
    private String summonerRequestsTopic;

    @Value("${riot.kafka.topics.match-requests:match-requests}")
    private String matchRequestsTopic;

    @Value("${riot.kafka.topics.ranked-stats-requests:ranked-stats-requests}")
    private String rankedStatsRequestsTopic;


    @Bean
    public NewTopic summonerRequests(){

        return TopicBuilder.name(summonerRequestsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic matchRequestsTopic() {
        return TopicBuilder.name(matchRequestsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean

    public NewTopic rankedStatsRequestsTopic() {
        return TopicBuilder.name(rankedStatsRequestsTopic)
                .partitions(3)
                .replicas(1)
                .build();
    }


}
