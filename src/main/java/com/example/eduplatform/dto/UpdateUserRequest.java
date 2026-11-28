package com.example.eduplatform.dto;

import com.example.eduplatform.entity.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

// частичное обновление, null поля не трогаем
public record UpdateUserRequest(
        @Size(max = 100, message = "Имя не должно превышать 100 символов")
        String name,

        @Email(message = "Некорректный формат email")
        @Size(max = 150, message = "Email не должен превышать 150 символов")
        String email,

        UserRole role,

        Boolean active
) {
}
