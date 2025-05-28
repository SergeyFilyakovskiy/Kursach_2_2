// risk-server/src/main/java/com/risk/server/dto/DatasetDto.java
package com.risk.server.dto;

public record DatasetDto(
        Long   id,
        String name,
        String uploadedAt   // ISO-строка, как у клиента
) {}
