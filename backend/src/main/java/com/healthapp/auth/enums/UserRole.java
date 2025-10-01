package com.healthapp.auth.enums;

public enum UserRole {
    USER("USER"),
    DOCTOR("DOCTOR"),
    ADMIN("ADMIN");
    
    private final String value;
    
    UserRole(String value) {
        this.value = value;
    }
    
    public String getValue() {
        return value;
    }
}

