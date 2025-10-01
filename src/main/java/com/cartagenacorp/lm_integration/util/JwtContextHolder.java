package com.cartagenacorp.lm_integration.util;

import java.util.UUID;

public class JwtContextHolder {
    private static final ThreadLocal<UUID> currentUserId = new ThreadLocal<>();
    private static final ThreadLocal<String> currentToken = new ThreadLocal<>();
    private static final ThreadLocal<String> currentUserEmail = new ThreadLocal<>();
    private static final ThreadLocal<UUID> currentOrganizationId = new ThreadLocal<>();

    public static void setUserId(UUID userId) {
        currentUserId.set(userId);
    }

    public static UUID getUserId() {
        return currentUserId.get();
    }

    public static void setToken(String token) {
        currentToken.set(token);
    }

    public static String getToken() {
        return currentToken.get();
    }

    public static void setUserEmail(String email) {
        currentUserEmail.set(email);
    }

    public static String getUserEmail() {
        return currentUserEmail.get();
    }

    public static void setOrganizationId(UUID organizationId) {
        currentOrganizationId.set(organizationId);
    }

    public static UUID getOrganizationId() {
        return currentOrganizationId.get();
    }

    public static void clear() {
        currentUserId.remove();
        currentToken.remove();
        currentUserEmail.remove();
        currentOrganizationId.remove();
    }
}

