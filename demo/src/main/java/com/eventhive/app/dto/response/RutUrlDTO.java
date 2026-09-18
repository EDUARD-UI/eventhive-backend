package com.eventhive.app.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class RutUrlDTO {
    private String url;
    private long expiresInSeconds;
}
