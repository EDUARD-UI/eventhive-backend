package com.eventhive.app.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;

import com.eventhive.app.exception.CorreoException;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

class GeneradorCodigoQRTest {

    private final GeneradorCodigoQR generador = new GeneradorCodigoQR();

    @Test
    void generaUnQrQueDecodificaAlCodigoOriginalDelBoleto() throws Exception {
        String codigo = "5f11d688-aefc-4b99-8ee3-98668c7e1f5d";

        byte[] png = generador.generarPng(codigo);
        var imagen = ImageIO.read(new ByteArrayInputStream(png));
        var bitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(imagen)));

        assertTrue(png.length > 0);
        assertEquals(codigo, new MultiFormatReader().decode(bitmap).getText());
    }

    @Test
    void rechazaGeneracionSinCodigoDeBoleto() {
        assertThrows(CorreoException.class, () -> generador.generarPng(" "));
    }
}
