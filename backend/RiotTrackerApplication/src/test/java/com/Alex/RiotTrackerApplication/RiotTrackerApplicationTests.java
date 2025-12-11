package com.Alex.RiotTrackerApplication;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@TestPropertySource(properties = {
		"spring.kafka.bootstrap-servers=localhost:9092",
		"spring.data.redis.host=localhost",
		"spring.data.redis.port=6379"
})
class RiotTrackerApplicationTests {

	@Test
	void contextLoads() {
	}

}
