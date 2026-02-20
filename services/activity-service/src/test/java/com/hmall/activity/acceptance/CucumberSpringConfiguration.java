package com.hmall.activity.acceptance;

import com.hmall.activity.ActivityApplication;
import com.hmall.activity.acceptance.config.ActivityAcceptanceTestConfig;
import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.util.DefaultUriBuilderFactory;

@CucumberContextConfiguration
@SpringBootTest(
    classes = { ActivityApplication.class, ActivityAcceptanceTestConfig.class },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
public class CucumberSpringConfiguration {

    @Bean
    public TestRestTemplate testRestTemplate(@LocalServerPort int port) {
        TestRestTemplate t = new TestRestTemplate();
        t.getRestTemplate().setUriTemplateHandler(new DefaultUriBuilderFactory("http://localhost:" + port));
        return t;
    }
}
