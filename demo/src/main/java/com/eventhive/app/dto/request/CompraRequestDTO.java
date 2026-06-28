package com.eventhive.app.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CompraRequestDTO {

    @NotEmpty(message = "La lista de ítems no puede estar vacía")
    @Valid
    private List<ItemRequest> items;

    @Getter
    @Setter
    public static class ItemRequest {

        @NotNull(message = "La localidad es requerida")
        private Long localidadId;

        @NotNull(message = "La cantidad es requerida")
        @Min(value = 1, message = "La cantidad mínima es 1")
        private Integer cantidad;
    }
}