package com.risk.server.model;

import jakarta.persistence.*;

@Entity
@Table(name = "usr")
public class AppUser {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    private String password;          // BCrypt
    private String role;              // "ROLE_ADMIN" / …

    public AppUser() { }
    public AppUser(String u, String p, String r) {
        this.username = u; this.password = p; this.role = r;
    }

    /* getters / setters */
    public String getUsername() { return username; }
    public String getPassword() { return password; }
    public String getRole()     { return role; }
}

