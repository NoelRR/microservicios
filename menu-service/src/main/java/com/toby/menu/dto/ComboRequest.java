package com.toby.menu.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;
import java.util.Set;

public record ComboRequest(
        @NotBlank(message = "El nombre es obligatorio")
        String nombre,

        String descripcion,

        @NotNull(message = "El precio es obligatorio")
        @DecimalMin(value = "0.0", inclusive = false, message = "El precio debe ser mayor a 0")
        BigDecimal precio,

        @NotEmpty(message = "El combo debe tener al menos un plato")
        Set<Long> platoIds
) {
}
