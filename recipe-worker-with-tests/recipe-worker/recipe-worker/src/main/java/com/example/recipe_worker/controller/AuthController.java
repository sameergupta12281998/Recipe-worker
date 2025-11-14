package com.example.recipe_worker.controller;

import com.example.recipe_worker.dto.SignupRequest;
import com.example.recipe_worker.entity.User;
import com.example.recipe_worker.security.JwtTokenProvider;
import com.example.recipe_worker.service.UserService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authManager;
    private final UserService userService;
    private final JwtTokenProvider tokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AuthController(AuthenticationManager authManager,
                          UserService userService,
                          JwtTokenProvider tokenProvider,
                          PasswordEncoder passwordEncoder) {
        this.authManager = authManager;
        this.userService = userService;
        this.tokenProvider = tokenProvider;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest req) {
        String email = req.getEmail();
        String password = req.getPassword();
        String handle = req.getHandle();
        String role = req.getRole() == null ? "ROLE_USER" : req.getRole();
        User u = userService.createUser(email, password, handle, role);
        return ResponseEntity.status(201).body(Map.of("message","user_created","id", u.getId()));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String,String> body) {
        String email = body.get("email"), password = body.get("password");
        try {
            var userOpt = userService.findByEmail(email);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(401).body(Map.of("error","invalid_credentials"));
            }
            var user = userOpt.get();

            Authentication auth = authManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, password)
            );

            if (!user.isEmailVerified()) {
                // generate temporary 2-min token
                String token = userService.createVerificationTokenAndSend(user);

                // return verification link in response (for dev/testing)
                String verificationLink = "http://localhost:8081/api/auth/verify?token=" + token;

                return ResponseEntity.status(403).body(Map.of(
                        "error", "email_not_verified",
                        "message", "Please verify your email using the link below.",
                        "verification_link", verificationLink,
                        "expires_in", "2 minutes"
                ));
            }

            Set<String> roles = Set.of(user.getRole() != null ? user.getRole().getName() : "ROLE_USER");
            String access = tokenProvider.generateAccessToken(user.getId(), user.getEmail(), roles);

            return ResponseEntity.ok(Map.of("accessToken", access, "expiresIn", 3600));

        } catch (AuthenticationException ex) {
            return ResponseEntity.status(401).body(Map.of("error","invalid_credentials"));
        } catch (Exception ex) {
            log.error("Login error", ex);
            return ResponseEntity.status(500).body(Map.of("error","internal_error"));
        }
    }


    @GetMapping("/debug/whoami")
    public ResponseEntity<?> whoami(Authentication auth) {
        return ResponseEntity.ok(Map.of(
                "principal", auth == null ? null : auth.getPrincipal(),
                "authorities", auth == null ? null : auth.getAuthorities()
        ));
    }

    @PostMapping("/promote")
    public ResponseEntity<?> promote(@RequestParam String email, @RequestParam String role) {
        try {
            var u = userService.promoteUserRole(email, role);
            return ResponseEntity.ok(Map.of("message","promoted","email",u.getEmail(),"role",u.getRole().getName()));
        } catch (Exception ex) {
            return ResponseEntity.status(400).body(Map.of("error", ex.getMessage()));
        }
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verify(@RequestParam("token") String token) {
    boolean ok = userService.verifyEmailToken(token);
    if (!ok) {
        return ResponseEntity.badRequest().body(Map.of("verified", false, "message", "token_invalid_or_expired"));
    }
    return ResponseEntity.ok(Map.of("verified", true, "message", "email_verified"));
}


}
