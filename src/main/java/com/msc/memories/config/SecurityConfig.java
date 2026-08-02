package com.msc.memories.config;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl; // <--- ADD THIS IMPORT
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.msc.memories.service.AuditLogService;
import com.msc.memories.service.CustomUserDetailsService;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

    private final AuditLogService auditLogService;

    // Remove SessionRegistry from constructor injection
    public SecurityConfig(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    // Define the SessionRegistry Bean here
    @Bean
    public SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

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
    SecurityFilterChain securityFilterChain(HttpSecurity http, DaoAuthenticationProvider authenticationProvider, SessionRegistry sessionRegistry)
            throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .authenticationProvider(authenticationProvider)
            .csrf(csrf -> csrf.disable())
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint((request, response, authException) -> {
                    String ip = request.getRemoteAddr();
                    String uri = request.getRequestURI();
                    auditLogService.logActivity("ANONYMOUS", "N/A", "UNAUTHORIZED_ACCESS", 
                        "Attempted unauthenticated access to: " + uri, ip);

                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("""
                    {
                      "status": "error",
                      "message": "Authentication required to access this resource"
                    }
                    """);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    String username = request.getUserPrincipal() != null ? request.getUserPrincipal().getName() : "UNKNOWN";
                    String ip = request.getRemoteAddr();
                    String uri = request.getRequestURI();
                    auditLogService.logActivity(username, username, "ACCESS_DENIED", 
                        "Forbidden access attempt to: " + uri, ip);

                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("""
                    {
                      "status": "error",
                      "message": "Access Denied: You do not have permission to access this resource"
                    }
                    """);
                })
            )
            .authorizeHttpRequests(auth -> auth
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
                .requestMatchers(
                        "/admin-dashboard",
                        "/admin-dashboard.html",
                        "/admin-dashboard/**",
                        "/admin/**",
                        "/api/admin/**"
                ).hasAnyAuthority("ADMIN")
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

                    auditLogService.logActivity(username, username, "LOGIN_SUCCESS", "User logged in via form", ip);

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
                    String passwordAttempt = request.getParameter("password");
                    String ip = request.getRemoteAddr();

                    String user = (usernameAttempt != null && !usernameAttempt.isBlank()) ? usernameAttempt : "UNKNOWN";
                    String pwd = (passwordAttempt != null && !passwordAttempt.isBlank()) ? passwordAttempt : "[EMPTY]";
                    
                    String logDetails = String.format("Failed Login Attempt - Reason: %s | Entered Password: %s", 
                        exception.getMessage(), pwd);

                    auditLogService.logActivity(user, "N/A", "LOGIN_FAILED", logDetails, ip);

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
                .maximumSessions(-1)
                .sessionRegistry(sessionRegistry) // Passed dynamically to securityFilterChain method
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .addLogoutHandler((request, response, authentication) -> {
                    String username = authentication != null ? authentication.getName() : "ANONYMOUS";
                    String ip = request.getRemoteAddr();

                    auditLogService.logActivity(username, username, "LOGOUT_SUCCESS", "User logged out successfully", ip);

                    if (authentication != null) {
                        sessionRegistry.removeSessionInformation(request.getSession().getId());
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