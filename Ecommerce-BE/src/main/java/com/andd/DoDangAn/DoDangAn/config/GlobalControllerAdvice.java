package com.andd.DoDangAn.DoDangAn.config;

import com.andd.DoDangAn.DoDangAn.models.Category;
import com.andd.DoDangAn.DoDangAn.repository.jpa.CategoryRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.time.Instant;
import java.util.List;

@ControllerAdvice
public class GlobalControllerAdvice {

    @Autowired
    private CategoryRepository categoryRepository;

    // Simple local cache to avoid executing SELECT categories on every request
    private volatile List<Category> cachedCategories;
    private volatile Instant lastLoaded = Instant.EPOCH;
    private static final long CACHE_TTL_SECONDS = 300; // 5 minutes

    @ModelAttribute
    public void addCategoriesToModel(org.springframework.ui.Model model, HttpServletRequest request) {
        String uri = request.getRequestURI();
        String accept = request.getHeader("Accept");
        // Skip adding categories for pure JSON API endpoints to eliminate duplicate queries
        if (uri != null && uri.startsWith("/api/") && (accept == null || accept.contains("application/json"))) {
            return;
        }
        Instant now = Instant.now();
        if (cachedCategories == null || now.isAfter(lastLoaded.plusSeconds(CACHE_TTL_SECONDS))) {
            cachedCategories = categoryRepository.findAll();
            lastLoaded = now;
        }
        model.addAttribute("category", cachedCategories);
    }
}