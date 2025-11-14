package com.example.recipe_worker.controller;

import com.example.recipe_worker.dto.SignupRequest;
import com.example.recipe_worker.entity.Role;
import com.example.recipe_worker.entity.User;
import com.example.recipe_worker.security.JwtTokenProvider;
import com.example.recipe_worker.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.util.Map;
import java.util.Optional;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;


@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)   // <--- disable security filters for test
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean private UserService userService;
    @MockBean private AuthenticationManager authManager;
    @MockBean private JwtTokenProvider jwtTokenProvider;
    @MockBean private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void signup_shouldReturnCreatedUser() throws Exception {
        User mockUser = new User();
        mockUser.setId(8L);
        mockUser.setEmail("new@example.com");
        Role r = new Role();
        r.setName("ROLE_USER");
        mockUser.setRole(r);

        when(userService.createUser(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockUser);

        SignupRequest req = new SignupRequest();
        req.setEmail("new@example.com");
        req.setPassword("new123");
        req.setHandle("user1");
        req.setRole("ROLE_USER");

        mockMvc.perform(post("/api/auth/signup")
        .contentType(MediaType.APPLICATION_JSON)
        .content(mapper.writeValueAsString(req))
        .with(csrf()))
            .andExpect(status().isCreated())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.message").value("user_created"))
            .andExpect(jsonPath("$.id").value(8));
    }

    
     @Test
    void login_shouldReturnVerificationLink_whenEmailNotVerified() throws Exception {
        // create a not-verified user
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("notverified@example.com");
        mockUser.setEmailVerified(false);

        // stub userService.findByEmail
        when(userService.findByEmail("notverified@example.com"))
                .thenReturn(Optional.of(mockUser));

        // stub authentication manager authenticate() to succeed
        when(authManager.authenticate(any()))
                .thenReturn(Mockito.mock(Authentication.class));

        // when userService.createVerificationTokenAndSend is called, return "dummy-token"
        when(userService.createVerificationTokenAndSend(User.class.cast(mockUser)))
                .thenReturn("dummy-token");

        // perform login POST
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", "notverified@example.com",
                                "password", "password"
                        )))
                        .with(csrf()))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.error").value("email_not_verified"))
                .andExpect(jsonPath("$.verification_link").value("http://localhost:8081/api/auth/verify?token=dummy-token"));
    }


    @Test
void login_shouldReturnAccessToken_whenEmailVerified() throws Exception {
    // Arrange: create a verified user with role
    User mockUser = new User();
    mockUser.setId(1L);
    mockUser.setEmail("new123@example.com");
    mockUser.setEmailVerified(true);
    Role role = new Role();
    role.setName("ROLE_USER");
    mockUser.setRole(role);

    when(userService.findByEmail("new123@example.com"))
            .thenReturn(Optional.of(mockUser));


    when(authManager.authenticate(ArgumentMatchers.<UsernamePasswordAuthenticationToken>any()))
            .thenReturn(Mockito.mock(Authentication.class));

    when(jwtTokenProvider.generateAccessToken(
            anyLong(),                 // for user id (primitive long/Long)
            anyString(),               // for email
            anySet() // for roles
    )).thenReturn("access-token-123");


    mockMvc.perform(post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(mapper.writeValueAsString(Map.of(
                            "email", "new123@example.com", // <- must match stub above
                            "password", "secret"
                    )))
                    .with(csrf()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").value("access-token-123"))
            .andExpect(jsonPath("$.expiresIn").value(3600));
}


    @Test
    void verify_shouldReturnVerifiedTrue_whenTokenValid() throws Exception {
        when(userService.verifyEmailToken("dummy")).thenReturn(true);

        mockMvc.perform(get("/api/auth/verify").param("token", "dummy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true))
                .andExpect(jsonPath("$.message").value("email_verified"));
    }

    @Test
    void verify_shouldReturnBadRequest_whenTokenInvalid() throws Exception {
        when(userService.verifyEmailToken("invalid")).thenReturn(false);

        mockMvc.perform(get("/api/auth/verify").param("token", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.verified").value(false))
                .andExpect(jsonPath("$.message").value("token_invalid_or_expired"));
    }

    @Test
    void promote_shouldReturnPromotedUser() throws Exception {
        User user = new User();
        user.setEmail("chef@example.com");
        Role role = new Role();
        role.setName("ROLE_CHEF");
        user.setRole(role);

        when(userService.promoteUserRole("chef@example.com", "ROLE_CHEF"))
                .thenReturn(user);

        mockMvc.perform(post("/api/auth/promote")
                        .param("email", "chef@example.com")
                        .param("role", "ROLE_CHEF")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("promoted"))
                .andExpect(jsonPath("$.email").value("chef@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_CHEF"));
    }

}