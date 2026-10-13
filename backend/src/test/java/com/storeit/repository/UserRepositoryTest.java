package com.storeit.repository;

import com.storeit.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Testcontainers
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("storeit_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("findByEmail returns user when present")
    void findByEmail_found() {
        User user = User.builder()
                .email("repo@test.com")
                .fullName("Repo Test")
                .passwordHash("hashed")
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("repo@test.com");

        assertTrue(found.isPresent());
        assertEquals("Repo Test", found.get().getFullName());
    }

    @Test
    @DisplayName("findByEmail returns empty when not present")
    void findByEmail_notFound() {
        Optional<User> found = userRepository.findByEmail("nobody@test.com");
        assertFalse(found.isPresent());
    }

    @Test
    @DisplayName("existsByEmail returns correct boolean")
    void existsByEmail() {
        User user = User.builder()
                .email("exists@test.com")
                .fullName("Exists Test")
                .passwordHash("hashed")
                .build();
        userRepository.save(user);

        assertTrue(userRepository.existsByEmail("exists@test.com"));
        assertFalse(userRepository.existsByEmail("nope@test.com"));
    }

    @Test
    @DisplayName("Saved user gets a generated UUID")
    void save_generatesId() {
        User user = User.builder()
                .email("uuid@test.com")
                .fullName("UUID Test")
                .passwordHash("hashed")
                .build();
        User saved = userRepository.save(user);

        assertNotNull(saved.getId());
    }
}
