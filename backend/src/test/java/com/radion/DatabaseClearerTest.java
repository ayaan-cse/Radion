package com.radion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
public class DatabaseClearerTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    public void clearOAuthTokens() {
        System.out.println("Starting Database Clearance...");

        String userEmail = "cu23250013@coeruniversity.ac.in";
        
        // Find user ID
        String getUserIdSql = "SELECT id FROM users WHERE email = ?";
        java.util.List<java.util.UUID> userIds = jdbcTemplate.queryForList(getUserIdSql, java.util.UUID.class, userEmail);
        
        if (userIds.isEmpty()) {
            System.out.println("User not found: " + userEmail);
            return;
        }
        java.util.UUID userId = userIds.get(0);
        System.out.println("Found user: " + userId);

        // 1. Delete the Dashboard Login Google Calendar tokens stored in the User table.
        String clearUserTokensSql = "UPDATE users SET google_access_token = NULL, google_refresh_token = NULL, google_token_expires_at = NULL WHERE id = ?";
        int updatedUsers = jdbcTemplate.update(clearUserTokensSql, userId);
        System.out.println("Cleared Google tokens for " + updatedUsers + " user(s).");

        // 2. Delete all ConnectedService records for this user (Gmail, Classroom)
        String deleteConnectionsSql = "DELETE FROM connected_services WHERE user_id = ?";
        int deletedConnections = jdbcTemplate.update(deleteConnectionsSql, userId);
        System.out.println("Deleted " + deletedConnections + " connected service(s) for user.");

        System.out.println("Database Clearance Complete.");
    }
}
