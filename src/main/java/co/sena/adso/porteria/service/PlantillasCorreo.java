package co.sena.adso.porteria.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

// Todo dato que viene de una persona se escapa: los correos son HTML
@Component
public class PlantillasCorreo {

    private static final String PIE = """
            <div style="background-color: #f4f4f4; padding: 16px; text-align: center; font-size: 12px; color: #777;">
              <p style="margin: 0;">Correo generado automáticamente por el sistema. Por favor, no respondas a este mensaje.</p>
            </div>
            </div>""";

    private final String regional;
    private final String centro;
    private final String url;

    public PlantillasCorreo(@Value("${app.carnet.regional}") String regional, @Value("${app.carnet.centro}") String centro,
                            @Value("${app.correo.url-sistema:http://localhost:5173}") String url) {
        this.regional = e(regional);
        this.centro = e(centro);
        this.url = url.replaceAll("/+$", "");
    }

    public String verificacion(String nombre, String codigo, boolean reenvio) {
        String enlace = url + "/verificar";
        String intro = reenvio ? "Solicitaste un nuevo código para verificar tu correo. Usa el siguiente código:"
                : "Tu cuenta fue creada correctamente en el Sistema de Acceso SENA. Para activar tu acceso, verifica "
                  + "tu correo usando el siguiente código:";
        return cabecera() + """
                <div style="padding: 24px;">
                  <h3>Hola, %s</h3>
                  <p>%s</p>
                  %s
                  <p>Este código vence en 15 minutos.</p>
                  %s
                  <div style="background-color: #f9f9f9; border-left: 4px solid #39A900; padding: 12px; margin-top: 24px; font-size: 13px; color: #555;">
                    <strong>Política de privacidad:</strong> La información enviada en este correo es de uso exclusivo del Sistema de Acceso SENA y será tratada conforme a las políticas institucionales de protección de datos personales. No compartas este código con terceros.
                  </div>
                </div>""".formatted(e(nombre), intro, codigo(codigo), boton(enlace, "Ir a verificar mi correo")) + PIE;
    }

    public String recuperacion(String nombre, String codigo) {
        return cabecera() + """
                <div style="padding: 24px;">
                  <h3 style="margin-top: 0;">Hola, %s</h3>
                  <p>Recibimos una solicitud para restablecer la contraseña de tu cuenta en el Sistema de Acceso. Usa el siguiente código para continuar:</p>
                  %s
                  <p>Este código vence en <strong>15 minutos</strong> y solo admite 5 intentos.</p>
                  <div style="background-color: #fff8f8; border-left: 4px solid #e74c3c; padding: 14px; margin-top: 22px; font-size: 13px; color: #555;">
                    <strong>¿No solicitaste este cambio?</strong> Ignora este mensaje: tu contraseña seguirá siendo la misma. No compartas este código con nadie; el personal del SENA nunca te lo pedirá.
                  </div>
                  <div style="background-color: #f9f9f9; border-left: 4px solid #39A900; padding: 12px; margin-top: 18px; font-size: 13px; color: #555;">
                    <strong>Tratamiento de datos:</strong> tus datos se manejan conforme a la Ley 1581 de 2012 y a la Política de Tratamiento de Datos Personales del Centro.
                  </div>
                </div>""".formatted(e(nombre), codigo(codigo)) + PIE;
    }

    public String bienvenida(String nombre, String correo, String clave) {
        String enlace = url + "/login";
        return """
                <div style="font-family: Arial, sans-serif; color: #333333; max-width: 680px; margin: 0 auto; border: 1px solid #dddddd; border-radius: 10px; overflow: hidden; background-color: #ffffff;">
                <div style="background-color: #ffffff; padding: 24px 24px 16px 24px; text-align: center; border-bottom: 5px solid #39A900;">
                  <div style="font-size: 44px; font-weight: 900; color: #39A900; letter-spacing: 1px; line-height: 1;">SENA</div>
                  <h2 style="color: #39A900; margin: 12px 0 4px 0;">%s</h2>
                  <p style="color: #555555; margin: 0;">Sistema de Acceso y Carnet Digital</p>
                </div>
                <div style="padding: 26px;">
                  <h3>Hola, %s</h3>
                  <p>Para el ingreso a nuestra institución por portería debes contar con un <strong>carnet digital activo</strong>. Para iniciar el proceso, ingresa a la plataforma:</p>
                  %s
                  <p>Estas son tus credenciales de acceso temporal:</p>
                  <div style="background-color: #f5f5f5; padding: 16px; border-radius: 7px; margin-bottom: 22px; border-left: 4px solid #39A900;">
                    <p style="margin: 6px 0;"><strong>Usuario:</strong> %s</p>
                    <p style="margin: 6px 0;"><strong>Contraseña temporal:</strong> %s</p>
                  </div>
                  <h4 style="color: #39A900;">Paso a paso obligatorio</h4>
                  <ol>
                    <li>Inicia sesión con tu usuario y contraseña temporal.</li>
                    <li>Cambia la contraseña temporal apenas ingreses por una personal y segura.</li>
                    <li>Completa tu perfil con toda tu información personal.</li>
                    <li>Carga una fotografía clara y actual de ti. No deben aparecer otras personas, logos, objetos ni paisajes.</li>
                    <li>Si vas a ingresar un computador u otro equipo tecnológico, regístralo en la plataforma antes de llegar a portería.</li>
                  </ol>
                  <div style="background-color: #fff8f8; border-left: 4px solid #e74c3c; padding: 14px; margin-top: 22px; color: #555555;">
                    <strong>Importante:</strong> Si tu perfil no tiene una foto válida de ti, no podrás ingresar a la institución hasta que la fotografía sea corregida y verificada.
                  </div>
                  <div style="background-color: #f9f9f9; border-left: 4px solid #39A900; padding: 12px; margin-top: 22px; font-size: 13px; color: #555555;">
                    <strong>Política de privacidad:</strong> La información registrada se usa únicamente para identificación, control de acceso y seguridad institucional, conforme a la Ley 1581 de 2012.
                  </div>
                </div>""".formatted(centro, e(nombre), boton(enlace, "Ingresar a la plataforma"), e(correo), e(clave)) + PIE;
    }

    public String fotoAprobada(String nombre) {
        return cabecera() + """
                <div style="padding: 24px;">
                  <h3>Hola, %s</h3>
                  <p>Tu foto de perfil fue <strong>aprobada</strong>. Tu carnet digital ya está activo y puedes usarlo para ingresar por portería.</p>
                  %s
                </div>""".formatted(e(nombre), boton(url + "/perfil", "Ver mi carnet")) + PIE;
    }

    public String fotoRechazada(String nombre, String motivo) {
        return cabecera() + """
                <div style="padding: 24px;">
                  <h3>Hola, %s</h3>
                  <p>Tu foto de perfil <strong>no fue aprobada</strong> y tu carnet digital sigue inactivo. Motivo:</p>
                  <div style="background-color:#fff8f8; border-left:4px solid #e74c3c; padding:14px; margin:18px 0;">%s</div>
                  <p>Sube una foto nueva que cumpla los requisitos y vuelve a quedar en revisión.</p>
                  %s
                </div>""".formatted(e(nombre), e(motivo), boton(url + "/perfil", "Subir otra foto")) + PIE;
    }

    public String falloRespaldo(String mes, String motivo) {
        return """
                <h3>El respaldo mensual no se completó</h3>
                <p><b>Mes:</b> %s</p>
                <p><b>Motivo:</b> %s</p>
                <p>No se borró ningún dato de la base: la limpieza se aborta cuando el respaldo no se puede verificar. Revisa el espacio en disco del servidor y los registros de la aplicación.</p>
                """.formatted(e(mes), e(motivo));
    }

    private String cabecera() {
        return """
                <div style="font-family: Arial, sans-serif; color: #333; max-width: 640px; margin: 0 auto; border: 1px solid #ddd; border-radius: 8px; overflow: hidden; background-color: #ffffff;">
                <div style="background-color: #39A900; padding: 24px; text-align: center;">
                  <h2 style="color: white; margin: 0;">SENA - %s</h2>
                  <p style="color: white; margin: 6px 0 0 0;">%s</p>
                </div>""".formatted(regional, centro);
    }

    private static String codigo(String codigo) {
        return """
                <div style="background-color: #f5f5f5; padding: 20px; border-radius: 6px; text-align: center; margin: 22px 0;">
                  <span style="font-size: 28px; font-weight: bold; letter-spacing: 4px; color: #39A900;">%s</span>
                </div>""".formatted(e(codigo));
    }

    private static String boton(String enlace, String texto) {
        String destino = e(enlace);
        return """
                <p style="text-align: center; margin: 28px 0;">
                  <a href="%1$s" style="background-color: #39A900; color: #ffffff; padding: 12px 22px; text-decoration: none; border-radius: 6px; font-weight: bold;">%2$s</a>
                </p>
                <p>Si el botón no funciona, copia y pega este enlace en tu navegador:</p>
                <p style="word-break: break-all;"><a href="%1$s" style="color: #39A900;">%1$s</a></p>""".formatted(destino, texto);
    }

    private static String e(String texto) {
        return texto == null ? "" : HtmlUtils.htmlEscape(texto, "UTF-8");
    }
}
