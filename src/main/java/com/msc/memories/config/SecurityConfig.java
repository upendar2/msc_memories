package com.msc.memories.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.msc.memories.service.AuditLogService;
import com.msc.memories.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {
	private final AuditLogService auditLogService;

    public SecurityConfig(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }
 // 2. Event publisher to notify SessionRegistry when a session is destroyed/invalidated
    @Bean
    public HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
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
                ).hasAnyAuthority("ADMIN")

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
                ).hasAnyAuthority("USER", "ADMIN")

                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("username")
                .passwordParameter("password")

                .successHandler((request, response, authentication) -> {
                    String username = authentication.getName();
                    String role = authentication.getAuthorities().stream()
                            .map(GrantedAuthority::getAuthority)
                            .findFirst()
                            .orElse("USER");
                    String ip = request.getRemoteAddr();

                    // Record DB Audit Log
                    auditLogService.logActivity(username, username, "LOGIN_SUCCESS", "User logged in via form", ip);

                    // 1. SUCCESSFUL LOGIN LOG
                    System.out.println("[AUTH SUCCESS] User logged in: " + username + " | Role: " + role);

                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");

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
                    String usernameAttempt = request.getParameter("username");
                    String ip = request.getRemoteAddr();
                    auditLogService.logActivity(usernameAttempt != null ? usernameAttempt : "UNKNOWN", "N/A", "LOGIN_FAILED", exception.getMessage(), ip);

                    // 2. FAILED LOGIN LOG
                    System.err.println("[AUTH FAILURE] Failed login attempt for ID/Email: " + usernameAttempt + " | Reason: " + exception.getMessage());

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
            .sessionManagement(session -> session
                    .maximumSessions(-1) // Allow multiple active sessions if needed, or set to 1
                    .sessionRegistry(sessionRegistry()) // Register SessionRegistry
                )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler((request, response, authentication) -> {
                    if (authentication != null) {
                        // 3. LOGOUT LOG
                        System.out.println("[AUTH LOGOUT] User logged out: " + authentication.getName());
                    }
                })
                .logoutSuccessUrl("/")
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