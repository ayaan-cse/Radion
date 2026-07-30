package com.radion.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class LogQuery {
    public static void main(String[] args) {
        String url = "jdbc:postgresql://localhost:5432/radion_db";
        try (Connection conn = DriverManager.getConnection(url, "radion_user", "radion_password")) {
            Statement stmt = conn.createStatement();
            
            System.out.println("\n========== USERS INFO ==========");
            ResultSet rs2 = stmt.executeQuery("SELECT email, google_access_token, google_refresh_token FROM users");
            while (rs2.next()) {
                String at = rs2.getString("google_access_token");
                String rt = rs2.getString("google_refresh_token");
                System.out.println("Email: " + rs2.getString("email") + 
                                   ", AT Length: " + (at == null ? "NULL" : at.length()) + 
                                   ", RT Length: " + (rt == null ? "NULL" : rt.length()));
            }

            System.out.println("\n========== EVENTS INFO ==========");
            ResultSet rs3 = stmt.executeQuery("SELECT id, title, calendar_sync_status, google_calendar_event_id, calendar_sync_error FROM events ORDER BY event_time DESC LIMIT 5");
            while (rs3.next()) {
                System.out.println("Event ID: " + rs3.getString("id") + 
                                   ", Title: " + rs3.getString("title") + 
                                   ", Sync Status: " + rs3.getString("calendar_sync_status") + 
                                   ", GCal ID: " + rs3.getString("google_calendar_event_id") + 
                                   ", Error: " + rs3.getString("calendar_sync_error"));
            }
            rs2.close();
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
