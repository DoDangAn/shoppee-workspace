package com.andd.DoDangAn.DoDangAn.config;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtRequestFilter jwtRequestFilter;

    @Autowired
    public SecurityConfig(JwtRequestFilter jwtRequestFilter) {
        this.jwtRequestFilter = jwtRequestFilter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/user/register", "/api/user/login").permitAll()
                        .requestMatchers(HttpMethod.GET,
                                "/api/user/login",
                                "/api/user/logout",
                                "/api/categories/**",
                                "/api/home",
                                "/api/product/**",
                                "/api/products",
                                "/api/products/page").permitAll()
                        // Allow anonymous cart operations (guest cart) all HTTP methods
                        .requestMatchers("/api/cart/**").permitAll()
                        .requestMatchers("/assets/**", "/uploads/**", "/upload_dir/**", "/user/login").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers(
                                "/api/order/**").authenticated()
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\": \"Vui lòng đăng nhập để tiếp tục!\"}");
                        })
                )
                .logout(logout -> logout
                        .logoutUrl("/api/user/logout")
                        .logoutSuccessHandler((request, response, authentication) -> {
                            response.setStatus(HttpServletResponse.SC_OK);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\": \"Đăng xuất thành công!\"}");
                        })
                        .clearAuthentication(true)
                )
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration cfg = new CorsConfiguration();
                cfg.addAllowedOriginPattern("http://localhost:4200");
                cfg.addAllowedOriginPattern("http://127.0.0.1:4200");
                cfg.setAllowCredentials(true);
                cfg.addAllowedHeader("*");
                cfg.addAllowedMethod("*");
                cfg.addExposedHeader("Authorization");
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/api/**", cfg);
                return source;
        }
}