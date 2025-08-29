package com.andd.DoDangAn.DoDangAn.Controller;

import com.andd.DoDangAn.DoDangAn.Util.JwtUtil;
import com.andd.DoDangAn.DoDangAn.models.User;
import com.andd.DoDangAn.DoDangAn.models.Role;
import com.andd.DoDangAn.DoDangAn.repository.jpa.UserRepository;
import com.andd.DoDangAn.DoDangAn.repository.jpa.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/user")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    @Autowired
    public AuthController(JwtUtil jwtUtil, UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder, AuthenticationManager authenticationManager) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
    }

    @GetMapping("/login")
    public ResponseEntity<?> showLoginPage(@RequestParam(value = "error", required = false) String error) {
        Map<String, String> response = new HashMap<>();
        if (error != null) {
            if (error.equals("PleaseLogin")) {
                response.put("error", "Vui lòng đăng nhập để tiếp tục!");
            } else if (error.equals("TokenExpired")) {
                response.put("error", "Phiên đăng nhập đã hết hạn, vui lòng đăng nhập lại!");
            } else if (error.equals("InvalidToken")) {
                response.put("error", "Token không hợp lệ, vui lòng đăng nhập lại!");
            } else {
                response.put("error", "Lỗi xác thực, vui lòng thử lại!");
            }
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
        response.put("message", "Vui lòng đăng nhập");
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String username = credentials.get("username");
            String password = credentials.get("password");

            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password)
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            Optional<User> userOptional = userRepository.findByUsername(username);
            if (!userOptional.isPresent()) {
                logger.warn("Login failed: User {} not found", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Tài khoản không tồn tại!"));
            }

            User user = userOptional.get();
            String fullName = user.getFullname() != null ? user.getFullname() : username;
            String jwt = jwtUtil.generateToken(username, fullName);
            logger.info("User {} logged in successfully, token generated", username);

            Map<String, Object> response = new HashMap<>();
            response.put("jwt", jwt);
            response.put("isAdmin", user.getRoles().stream().anyMatch(role -> "ADMIN".equals(role.getName())));
            response.put("username", username);
            response.put("fullName", fullName);
            response.put("roles", user.getRoles().stream().map(Role::getName).toList());
            response.put("message", "Đăng nhập thành công!");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error during login for user {}: {}", credentials.get("username"), e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Tài khoản hoặc mật khẩu không đúng!"));
        }
    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout() {
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok(Map.of("message", "Đăng xuất thành công!"));
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> payload) {
        String username = payload.get("username");
        String password = payload.get("password");
        String email = payload.getOrDefault("email", username + "@example.com");
        String fullname = payload.getOrDefault("fullname", username);
    String adminCode = payload.get("adminCode");
        String genderStr = payload.get("gender");
        String birthdayStr = payload.get("birthday");
        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Thiếu username hoặc password"));
        }
        if (userRepository.existsByUsername(username)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "Username đã tồn tại"));
        }
        if (adminCode != null) {
            adminCode = adminCode.trim();
        }
        logger.info("Register attempt user='{}' adminCodePresent={} ", username, adminCode != null && !adminCode.isEmpty());
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setEnabled(true);
        user.setFullname(fullname);
        // Parse gender (default true if not provided)
        boolean gender = true;
        if (genderStr != null) {
            gender = genderStr.equalsIgnoreCase("true") || genderStr.equalsIgnoreCase("male") || genderStr.equals("1");
        }
        user.setGender(gender);
        // Parse birthday (YYYY-MM-DD)
        LocalDate birthday = LocalDate.now();
        if (birthdayStr != null && !birthdayStr.isBlank()) {
            try {
                birthday = LocalDate.parse(birthdayStr);
            } catch (Exception ignored) {}
        }
        user.setBirthday(birthday);
        // Provide placeholder defaults for mandatory @NotBlank fields to avoid validation failure if FE does not send them yet.
        user.setAddress(payload.getOrDefault("address", "N/A"));
        user.setEmail(email);
        user.setTelephone(payload.getOrDefault("telephone", "0000000000"));
        // default role USER
        Role userRole = roleRepository.findByName("USER").orElseGet(() -> {
            Role r = new Role();
            r.setName("USER");
            return roleRepository.save(r);
        });
        user.getRoles().add(userRole);
        // Optional admin elevation with fixed code (simple, not for production)
        final String FIXED_ADMIN_CODE = "CREATE-ADMIN-2025"; // change as needed / move to config
        if (adminCode != null && adminCode.equals(FIXED_ADMIN_CODE)) {
            Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
                Role r = new Role();
                r.setName("ADMIN");
                return roleRepository.save(r);

            });
            user.getRoles().add(adminRole);
            logger.info("User '{}' elevated to ADMIN via adminCode", username);
        } else {
            if (adminCode != null && !adminCode.isEmpty()) {
                logger.warn("User '{}' provided invalid adminCode: '{}'", username, adminCode);
            }
        }
        userRepository.save(user);
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> "ADMIN".equals(r.getName()));
        List<String> roles = user.getRoles().stream().map(Role::getName).toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Đăng ký thành công",
                "isAdmin", isAdmin,
                "roles", roles
        ));
    }

    @PostMapping("/promote-admin")
    public ResponseEntity<?> promoteSelfToAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","Yêu cầu đăng nhập"));
        }
        Optional<User> opt = userRepository.findByUsername(auth.getName());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","User không tồn tại"));
        }
        User user = opt.get();
        boolean already = user.getRoles().stream().anyMatch(r->"ADMIN".equals(r.getName()));
        if (already) {
            return ResponseEntity.ok(Map.of("message","Đã là admin","roles", user.getRoles().stream().map(Role::getName).toList()));
        }
        Role adminRole = roleRepository.findByName("ADMIN").orElseGet(() -> {
            Role r = new Role();
            r.setName("ADMIN");
            return roleRepository.save(r);
        });
        user.getRoles().add(adminRole);
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("message","Thăng cấp ADMIN thành công","roles", user.getRoles().stream().map(Role::getName).toList()));
    }

    @GetMapping("/profile")
    public ResponseEntity<?> profile() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error","Chưa đăng nhập"));
        }
        Optional<User> opt = userRepository.findByUsername(auth.getName());
        if (opt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error","User không tồn tại"));
        }
        User u = opt.get();
        Map<String,Object> body = new LinkedHashMap<>();
        body.put("username", u.getUsername());
        body.put("fullName", u.getFullname());
        body.put("email", u.getEmail());
        body.put("roles", u.getRoles().stream().map(Role::getName).toList());
        body.put("isAdmin", u.getRoles().stream().anyMatch(r->"ADMIN".equals(r.getName())));
        return ResponseEntity.ok(body);
    }
}