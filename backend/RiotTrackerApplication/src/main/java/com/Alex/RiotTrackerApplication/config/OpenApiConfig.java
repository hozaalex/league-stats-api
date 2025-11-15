package com.Alex.RiotTrackerApplication.config;


import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {


    @Bean
    public OpenAPI leagueTrackerOpenAPI() {
        Server devServer = new Server();
        devServer.setUrl("http://localhost:8080");
        devServer.setDescription("Development Server");

        Contact contact = new Contact();
        contact.setName("Ioan Alexandru Hoza");
        contact.setUrl("https://github.com/hozaalex");

        License license = new License()
                .name("Apache License 2.0")
                .url("https://opensource.org/licenses/Apache-2.0");

        Info info = new Info()
                .title("League of Legends Tracker API")
                .version("1.0.0")
                .description("RESTful API for tracking League of Legends player statistics, " +
                        "match history, and ranked performance. Built with Spring WebFlux " +
                        "for reactive, non-blocking data aggregation from Riot Games API.")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(devServer));
    }

}

