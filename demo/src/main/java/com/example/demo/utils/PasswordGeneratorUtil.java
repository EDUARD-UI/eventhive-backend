package com.example.demo.utils;

import java.security.SecureRandom;

public class PasswordGeneratorUtil {

    private static final String CHARS =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789!@#$%&*?";

    private static final SecureRandom RANDOM = new SecureRandom();

    // Genera una contraseña aleatoria segura de la longitud indicada
    public static String generar(int longitud) {
        StringBuilder sb = new StringBuilder(longitud);
        for (int i = 0; i < longitud; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}