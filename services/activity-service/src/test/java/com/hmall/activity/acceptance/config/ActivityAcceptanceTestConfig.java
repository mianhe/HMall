package com.hmall.activity.acceptance.config;

import com.hmall.activity.acceptance.ActivityConsumeStepDefinitions;
import com.hmall.activity.acceptance.ActivityQueryStepDefinitions;
import com.hmall.activity.acceptance.ActivitySmokeStepDefinitions;
import com.hmall.activity.acceptance.ActivityStatsStepDefinitions;
import com.hmall.activity.application.ActivityApplicationService;
import com.hmall.activity.domain.ActivityRepository;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class ActivityAcceptanceTestConfig {

    @Bean
    @Primary
    public ActivitySmokeStepDefinitions activitySmokeStepDefinitions(TestRestTemplate restTemplate) {
        return new ActivitySmokeStepDefinitions(restTemplate);
    }

    @Bean
    @Primary
    public ActivityConsumeStepDefinitions activityConsumeStepDefinitions(
            TestRestTemplate restTemplate,
            ActivityApplicationService applicationService,
            ActivityRepository repository) {
        return new ActivityConsumeStepDefinitions(restTemplate, applicationService, repository);
    }

    @Bean
    @Primary
    public ActivityQueryStepDefinitions activityQueryStepDefinitions(
            TestRestTemplate restTemplate,
            ActivityApplicationService applicationService,
            ActivityRepository repository) {
        return new ActivityQueryStepDefinitions(restTemplate, applicationService, repository);
    }

    @Bean
    @Primary
    public ActivityStatsStepDefinitions activityStatsStepDefinitions(
            TestRestTemplate restTemplate,
            ActivityApplicationService applicationService) {
        return new ActivityStatsStepDefinitions(restTemplate, applicationService);
    }
}
