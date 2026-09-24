package com.dormrepair.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.dev-seed")
public class DevAccountProperties {
    private boolean enabled;
    private Account admin = new Account();
    private Account worker = new Account();
    private Account student = new Account();
    private Account disabled = new Account();
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Account getAdmin() { return admin; } public void setAdmin(Account admin) { this.admin = admin; }
    public Account getWorker() { return worker; } public void setWorker(Account worker) { this.worker = worker; }
    public Account getStudent() { return student; } public void setStudent(Account student) { this.student = student; }
    public Account getDisabled() { return disabled; } public void setDisabled(Account disabled) { this.disabled = disabled; }
    public static class Account {
        private String username; private String password;
        public String getUsername() { return username; } public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; } public void setPassword(String password) { this.password = password; }
    }
}
