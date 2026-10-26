package com.eventhive.app.utils;

import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class CorreoPlantillas {

    public record ItemPago(String evento, String localidad, int cantidad, String subtotal) {}

    public record Boleto(String evento, String fecha, String hora, String lugar,
                         String localidad, String codigoQR) {}

    // ---------- Paleta (alineada con panel-theme.css) ----------

    private static final String FONDO       = "#f4f6fa";
    private static final String TARJETA     = "#ffffff";
    private static final String BORDE       = "#e2e8f0";
    private static final String TEXTO       = "#172033";
    private static final String TEXTO_SUAVE = "#64748b";
    private static final String MARCA       = "#087fea";
    private static final String OSCURO      = "#131b2e";
    private static final String AMBAR       = "#FFC107";
    private static final String LINEA       = "#cbd5e1";
    private static final String FUENTE      = "'Segoe UI',Helvetica,Arial,sans-serif";

    /** Identificador del adjunto en línea del QR de cada boleto (cid:). */
    public static String cidQr(int indice) {
        return "qr-boleto-" + indice;
    }

    // ---------- Correos del sistema ----------

    public String pagoConfirmado(String nombre, Long compraId, String fecha,
                                 String total, String metodoPago, List<ItemPago> items) {
        StringBuilder filas = new StringBuilder();
        for (ItemPago i : items) {
            filas.append("<tr>")
                    .append(celda(esc(i.evento()), "left", true))
                    .append(celda(esc(i.localidad()), "left", false))
                    .append(celda(String.valueOf(i.cantidad()), "center", false))
                    .append(celda("$" + esc(i.subtotal()), "right", false))
                    .append("</tr>");
        }

        String tabla =
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"border-collapse:collapse;margin-top:20px;font-size:14px;\">"
                + "<thead><tr>"
                + encabezado("Evento", "left")
                + encabezado("Localidad", "left")
                + encabezado("Cant.", "center")
                + encabezado("Subtotal", "right")
                + "</tr></thead><tbody>" + filas + "</tbody></table>";

        String totalBloque =
                "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin-top:16px;\"><tr>"
                + "<td style=\"background:" + OSCURO + ";border-radius:8px;padding:16px 20px;"
                + "color:#ffffff;font-size:14px;\">Total pagado</td>"
                + "<td align=\"right\" style=\"background:" + OSCURO + ";border-radius:8px;padding:16px 20px;"
                + "color:" + AMBAR + ";font-size:20px;font-weight:700;\">$" + esc(total) + "</td>"
                + "</tr></table>";

        String detalle = tarjeta(
                fila("Fecha", esc(fecha))
                + fila("Método de pago", esc(metodoPago)));

        return base("¡Pago confirmado!",
                "Hola " + esc(nombre) + ", tu pago fue procesado con éxito. "
                        + "Los boletos te llegan en un correo aparte.",
                tabla + totalBloque + detalle);
    }

    public String boletos(String nombre, Long compraId, List<Boleto> boletos) {
        StringBuilder tickets = new StringBuilder();
        for (int n = 0; n < boletos.size(); n++) {
            tickets.append(ticket(boletos.get(n), n, n == 0, boletos.size()));
        }
        return marcoTicket(tickets
                + "<p style=\"margin:24px 0 0;text-align:center;color:" + TEXTO + ";"
                + "font-size:15px;font-weight:600;\">Presenta el código QR en el ingreso.</p>");
    }

    public String invitacionRol(String rol, String enlace, int horasVigencia) {
        return invitacionRol(rol, null, enlace, horasVigencia);
    }

    public String invitacionRol(String rol, String organizacionNombre, String enlace, int horasVigencia) {
        String mensajeRol;
        if (organizacionNombre != null && !organizacionNombre.isBlank()) {
            mensajeRol = "Recibiste una invitación para unirte a <strong>" + esc(organizacionNombre)
                    + "</strong> en EventHive con el rol <strong>" + esc(rol) + "</strong>.";
        } else {
            mensajeRol = "Recibiste una invitación para crear una cuenta EventHive con el rol <strong>"
                    + esc(rol) + "</strong>.";
        }
        return base("Completa tu registro",
                mensajeRol,
                boton("Aceptar invitación", enlace)
                + nota("El enlace es de un solo uso y vence en " + horasVigencia + " horas. "
                        + "Si no solicitaste esta invitación, puedes ignorar este correo."));
    }

    public String recuperarClave(String nombre, String enlace, long minutosVigencia) {
        return base("Restablecer contraseña",
                "Hola " + esc(nombre) + ", recibimos una solicitud para restablecer tu contraseña.",
                boton("Crear nueva contraseña", enlace)
                + nota("El enlace vence en " + minutosVigencia + " minutos. "
                        + "Si no fuiste tú, ignora este correo: tu contraseña actual sigue siendo válida."));
    }

    public String promocionConfirmada(String nombre, String eventoTitulo, String planNombre, String precio) {
        String contenido = tarjeta(
                fila("Evento", esc(eventoTitulo))
                + fila("Plan contratado", esc(planNombre))
                + fila("Importe pagado", "$" + esc(precio) + " COP"))
                + nota("Tu evento ya se encuentra registrado con promoción activa y estado Destacado.");

        return base("¡Promoción confirmada!",
                "Hola " + esc(nombre) + ", el pago del plan publicitario para tu evento fue procesado con éxito.",
                contenido);
    }

    public String correoAdministrativo(String remitenteAdmin, String mensajeContenido) {
        String mensaje =
                "<div style=\"border-left:4px solid " + AMBAR + ";background:" + FONDO + ";"
                + "border-radius:0 8px 8px 0;padding:16px 20px;margin-top:20px;\">"
                + "<p style=\"margin:0;white-space:pre-wrap;line-height:1.6;color:" + TEXTO + ";font-size:15px;\">"
                + esc(mensajeContenido) + "</p></div>";

        return base("Comunicado oficial",
                "Has recibido un mensaje directo del equipo de administración de EventHive.",
                mensaje
                + nota("Comunicación iniciada por el administrador: <strong>" + esc(remitenteAdmin) + "</strong>"));
    }

    // ---------- Piezas reutilizables ----------

    private String base(String titulo, String introduccion, String contenido) {
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:" + FONDO + ";font-family:" + FUENTE + ";\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:" + FONDO + ";padding:24px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"width:100%;max-width:600px;background:" + TARJETA + ";border-radius:12px;"
                + "overflow:hidden;border:1px solid " + BORDE + ";\">"
                // cabecera
                + "<tr><td style=\"background:" + OSCURO + ";padding:28px 32px 24px;\">"
                + "<div style=\"color:#ffffff;font-size:13px;font-weight:600;letter-spacing:2px;"
                + "text-transform:uppercase;\">EventHive</div>"
                + "<h1 style=\"margin:10px 0 0;color:" + AMBAR + ";font-size:26px;line-height:1.25;"
                + "font-weight:700;\">" + esc(titulo) + "</h1></td></tr>"
                + "<tr><td style=\"background:" + AMBAR + ";height:4px;line-height:4px;font-size:0;\">&nbsp;</td></tr>"
                // cuerpo
                + "<tr><td style=\"padding:32px;color:" + TEXTO + ";\">"
                + "<p style=\"margin:0;font-size:16px;line-height:1.6;color:" + TEXTO + ";\">" + introduccion + "</p>"
                + contenido
                + "</td></tr>"
                // pie
                + "<tr><td style=\"background:" + FONDO + ";padding:20px 32px;border-top:1px solid " + BORDE + ";\">"
                + "<p style=\"margin:0;color:" + TEXTO_SUAVE + ";font-size:12px;line-height:1.5;\">"
                + "Correo automático de EventHive. Por favor, no respondas a este mensaje.</p>"
                + "</td></tr>"
                + "</table></td></tr></table></body></html>";
    }

    // ---------- Piezas del boleto ----------

    /** Marco del correo de boletos: fondo de la app y columna angosta tipo tiquete. */
    private String marcoTicket(String contenido) {
        return "<!DOCTYPE html><html lang=\"es\"><head><meta charset=\"UTF-8\">"
                + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
                + "<body style=\"margin:0;padding:0;background:" + FONDO + ";font-family:" + FUENTE + ";\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:" + FONDO + ";padding:24px 12px;\"><tr><td align=\"center\">"
                + "<table role=\"presentation\" width=\"420\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"width:100%;max-width:420px;\"><tr><td>"
                + contenido
                + "<p style=\"margin:20px 0 0;text-align:center;color:" + TEXTO_SUAVE + ";font-size:12px;\">"
                + "Correo automático de EventHive. Por favor, no respondas a este mensaje.</p>"
                + "</td></tr></table></td></tr></table></body></html>";
    }

    /** Un boleto completo: bienvenida (solo el primero), QR y datos. */
    private String ticket(Boleto b, int indice, boolean conBienvenida, int total) {
        StringBuilder filas = new StringBuilder();
        if (conBienvenida) {
            filas.append(bienvenidaTicket(total)).append(separadorTicket());
        }
        filas.append(qrTicket(indice)).append(separadorTicket()).append(datosTicket(b));

        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin-top:" + (indice == 0 ? "0" : "20px") + ";background:" + TARJETA + ";"
                + "border:1px solid " + BORDE + ";border-radius:16px;overflow:hidden;\">"
                + filas + "</table>";
    }

    private String bienvenidaTicket(int total) {
        String subtitulo = total == 1
                ? "Tu boleto fue emitido con éxito"
                : "Tus boletos fueron emitidos con éxito";
        return "<tr><td align=\"center\" style=\"background:" + OSCURO + ";padding:32px 24px 28px;\">"
                + "<div style=\"font-size:44px;line-height:48px;\">&#127881;</div>"
                + "<h1 style=\"margin:12px 0 6px;color:" + AMBAR + ";font-size:28px;font-weight:700;\">¡Gracias!</h1>"
                + "<p style=\"margin:0;color:#cbd5e1;font-size:14px;line-height:1.5;\">" + subtitulo + "</p>"
                + "</td></tr>";
    }

    /** Línea punteada con muescas laterales, como el corte de un tiquete. */
    private String separadorTicket() {
        String muesca = "width:12px;height:24px;background:" + FONDO + ";font-size:0;line-height:0;";
        return "<tr><td style=\"padding:0;background:" + TARJETA + ";\">"
                + "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>"
                + "<td width=\"12\" style=\"" + muesca + "border-radius:0 12px 12px 0;\">&nbsp;</td>"
                + "<td style=\"padding:0 8px;\"><div style=\"border-top:2px dashed " + LINEA + ";"
                + "font-size:0;line-height:0;\">&nbsp;</div></td>"
                + "<td width=\"12\" style=\"" + muesca + "border-radius:12px 0 0 12px;\">&nbsp;</td>"
                + "</tr></table></td></tr>";
    }

    private String qrTicket(int indice) {
        return "<tr><td align=\"center\" style=\"padding:24px;\">"
                + "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"border:1px solid " + BORDE + ";border-radius:12px;\"><tr>"
                + "<td style=\"padding:12px;\">"
                + "<img src=\"cid:" + cidQr(indice) + "\" width=\"180\" height=\"180\" "
                + "alt=\"Código QR del boleto\" style=\"display:block;width:180px;height:180px;\">"
                + "</td></tr></table></td></tr>";
    }

    private String datosTicket(Boleto b) {
        String localidad =
                "<div style=\"background:" + FONDO + ";border-left:4px solid " + AMBAR + ";"
                + "border-radius:0 8px 8px 0;padding:12px 16px;\">"
                + campo("Localidad", esc(b.localidad()), "0") + "</div>";

        return "<tr><td style=\"padding:8px 24px 28px;\">"
                + "<h2 style=\"margin:0 0 18px;color:" + TEXTO + ";font-size:19px;line-height:1.3;\">"
                + esc(b.evento()) + "</h2>"
                + parCampos(campo("Fecha", esc(b.fecha()), "16px"), campo("Hora", esc(b.hora()), "16px"))
                + campo("Lugar", esc(b.lugar()), "16px")
                + localidad
                + "</td></tr>";
    }

    /** Etiqueta pequeña en mayúsculas sobre el valor, como en el diseño de referencia. */
    private String campo(String etiqueta, String valor, String margenInferior) {
        return "<p style=\"margin:0 0 2px;color:" + TEXTO_SUAVE + ";font-size:11px;letter-spacing:1px;"
                + "text-transform:uppercase;\">" + etiqueta + "</p>"
                + "<p style=\"margin:0 0 " + margenInferior + ";color:" + TEXTO + ";font-size:15px;"
                + "font-weight:600;\">" + valor + "</p>";
    }

    private String parCampos(String izquierda, String derecha) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>"
                + "<td width=\"50%\" valign=\"top\">" + izquierda + "</td>"
                + "<td width=\"50%\" valign=\"top\">" + derecha + "</td>"
                + "</tr></table>";
    }

    private String boton(String texto, String enlace) {
        return "<table role=\"presentation\" cellpadding=\"0\" cellspacing=\"0\" style=\"margin:28px 0 8px;\"><tr>"
                + "<td style=\"background:" + AMBAR + ";border-radius:8px;\">"
                + "<a href=\"" + esc(enlace) + "\" style=\"display:inline-block;padding:14px 28px;"
                + "color:" + OSCURO + ";font-size:15px;font-weight:700;text-decoration:none;\">"
                + esc(texto) + "</a></td></tr></table>";
    }

    private String tarjeta(String filas) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin-top:20px;background:" + FONDO + ";border:1px solid " + BORDE + ";"
                + "border-radius:8px;\"><tr><td style=\"padding:12px 20px;\">" + filas + "</td></tr></table>";
    }

    private String fila(String etiqueta, String valor) {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\"><tr>"
                + "<td style=\"padding:5px 0;color:" + TEXTO_SUAVE + ";font-size:13px;width:38%;\">"
                + etiqueta + "</td>"
                + "<td style=\"padding:5px 0;color:" + TEXTO + ";font-size:14px;font-weight:600;\">"
                + valor + "</td></tr></table>";
    }

    private String nota(String texto) {
        return "<p style=\"margin:20px 0 0;color:" + TEXTO_SUAVE + ";font-size:13px;line-height:1.6;\">"
                + texto + "</p>";
    }

    private String encabezado(String texto, String alineacion) {
        return "<th align=\"" + alineacion + "\" style=\"padding:10px 8px;background:" + FONDO + ";"
                + "color:" + TEXTO_SUAVE + ";font-size:12px;text-transform:uppercase;letter-spacing:.5px;"
                + "border-bottom:2px solid " + AMBAR + ";\">" + texto + "</th>";
    }

    private String celda(String valor, String alineacion, boolean destacada) {
        return "<td align=\"" + alineacion + "\" style=\"padding:12px 8px;border-bottom:1px solid " + BORDE + ";"
                + "color:" + TEXTO + ";" + (destacada ? "font-weight:600;" : "") + "\">" + valor + "</td>";
    }

    private static String esc(String valor) {
        if (valor == null) return "";
        return valor.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
