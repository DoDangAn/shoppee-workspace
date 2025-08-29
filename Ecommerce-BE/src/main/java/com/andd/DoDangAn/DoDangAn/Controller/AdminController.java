package com.andd.DoDangAn.DoDangAn.Controller;

import com.andd.DoDangAn.DoDangAn.models.*;
import com.andd.DoDangAn.DoDangAn.models.cache.PendingCacheUpdate;
import com.andd.DoDangAn.DoDangAn.models.cache.PendingNotification;
import com.andd.DoDangAn.DoDangAn.repository.Cache.PendingCacheUpdateRepository;
import com.andd.DoDangAn.DoDangAn.repository.Cache.PendingNotificationRepository;
import com.andd.DoDangAn.DoDangAn.repository.jpa.*;
import com.andd.DoDangAn.DoDangAn.services.Cloudinary.CloudinaryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import java.util.function.Function;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@RestController
@RequestMapping(path = "/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);
    private static final long MAX_IMAGE_SIZE = 10 * 1024 * 1024; // 10MB
    private static final long MAX_VIDEO_SIZE = 100 * 1024 * 1024; // 100MB

    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private CategoryRepository categoryRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductVariantRepository productVariantRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private CountryRepository countryRepository;
    @Autowired
    private CommentRepository commentRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PendingCacheUpdateRepository pendingCacheUpdateRepository;
    @Autowired
    private PendingNotificationRepository pendingNotificationRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderInfoRepository orderInfoRepository;


    private static class ErrorResponse {
        private final String error;
        private final String details;

        public ErrorResponse(String error, String details) {
            this.error = error;
            this.details = details;
        }

        public String getError() {
            return error;
        }

        public String getDetails() {
            return details;
        }
    }

    // Map Category entity to lightweight DTO to avoid serializing lazy collections
    private Map<String, Object> toCategoryDto(Category c) {
        Map<String, Object> m = new HashMap<>();
        m.put("categoryID", c.getCategoryID());
        m.put("categoryName", c.getCategoryName());
        m.put("description", c.getDescription());
        return m;
    }

    // Map Product entity to lightweight DTO to avoid serializing lazy relations
    private Map<String, Object> toProductDto(Product p) {
        Map<String, Object> m = new HashMap<>();
        m.put("productID", p.getId());
        m.put("productName", p.getProductName());
        m.put("price", p.getPrice());
        m.put("imageUrl", p.getImageUrl());
        m.put("viewCount", p.getViewCount());
        m.put("rate", p.getRate());
        m.put("releaseDate", p.getReleaseDate());
        // do not include collections or relations to avoid LazyInitializationException
        return m;
    }

    @GetMapping("/order")
    public Mono<ResponseEntity<? extends List<?>>> getOrders() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (!isAuthenticated(auth)) {
            return Mono.just(createErrorResponse(HttpStatus.UNAUTHORIZED,
                    "Không được phép", "Người dùng chưa được xác thực"));
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(grantedAuthority -> grantedAuthority.getAuthority().equals("ROLE_ADMIN"));
        if (!isAdmin) {
            return Mono.just(createErrorResponse(HttpStatus.FORBIDDEN,
                    "Không được phép", "Chỉ admin mới có quyền truy cập"));
        }

        return Mono.fromCallable(() -> userRepository.findByUsername(auth.getName()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalUser -> {
                    if (optionalUser.isEmpty()) {
                        return Mono.just(createErrorResponse(HttpStatus.NOT_FOUND,
                                "Không tìm thấy người dùng", "Username: " + auth.getName()));
                    }

                    User user = optionalUser.get();
                    return Mono.fromCallable(() -> orderRepository.findByUserId(user.getId()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(orders -> {
                                if (orders.isEmpty()) {
                                    return ResponseEntity.ok(Collections.emptyList());
                                }
                                return ResponseEntity.ok().body(orders);
                            })
                            .switchIfEmpty(Mono.just(ResponseEntity.ok(Collections.emptyList())));
                })
                .onErrorResume(e -> {
                    logger.error("Lỗi khi lấy đơn hàng cho người dùng {}: {}", auth.getName(), e.getMessage(), e);
                    return Mono.just(createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Lỗi máy chủ", "Lỗi khi lấy danh sách đơn hàng: " + e.getMessage()));
                });
    }



    @GetMapping("/home")
    @Cacheable(value = "adminDashboard", key = "#field.orElse('viewCount') + '-' + #field2.orElse('rate') + '-' + #field3.orElse('releaseDate')")
    public ResponseEntity<?> home(@RequestParam("field") Optional<String> field,
                                  @RequestParam("field2") Optional<String> field2,
                                  @RequestParam("field3") Optional<String> field3) {
        logger.debug("Starting home endpoint in AdminController");
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            Map<String, Object> response = new HashMap<>();
            response.put("loggedInUser", auth.getPrincipal());

            Sort sort = Sort.by(Sort.Direction.DESC, field.orElse("viewCount"));
            // default to 'rate' which exists on Product
            Sort sort2 = Sort.by(Sort.Direction.DESC, field2.orElse("rate"));
            Sort sort3 = Sort.by(Sort.Direction.DESC, field3.orElse("releaseDate"));

            Map<String, Object> data = new HashMap<>();
            data.put("totalProducts", productRepository.count());
            data.put("totalCategories", categoryRepository.count());
            data.put("totalUsers", userRepository.count());
            response.put("data", data);

            // Map categories to lightweight DTOs to avoid lazy-init during JSON serialization
            List<Map<String, Object>> categoryDtos = categoryRepository.findAll().stream().map(c -> {
                Map<String, Object> m = new HashMap<>();
                m.put("categoryID", c.getCategoryID());
                m.put("categoryName", c.getCategoryName());
                m.put("description", c.getDescription());
                return m;
            }).collect(Collectors.toList());

            // Map products to lightweight DTOs (avoid relations that may be lazily loaded)
        List<Map<String, Object>> productDtos = (List<Map<String, Object>>) StreamSupport.stream(productRepository.findAll(sort).spliterator(), false)
            .map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("productID", p.getId());
                m.put("productName", p.getProductName());
                m.put("price", p.getPrice());
                m.put("imageUrl", p.getImageUrl());
                m.put("viewCount", p.getViewCount());
                m.put("rate", p.getRate());
                m.put("releaseDate", p.getReleaseDate());
                return m;
                    }).collect(Collectors.toList());

            response.put("categories", categoryDtos);
            response.put("products", productDtos);
            response.put("prices", productDtos); // reuse lightweight list for price view
            // For sorted-by-rate view
        List<Map<String, Object>> scoreDtos = (List<Map<String, Object>>) StreamSupport.stream(productRepository.findAll(sort2).spliterator(), false)
            .map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("productID", p.getId());
                m.put("productName", p.getProductName());
                m.put("rate", p.getRate());
                return m;
                    }).collect(Collectors.toList());
            response.put("scores", scoreDtos);
            response.put("releases", productDtos); // reuse lightweight list for releases

            logger.debug("Successfully loaded admin dashboard data");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error in AdminController.home: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi tải dashboard admin: " + e.getMessage());
        }
    }

    @GetMapping("/product/view/{productID}")
    @Cacheable(value = "product", key = "#productID")
    public ResponseEntity<?> showProduct(@PathVariable("productID") String productID) {
        try {
            if (productID == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "ID sản phẩm không được để trống");
            }

            Optional<Product> productOpt = productRepository.findById(productID);
            if (productOpt.isPresent()) {
                Product product = productOpt.get();
                product.setViewCount(product.getViewCount() != null ? product.getViewCount() + 1 : 1);
                productRepository.save(product);
                return ResponseEntity.ok(toProductDto(product));
            }
            return createErrorResponse(HttpStatus.NOT_FOUND,
                    "Không tìm thấy", "Sản phẩm không tồn tại");
        } catch (Exception e) {
            logger.error("Error fetching product {}: {}", productID, e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy sản phẩm: " + e.getMessage());
        }
    }

    @PostMapping(value = "/products", consumes = {"multipart/form-data"})
    @Transactional
    @CachePut(value = "products", key = "#result.body.id")
    public ResponseEntity<?> addProduct(
            @Valid @ModelAttribute Product product,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "categoryIds", required = true) List<String> categoryIds,
            @RequestParam(value = "countryId", required = true) String countryId) {
        logger.info("POST /api/admin/products - Processing new product request");
        try {
            if (bindingResult.hasErrors()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Lỗi xác thực", getValidationErrors(bindingResult));
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            if (productRepository.existsByProductName(product.getProductName())) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "Sản phẩm với tên này đã tồn tại");
            }

            Set<Category> categories = new HashSet<>();
            for (String categoryId : categoryIds) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("ID danh mục không hợp lệ: " + categoryId));
                categories.add(category);
            }
            product.setCategories(categories);

            Country country = countryRepository.findByCountryId(countryId)
                    .orElseThrow(() -> new IllegalArgumentException("ID quốc gia không hợp lệ: " + countryId));
            product.setCountry(country);

            if (imageFile != null && !imageFile.isEmpty()) {
                if (!isValidImageFile(imageFile)) {
                    return createErrorResponse(HttpStatus.BAD_REQUEST,
                            "Yêu cầu không hợp lệ", "Định dạng ảnh không hợp lệ hoặc ảnh quá lớn");
                }
                @SuppressWarnings("unchecked") Map<String,Object> uploadResult = cloudinaryService.uploadImage(imageFile);
                product.setImageUrl((String) uploadResult.get("secure_url"));
            } else {
                product.setImageUrl("/uploads/default.png");
            }

            product.setViewCount(product.getViewCount() != null ? product.getViewCount() : 0);
            product.setReleaseDate(product.getReleaseDate() != null ? product.getReleaseDate() : LocalDateTime.now());

            Product savedProduct = productRepository.save(product);
            savePendingCacheUpdate("product:" + savedProduct.getId(), "add", Collections.singletonMap("product", savedProduct));
            savePendingNotification("product:added", Collections.singletonMap("productId", savedProduct.getId()));

            logger.info("Successfully added product: {}", savedProduct.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProduct);
        } catch (IllegalArgumentException e) {
            logger.error("Error adding product: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    "Yêu cầu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            logger.error("Server error adding product: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi thêm sản phẩm: " + e.getMessage());
        }
    }

    @PostMapping("/category")
    @Transactional
    @CacheEvict(value = {"category", "adminDashboard"}, allEntries = true)
    public ResponseEntity<?> addCategory(@RequestBody Map<String, Object> body) {
        try {
            logger.info("POST /api/admin/category payload={}", body);
            Object nameObj = body.get("categoryName");
            if (nameObj == null) nameObj = body.get("name");
            String categoryName = nameObj != null ? String.valueOf(nameObj).trim() : null;
            if (categoryName == null || categoryName.isEmpty()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Lỗi xác thực", "categoryName là bắt buộc");
            }

            if (categoryRepository.existsByCategoryName(categoryName)) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "Thể loại đã tồn tại");
            }

            Category category = new Category();
            Object idObj = body.get("categoryID");
            if (idObj == null) idObj = body.get("id");
            if (idObj != null && String.valueOf(idObj).trim().length() > 0) {
                category.setCategoryID(String.valueOf(idObj).trim());
            }
            category.setCategoryName(categoryName);
            category.setDescription(String.valueOf(body.getOrDefault("description", "")));

            Category savedCategory = categoryRepository.save(category);
            savePendingCacheUpdate("category:" + savedCategory.getCategoryID(), "add",
                    Collections.singletonMap("category", savedCategory));
            // return lightweight DTO to avoid serializing lazy collections
            Map<String, Object> dto = toCategoryDto(savedCategory);
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (Exception e) {
            logger.error("Error adding category: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi thêm thể loại: " + e.getMessage());
        }
    }

    @PutMapping("/category/{categoryID}")
    @Transactional
    @CacheEvict(value = {"category", "product", "adminDashboard"}, allEntries = true)
    public ResponseEntity<?> updateCategory(@PathVariable String categoryID,
                                            @Valid @RequestBody Category category,
                                            BindingResult bindingResult,
                                            @RequestParam(value = "newCategoryId", required = false) String newCategoryId) {
        if (bindingResult.hasErrors()) {
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    "Lỗi xác thực", getValidationErrors(bindingResult));
        }
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(categoryID);
            if (categoryOpt.isEmpty()) {
                return createErrorResponse(HttpStatus.NOT_FOUND,
                        "Không tìm thấy", "Thể loại không tồn tại");
            }

            Category oldCategory = categoryOpt.get();
            if (!oldCategory.getCategoryName().equals(category.getCategoryName()) &&
                    categoryRepository.existsByCategoryName(category.getCategoryName())) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "Tên thể loại đã tồn tại");
            }

            if (newCategoryId != null && !newCategoryId.equals(categoryID)) {
                if (categoryRepository.existsById(newCategoryId)) {
                    return createErrorResponse(HttpStatus.BAD_REQUEST,
                            "Yêu cầu không hợp lệ", "ID thể loại mới đã tồn tại");
                }

                Category newCategory = new Category();
                newCategory.setCategoryID(newCategoryId);
                newCategory.setCategoryName(category.getCategoryName());
                newCategory.setDescription(category.getDescription());
                categoryRepository.save(newCategory);

                List<Product> products = productRepository.findByCategories_CategoryID(categoryID);
                for (Product product : products) {
                    Set<Category> categories = product.getCategories();
                    categories.remove(oldCategory);
                    categories.add(newCategory);
                    product.setCategories(categories);
                    productRepository.save(product);
                }

                categoryRepository.delete(oldCategory);
        savePendingCacheUpdate("category:" + newCategoryId, "update",
            Collections.singletonMap("category", newCategory));
        return ResponseEntity.ok(toCategoryDto(newCategory));
            } else {
                oldCategory.setCategoryName(category.getCategoryName());
                oldCategory.setDescription(category.getDescription());
        Category updatedCategory = categoryRepository.save(oldCategory);
        savePendingCacheUpdate("category:" + categoryID, "update",
            Collections.singletonMap("category", updatedCategory));
        return ResponseEntity.ok(toCategoryDto(updatedCategory));
            }
        } catch (Exception e) {
            logger.error("Error updating category: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi cập nhật thể loại: " + e.getMessage());
        }
    }

    @DeleteMapping("/category/{categoryID}")
    @Transactional
    @CacheEvict(value = {"category", "adminDashboard"}, allEntries = true)
    public ResponseEntity<?> deleteCategory(@PathVariable String categoryID) {
        try {
            Optional<Category> categoryOpt = categoryRepository.findById(categoryID);
            if (categoryOpt.isEmpty()) {
                return createErrorResponse(HttpStatus.NOT_FOUND,
                        "Không tìm thấy", "Thể loại không tồn tại");
            }

            Category category = categoryOpt.get();
            if (category.getProducts() != null && !category.getProducts().isEmpty()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "Không thể xóa thể loại có sản phẩm liên kết");
            }

            categoryRepository.delete(category);
            savePendingCacheUpdate("category:" + categoryID, "delete", Collections.emptyMap());
            return ResponseEntity.ok(Collections.singletonMap("message", "Xóa thể loại thành công"));
        } catch (Exception e) {
            logger.error("Error deleting category: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi xóa thể loại: " + e.getMessage());
        }
    }

    @PostMapping(value = "/product/variant/{productId}", consumes = {"multipart/form-data"})
    @CacheEvict(value = {"productVariants", "adminDashboard"}, allEntries = true)
    public ResponseEntity<?> insertProductVariant(
            @PathVariable String productId,
            @Valid @ModelAttribute ProductVariant productVariant,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            @RequestParam(value = "videoFiles", required = false) MultipartFile[] videoFiles,
            @RequestParam(value = "selectedCountry", required = true) String selectedCountry) {
        logger.info("POST /api/admin/product/variant/{} - Processing new product variant request", productId);
        try {
            if (bindingResult.hasErrors()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Lỗi xác thực", getValidationErrors(bindingResult));
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            if (productId == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "ID sản phẩm không được để trống");
            }

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("ID sản phẩm không hợp lệ: " + productId));
            productVariant.setProduct(product);

            if (productVariant.getCategory() == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "Thể loại là bắt buộc");
            }

            Category category = categoryRepository.findById(productVariant.getCategory().getCategoryID())
                    .orElseThrow(() -> new IllegalArgumentException("ID thể loại không hợp lệ"));
            productVariant.setCategory(category);

            if (selectedCountry == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "Quốc gia là bắt buộc");
            }
            Country country = countryRepository.findByCountryName(selectedCountry)
                    .orElseGet(() -> {
                        Country newCountry = new Country();
                        newCountry.setCountryName(selectedCountry.trim());
                        return countryRepository.save(newCountry);
                    });
            productVariant.setCountry(country);

            StringBuilder videoPublicIds = new StringBuilder();
            if (videoFiles != null && videoFiles.length > 0) {
                for (MultipartFile videoFile : videoFiles) {
                    if (!videoFile.isEmpty()) {
                        if (!isValidVideoFile(videoFile)) {
                            return createErrorResponse(HttpStatus.BAD_REQUEST,
                                    "Yêu cầu không hợp lệ",
                                    "Định dạng video không hợp lệ hoặc video quá lớn: " + videoFile.getOriginalFilename());
                        }
                        @SuppressWarnings("unchecked") Map<String,Object> uploadResult = cloudinaryService.uploadVideo(videoFile);
                        String publicId = (String) uploadResult.get("public_id");
                        if (videoPublicIds.length() > 0) {
                            videoPublicIds.append(",");
                        }
                        videoPublicIds.append(publicId);
                    }
                }
                productVariant.setVideoPublicIds(videoPublicIds.toString());
            } else {
                productVariant.setVideoPublicIds("");
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                if (!isValidImageFile(imageFile)) {
                    return createErrorResponse(HttpStatus.BAD_REQUEST,
                            "Yêu cầu không hợp lệ", "Định dạng ảnh không hợp lệ hoặc ảnh quá lớn");
                }
                @SuppressWarnings("unchecked") Map<String,Object> imageResult = cloudinaryService.uploadImage(imageFile);
                productVariant.setImageUrl((String) imageResult.get("secure_url"));
            } else {
                productVariant.setImageUrl("/uploads/default.png");
            }

            // Initialize view count if null
            if (productVariant.getViewCount() == null) {
                productVariant.setViewCount(0);
            }

            ProductVariant savedProductVariant = productVariantRepository.save(productVariant);
            savePendingCacheUpdate("productVariant:" + savedProductVariant.getId(), "add",
                    Collections.singletonMap("productVariant", savedProductVariant));
            return ResponseEntity.status(HttpStatus.CREATED).body(savedProductVariant);
        } catch (IllegalArgumentException e) {
            logger.error("Error adding product variant: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    "Yêu cầu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            logger.error("Server error adding product variant: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi thêm biến thể sản phẩm: " + e.getMessage());
        }
    }

    @GetMapping("/product/{productID}")
    @Cacheable(value = "product", key = "#productID")
    public ResponseEntity<?> getProduct(@PathVariable String productID) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            if (productID == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "ID sản phẩm không được để trống");
            }

            Product product = productRepository.findById(productID)
                    .orElseThrow(() -> new IllegalArgumentException("ID sản phẩm không hợp lệ"));
            Map<String, Object> response = new HashMap<>();
            response.put("product", product);
            response.put("categories", categoryRepository.findAll());
            response.put("countries", countryRepository.findAll());

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error fetching product: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.NOT_FOUND,
                    "Không tìm thấy", "Sản phẩm không tồn tại: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Server error fetching product: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy sản phẩm: " + e.getMessage());
        }
    }

    @DeleteMapping("/product/{productID}")
    @Transactional
    @CacheEvict(value = {"product", "adminDashboard"}, allEntries = true)
    public ResponseEntity<?> deleteProduct(@PathVariable String productID) {
        try {
            if (productID == null) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "ID sản phẩm không được để trống");
            }

            if (!productRepository.existsById(productID)) {
                return createErrorResponse(HttpStatus.NOT_FOUND,
                        "Không tìm thấy", "Sản phẩm không tồn tại");
            }

            commentRepository.deleteByProductId(productID);
            productVariantRepository.deleteByProductId(productID);
            productRepository.deleteById(productID);
            savePendingCacheUpdate("product:" + productID, "delete", Collections.emptyMap());
            return ResponseEntity.ok(Collections.singletonMap("message", "Xóa sản phẩm thành công"));
        } catch (Exception e) {
            logger.error("Error deleting product: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi xóa sản phẩm: " + e.getMessage());
        }
    }

    @GetMapping("/products")
    @Cacheable(value = "products")
    public ResponseEntity<?> getProducts() {
        try {
        List<Product> products = productRepository.findAll();
        List<Map<String,Object>> dtos = products.stream()
            .map(this::toProductDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error fetching products: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy danh sách sản phẩm: " + e.getMessage());
        }
    }

    @GetMapping("/products/search")
    @Cacheable(value = "productsSearch")
    public ResponseEntity<?> searchProducts(@RequestParam("query") String query,
                                            @RequestParam(value = "size", required = false) Integer size) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            int pageSize = (size == null || size <= 0) ? 10 : Math.min(size, 50);
            Pageable pageable = PageRequest.of(0, pageSize);
            Page<Product> page = productRepository.findByNameContaining(query, pageable);
            List<Map<String, Object>> results = page.getContent().stream().map(p -> {
                Map<String, Object> m = new HashMap<>();
                m.put("productID", p.getId());
                m.put("productName", p.getProductName());
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error searching products: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi tìm kiếm sản phẩm: " + e.getMessage());
        }
    }

    @GetMapping("/categories/search")
    @Cacheable(value = "categoriesSearch")
    public ResponseEntity<?> searchCategories(@RequestParam("query") String query,
                                              @RequestParam(value = "size", required = false) Integer size) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            int limit = (size == null || size <= 0) ? 10 : Math.min(size, 50);
            List<Category> list = categoryRepository.searchByNameFragment(query);
            List<Map<String,Object>> results = list.stream().limit(limit).map(c -> {
                Map<String,Object> m = new HashMap<>();
                m.put("categoryID", c.getCategoryID());
                m.put("categoryName", c.getCategoryName());
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(results);
        } catch (Exception e) {
            logger.error("Error searching categories: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi tìm kiếm thể loại: " + e.getMessage());
        }
    }

    @GetMapping("/categories")
    @Transactional(readOnly = true)
    @Cacheable(value = "categories")
    public ResponseEntity<?> getCategories() {
        try {
        List<Map<String, Object>> dtos = categoryRepository.findAll().stream()
            .map(this::toCategoryDto)
            .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error fetching categories: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy danh sách thể loại: " + e.getMessage());
        }
    }

    //@PostMapping(value = "/product/variant/{productId}", consumes = {"multipart/form-data"})
    //    @CacheEvict(value = {"productVariants", "adminDashboard"}, allEntries = true)
    //    public ResponseEntity<?> addProductVariant(@PathVariable String productId,
    //                                               @Valid @ModelAttribute ProductVariant productVariant,
    //                                               BindingResult bindingResult,
    //                                               @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
    //                                               @RequestParam(value = "videoFile", required = false) MultipartFile videoFile) {
    //        logger.info("POST /api/admin/product/variant/{} - Processing new product variant request", productId);
    //        try {
    //            if (bindingResult.hasErrors()) {
    //                return createErrorResponse(HttpStatus.BAD_REQUEST,
    //                        "Lỗi xác thực", getValidationErrors(bindingResult));
    //            }
    //
    //            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    //            if (!isAuthenticated(auth)) {
    //                return createErrorResponse(HttpStatus.UNAUTHORIZED,
    //                        "Không được phép", "Người dùng chưa được xác thực");
    //            }
    //
    //            Product product = productRepository.findById(productId)
    //                    .orElseThrow(() -> new IllegalArgumentException("ID sản phẩm không hợp lệ: " + productId));
    //            productVariant.setProduct(product);
    //
        
    //                Category category = categoryRepository.findById(productVariant.getCategory().getCategoryID())
    //                        .orElseThrow(() -> new IllegalArgumentException("ID danh mục không hợp lệ"));
    //                productVariant.setCategory(category);
    //            }
    //
    //            if (videoFile != null && !videoFile.isEmpty()) {
    //                if (!isValidVideoFile(videoFile)) {
    //                    return createErrorResponse(HttpStatus.BAD_REQUEST,
    //                            "Yêu cầu không hợp lệ", "Định dạng video không hợp lệ hoặc video quá lớn");
    //                }
    //                Map uploadResult = cloudinaryService.uploadVideo(videoFile);
    //                productVariant.setVideoPublicIds((String) uploadResult.get("public_id"));
    //            }
    //
    //            if (imageFile != null && !imageFile.isEmpty()) {
    //                if (!isValidImageFile(imageFile)) {
    //                    return createErrorResponse(HttpStatus.BAD_REQUEST,
    //                            "Yêu cầu không hợp lệ", "Định dạng ảnh không hợp lệ hoặc ảnh quá lớn");
    //                }
    //                Map imageResult = cloudinaryService.uploadImage(imageFile);
    //                productVariant.setImageUrl((String) imageResult.get("secure_url"));
    //            } else {
    //                productVariant.setImageUrl("/uploads/default.png");
    //            }
    //
    //            ProductVariant savedProductVariant = productVariantRepository.save(productVariant);
    //            savePendingCacheUpdate("productVariant:" + savedProductVariant.getId(), "add",
    //                    Collections.singletonMap("productVariant", savedProductVariant));
    //            return ResponseEntity.status(HttpStatus.CREATED).body(savedProductVariant);
    //        } catch (IllegalArgumentException e) {
    //            logger.error("Error adding product variant: {}", e.getMessage(), e);
    //            return createErrorResponse(HttpStatus.BAD_REQUEST,
    //                    "Yêu cầu không hợp lệ", e.getMessage());
    //        } catch (Exception e) {
    //            logger.error("Server error adding product variant: {}", e.getMessage(), e);
    //            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
    //                    "Lỗi máy chủ", "Lỗi khi thêm biến thể sản phẩm: " + e.getMessage());
    //        }
    //    }

    @GetMapping("/product/variant/{variantId}")
    @Cacheable(value = "productVariant", key = "#variantId")
    public ResponseEntity<?> getProductVariant(@PathVariable String variantId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            Optional<ProductVariant> productVariantOpt = productVariantRepository.findById(variantId);
            if (productVariantOpt.isPresent()) {
                ProductVariant pv = productVariantOpt.get();
                pv.setViewCount(pv.getViewCount() != null ? pv.getViewCount() + 1 : 1);
                productVariantRepository.save(pv);
                Map<String, Object> response = new HashMap<>();
                response.put("productVariant", pv);
                response.put("product", pv.getProduct());
                return ResponseEntity.ok(response);
            }
            return createErrorResponse(HttpStatus.NOT_FOUND,
                    "Không tìm thấy", "Biến thể sản phẩm không tồn tại");
        } catch (Exception e) {
            logger.error("Error fetching product variant: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy biến thể sản phẩm: " + e.getMessage());
        }
    }

    @PutMapping(value = "/product/variant/{variantId}", consumes = {"multipart/form-data"})
    @Transactional
    @CachePut(value = "productVariant", key = "#variantId")
    public ResponseEntity<?> updateProductVariant(@PathVariable String variantId,
                                                  @Valid @ModelAttribute ProductVariant productVariant,
                                                  BindingResult bindingResult,
                                                  @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                                                  @RequestParam(value = "videoFile", required = false) MultipartFile videoFile,
                                                  @RequestParam(value = "productId", required = false) String productId) {
        logger.info("PUT /api/admin/product/variant/{} - Processing update product variant request", variantId);
        try {
            if (bindingResult.hasErrors()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Lỗi xác thực", getValidationErrors(bindingResult));
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            ProductVariant existingVariant = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new IllegalArgumentException("ID biến thể sản phẩm không hợp lệ: " + variantId));

            existingVariant.setProductName(productVariant.getProductName());
            existingVariant.setDescription(productVariant.getDescription());
            existingVariant.setQuantity(productVariant.getQuantity());
            existingVariant.setPrice(productVariant.getPrice());
            existingVariant.setNewPrice(productVariant.getNewPrice());
            existingVariant.setDate(productVariant.getDate() != null ?
                    productVariant.getDate() : existingVariant.getDate());

            if (productId != null) {
                Product product = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("ID sản phẩm không hợp lệ: " + productId));
                existingVariant.setProduct(product);
            } else if (productVariant.getProduct() != null) {
                Product product = productRepository.findById(productVariant.getProduct().getId())
                        .orElseThrow(() -> new IllegalArgumentException("ID sản phẩm không hợp lệ"));
                existingVariant.setProduct(product);
            }

            if (productVariant.getCategory() != null) {
                Category category = categoryRepository.findById(productVariant.getCategory().getCategoryID())
                        .orElseThrow(() -> new IllegalArgumentException("ID danh mục không hợp lệ"));
                existingVariant.setCategory(category);
            }

            if (productVariant.getCountry() != null) {
                Country country = countryRepository.findByCountryId(productVariant.getCountry().getCountryId())
                        .orElseThrow(() -> new IllegalArgumentException("ID quốc gia không hợp lệ"));
                existingVariant.setCountry(country);
            }

            if (videoFile != null && !videoFile.isEmpty()) {
                if (!isValidVideoFile(videoFile)) {
                    return createErrorResponse(HttpStatus.BAD_REQUEST,
                            "Yêu cầu không hợp lệ", "Định dạng video không hợp lệ hoặc video quá lớn");
                }
                @SuppressWarnings("unchecked") Map<String,Object> uploadResult = cloudinaryService.uploadVideo(videoFile);
                existingVariant.setVideoPublicIds((String) uploadResult.get("public_id"));
            }

            if (imageFile != null && !imageFile.isEmpty()) {
                if (!isValidImageFile(imageFile)) {
                    return createErrorResponse(HttpStatus.BAD_REQUEST,
                            "Yêu cầu không hợp lệ", "Định dạng ảnh không hợp lệ hoặc ảnh quá lớn");
                }
                @SuppressWarnings("unchecked") Map<String,Object> imageResult = cloudinaryService.uploadImage(imageFile);
                existingVariant.setImageUrl((String) imageResult.get("secure_url"));
            }

            ProductVariant updatedVariant = productVariantRepository.save(existingVariant);
            savePendingCacheUpdate("productVariant:" + updatedVariant.getId(), "update",
                    Collections.singletonMap("productVariant", updatedVariant));
            savePendingNotification("productVariant:updated",
                    Collections.singletonMap("variantId", updatedVariant.getId()));

            logger.info("Successfully updated product variant: {}", updatedVariant.getId());
            return ResponseEntity.ok(updatedVariant);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating product variant: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    "Yêu cầu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            logger.error("Server error updating product variant: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi cập nhật biến thể sản phẩm: " + e.getMessage());
        }
    }

    @GetMapping("/users")
    @Cacheable(value = "users")
    public ResponseEntity<?> getUsers(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }
            // If pagination params provided, return paged structure with lightweight DTOs
            Function<User, Map<String, Object>> toDto = user -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("id", user.getId());
                m.put("username", user.getUsername());
                m.put("fullname", user.getFullname());
                m.put("email", user.getEmail());
                m.put("enabled", user.isEnabled());
                m.put("telephone", user.getTelephone());
                m.put("birthday", user.getBirthday());
                m.put("address", user.getAddress());
                // do not access lazy collections (e.g. userMovieLists) to avoid LazyInitializationException
                return m;
            };

            if (page != null && size != null) {
                int p = Math.max(page, 0);
                int s = Math.min(Math.max(size, 1), 200); // cap size to 200
                Pageable pageable = PageRequest.of(p, s);
                Page<User> userPage = userRepository.findAll(pageable);
                List<Map<String,Object>> content = userPage.getContent().stream().map(toDto).collect(Collectors.toList());
                Map<String,Object> meta = new LinkedHashMap<>();
                meta.put("content", content);
                meta.put("page", userPage.getNumber());
                meta.put("size", userPage.getSize());
                meta.put("totalElements", userPage.getTotalElements());
                meta.put("totalPages", userPage.getTotalPages());
                return ResponseEntity.ok(meta);
            }

            // Fallback: full list (legacy behavior) - return lightweight DTOs to avoid lazy-init issues
            List<User> users = userRepository.findAll();
            List<Map<String,Object>> dtos = users.stream().map(toDto).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error fetching users: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy danh sách người dùng: " + e.getMessage());
        }
    }

    /**
     * Admin: list all orders grouped by OrderInfo with summary fields
     */
    @GetMapping("/orders")
    public ResponseEntity<?> listAllOrders(
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                return createErrorResponse(HttpStatus.FORBIDDEN,
                        "Không được phép", "Chỉ admin mới có quyền truy cập");
            }

            Function<List<OrderInfo>, List<Map<String,Object>>> buildList = (infos) -> {
                List<Map<String,Object>> result = new ArrayList<>();
                for (OrderInfo info : infos) {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("id", info.getId());
                    m.put("status", info.getStatus());
                    m.put("amount", info.getAmount());
                    m.put("createdDate", info.getCreatedDate());
                    if (info.getUser() != null) {
                        m.put("userId", info.getUser().getId());
                        m.put("username", info.getUser().getUsername());
                        m.put("fullname", info.getUser().getFullname());
                    }
                    List<Order> orders = orderRepository.findByOrderInfo_Id(info.getId());
                    List<Map<String,Object>> items = new ArrayList<>();
                    for (Order o : orders) {
                        Map<String,Object> line = new LinkedHashMap<>();
                        line.put("orderId", o.getId());
                        line.put("productId", o.getProduct()!=null?o.getProduct().getId():null);
                        line.put("productName", o.getProduct()!=null?o.getProduct().getProductName():null);
                        line.put("quantity", o.getQuantity());
                        line.put("price", o.getTotalPrice());
                        items.add(line);
                    }
                    m.put("items", items);
                    result.add(m);
                }
                return result;
            };

            if (page != null && size != null) {
                int p = Math.max(page, 0);
                int s = Math.min(Math.max(size, 1), 200);
                Pageable pageable = PageRequest.of(p, s);
                Page<OrderInfo> infoPage = orderInfoRepository.findAll(pageable);
                Map<String,Object> meta = new LinkedHashMap<>();
                meta.put("content", buildList.apply(infoPage.getContent()));
                meta.put("page", infoPage.getNumber());
                meta.put("size", infoPage.getSize());
                meta.put("totalElements", infoPage.getTotalElements());
                meta.put("totalPages", infoPage.getTotalPages());
                return ResponseEntity.ok(meta);
            }

            List<OrderInfo> infos = orderInfoRepository.findAll();
            return ResponseEntity.ok(buildList.apply(infos));
        } catch (Exception e) {
            logger.error("Error listing all orders: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy danh sách đơn hàng: " + e.getMessage());
        }
    }

    /**
     * Admin: get single order info with items
     */
    @GetMapping("/orders/{orderInfoId}")
    public ResponseEntity<?> getOrderInfo(@PathVariable String orderInfoId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                return createErrorResponse(HttpStatus.FORBIDDEN,
                        "Không được phép", "Chỉ admin mới có quyền truy cập");
            }
            OrderInfo info = orderInfoRepository.findById(orderInfoId)
                    .orElseThrow(() -> new IllegalArgumentException("ID đơn hàng không hợp lệ"));
            Map<String,Object> m = new LinkedHashMap<>();
            m.put("id", info.getId());
            m.put("status", info.getStatus());
            m.put("amount", info.getAmount());
            m.put("createdDate", info.getCreatedDate());
            if (info.getUser() != null) {
                m.put("userId", info.getUser().getId());
                m.put("username", info.getUser().getUsername());
                m.put("fullname", info.getUser().getFullname());
            }
            List<Order> orders = orderRepository.findByOrderInfo_Id(info.getId());
            List<Map<String,Object>> items = new ArrayList<>();
            for (Order o : orders) {
                Map<String,Object> line = new LinkedHashMap<>();
                line.put("orderId", o.getId());
                line.put("productId", o.getProduct()!=null?o.getProduct().getId():null);
                line.put("productName", o.getProduct()!=null?o.getProduct().getProductName():null);
                line.put("quantity", o.getQuantity());
                line.put("price", o.getTotalPrice());
                items.add(line);
            }
            m.put("items", items);
            return ResponseEntity.ok(m);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND,
                    "Không tìm thấy", e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching order info: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy đơn hàng: " + e.getMessage());
        }
    }

    /**
     * Update order status
     */
    @PatchMapping("/orders/{orderInfoId}/status")
    @Transactional
    public ResponseEntity<?> updateOrderStatus(@PathVariable String orderInfoId,
                                               @RequestBody Map<String,String> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                return createErrorResponse(HttpStatus.FORBIDDEN,
                        "Không được phép", "Chỉ admin mới có quyền truy cập");
            }
            String status = body.getOrDefault("status", "");
            List<String> allowed = Arrays.asList("PENDING","PROCESSING","SHIPPED","DELIVERED","CANCELLED");
            if (!allowed.contains(status)) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "Trạng thái không hợp lệ");
            }
            OrderInfo info = orderInfoRepository.findById(orderInfoId)
                    .orElseThrow(() -> new IllegalArgumentException("ID đơn hàng không hợp lệ"));
            info.setStatus(status);
            orderInfoRepository.save(info);
            // Trả về chi tiết mới
            return getOrderInfo(orderInfoId);
        } catch (IllegalArgumentException e) {
            return createErrorResponse(HttpStatus.NOT_FOUND,
                    "Không tìm thấy", e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating order status: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi cập nhật trạng thái đơn hàng: " + e.getMessage());
        }
    }

    @GetMapping("/user/{id}")
    @Cacheable(value = "user", key = "#id")
    public ResponseEntity<?> getUser(@PathVariable String id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            if (id == null || id.trim().isEmpty()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "ID người dùng không được để trống");
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("ID người dùng không hợp lệ"));
            Map<String, Object> response = new HashMap<>();
            response.put("user", user);
            response.put("currentRole", user.getRoles().stream()
                    .map(Role::getName)
                    .findFirst()
                    .orElse("USER"));
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            logger.error("Error fetching user: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.NOT_FOUND,
                    "Không tìm thấy", "Người dùng không tồn tại: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Server error fetching user: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy người dùng: " + e.getMessage());
        }
    }

    @PutMapping("/user/{id}")
    @Transactional
    @CachePut(value = "user", key = "#id")
    public ResponseEntity<?> updateUser(@PathVariable String id,
                                        @Valid @RequestBody User user,
                                        BindingResult bindingResult,
                                        @RequestParam(value = "role", required = false) String role,
                                        @RequestParam(value = "password", required = false) String password) {
        try {
            if (bindingResult.hasErrors()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Lỗi xác thực", getValidationErrors(bindingResult));
            }

            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            if (id == null || id.trim().isEmpty()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "ID người dùng không được để trống");
            }

            if (userRepository.existsByUsername(user.getUsername()) &&
                    !userRepository.findById(id).map(User::getUsername).orElse("").equals(user.getUsername())) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "Tên đăng nhập đã tồn tại");
            }

            User currentUser = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("ID người dùng không hợp lệ"));

            currentUser.setUsername(user.getUsername());
            currentUser.setFullname(user.getFullname());
            currentUser.setEmail(user.getEmail());
            currentUser.setTelephone(user.getTelephone());
            currentUser.setGender(user.isGender());
            if (user.getBirthday() != null) {
                currentUser.setBirthday(user.getBirthday());
            }
            currentUser.setAddress(user.getAddress());
            currentUser.setEnabled(user.isEnabled());

            if (password != null && !password.trim().isEmpty()) {
                currentUser.setPassword(passwordEncoder.encode(password));
            }

            if (role != null && !role.trim().isEmpty()) {
                Role userRole = roleRepository.findByName(role)
                        .orElseThrow(() -> new IllegalArgumentException("Vai trò không hợp lệ: " + role));
                currentUser.setRoles(new HashSet<>(Collections.singletonList(userRole)));
            }

            User updatedUser = userRepository.save(currentUser);
            savePendingCacheUpdate("user:" + id, "update", Collections.singletonMap("user", updatedUser));
            savePendingNotification("user:updated", Collections.singletonMap("userId", id));
            return ResponseEntity.ok(updatedUser);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating user: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    "Yêu cầu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            logger.error("Server error updating user: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi cập nhật người dùng: " + e.getMessage());
        }
    }

    @DeleteMapping("/user/{id}")
    @Transactional
    @CacheEvict(value = {"user", "adminDashboard", "users"}, allEntries = true)
    public ResponseEntity<?> deleteUser(@PathVariable String id) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }
            boolean isAdmin = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!isAdmin) {
                return createErrorResponse(HttpStatus.FORBIDDEN,
                        "Không được phép", "Chỉ admin mới có quyền truy cập");
            }

            if (id == null || id.trim().isEmpty()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "ID người dùng không được để trống");
            }

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("ID người dùng không hợp lệ"));

            // Prevent admins from deleting their own account accidentally
            String currentUsername = auth.getName();
            if (currentUsername != null && (currentUsername.equals(user.getUsername()) || currentUsername.equals(user.getId()))) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "Không thể xóa chính bạn");
            }

            // Soft delete: disable the account to avoid cascading removal issues
            user.setEnabled(false);
            userRepository.save(user);

            savePendingCacheUpdate("user:" + id, "delete", Collections.emptyMap());
            savePendingNotification("user:deleted", Collections.singletonMap("userId", id));

            return ResponseEntity.ok(Collections.singletonMap("message", "Xóa người dùng thành công"));
        } catch (IllegalArgumentException e) {
            logger.error("Error deleting user: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.NOT_FOUND,
                    "Không tìm thấy", e.getMessage());
        } catch (Exception e) {
            logger.error("Server error deleting user: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi xóa người dùng: " + e.getMessage());
        }
    }

    @GetMapping("/product/variants/{productID}")
    @Cacheable(value = "productVariants", key = "#productID")
    public ResponseEntity<?> getProductVariants(@PathVariable String productID) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            if (productID == null || productID.trim().isEmpty()) {
                return createErrorResponse(HttpStatus.BAD_REQUEST,
                        "Yêu cầu không hợp lệ", "ID sản phẩm không được để trống");
            }

            List<ProductVariant> productVariants = productVariantRepository.findByProductId(productID);
            return ResponseEntity.ok(productVariants);
        } catch (Exception e) {
            logger.error("Error fetching product variants: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy danh sách biến thể sản phẩm: " + e.getMessage());
        }
    }

    // New: return all product variants as lightweight DTOs
    @GetMapping("/product-variants")
    @Cacheable(value = "productVariantsAll")
    public ResponseEntity<?> getAllProductVariants() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            List<ProductVariant> variants = productVariantRepository.findAll();
            List<Map<String,Object>> dtos = variants.stream().map(v -> {
                Map<String,Object> m = new HashMap<>();
                m.put("id", v.getId());
                m.put("productName", v.getProductName());
                m.put("description", v.getDescription());
                m.put("price", v.getPrice());
                m.put("newPrice", v.getNewPrice());
                m.put("quantity", v.getQuantity());
                m.put("imageUrl", v.getImageUrl());
                m.put("viewCount", v.getViewCount());
                m.put("date", v.getDate());
                if (v.getProduct() != null) m.put("productId", v.getProduct().getId());
                if (v.getCategory() != null) {
                    m.put("categoryId", v.getCategory().getCategoryID());
                    m.put("categoryName", v.getCategory().getCategoryName());
                }
                if (v.getCountry() != null) {
                    m.put("countryId", v.getCountry().getCountryId());
                    m.put("countryName", v.getCountry().getCountryName());
                }
                return m;
            }).collect(Collectors.toList());
            return ResponseEntity.ok(dtos);
        } catch (Exception e) {
            logger.error("Error fetching all product variants: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi lấy danh sách biến thể sản phẩm: " + e.getMessage());
        }
    }

    @PostMapping("/product-variants")
    @Transactional
    @CacheEvict(value = {"productVariants", "productVariantsAll", "adminDashboard"}, allEntries = true)
    public ResponseEntity<?> createProductVariant(@RequestBody Map<String, Object> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            // tolerate multiple key spellings from various clients (productId, productID, product)
            Object pidObj = body.get("productId");
            if (pidObj == null) pidObj = body.get("productID");
            if (pidObj == null) pidObj = body.get("product");
            String productId = pidObj != null ? String.valueOf(pidObj).trim() : null;
            if (productId == null || productId.isEmpty()) {
                logger.warn("createProductVariant missing productId in request body: {}", body);
                return createErrorResponse(HttpStatus.BAD_REQUEST, "Yêu cầu không hợp lệ", "productId là bắt buộc");
            }
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new IllegalArgumentException("ID sản phẩm không hợp lệ: " + productId));

            ProductVariant pv = new ProductVariant();
            pv.setProduct(product);
            // accept either productName or variantName from clients
            Object pname = body.get("productName");
            if (pname == null) pname = body.get("variantName");
            pv.setProductName(pname != null ? String.valueOf(pname) : "");
            pv.setDescription((String) body.getOrDefault("description", ""));
            pv.setPrice(parseDouble(body.get("price")));
            pv.setNewPrice(parseDouble(body.get("newPrice")));
            pv.setQuantity(parseInt(body.get("quantity")));
            pv.setImageUrl((String) body.getOrDefault("imageUrl", "/uploads/default.png"));

            // tolerate categoryId, category_id, or category => set Category if provided
            Object catObj = body.get("categoryId");
            if (catObj == null) catObj = body.get("category_id");
            if (catObj == null) catObj = body.get("category");
            String categoryId = catObj != null ? String.valueOf(catObj).trim() : null;
            if (categoryId != null && !categoryId.isEmpty()) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new IllegalArgumentException("ID thể loại không hợp lệ"));
                pv.setCategory(category);
            }

            String countryId = (String) body.get("countryId");
            if (countryId != null) {
                Country country = countryRepository.findByCountryId(countryId)
                        .orElseThrow(() -> new IllegalArgumentException("ID quốc gia không hợp lệ"));
                pv.setCountry(country);
            }

            ProductVariant saved = productVariantRepository.save(pv);
            savePendingCacheUpdate("productVariant:" + saved.getId(), "add", Collections.singletonMap("productVariant", saved));
            // build lightweight DTO including category info for FE convenience
            Map<String,Object> dto = new HashMap<>();
            dto.put("id", saved.getId());
            dto.put("productName", saved.getProductName());
            dto.put("price", saved.getPrice());
            dto.put("quantity", saved.getQuantity());
            if (saved.getProduct() != null) dto.put("productId", saved.getProduct().getId());
            if (saved.getCategory() != null) {
                dto.put("categoryId", saved.getCategory().getCategoryID());
                dto.put("categoryName", saved.getCategory().getCategoryName());
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(dto);
        } catch (IllegalArgumentException e) {
            logger.error("Error creating product variant: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    "Yêu cầu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            logger.error("Server error creating product variant: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi tạo biến thể sản phẩm: " + e.getMessage());
        }
    }

    @PutMapping("/product-variants/{variantId}")
    @Transactional
    @CacheEvict(value = {"productVariants", "productVariantsAll", "adminDashboard"}, allEntries = true)
    public ResponseEntity<?> updateProductVariantJson(@PathVariable String variantId, @RequestBody Map<String, Object> body) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            ProductVariant existing = productVariantRepository.findById(variantId)
                    .orElseThrow(() -> new IllegalArgumentException("ID biến thể sản phẩm không hợp lệ: " + variantId));

            if (body.containsKey("productName")) existing.setProductName((String) body.get("productName"));
            if (body.containsKey("description")) existing.setDescription((String) body.get("description"));
            if (body.containsKey("price")) existing.setPrice(parseDouble(body.get("price")));
            if (body.containsKey("newPrice")) existing.setNewPrice(parseDouble(body.get("newPrice")));
            if (body.containsKey("quantity")) existing.setQuantity(parseInt(body.get("quantity")));
            if (body.containsKey("imageUrl")) existing.setImageUrl((String) body.get("imageUrl"));

            if (body.containsKey("productId")) {
                String productId = (String) body.get("productId");
                Product p = productRepository.findById(productId)
                        .orElseThrow(() -> new IllegalArgumentException("ID sản phẩm không hợp lệ: " + productId));
                existing.setProduct(p);
            }

            // tolerate categoryId, category_id, category
            if (body.containsKey("categoryId") || body.containsKey("category_id") || body.containsKey("category")) {
                Object catObj = body.get("categoryId");
                if (catObj == null) catObj = body.get("category_id");
                if (catObj == null) catObj = body.get("category");
                String categoryId = catObj != null ? String.valueOf(catObj).trim() : null;
                if (categoryId != null && !categoryId.isEmpty()) {
                    Category c = categoryRepository.findById(categoryId)
                            .orElseThrow(() -> new IllegalArgumentException("ID thể loại không hợp lệ"));
                    existing.setCategory(c);
                } else {
                    existing.setCategory(null);
                }
            }

            if (body.containsKey("countryId")) {
                String countryId = (String) body.get("countryId");
                Country co = countryRepository.findByCountryId(countryId)
                        .orElseThrow(() -> new IllegalArgumentException("ID quốc gia không hợp lệ"));
                existing.setCountry(co);
            }

            ProductVariant saved = productVariantRepository.save(existing);
            savePendingCacheUpdate("productVariant:" + saved.getId(), "update", Collections.singletonMap("productVariant", saved));
            Map<String,Object> dto = new HashMap<>();
            dto.put("id", saved.getId());
            dto.put("productName", saved.getProductName());
            dto.put("price", saved.getPrice());
            dto.put("quantity", saved.getQuantity());
            if (saved.getProduct() != null) dto.put("productId", saved.getProduct().getId());
            if (saved.getCategory() != null) {
                dto.put("categoryId", saved.getCategory().getCategoryID());
                dto.put("categoryName", saved.getCategory().getCategoryName());
            }
            return ResponseEntity.ok(dto);
        } catch (IllegalArgumentException e) {
            logger.error("Error updating product variant: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.BAD_REQUEST,
                    "Yêu cầu không hợp lệ", e.getMessage());
        } catch (Exception e) {
            logger.error("Server error updating product variant: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi cập nhật biến thể sản phẩm: " + e.getMessage());
        }
    }

    @DeleteMapping("/product-variants/{variantId}")
    @Transactional
    @CacheEvict(value = {"productVariants", "productVariantsAll", "adminDashboard"}, allEntries = true)
    public ResponseEntity<?> deleteProductVariantJson(@PathVariable String variantId) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (!isAuthenticated(auth)) {
                return createErrorResponse(HttpStatus.UNAUTHORIZED,
                        "Không được phép", "Người dùng chưa được xác thực");
            }

            if (!productVariantRepository.existsById(variantId)) {
                return createErrorResponse(HttpStatus.NOT_FOUND, "Không tìm thấy", "Biến thể sản phẩm không tồn tại");
            }
            productVariantRepository.deleteById(variantId);
            savePendingCacheUpdate("productVariant:" + variantId, "delete", Collections.emptyMap());
            savePendingNotification("productVariant:deleted", Collections.singletonMap("variantId", variantId));
            return ResponseEntity.ok(Collections.singletonMap("message", "Xóa biến thể sản phẩm thành công"));
        } catch (Exception e) {
            logger.error("Error deleting product variant: {}", e.getMessage(), e);
            return createErrorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi máy chủ", "Lỗi khi xóa biến thể sản phẩm: " + e.getMessage());
        }
    }

    // small helpers to parse numbers from request bodies
    private Double parseDouble(Object o) {
        if (o == null) return null;
        try { return Double.parseDouble(o.toString()); } catch (Exception e) { return null; }
    }

    private Integer parseInt(Object o) {
        if (o == null) return null;
        try { return Integer.parseInt(o.toString()); } catch (Exception e) { return null; }
    }

    private boolean isAuthenticated(Authentication auth) {
        return auth != null && auth.isAuthenticated();
    }

    private boolean isValidImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String ct = file.getContentType();
        return ct != null && ct.startsWith("image/") && file.getSize() <= MAX_IMAGE_SIZE;
    }

    private boolean isValidVideoFile(MultipartFile file) {
        if (file == null || file.isEmpty()) return false;
        String ct = file.getContentType();
        return ct != null && ct.startsWith("video/") && file.getSize() <= MAX_VIDEO_SIZE;
    }

    private ResponseEntity<List<?>> createErrorResponse(HttpStatus status, String error, String details) {
    // Avoid setting non-ASCII text in HTTP headers (Tomcat may reject characters outside 0-255).
    // Return the error details in the response body (JSON) instead.
    return ResponseEntity.status(status)
        .body(Collections.singletonList(new ErrorResponse(error, details)));
    }

    private String getValidationErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
    }

    private void savePendingNotification(String channel, Map<String, Object> data) {
        try {
            PendingNotification notification = new PendingNotification();
            notification.setId(UUID.randomUUID().toString());
            notification.setChannel(channel);
            notification.setPayload(objectMapper.writeValueAsString(data));
            notification.setCreatedAt(LocalDateTime.now());
            pendingNotificationRepository.save(notification);
        } catch (Exception e) {
            logger.error("Error saving pending notification: {}", e.getMessage(), e);
        }
    }

    private void savePendingCacheUpdate(String cacheKey, String action, Map<String, Object> data) {
        try {
            PendingCacheUpdate update = new PendingCacheUpdate();
            update.setId(UUID.randomUUID().toString());
            update.setCacheKey(cacheKey);
            update.setAction(action);
            update.setPayload(objectMapper.writeValueAsString(data));
            update.setCreatedAt(LocalDateTime.now());
            pendingCacheUpdateRepository.save(update);
        } catch (Exception e) {
            logger.error("Error saving pending cache update: {}", e.getMessage(), e);
        }
    }
}