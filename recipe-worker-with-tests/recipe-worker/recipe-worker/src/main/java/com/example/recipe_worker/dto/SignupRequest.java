package com.example.recipe_worker.dto;

public class SignupRequest {
    private String email;
    private String password;
    private String handle;
    private String role;

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getHandle() { return handle; }
    public void setHandle(String handle) { this.handle = handle; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}
