package com.example.chatapp;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class BackendClient {
    private static final String BASE_URL = "http://localhost:8000";
    private String currentUserMobile;
    private String currentUsername;
    private String currentUserAbout;

    public BackendClient() {
    }

    public boolean register(String mobile, String username) {
        return register(mobile, username, null);
    }

    public boolean register(String mobile, String username, String about) {
        try {
            String json;
            if (about != null && !about.isEmpty()) {
                String escapedAbout = about.replace("\"", "\\\"").replace("\n", "\\n");
                json = String.format("{\"mobile\":\"%s\",\"username\":\"%s\",\"about\":\"%s\"}", 
                    mobile, username, escapedAbout);
            } else {
                json = String.format("{\"mobile\":\"%s\",\"username\":\"%s\"}", mobile, username);
            }
            
            String response = sendPostRequest("/register", json);
            
            this.currentUserMobile = extractValue(response, "mobile");
            this.currentUsername = extractValue(response, "username");
            this.currentUserAbout = extractValue(response, "about");
            if (this.currentUserAbout == null || this.currentUserAbout.isEmpty()) {
                this.currentUserAbout = "Hey there! I am using Chat App";
            }
            return true;
        } catch (Exception e) {
            System.err.println("Registration failed: " + e.getMessage());
            return false;
        }
    }

    public boolean login(String mobile) {
        try {
            String json = String.format("{\"mobile\":\"%s\"}", mobile);
            String response = sendPostRequest("/login", json);
            
            this.currentUserMobile = extractValue(response, "mobile");
            this.currentUsername = extractValue(response, "username");
            this.currentUserAbout = extractValue(response, "about");
            if (this.currentUserAbout == null || this.currentUserAbout.isEmpty()) {
                this.currentUserAbout = "Hey there! I am using Chat App";
            }
            return true;
        } catch (Exception e) {
            System.err.println("Login failed: " + e.getMessage());
            return false;
        }
    }

    public boolean sendMessage(String receiverMobile, String content) {
        try {
            String escapedContent = content.replace("\"", "\\\"").replace("\n", "\\n");
            String json = String.format("{\"sender_mobile\":\"%s\",\"receiver_mobile\":\"%s\",\"content\":\"%s\"}", 
                currentUserMobile, receiverMobile, escapedContent);
            String response = sendPostRequest("/send_message", json);
            return response.contains("Message sent");
        } catch (Exception e) {
            System.err.println("Send message failed: " + e.getMessage());
            return false;
        }
    }

    public boolean sendFile(String receiverMobile, String fileName, String fileDataBase64) {
        try {
            String escapedFileName = fileName.replace("\"", "\\\"");
            String escapedFileData = fileDataBase64.replace("\"", "\\\"");
            String json = String.format("{\"sender_mobile\":\"%s\",\"receiver_mobile\":\"%s\",\"file_name\":\"%s\",\"file_data\":\"%s\"}", 
                currentUserMobile, receiverMobile, escapedFileName, escapedFileData);
            String response = sendPostRequest("/send_file", json);
            return response.contains("File sent");
        } catch (Exception e) {
            System.err.println("Send file failed: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, String>> getMessages() {
        try {
            String response = sendGetRequest("/messages/" + currentUserMobile);
            return parseMessagesArray(response);
        } catch (Exception e) {
            System.err.println("Get messages failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    public Map<String, String> getUserInfo(String mobile) {
        try {
            String response = sendGetRequest("/user/" + mobile);
            Map<String, String> userInfo = new HashMap<>();
            userInfo.put("mobile", extractValue(response, "mobile"));
            userInfo.put("username", extractValue(response, "username"));
            userInfo.put("about", extractValue(response, "about"));
            return userInfo;
        } catch (Exception e) {
            System.err.println("Get user info failed: " + e.getMessage());
            Map<String, String> fallback = new HashMap<>();
            fallback.put("mobile", mobile);
            fallback.put("username", mobile);
            fallback.put("about", "");
            return fallback;
        }
    }

    private String sendPostRequest(String endpoint, String jsonPayload) throws IOException {
        URL url = URI.create(BASE_URL + endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        }

        return readResponse(conn);
    }

    private String sendGetRequest(String endpoint) throws IOException {
        URL url = URI.create(BASE_URL + endpoint).toURL();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        return readResponse(conn);
    }

    private String readResponse(HttpURLConnection conn) throws IOException {
        int responseCode = conn.getResponseCode();
        InputStream is = (responseCode >= 200 && responseCode < 300) 
            ? conn.getInputStream() 
            : conn.getErrorStream();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line);
            }
            
            if (responseCode >= 400) {
                throw new IOException("HTTP Error: " + responseCode + " - " + response.toString());
            }
            
            return response.toString();
        }
    }

    private String extractValue(String json, String key) {
        String searchKey = "\"" + key + "\":";
        int start = json.indexOf(searchKey);
        if (start == -1) return "";
        
        start += searchKey.length();
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '"')) {
            start++;
        }
        
        int end = start;
        boolean inQuotes = false;
        if (start > 0 && json.charAt(start - 1) == '"') {
            inQuotes = true;
            while (end < json.length() && json.charAt(end) != '"') {
                end++;
            }
        } else {
            while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
                end++;
            }
        }
        
        return json.substring(start, end).trim();
    }

    private List<Map<String, String>> parseMessagesArray(String json) {
        List<Map<String, String>> messages = new ArrayList<>();
        if (!json.startsWith("[")) return messages;
        
        int depth = 0;
        int objStart = -1;
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') {
                if (depth == 0) objStart = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && objStart != -1) {
                    String objJson = json.substring(objStart, i + 1);
                    Map<String, String> msg = new HashMap<>();
                    msg.put("id", extractValue(objJson, "id"));
                    msg.put("sender_mobile", extractValue(objJson, "sender_mobile"));
                    msg.put("receiver_mobile", extractValue(objJson, "receiver_mobile"));
                    msg.put("content", extractValue(objJson, "content"));
                    msg.put("timestamp", extractValue(objJson, "timestamp"));
                    msg.put("is_file", extractValue(objJson, "is_file"));
                    msg.put("file_name", extractValue(objJson, "file_name"));
                    msg.put("file_data", extractValue(objJson, "file_data"));
                    messages.add(msg);
                }
            }
        }
        
        return messages;
    }

    public String getCurrentUserMobile() {
        return currentUserMobile;
    }

    public String getCurrentUsername() {
        return currentUsername;
    }
    
    public String getCurrentUserAbout() {
        return currentUserAbout != null ? currentUserAbout : "Hey there! I am using Chat App";
    }
}
