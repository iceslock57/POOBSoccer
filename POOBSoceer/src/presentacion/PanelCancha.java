package presentacion;

import Domain.*;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Renderiza la cancha, los países, el marcador, el tiempo y las barras de poder. */
public class PanelCancha extends JPanel {

    // Las imágenes del partido. Las posiciones están en píxeles de cada imagen original.
    // Si falta alguna, esa parte se dibuja como antes, con figuras sencillas.

    // Franja de pasto; su borde claro queda justo en el suelo del juego.
    private static final ImagenRecortada CESPED = ImagenRecortada.cargar("recursos/cesped.png",
            new Rectangle(0, 896, 1536, 100), 1536);
    private static final int CESPED_SOBRE_EL_SUELO = 8;

    // El arco completo, y dentro de él lo que mide el arco del juego: desde el fondo de la red
    // hasta el palo de adelante y desde la mitad del travesaño hasta el piso.
    private static final ImagenRecortada ARCO = ImagenRecortada.cargar("recursos/arco.png",
            new Rectangle(40, 300, 910, 1015), 300);
    private static final Rectangle ARCO_DEL_JUEGO = new Rectangle(60, 400, 800, 900);

    // Tablero con las dos cajas de los goles y el letrero de abajo.
    private static final ImagenRecortada TABLERO = ImagenRecortada.cargar("recursos/tablero.png",
            new Rectangle(8, 204, 1517, 601), 760);
    private static final Rectangle GOLES_J1 = new Rectangle(230, 302, 472, 279);
    private static final Rectangle GOLES_J2 = new Rectangle(833, 302, 474, 279);
    private static final Rectangle LETRERO = new Rectangle(185, 633, 1166, 117);

    // Panel de cada jugador con las barras de vida y de poder.
    private static final ImagenRecortada INFO = ImagenRecortada.cargar("recursos/info.png",
            new Rectangle(12, 304, 1509, 393), 760);
    private static final Rectangle BARRA_VIDA = new Rectangle(498, 409, 857, 77);
    private static final Rectangle BARRA_PODER = new Rectangle(500, 541, 863, 81);

    private static final Color AZUL = new Color(35, 105, 225), ROJO = new Color(220, 65, 65);

    private final POOBSoccer dominio;

    public PanelCancha(POOBSoccer d) {
        dominio = d;
        setPreferredSize(new Dimension(1000, 650));
        setBackground(new Color(115, 190, 235));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Partido p = dominio.getPartidoActual();
        if (p == null) return;

        Graphics2D x = (Graphics2D) g.create();

        double sx = getWidth() / (double) p.getCancha().getAncho();
        double sy = getHeight() / (double) p.getCancha().getAlto();

        x.scale(sx, sy);

        dibujarCancha(x, p);
        dibujarJugador(x, p.getJugador1(), AZUL, 1);
        dibujarJugador(x, p.getJugador2(), ROJO, -1);
        dibujarBalones(x, p);
        // Los arcos van encima, así el balón que entra se ve detrás de la red.
        dibujarArcos(x, p);
        dibujarHUD(x, p);

        if (dominio.isPausa()) {
            x.setColor(new Color(0, 0, 0, 150));
            x.fillRect(0, 0, p.getCancha().getAncho(), p.getCancha().getAlto());

            x.setColor(Color.WHITE);
            x.setFont(new Font("Arial", Font.BOLD, 44));
            x.drawString(
                "EN PAUSA",
                p.getCancha().getAncho() / 2 - 115,
                p.getCancha().getAlto() / 2
            );
        }

        if (p.isTerminado()) {
            x.setColor(new Color(0, 0, 0, 180));
            x.fillRoundRect(
                p.getCancha().getAncho() / 2 - 230,
                p.getCancha().getAlto() / 2 - 65,
                460,
                130,
                25,
                25
            );

            x.setColor(Color.WHITE);
            x.setFont(new Font("Arial", Font.BOLD, 30));

            Jugador win = p.getGanador();

            x.drawString(
                win == null ? "EMPATE" : "GANADOR: " + win.getPersonaje().getPais(),
                p.getCancha().getAncho() / 2 - 195,
                p.getCancha().getAlto() / 2
            );

            x.setFont(new Font("Arial", Font.PLAIN, 18));

            x.drawString(
                "Puntos obtenidos: " + (win == null ? 25 : 100),
                p.getCancha().getAncho() / 2 - 100,
                p.getCancha().getAlto() / 2 + 35
            );
        }

        x.dispose();
    }

    private void dibujarCancha(Graphics2D g, Partido p) {
        int w = p.getCancha().getAncho();
        int h = p.getCancha().getAlto();
        int floor = p.getCancha().getSuelo();

        g.setPaint(new GradientPaint(
            0, 0,
            new Color(115, 190, 235),
            0, floor,
            new Color(210, 235, 250)
        ));

        // El fondo es solo el cielo; las líneas de la cancha ya no se dibujan.
        g.fillRect(0, 0, w, h);

        if (CESPED != null) {
            // Debajo va un verde oscuro por si la imagen deja algún hueco.
            g.setColor(new Color(3, 66, 29));
            g.fillRect(0, floor, w, h - floor);
            CESPED.dibujar(g, new Rectangle2D.Double(0, floor - CESPED_SOBRE_EL_SUELO, w,
                    h - floor + CESPED_SOBRE_EL_SUELO), false);
        } else {
            g.setColor(new Color(30, 145, 65));
            g.fillRect(0, floor, w, h - floor);
        }
    }

    /**
     * Dibuja cada arco con su imagen, estirada para que el travesaño, el palo de adelante y el piso
     * caigan justo donde el balón rebota o entra. El arco derecho es el mismo dibujo en espejo.
     */
    private void dibujarArcos(Graphics2D g, Partido p) {
        for (Arco a : new Arco[]{p.getCancha().getArcoIzquierdo(), p.getCancha().getArcoDerecho()}) {
            if (ARCO == null) {
                g.setColor(Color.WHITE);
                g.setStroke(new BasicStroke(7));
                g.drawRect(a.getPosX(), a.getPosY(), a.getAncho(), a.getAlto());
                continue;
            }
            boolean voltear = !a.isEsArcoIzquierdo();
            Rectangle2D lugar = new Rectangle2D.Double(a.getPosX(), a.getPosY(), a.getAncho(), a.getAlto());
            ARCO.dibujar(g, ARCO.ajustarA(ARCO_DEL_JUEGO, lugar, voltear), voltear);
        }
    }

    /**
     * @param direccion 1 si el jugador mira a la derecha (jugador 1), -1 si mira a la izquierda (jugador 2)
     */
    private void dibujarJugador(Graphics2D g, Jugador j, Color color, int direccion) {
        Personaje p = j.getPersonaje();

        int x = (int) p.getPosX();
        int y = (int) p.getPosY();

        // El aura va detrás del personaje para no taparlo.
        if (j.tienePoderPreparado()) EfectosPoder.dibujarAura(g, p, x, y);

        AparienciaPersonaje apariencia = AparienciaPersonaje.de(p.getPais());
        if (apariencia != null) {
            // De 0 a 1 mientras dura la patada; 0 si no está pateando.
            double avancePatada = 1 - p.getFramesPateando() / (double) Personaje.DURACION_PATADA;
            if (!p.isPateando()) avancePatada = 0;
            apariencia.dibujar(g, x, y, direccion, avancePatada, p.isAturdido() || p.isInconsciente());
        } else {
            dibujarFiguraBasica(g, p, x, y, color);
        }

        dibujarIndicadores(g, p, x, y);
    }

    /** Figura de los países que todavía no tienen imágenes: rectángulo de color con ojos. */
    private void dibujarFiguraBasica(Graphics2D g, Personaje p, int x, int y, Color color) {
        g.setColor(p.isAturdido() || p.isInconsciente() ? Color.GRAY : color);
        int w = Personaje.ANCHO, h = Personaje.ALTO;
        g.fillRoundRect(x, y, w, h, 20, 20);

        // Ojos en proporción al tamaño de la caja.
        int ojo = w / 5;
        g.setColor(Color.WHITE);
        g.fillOval(x + w / 5, y + h / 6, ojo, ojo);
        g.fillOval(x + w * 3 / 5, y + h / 6, ojo, ojo);
    }

    /** Nombre del país y los efectos que esté sufriendo el personaje. */
    private void dibujarIndicadores(Graphics2D g, Personaje p, int x, int y) {
        g.setColor(Color.BLACK);
        g.drawString(p.getPais(), Math.max(2, x - 10), y - 10);

        EfectosPoder.dibujarEfectoSobre(g, p, x, y);

        if (p.isAturdido()) {
            g.setColor(Color.WHITE);
            g.drawString("STUN", x, y - 22);
        }
    }

    /**
     * Dibuja el balón real y los falsos de la misma manera. Primero van los efectos y luego los balones
     * ordenados por su altura, para que el real no se reconozca por quedar siempre encima o debajo.
     */
    private void dibujarBalones(Graphics2D g, Partido p) {
        List<Balon> balones = new ArrayList<>(p.getBalonesFalsos());
        balones.add(p.getBalon());
        balones.sort(Comparator.comparingDouble(Balon::getPosY));

        for (Balon b : balones) {
            Poder poder = p.getPoderVisible(b);
            if (poder != null) EfectosPoder.dibujarTiro(g, b, poder);
        }
        for (Balon b : balones) dibujarBalon(g, b);
    }

    private void dibujarBalon(Graphics2D g, Balon b) {
        int r = b.getRadio();
        int x = (int) b.getPosX();
        int y = (int) b.getPosY();

        g.setColor(Color.WHITE);
        g.fillOval(x - r, y - r, r * 2, r * 2);

        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(2));
        g.drawOval(x - r, y - r, r * 2, r * 2);
    }

    /**
     * Arriba van el tablero en el centro y el panel de cada jugador a su lado.
     * Los tres tienen el mismo ancho y se achican si la cancha es angosta.
     */
    private void dibujarHUD(Graphics2D g, Partido p) {
        if (TABLERO == null || INFO == null) {
            dibujarHUDSencillo(g, p);
            return;
        }
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = p.getCancha().getAncho();
        double ancho = Math.min(300, (w - 60) / 3.0), altoInfo = ancho * INFO.altoPorAncho();

        dibujarTablero(g, p, new Rectangle2D.Double((w - ancho) / 2, 4, ancho, ancho * TABLERO.altoPorAncho()));
        dibujarInfo(g, p.getJugador1(), new Rectangle2D.Double(10, 8, ancho, altoInfo), AZUL, false);
        dibujarInfo(g, p.getJugador2(), new Rectangle2D.Double(w - 10 - ancho, 8, ancho, altoInfo), ROJO, true);
    }

    /** Los goles de cada uno en su caja y, en el letrero de abajo, el tiempo, la meta de goles o el gol de oro. */
    private void dibujarTablero(Graphics2D g, Partido p, Rectangle2D lugar) {
        TABLERO.dibujar(g, lugar, false);

        Rectangle2D caja1 = TABLERO.ubicar(GOLES_J1, lugar), caja2 = TABLERO.ubicar(GOLES_J2, lugar);
        escribirCentrado(g, String.valueOf(p.getMarcador()[0]), caja1, new Color(125, 190, 255), caja1.getHeight() * .85);
        escribirCentrado(g, String.valueOf(p.getMarcador()[1]), caja2, new Color(255, 135, 135), caja2.getHeight() * .85);

        Rectangle2D letrero = TABLERO.ubicar(LETRERO, lugar);
        Color color = Color.WHITE;
        String texto = "";
        if (p instanceof PartidoPorTiempo) {
            PartidoPorTiempo t = (PartidoPorTiempo) p;
            // Cuando llega el gol de oro ya no hay reloj: se gana con el siguiente gol.
            if (t.isEsGolDeOro()) {
                texto = "¡GOL DE ORO!";
                color = new Color(255, 200, 40);
            } else {
                texto = "TIEMPO  " + formato(t.getTiempoRestante());
                // En los últimos segundos el reloj se pone rojo.
                if (t.estaPorTerminar()) color = new Color(255, 110, 90);
            }
        } else if (p instanceof PartidoPorGoles) {
            int meta = ((PartidoPorGoles) p).getGolesObjetivo();
            texto = "GANA QUIEN LLEGUE A " + meta + (meta == 1 ? " GOL" : " GOLES");
        }
        escribirCentrado(g, texto, letrero, color, letrero.getHeight() * .72);
    }

    /**
     * Panel de un jugador. Dentro de cada barra se escribe su valor: la vida que le queda
     * y cuánto lleva cargado el poder. Debajo va el nombre con el color de su equipo.
     */
    private void dibujarInfo(Graphics2D g, Jugador j, Rectangle2D lugar, Color equipo, boolean alaDerecha) {
        INFO.dibujar(g, lugar, false);
        Personaje p = j.getPersonaje();

        double vida = p.getVidaActual() / (double) p.getVidaMaxima();
        Rectangle2D barraVida = INFO.ubicar(BARRA_VIDA, lugar);
        llenarBarra(g, barraVida, vida, colorDeVida(vida));
        escribirCentrado(g, p.getVidaActual() + " / " + p.getVidaMaxima(), barraVida, Color.WHITE, barraVida.getHeight() * .8);

        Rectangle2D barraPoder = INFO.ubicar(BARRA_PODER, lugar);
        if (j.tienePoderPreparado()) {
            llenarBarra(g, barraPoder, 1, new Color(215, 75, 255));
            escribirCentrado(g, "¡PODER LISTO!", barraPoder, Color.WHITE, barraPoder.getHeight() * .8);
        } else {
            llenarBarra(g, barraPoder, j.getBarraPoder() / 100.0, new Color(255, 170, 30));
            escribirCentrado(g, j.getBarraPoder() + " %", barraPoder, Color.WHITE, barraPoder.getHeight() * .8);
        }

        dibujarNombre(g, j.getNombreVisible(), lugar, equipo, alaDerecha);
    }

    /** Verde con buena vida, amarillo a media vida y rojo cuando ya casi no le queda. */
    private static Color colorDeVida(double vida) {
        if (vida > .5) return new Color(45, 215, 95);
        if (vida > .25) return new Color(245, 200, 40);
        return new Color(235, 65, 50);
    }

    /** Llena la parte de la barra que corresponde al valor, con un brillo arriba para que se vea con volumen. */
    private static void llenarBarra(Graphics2D g, Rectangle2D barra, double valor, Color color) {
        double lleno = barra.getWidth() * Math.max(0, Math.min(1, valor));
        if (lleno <= 0) return;
        double y = barra.getY(), h = barra.getHeight();
        Graphics2D d = (Graphics2D) g.create();
        d.setPaint(new GradientPaint(0, (float) y, color.brighter(), 0, (float) (y + h), color.darker()));
        d.fill(new java.awt.geom.RoundRectangle2D.Double(barra.getX(), y, lleno, h, h, h));
        d.dispose();
    }

    /** Nombre del jugador en una etiqueta oscura bajo su panel, pegada al lado de la pantalla que le toca. */
    private static void dibujarNombre(Graphics2D g, String nombre, Rectangle2D panel, Color equipo, boolean alaDerecha) {
        Font letra = new Font("Arial", Font.BOLD, 14);
        int ancho = g.getFontMetrics(letra).stringWidth(nombre) + 22;
        double x = alaDerecha ? panel.getMaxX() - ancho - 8 : panel.getX() + 8, y = panel.getMaxY() + 3;

        g.setColor(new Color(4, 14, 55, 215));
        g.fill(new java.awt.geom.RoundRectangle2D.Double(x, y, ancho, 20, 10, 10));
        g.setColor(equipo);
        g.fill(new java.awt.geom.RoundRectangle2D.Double(x, y, 7, 20, 7, 7));
        g.setFont(letra);
        g.setColor(Color.WHITE);
        g.drawString(nombre, (float) (x + 13), (float) (y + 15));
    }

    /**
     * Escribe un texto centrado en una caja, con una sombra para que se lea sobre cualquier fondo.
     * Si no cabe a lo ancho, la letra se achica.
     */
    private static void escribirCentrado(Graphics2D g, String texto, Rectangle2D caja, Color color, double tamano) {
        Font letra = new Font("Arial", Font.BOLD, 12).deriveFont((float) tamano);
        FontMetrics medidas = g.getFontMetrics(letra);
        double cabe = caja.getWidth() * .9;
        if (medidas.stringWidth(texto) > cabe) {
            letra = letra.deriveFont((float) (tamano * cabe / medidas.stringWidth(texto)));
            medidas = g.getFontMetrics(letra);
        }
        float x = (float) (caja.getCenterX() - medidas.stringWidth(texto) / 2.0);
        float y = (float) (caja.getCenterY() + (medidas.getAscent() - medidas.getDescent()) / 2.0);

        g.setFont(letra);
        g.setColor(new Color(0, 0, 0, 170));
        g.drawString(texto, x + 1.5f, y + 1.5f);
        g.setColor(color);
        g.drawString(texto, x, y);
    }

    /** El marcador de antes, con texto y barras simples; se usa si faltan las imágenes del tablero. */
    private void dibujarHUDSencillo(Graphics2D g, Partido p) {
        int w = p.getCancha().getAncho();

        g.setFont(new Font("Arial", Font.BOLD, 24));
        g.setColor(Color.BLACK);
        g.drawString(
            p.getMarcador()[0] + "  -  " + p.getMarcador()[1],
            w / 2 - 40,
            35
        );

        g.setFont(new Font("Arial", Font.BOLD, 15));

        dibujarJugadorHUD(g, p.getJugador1(), 25, 25, Color.BLUE);
        dibujarJugadorHUD(g, p.getJugador2(), w - 225, 25, Color.RED);

        if (p instanceof PartidoPorTiempo) {
            PartidoPorTiempo t = (PartidoPorTiempo) p;

            g.setFont(new Font("Arial", Font.BOLD, 17));

            String reloj = "Tiempo: " + formato(t.getTiempoRestante());
            g.drawString(reloj, w / 2 - 55, 62);

            if (t.isEsGolDeOro()) {
                g.setColor(new Color(180, 20, 20));
                g.setFont(new Font("Arial", Font.BOLD, 22));
                g.drawString("¡GOL DE ORO!", w / 2 - 75, 91);
            }
        }
    }

    private String formato(int s) {
        return String.format("%02d:%02d", s / 60, s % 60);
    }

    private void dibujarJugadorHUD(Graphics2D g, Jugador j, int x, int y, Color color) {
        g.setColor(Color.DARK_GRAY);
        g.drawString(j.getNombreVisible(), x, y);

        g.setColor(new Color(70, 70, 70));
        g.fillRect(x, y + 8, 190, 13);

        g.setColor(color);
        g.fillRect(
            x,
            y + 8,
            (int) (190 * j.getPersonaje().getVidaActual()
            / (double) j.getPersonaje().getVidaMaxima()),
            13
        );

        g.setColor(Color.DARK_GRAY);
        g.drawString(
            "HP " + j.getPersonaje().getVidaActual() + "   Poder",
            x,
            y + 39
        );

        g.setColor(Color.GRAY);
        g.fillRect(x, y + 44, 190, 10);

        g.setColor(Color.ORANGE);
        g.fillRect(
            x,
            y + 44,
            (int) (190 * j.getBarraPoder() / 100.0),
            10
        );

        if (j.tienePoderPreparado()) {
            g.setColor(Color.MAGENTA);
            g.drawString("PODER LISTO", x, y + 72);
        }
    }
}