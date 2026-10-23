package com.example.RegisterPage;

import com.example.RegisterPage.entity.User;
import com.example.RegisterPage.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
class RegisterPageApplicationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final String testEmail = "testuser_unique@example.com";

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userRepository.findByEmail(testEmail).ifPresent(userRepository::delete);
    }

    @Test
    void contextLoads() {
    }

    @Test
    void testRegisterSuccess() throws Exception {
        String json = """
            {
                "name": "Test User",
                "email": "%s",
                "password": "password123"
            }
        """.formatted(testEmail);

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"));
    }

    @Test
    void testRegisterDuplicateEmailReturnsConflict() throws Exception {
        User user = new User("Duplicate User", testEmail, passwordEncoder.encode("password123"));
        userRepository.save(user);

        String json = """
            {
                "name": "Duplicate User",
                "email": "%s",
                "password": "password123"
            }
        """.formatted(testEmail);

        mockMvc.perform(post("/api/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Email is already registered"));
    }

    @Test
    void testLoginWithIncorrectEmailReturnsUnauthorized() throws Exception {
        String json = """
            {
                "email": "nonexistent_email_12345@example.com",
                "password": "password123"
            }
        """;

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLoginWithIncorrectPasswordReturnsUnauthorized() throws Exception {
        User user = new User("Test User", testEmail, passwordEncoder.encode("correctPassword123"));
        userRepository.save(user);

        String json = """
            {
                "email": "%s",
                "password": "wrongPassword123"
            }
        """.formatted(testEmail);

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void testLoginWithCorrectCredentialsReturnsOkAndCookie() throws Exception {
        User user = new User("Test User", testEmail, passwordEncoder.encode("correctPassword123"));
        userRepository.save(user);

        String json = """
            {
                "email": "%s",
                "password": "correctPassword123"
            }
        """.formatted(testEmail);

        mockMvc.perform(post("/api/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
                .andExpect(status().isOk())
                .andExpect(cookie().exists("JWT"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    void testLogoutClearsCookie() throws Exception {
        mockMvc.perform(post("/api/logout"))
                .andExpect(status().isOk())
                .andExpect(cookie().maxAge("JWT", 0))
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }
}
