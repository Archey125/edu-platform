package com.example.eduplatform.dto;

// ответ с категорией
public record CategoryResponse(
        Long id,
        String name,
        String description
) {
}
