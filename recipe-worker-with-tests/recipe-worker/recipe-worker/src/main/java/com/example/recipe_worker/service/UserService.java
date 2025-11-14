package com.example.recipe_worker.service;

import com.example.recipe_worker.entity.Role;
import com.example.recipe_worker.entity.User;
import com.example.recipe_worker.repository.RoleRepository;
import com.example.recipe_worker.repository.UserRepository; // adapt package if needed
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserService{

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final Map<String, VerificationTemp> tempTokens = new ConcurrentHashMap<>();


    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createUser(String email, String rawPassword, String handle, String roleName) {
        if (userRepository.findByEmail(email).isPresent()) {
            throw new IllegalArgumentException("email_already_exists");
        }

        String hashed = passwordEncoder.encode(rawPassword);
        Role notARole = new Role();
        notARole.setName(roleName);
        Role role = roleRepository.findByName(normalizeRole(roleName))
                .orElseGet(() -> roleRepository.save(notARole));

        User u = new User();
        u.setEmail(email);
        u.setPasswordHash(hashed);
        u.setHandle(handle);
        u.setRole(role);
        return userRepository.save(u);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Transactional
    public User promoteUserRole(String email, String roleName) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("user_not_found"));

                Role notARole = new Role();
                notARole.setName(roleName);
        Role role = roleRepository.findByName(normalizeRole(roleName))
                .orElseGet(() -> roleRepository.save(notARole));
        user.setRole(role);
        return userRepository.save(user);
    }

    private String normalizeRole(String r) {
        if (r == null || r.isBlank()) return "ROLE_USER";
        r = r.trim();
        return r.startsWith("ROLE_") ? r : "ROLE_" + r;
    }

    public String createVerificationTokenAndSend(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        Instant expiry = Instant.now().plusSeconds(120); // 2 minutes
        tempTokens.put(token, new VerificationTemp(user.getEmail(), expiry));
        return token;
    }

    public boolean verifyEmailToken(String token) {
        VerificationTemp data = tempTokens.get(token);
        if (data == null) return false;
        if (Instant.now().isAfter(data.expiry())) return false;

        var opt = findByEmail(data.email());
        if (opt.isEmpty()) return false;
        var user = opt.get();
        user.setEmailVerified(true);
        userRepository.save(user);
        tempTokens.remove(token);
        return true;
    }

    private record VerificationTemp(String email, Instant expiry) {}
}
