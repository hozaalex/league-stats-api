package com.Alex.RiotTrackerApplication.mappers.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ChampionMapping {

    private static final Map<Integer,String> champions = new HashMap<Integer,String>();


    static{
        try{
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(new ClassPathResource("champion.json").getInputStream());
            JsonNode data = root.get("data");
            Iterator<String> fields = data.fieldNames();

            while (fields.hasNext()) {
                String champKey = fields.next();
                JsonNode champNode = data.get(champKey);
                int id = Integer.parseInt(champNode.get("key").asText());
                String name = champNode.get("name").asText();
                champions.put(id, name);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
    public static String getChampionName(Integer championId) {
        return champions.getOrDefault(championId, "Unknown");
    }
}
