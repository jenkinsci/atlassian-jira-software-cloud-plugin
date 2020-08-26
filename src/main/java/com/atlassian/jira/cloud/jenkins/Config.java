package com.atlassian.jira.cloud.jenkins;

import com.google.common.collect.ImmutableMap;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.ratelimiter.RateLimiterRegistry;

import java.time.Duration;

public interface Config {

    String ATLASSIAN_API_URL = "https://api.atlassian.com";
    String ATLASSIAN_RATE_LIMITER_CONFIG = "atl";

    RateLimiterConfig DEFAULT_RATE_LIMITER_CONFIG =
            RateLimiterConfig.custom()
                    .limitRefreshPeriod(Duration.ofMinutes(5))
                    .limitForPeriod(5000)
                    .build();

    RateLimiterRegistry RATE_LIMITER_REGISTRY =
            RateLimiterRegistry.of(
                    ImmutableMap.of(ATLASSIAN_RATE_LIMITER_CONFIG, DEFAULT_RATE_LIMITER_CONFIG));
}
