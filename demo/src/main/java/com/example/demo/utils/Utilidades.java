package com.example.demo.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

import org.springframework.web.multipart.MultipartFile;

import com.example.demo.exception.BusinessException;

public class Utilidades {

    public static void validarFoto(MultipartFile foto) {
        if (foto.getSize() > 5 * 1024 * 1024)
            throw new BusinessException("La foto no puede superar los 5MB");
        String ct = foto.getContentType();
        if (ct == null || !ct.startsWith("image/"))
            throw new BusinessException("Solo se permiten archivos de imagen");
    }

    public static String guardarFoto(MultipartFile foto, String uploadPath) throws IOException {
        Path dir = Paths.get(uploadPath);
        if (!Files.exists(dir)) Files.createDirectories(dir);
        String ext = "";
        String original = foto.getOriginalFilename();
        if (original != null && original.contains("."))
            ext = original.substring(original.lastIndexOf("."));
        String fileName = UUID.randomUUID() + ext;
        Files.copy(foto.getInputStream(), dir.resolve(fileName), StandardCopyOption.REPLACE_EXISTING);
        return fileName;
    }

    public static void eliminarFoto(String nombreFoto, String uploadPath) {
        if (nombreFoto != null && !nombreFoto.isBlank()) {
            try { Files.deleteIfExists(Paths.get(uploadPath).resolve(nombreFoto)); }
            catch (IOException e) { System.err.println("Error al eliminar foto: " + e.getMessage()); }
        }
    }
}