package com.rental.config;

import org.springframework.boot.web.servlet.server.CookieSameSiteSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CookieConfig {

    @Bean
    public CookieSameSiteSupplier cookieSameSiteSupplier() {
        // SameSite=None + Secure=false (로컬 개발용)
        return CookieSameSiteSupplier.ofNone();
    }
}