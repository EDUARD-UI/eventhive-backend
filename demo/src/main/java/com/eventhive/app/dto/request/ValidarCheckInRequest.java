package com.eventhive.app.dto.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ValidarCheckInRequest {
    private String codigoQR;
    private String codigo;
    private String qr;
}
