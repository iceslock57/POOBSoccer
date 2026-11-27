package presentacion;

import Domain.Personaje;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * Imágenes de un país (cabeza y zapato) y cómo dibujarlas dentro de la caja del personaje
 * (Personaje.ANCHO x Personaje.ALTO).
 * Los países sin imágenes registradas se siguen dibujando con la figura básica de PanelCancha.
 *
 * Para agregar un país: una línea más en REGISTRO, indicando hacia dónde mira cada imagen original.
 */
final class AparienciaPersonaje {

    // Caja del personaje en el dominio.
    private static final int ANCHO = Personaje.ANCHO, ALTO = Personaje.ALTO;
    // Tamaño con el que se dibuja cada pieza: la cabeza arriba y el zapato abajo, ocupando todo el alto de la caja.
    private static final int ANCHO_CABEZA = 60, ALTO_CABEZA = 55;
    // El zapato es grande para que se note con qué parte se le pega al balón; su parte de arriba queda bajo la cabeza.
    private static final int ANCHO_ZAPATO = 46, ALTO_ZAPATO = ALTO - ALTO_CABEZA + 8;
    // Lo que avanza el pie en la mitad de la patada.
    private static final int AVANCE_PATADA = 16;
    // Ángulo máximo que sube la punta del zapato a mitad de la patada.
    private static final double ANGULO_PATADA = Math.toRadians(65);

    // País -> apariencia. Las imágenes se cargan una sola vez, la primera vez que se piden.
    private static final Map<String, AparienciaPersonaje> REGISTRO = new HashMap<>();

    static {
        // Cada booleano dice hacia dónde mira la imagen ORIGINAL (true = derecha). No depende del jugador:
        registrar("Corea del Sur", "recursos/cabeza_corea.png", true, "recursos/zapato_corea.png", false);
        registrar("Camerún", "recursos/cabeza_camerun.png", true, "recursos/zapato_camerun.png", true);
        registrar("Rusia", "recursos/cabeza_rusia.png", true, "recursos/zapato_rusia.png", false);
        registrar("Japón", "recursos/cabeza_japon.png", true, "recursos/zapato_japon.png", false);
        registrar("Estados Unidos", "recursos/cabeza_estadosunidos.png", true, "recursos/zapato_estadosunidos.png", true);
        registrar("Nigeria", "recursos/cabeza_nigeria.png", true, "recursos/zapato_nigeria.png", true);
    }

    private final Image cabeza, zapato;
    private final boolean cabezaMiraDerecha, zapatoMiraDerecha;

    private AparienciaPersonaje(Image cabeza, boolean cabezaMiraDerecha, Image zapato, boolean zapatoMiraDerecha) {
        this.cabeza = cabeza;
        this.zapato = zapato;
        this.cabezaMiraDerecha = cabezaMiraDerecha;
        this.zapatoMiraDerecha = zapatoMiraDerecha;
    }

    private static void registrar(String pais, String rutaCabeza, boolean cabezaDerecha, String rutaZapato, boolean zapatoDerecha) {
        Image cabeza = EstiloGUI.cargarImagen(rutaCabeza);
        Image zapato = EstiloGUI.cargarImagen(rutaZapato);
        // Si falta alguna imagen, el país se dibuja con la figura básica en vez de fallar.
        if (cabeza != null && zapato != null) {
            REGISTRO.put(pais, new AparienciaPersonaje(cabeza, cabezaDerecha, zapato, zapatoDerecha));
        }
    }

    /** Apariencia del país, o null si no tiene imágenes. */
    static AparienciaPersonaje de(String pais) {
        return REGISTRO.get(pais);
    }

    /**
     * Dibuja cabeza y zapato dentro de la caja del personaje.
     *
     * @param x, y          esquina superior izquierda de la caja (posición del dominio)
     * @param direccion     1 si mira a la derecha (jugador 1), -1 si mira a la izquierda (jugador 2)
     * @param avancePatada  0 si no patea; de 0 a 1 mientras dura la patada
     * @param atenuado      true para dibujarlo semitransparente (por ejemplo, aturdido)
     */
    void dibujar(Graphics2D g, int x, int y, int direccion, double avancePatada, boolean atenuado) {
        Graphics2D d = (Graphics2D) g.create();
        d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        if (atenuado) d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, .45f));

        dibujarZapato(d, x, y, direccion, avancePatada);

        // Cabeza centrada arriba; puede sobresalir un poco a los lados de la caja.
        int cx = x + ANCHO / 2 - ANCHO_CABEZA / 2;
        dibujarOrientada(d, cabeza, cx, y, ANCHO_CABEZA, ALTO_CABEZA, cabezaMiraDerecha, direccion);
        d.dispose();
    }

    private void dibujarZapato(Graphics2D g, int x, int y, int direccion, double avance) {
        // La patada sube la punta y la vuelve a bajar: el máximo es a mitad del movimiento.
        double curva = Math.sin(avance * Math.PI);
        // Punto de giro: el talón, abajo y hacia atrás del centro de la caja.
        double talonX = x + ANCHO / 2.0 - direccion * 8;
        double talonY = y + ALTO - 5;

        Graphics2D z = (Graphics2D) g.create();
        z.translate(talonX + direccion * curva * AVANCE_PATADA, talonY);  // el pie avanza al patear
        z.rotate(-direccion * curva * ANGULO_PATADA);           // y la punta sube hacia adelante
        // Coordenadas relativas al talón: el zapato se extiende hacia adelante (direccion).
        int zx = direccion > 0 ? -8 : 8 - ANCHO_ZAPATO;
        dibujarOrientada(z, zapato, zx, -ALTO_ZAPATO + 5, ANCHO_ZAPATO, ALTO_ZAPATO, zapatoMiraDerecha, direccion);
        z.dispose();
    }

    /** Dibuja la imagen volteándola horizontalmente si mira al lado contrario de la dirección pedida. */
    private static void dibujarOrientada(Graphics2D g, Image img, int x, int y, int w, int h, boolean miraDerecha, int direccion) {
        int iw = img.getWidth(null), ih = img.getHeight(null);
        boolean voltear = miraDerecha != (direccion > 0);
        if (voltear) g.drawImage(img, x, y, x + w, y + h, iw, 0, 0, ih, null);
        else g.drawImage(img, x, y, x + w, y + h, 0, 0, iw, ih, null);
    }
}
