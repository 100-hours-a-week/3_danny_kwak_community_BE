package com.ktb.community.config;

import com.ktb.community.purejwt.PureJwtAuthenticationFilter;
import jakarta.servlet.Filter;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("pure_jwt")
public class PureJwtWebFilterConfig {
    private final PureJwtAuthenticationFilter pureJwtAuthenticationFilter;

    public PureJwtWebFilterConfig(PureJwtAuthenticationFilter pureJwtAuthenticationFilter) {
        this.pureJwtAuthenticationFilter = pureJwtAuthenticationFilter;
    }

    @Bean
    public FilterRegistrationBean<Filter> jwtFilter() {
        FilterRegistrationBean<Filter> filterRegistrationBean = new FilterRegistrationBean<>();
        filterRegistrationBean.setFilter(pureJwtAuthenticationFilter);
        filterRegistrationBean.addUrlPatterns("/*");
        filterRegistrationBean.setOrder(1);

        return filterRegistrationBean;
    }
}
