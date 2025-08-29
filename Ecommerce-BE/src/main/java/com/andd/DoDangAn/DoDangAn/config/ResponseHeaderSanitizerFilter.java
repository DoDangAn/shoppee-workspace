package com.andd.DoDangAn.DoDangAn.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.lang.NonNull;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ResponseHeaderSanitizerFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(ResponseHeaderSanitizerFilter.class);

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response, @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        HttpServletResponseWrapper wrapper = new HttpServletResponseWrapper(response) {
            @Override
            public void setHeader(String name, String value) {
                super.setHeader(name, sanitizeHeaderValue(name, value));
            }

            @Override
            public void addHeader(String name, String value) {
                super.addHeader(name, sanitizeHeaderValue(name, value));
            }
        };

        filterChain.doFilter(request, wrapper);
    }

    private String sanitizeHeaderValue(String name, String value) {
        if (value == null) return null;
        // If value contains non-ASCII characters, replace them with '?' to avoid Tomcat header encoding errors
        boolean ascii = isAscii(value);
        if (!ascii) {
            logger.debug("Sanitizing header '{}' because value contains non-ASCII characters: {}", name, value);
            // For the specific X-Error-Message header, prefer dropping the header entirely to avoid leaking details
            if ("X-Error-Message".equalsIgnoreCase(name)) {
                return null; // do not set this header
            }
            // otherwise, replace non-ascii chars
            return value.replaceAll("[^\\x00-\\x7F]", "?");
        }
        return value;
    }

    private boolean isAscii(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (s.charAt(i) > 127) return false;
        }
        return true;
    }
}
