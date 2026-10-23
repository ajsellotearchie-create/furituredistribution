package com.scc.model;

public record User(int user_id, String username, String password, String full_name, String roles, String status) {
    @Override
    public String toString() {
        return String.format("[%d] %s (%s) - role: %s, status: %s", user_id, username, full_name, roles, status);
    }
}