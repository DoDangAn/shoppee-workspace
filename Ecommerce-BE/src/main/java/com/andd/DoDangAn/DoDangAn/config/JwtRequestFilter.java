package com.andd.DoDangAn.DoDangAn.config;

import com.andd.DoDangAn.DoDangAn.Util.JwtUtil;
import com.andd.DoDangAn.DoDangAn.services.CustomUserDetailsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import java.io.IOException;
import java.util.Set;

@Component
public class JwtRequestFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(JwtRequestFilter.class);

    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
        "/api/user/register",
        "/api/user/login",
        "/api/home",
        "/api/product",      // chi tiết sản phẩm và biến thể
        "/api/products",     // danh sách sản phẩm/biến thể
        "/api/categories",   // danh mục
        "/api/cart",         // giỏ hàng public tạm
        "/uploads", "/assets", "/upload_dir"
    );

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private JwtUtil jwtUtil;

    private boolean isExcluded(HttpServletRequest request) {
        String servletPath = request.getServletPath();
        boolean excluded = EXCLUDED_PREFIXES.stream().anyMatch(servletPath::startsWith);
        if (excluded) {
            logger.debug("Skip JWT filter for public path: {}", servletPath);
        }
        return excluded;
    }

    private String getTokenFromHeader(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        logger.info("Checking Authorization header for request: {}", request.getRequestURI());
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            logger.info("Found JWT in Authorization header for request {}: {}", request.getRequestURI(), token);
            return token;
        }
        logger.warn("No JWT found in Authorization header for request: {}", request.getRequestURI());
        return null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain chain)
            throws ServletException, IOException {
        logger.info("Processing JWT filter for path: {}", request.getRequestURI());
        // Ensure excluded paths bypass any auth logic
    if (isExcluded(request)) {
            chain.doFilter(request, response);
            return;
        }
        try {
            String jwt = getTokenFromHeader(request);
            String username = null;

            if (jwt != null) {
                username = jwtUtil.extractUsername(jwt);
                logger.info("Extracted username from JWT: {}", username);
            } else {
                logger.warn("No JWT provided for request: {}", request.getRequestURI());
            }

            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);
                logger.info("Loaded user details for: {}", username);
                if (jwtUtil.validateToken(jwt, userDetails)) {
                    logger.info("JWT is valid for user: {}", username);
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    logger.info("SecurityContextHolder set for user: {}", username);
                } else {
                    logger.warn("JWT token is invalid for user: {}", username);
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.getWriter().write("{\"error\": \"Token không hợp lệ, vui lòng đăng nhập lại!\"}");
                    return;
                }
            } else if (SecurityContextHolder.getContext().getAuthentication() == null) {
                logger.warn("Unauthorized access attempt. Path={}, Headers Authorization={}, Cookies={}",
                        request.getRequestURI(), request.getHeader("Authorization"), request.getHeader("Cookie"));
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\": \"Vui lòng đăng nhập để tiếp tục!\"}");
                return;
            }
            chain.doFilter(request, response);
        } catch (Exception e) {
            logger.error("JWT authentication error for path {}: {}", request.getRequestURI(), e.getMessage());
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write("{\"error\": \"Lỗi xác thực JWT: " + e.getMessage() + "\"}");
        }
    }
}