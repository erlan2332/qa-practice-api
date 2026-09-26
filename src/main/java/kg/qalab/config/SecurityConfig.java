package kg.qalab.config;

import kg.qalab.security.BearerAuthenticationFilter;
import kg.qalab.security.PayloadSizeFilter;
import kg.qalab.security.RateLimitFilter;
import kg.qalab.security.RequestIdFilter;
import kg.qalab.security.SecurityErrorWriter;
import kg.qalab.service.TokenService;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    org.springframework.security.core.userdetails.UserDetailsService
    noDefaultPasswordUser() {
        return username -> {
            throw new org.springframework.security.core.userdetails
                .UsernameNotFoundException(
                    "Bearer authentication only"
                );
        };
    }

    @Bean
    FilterRegistrationBean<RequestIdFilter>
    requestIdFilterRegistration() {
        FilterRegistrationBean<RequestIdFilter> registration =
            new FilterRegistrationBean<>(
                new RequestIdFilter()
            );

        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RateLimitFilter>
    rateLimitFilterRegistration(
        SecurityErrorWriter errorWriter
    ) {
        FilterRegistrationBean<RateLimitFilter> registration =
            new FilterRegistrationBean<>(
                new RateLimitFilter(errorWriter)
            );

        registration.setOrder(
            Ordered.HIGHEST_PRECEDENCE + 1
        );
        return registration;
    }

    @Bean
    FilterRegistrationBean<PayloadSizeFilter>
    payloadSizeFilterRegistration(
        SecurityErrorWriter errorWriter
    ) {
        FilterRegistrationBean<PayloadSizeFilter> registration =
            new FilterRegistrationBean<>(
                new PayloadSizeFilter(errorWriter)
            );

        registration.setOrder(
            Ordered.HIGHEST_PRECEDENCE + 2
        );
        return registration;
    }

    @Bean
    SecurityFilterChain security(
        HttpSecurity http,
        TokenService tokenService,
        SecurityErrorWriter errorWriter
    ) throws Exception {
        BearerAuthenticationFilter bearerFilter =
            new BearerAuthenticationFilter(tokenService);

        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session ->
                session.sessionCreationPolicy(
                    SessionCreationPolicy.STATELESS
                )
            )
            .requestCache(cache -> cache.disable())
            .httpBasic(basic -> basic.disable())
            .formLogin(form -> form.disable())
            .logout(logout -> logout.disable())
            .headers(headers ->
                headers.contentSecurityPolicy(csp ->
                    csp.policyDirectives(
                        "default-src 'self'; " +
                        "script-src 'self'; " +
                        "style-src 'self'; " +
                        "img-src 'self' data:; " +
                        "connect-src 'self'; " +
                        "frame-ancestors 'none'; " +
                        "base-uri 'none'; " +
                        "form-action 'self'"
                    )
                )
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/app.js",
                    "/style.css",
                    "/favicon.svg",
                    "/openapi.yaml",
                    "/postman/**",
                    "/guide.html",
                    "/swagger-ui.html",
                    "/swagger-ui/**",
                    "/v3/api-docs",
                    "/v3/api-docs.yaml",
                    "/v3/api-docs/**",
                    "/webjars/**",
                    "/error"
                )
                .permitAll()
                .requestMatchers(
                    "/api/health",
                    "/api/catalog",
                    "/api/auth/register",
                    "/api/auth/login"
                )
                .permitAll()
                .requestMatchers("/api/admin/**")
                .hasRole("ADMIN")
                .anyRequest()
                .authenticated()
            )
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint(
                    (request, response, exception) -> {
                        response.setHeader(
                            "WWW-Authenticate",
                            "Bearer"
                        );

                        errorWriter.write(
                            request,
                            response,
                            401,
                            "UNAUTHORIZED",
                            "Добавьте действующий Authorization: Bearer <token>"
                        );
                    }
                )
                .accessDeniedHandler(
                    (request, response, exception) ->
                        errorWriter.write(
                            request,
                            response,
                            403,
                            "FORBIDDEN",
                            "Вы вошли, но роль USER не даёт доступ к этому ресурсу"
                        )
                )
            )
            .addFilterBefore(
                bearerFilter,
                UsernamePasswordAuthenticationFilter.class
            )
            .build();
    }
}
