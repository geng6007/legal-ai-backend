package com.hs.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

/**
 * CORS 跨域配置 用来解决浏览器同源策略限制
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.addAllowedOriginPattern("*");   // 允许所有来源（前端域名）访问
        config.addAllowedHeader("*");          // 允许所有请求头（如 Authorization、Content-Type）
        config.addAllowedMethod("*");          // 允许所有 HTTP 方法（GET/POST/PUT/DELETE 等）
        config.setAllowCredentials(true);      // 允许携带 Cookie 等凭证

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);  // 对所有接口路径生效
        return new CorsFilter(source);
    }
}