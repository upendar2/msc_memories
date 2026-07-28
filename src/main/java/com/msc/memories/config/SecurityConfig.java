package com.msc.memories.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.msc.memories.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService service, PasswordEncoder encoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(service);
        provider.setPasswordEncoder(encoder);
        return provider;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider)
            throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authenticationProvider(authenticationProvider)
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth

                // 1. Public Endpoints, Assets, and Error Routing
                .requestMatchers(
                        "/",
                        "/index",
                        "/login",
                        "/login.html",
                        "/forgot-password",
                        "/generate-otp",
                        "/verify-otp",
                        "/reset-password",
                        "/error",
                        "/css/**",
                        "/js/**",
                        "/images/**",
                        "/*.css",
                        "/*.js",
                        "/*.html",
                        "/logo.png"
                ).permitAll()

                // 2. Admin Dashboard Access
                .requestMatchers(
                        "/admin-dashboard",
                        "/admin-dashboard.html",
                        "/admin-dashboard/**",
                        "/admin/**",
                        "/api/admin/**"
                ).hasAnyAuthority("ADMIN", "ROLE_ADMIN")

                // 3. User Dashboard & Standard Authenticated Routes
                .requestMatchers(
                        "/user-dashboard",
                        "/user-dashboard.html",
                        "/user-dashboard/**",
                        "/gallery/**",
                        "/upload/**",
                        "/memories/**",
                        "/api/images/**",
                        "/api/user/**",
                        "/profile/**"
                ).hasAnyAuthority("USER", "ROLE_USER", "ADMIN", "ROLE_ADMIN")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")

                .successHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

                    String role = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .findFirst()
                            .orElse("USER");

                    String redirectUrl = role.toUpperCase().contains("ADMIN") ? "/admin-dashboard" : "/user-dashboard";

                    response.getWriter().write(String.format("""
                    {
                      "status": "success",
                      "message": "Login Successful",
                      "role": "%s",
                      "redirectUrl": "%s"
                    }
                    """, role, redirectUrl));
                })

                .failureHandler((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("""
                    {
                      "status": "error",
                      "message": "Invalid Registration Number/Email or Password"
                    }
                    """);
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID")
                .permitAll()
            );

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOriginPatterns(List.of(
                "http://localhost:*",
                "http://127.0.0.1:*"
        ));

        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}