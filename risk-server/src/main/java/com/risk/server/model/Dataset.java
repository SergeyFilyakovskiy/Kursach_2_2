// src/main/java/com/risk/server/model/Dataset.java
package com.risk.server.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "dataset")
public class Dataset {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long   id;

    /** Имя файла или пользовательское имя */
    private String name;

    /** Время загрузки */
    private LocalDateTime uploadedAt;

    public Dataset() {}
    public Dataset(String name, LocalDateTime uploadedAt) {
        this.name = name;
        this.uploadedAt = uploadedAt;
    }

    public Long getId()               { return id; }
    public String getName()           { return name; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }

    public void setName(String n)           { this.name = n; }
    public void setUploadedAt(LocalDateTime t) { this.uploadedAt = t; }
}
