package com.ticketbooking.auth.domain.model;

/**
 * Aggregate Root del bounded context de Autenticación. POJO puro.
 */
public class User {

    private Long id;
    private Email email;
    private String name;
    private String phone;
    private String passwordHash;

    public User(Email email, String name, String passwordHash) {
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
    }

    public User(Long id, Email email, String name, String phone, String passwordHash) {
        this.id = id;
        this.email = email;
        this.name = name;
        this.phone = phone;
        this.passwordHash = passwordHash;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Email getEmail() { return email; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
}
