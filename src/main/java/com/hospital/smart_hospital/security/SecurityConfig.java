package com.hospital.smart_hospital.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(auth -> auth
                        // 1. Public static assets & login page
                        .requestMatchers("/login.html", "/css/**", "/js/**", "/favicon.ico").permitAll()

                        // 2. Auth endpoints
                        .requestMatchers("/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register-patient").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register-doctor").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/auth/change-password").authenticated()
                        .requestMatchers("/api/auth/doctors/**").hasRole("ADMIN")
                        .requestMatchers("/api/auth/staff/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/auth/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/auth/register").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()

                        // 3. Room & Bed management mutations (ADMIN only)
                        .requestMatchers(HttpMethod.POST, "/api/rooms/**", "/api/beds/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/rooms/**", "/api/beds/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/rooms/**", "/api/beds/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/rooms/**", "/api/beds/**").hasRole("ADMIN")

                        // 4. Room & Bed read operations (ADMIN, DOCTOR, RECEPTIONIST)
                        .requestMatchers(HttpMethod.GET, "/api/rooms/**", "/api/beds/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")

                        // 5. Bed allocation recommendations and history (ADMIN, DOCTOR, RECEPTIONIST)
                        .requestMatchers("/api/allocations/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")

                        // 6. Patient admissions, discharges, and portal account provisioning
                        .requestMatchers(HttpMethod.POST, "/api/patients/*/create-account").hasAnyRole("ADMIN", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.POST, "/api/patients/admit", "/api/patients/*/discharge").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")

                        // 7. Patient record querying
                        .requestMatchers(HttpMethod.GET, "/api/patients/me").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/patients").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.GET, "/api/patients/by-code/**").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST")
                        .requestMatchers(HttpMethod.GET, "/api/patients/*").hasAnyRole("ADMIN", "DOCTOR", "RECEPTIONIST", "PATIENT")

                        // 8. Analytics endpoints (ADMIN and DOCTOR only - strictly forbidden for RECEPTIONIST and PATIENT)
                        .requestMatchers("/api/analytics/**").hasAnyRole("ADMIN", "DOCTOR")

                        // 9. Appointments endpoints (Authenticated users)
                        .requestMatchers("/api/appointments/**").authenticated()

                        // 10. Consultations endpoints (Authenticated users)
                        .requestMatchers("/api/consultations/**").authenticated()

                        // 10. Admin dashboard UI (ADMIN only)
                        .requestMatchers("/", "/index.html").hasRole("ADMIN")

                        // 11. All other requests require authentication
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            String accept = request.getHeader("Accept");
                            if (accept != null && accept.contains("text/html")) {
                                response.sendRedirect("/login.html");
                            } else {
                                response.sendError(HttpStatus.UNAUTHORIZED.value(), "Unauthorized");
                            }
                        })
                        .accessDeniedHandler((request, response, accessDeniedException) -> {
                            response.setStatus(HttpStatus.FORBIDDEN.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":false,\"message\":\"Access Denied: You do not have sufficient permissions.\"}");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpStatus.OK.value());
                            response.setContentType("application/json");
                            response.getWriter().write("{\"success\":true,\"message\":\"Logged out successfully.\"}");
                        })
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                );

        return http.build();
    }
}

