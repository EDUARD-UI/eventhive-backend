package com.eventhive.app.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import com.eventhive.app.exception.CorreoException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;

@Component
public class GeneradorCodigoQR {

    private static final int TAMANO = 300;

    public byte[] generarPng(String codigo) {
        if (!StringUtils.hasText(codigo)) {
            throw new CorreoException("No se puede generar el QR de un boleto sin código");
        }

        try {
            var matriz = new QRCodeWriter().encode(codigo, BarcodeFormat.QR_CODE, TAMANO, TAMANO);
            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matriz, "PNG", salida);
            return salida.toByteArray();
        } catch (WriterException | IOException e) {
            throw new CorreoException("No se pudo generar el código QR del boleto", e);
        }
    }
}
