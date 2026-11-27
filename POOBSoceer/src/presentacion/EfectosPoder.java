package presentacion;

import Domain.*;
import java.awt.*;
import java.awt.geom.Path2D;
import java.util.HashMap;
import java.util.Map;

/**
 * Dibuja todo lo que se ve de los poderes: el aura de quien lo tiene listo, el tiro y el efecto sobre el rival.
 * Un poder que no esté registrado aquí se dibuja con un brillo amarillo.
 */
final class EfectosPoder {

    private static final Color COLOR_POR_DEFECTO = Color.YELLOW;
    private static final Color AZUL = new Color(60, 170, 255);
    private static final Color ELECTRICO = new Color(255, 235, 60);
    private static final Color VIENTO = new Color(150, 165, 175);
    private static final Color ILUSION = new Color(215, 80, 200);
    private static final Color HIELO = new Color(150, 230, 255);
    private static final Color FUEGO = new Color(255, 105, 25);
    private static final Color BRASA = new Color(255, 220, 80);
    private static final Color POLVO = new Color(165, 130, 85);
    private static final Color HOJA = new Color(70, 150, 60);

    /** Forma de dibujar un tiro alrededor del balón. */
    private interface Tiro {
        void dibujar(Graphics2D g, Balon b, long tiempo);
    }

    /** Forma de animar el aura alrededor de un personaje. */
    private interface Aura {
        void dibujar(Graphics2D g, int x, int y, long tiempo);
    }

    private static final Map<Class<?>, Color> COLORES = new HashMap<>();
    private static final Map<Class<?>, Aura> AURAS = new HashMap<>();
    private static final Map<Class<?>, Tiro> TIROS = new HashMap<>();

    static {
        COLORES.put(PoderAuraAzul.class, AZUL);
        COLORES.put(PoderRayo.class, ELECTRICO);
        COLORES.put(PoderTornado.class, VIENTO);
        COLORES.put(PoderIlusion.class, ILUSION);
        COLORES.put(PoderHielo.class, HIELO);
        COLORES.put(PoderFuego.class, FUEGO);

        // Los poderes que no estén aquí muestran solo el brillo de su color.
        AURAS.put(PoderAuraAzul.class, EfectosPoder::dibujarEnergia);
        AURAS.put(PoderRayo.class, EfectosPoder::dibujarTormenta);
        AURAS.put(PoderTornado.class, EfectosPoder::dibujarBrisa);
        AURAS.put(PoderIlusion.class, EfectosPoder::dibujarEspejismo);
        AURAS.put(PoderHielo.class, EfectosPoder::dibujarNevada);
        AURAS.put(PoderFuego.class, EfectosPoder::dibujarHoguera);

        TIROS.put(PoderAuraAzul.class, EfectosPoder::dibujarCometa);
        TIROS.put(PoderRayo.class, EfectosPoder::dibujarEsferaElectrica);
        TIROS.put(PoderTornado.class, EfectosPoder::dibujarTornado);
        TIROS.put(PoderHielo.class, EfectosPoder::dibujarBloqueDeHielo);
        TIROS.put(PoderFuego.class, EfectosPoder::dibujarOlaDeFuego);
        TIROS.put(PoderIlusion.class, EfectosPoder::dibujarDestellos);
    }

    private EfectosPoder() {
    }

    static Color colorDe(Poder poder) {
        return poder == null ? COLOR_POR_DEFECTO : COLORES.getOrDefault(poder.getClass(), COLOR_POR_DEFECTO);
    }

    /** Brillo alrededor del personaje que ya tiene su poder listo. */
    static void dibujarAura(Graphics2D g, Personaje p, int x, int y) {
        Color color = colorDe(p.getPoderEspecial());
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 70));
        g.fillOval(x - 13, y - 13, Personaje.ANCHO + 26, Personaje.ALTO + 26);
        g.setColor(color);
        g.setStroke(new BasicStroke(4));
        g.drawOval(x - 9, y - 9, Personaje.ANCHO + 18, Personaje.ALTO + 18);

        Aura aura = p.getPoderEspecial() == null ? null : AURAS.get(p.getPoderEspecial().getClass());
        if (aura != null) aura.dibujar(g, x, y, System.currentTimeMillis());
    }

    /** Adorno del balón cuando lleva un poder. Se dibuja detrás del balón. */
    static void dibujarTiro(Graphics2D g, Balon b, Poder poder) {
        dibujarTiro(g, b, poder, System.currentTimeMillis());
    }

    /** Igual que el anterior, pero en el instante de la animación que se indique. */
    static void dibujarTiro(Graphics2D g, Balon b, Poder poder, long tiempo) {
        Tiro tiro = TIROS.get(poder.getClass());
        if (tiro == null) {
            dibujarBrillo(g, b, colorDe(poder));
        } else {
            tiro.dibujar(g, b, tiempo);
        }
    }

    /** Muestra el efecto que está sufriendo el personaje. */
    static void dibujarEfectoSobre(Graphics2D g, Personaje p, int x, int y) {
        long tiempo = System.currentTimeMillis();

        if (p.isEmpujado()) {
            dibujarEmpuje(g, x, y, tiempo);
            etiqueta(g, "EMPUJADO", AZUL, x, y);
        }
        if (p.isElectrificado()) {
            dibujarDescarga(g, x, y, tiempo);
            etiqueta(g, "ELECTROCUTADO", ELECTRICO, x, y);
        }
        if (p.isInconsciente()) {
            dibujarTorbellino(g, x, y, tiempo);
            etiqueta(g, "INCONSCIENTE", Color.WHITE, x, y);
        }
        if (p.isConfundido()) {
            dibujarDudas(g, x, y, tiempo);
            etiqueta(g, "¿CUÁL ES?", ILUSION, x, y);
        }
        if (p.isCongelado()) {
            dibujarPrisionDeHielo(g, x, y, tiempo);
            etiqueta(g, "CONGELADO", HIELO, x, y);
        }
        if (p.isQuemado()) {
            dibujarCuerpoEnLlamas(g, x, y, tiempo);
            etiqueta(g, "QUEMÁNDOSE", BRASA, x, y);
        }
    }

    private static void dibujarBrillo(Graphics2D g, Balon b, Color color) {
        int r = b.getRadio() + 7;
        g.setColor(color);
        g.fillOval((int) b.getPosX() - r, (int) b.getPosY() - r, r * 2, r * 2);
    }

    /** Ondas de energía que se abren y chispas azules que suben alrededor de Corea. */
    private static void dibujarEnergia(Graphics2D g, int x, int y, long tiempo) {
        g.setStroke(new BasicStroke(3));
        for (int i = 0; i < 3; i++) {
            double fase = parte(tiempo, 900, i / 3.0);
            int margen = (int) (6 + fase * 32);
            g.setColor(transparente(AZUL, (int) (210 * (1 - fase))));
            g.drawOval(x - margen, y - margen, Personaje.ANCHO + margen * 2, Personaje.ALTO + margen * 2);
        }
        for (int i = 0; i < 9; i++) {
            double fase = parte(tiempo, 700, i * .37);
            int px = x - 12 + i * 37 % (Personaje.ANCHO + 24);
            int py = (int) (y + Personaje.ALTO - fase * (Personaje.ALTO + 30));
            g.setColor(transparente(Color.WHITE, (int) (230 * (1 - fase))));
            g.fillOval(px, py, 5, 11);
        }
    }

    /** Cometa azul de Corea: una punta de luz con anillos que se van quedando atrás. */
    private static void dibujarCometa(Graphics2D g, Balon b, long tiempo) {
        int atras = b.getVelX() >= 0 ? -1 : 1;
        double x = b.getPosX(), y = b.getPosY();
        int r = b.getRadio();
        for (int i = 0; i < 3; i++) {
            Path2D cola = new Path2D.Double();
            double alto = r + 12 - i * 6;
            cola.moveTo(x, y - alto);
            cola.lineTo(x + atras * (125 - i * 25 + Math.sin(tiempo / 50.0 + i) * 8), y);
            cola.lineTo(x, y + alto);
            cola.closePath();
            g.setColor(transparente(i == 2 ? Color.WHITE : AZUL, 90 + i * 45));
            g.fill(cola);
        }
        g.setStroke(new BasicStroke(3));
        for (int i = 0; i < 3; i++) {
            double fase = parte(tiempo, 450, i / 3.0);
            int alto = (int) (22 + fase * 46);
            g.setColor(transparente(AZUL, (int) (220 * (1 - fase))));
            g.drawOval((int) (x + atras * (18 + fase * 80)) - 6, (int) y - alto / 2, 12, alto);
        }
        dibujarBrillo(g, b, AZUL);
    }

    /** Tormenta eléctrica alrededor de Camerún. */
    private static void dibujarTormenta(Graphics2D g, int x, int y, long tiempo) {
        long cambio = tiempo / 90;
        for (int i = 0; i < 5; i++) {
            double px = x - 18 + i * (Personaje.ANCHO + 36) / 4.0;
            dibujarRayo(g, px, y - 24, px + ruido(cambio, i) * 16, y + Personaje.ALTO + 6, 10, cambio + i);
        }
    }

    /** Esfera eléctrica de Camerún, con rayos que saltan hacia atrás y alrededor. */
    private static void dibujarEsferaElectrica(Graphics2D g, Balon b, long tiempo) {
        long cambio = tiempo / 55;
        int atras = b.getVelX() >= 0 ? -1 : 1;
        double x = b.getPosX(), y = b.getPosY();
        int r = b.getRadio();
        for (int i = -1; i <= 1; i++) {
            dibujarRayo(g, x, y + i * 12, x + atras * 125, y + i * 30 + ruido(cambio, i + 7) * 14, 13, cambio + i * 5);
        }
        int halo = r + 14;
        g.setColor(transparente(ELECTRICO, 110));
        g.fillOval((int) x - halo, (int) y - halo, halo * 2, halo * 2);
        for (int i = 0; i < 6; i++) {
            double angulo = Math.PI * i / 3 + ruido(cambio, i) * .5;
            dibujarRayo(g, x + Math.cos(angulo) * r, y + Math.sin(angulo) * r,
                    x + Math.cos(angulo) * (r + 26), y + Math.sin(angulo) * (r + 26), 6, cambio + i);
        }
    }

    /** Ráfagas que giran alrededor de Nigeria y arrastran hojas. */
    private static void dibujarBrisa(Graphics2D g, int x, int y, long tiempo) {
        g.setStroke(new BasicStroke(4));
        for (int i = 0; i < 5; i++) {
            int ancho = Personaje.ANCHO + 26 + i % 2 * 18;
            int giro = (int) (tiempo / 3 + i * 72) % 360;
            g.setColor(transparente(VIENTO, 220));
            g.drawArc(x + Personaje.ANCHO / 2 - ancho / 2, y + i * Personaje.ALTO / 5, ancho, 20, giro, 200);
        }
        for (int i = 0; i < 5; i++) {
            double angulo = tiempo / 260.0 + i * 2 * Math.PI / 5;
            dibujarHoja(g, x + Personaje.ANCHO / 2.0 + Math.cos(angulo) * 48,
                    y + 10 + i * 16 + Math.sin(angulo) * 9, angulo);
        }
    }

    /** Tornado de Nigeria: un embudo que se ensancha hacia arriba, con polvo en el suelo y hojas girando. */
    private static void dibujarTornado(Graphics2D g, Balon b, long tiempo) {
        double x = b.getPosX();
        int base = (int) b.getPosY() + b.getRadio();
        for (int i = 0; i < 6; i++) {
            double fase = parte(tiempo, 500, i / 6.0);
            int lado = i % 2 == 0 ? 1 : -1, tamano = (int) (8 + fase * 12);
            g.setColor(transparente(POLVO, (int) (170 * (1 - fase))));
            g.fillOval((int) (x + lado * (8 + fase * 46)) - tamano / 2, base - tamano, tamano, tamano);
        }
        g.setStroke(new BasicStroke(5));
        for (int i = 0; i < 10; i++) {
            int ancho = 26 + i * 10;
            int vaiven = (int) (Math.sin(tiempo / 90.0 + i * .6) * (3 + i));
            int giro = (int) (tiempo / 2 + i * 50) % 360;
            g.setColor(transparente(i % 2 == 0 ? VIENTO : Color.WHITE, 225 - i * 12));
            g.drawArc((int) x - ancho / 2 + vaiven, base - 14 - i * 11, ancho, 15, giro, 260);
        }
        for (int i = 0; i < 4; i++) {
            double angulo = tiempo / 120.0 + i * Math.PI / 2;
            dibujarHoja(g, x + Math.cos(angulo) * (22 + i * 9), base - 24 - i * 22 + Math.sin(angulo) * 6, angulo);
        }
    }

    /** Reflejos de Estados Unidos: dos siluetas que van y vienen y ocho balones pequeños que giran. */
    private static void dibujarEspejismo(Graphics2D g, int x, int y, long tiempo) {
        int vaiven = (int) (12 + Math.sin(tiempo / 180.0) * 14);
        g.setColor(transparente(ILUSION, 75));
        g.fillOval(x - vaiven, y, Personaje.ANCHO, Personaje.ALTO);
        g.setColor(transparente(AZUL, 75));
        g.fillOval(x + vaiven, y, Personaje.ANCHO, Personaje.ALTO);

        for (int i = 0; i < 8; i++) {
            double angulo = tiempo / 420.0 + Math.PI * i / 4;
            int bx = (int) (x + Personaje.ANCHO / 2.0 + Math.cos(angulo) * 50);
            int by = (int) (y + Personaje.ALTO / 2.0 + Math.sin(angulo) * 56);
            g.setColor(Color.WHITE);
            g.fillOval(bx - 6, by - 6, 12, 12);
            g.setColor(ILUSION);
            g.setStroke(new BasicStroke(2));
            g.drawOval(bx - 6, by - 6, 12, 12);
        }
    }

    /** Estrellas que giran alrededor del balón. Solo dependen del tiempo, así los ocho balones se ven iguales. */
    private static void dibujarDestellos(Graphics2D g, Balon b, long tiempo) {
        int r = b.getRadio() + 9;
        g.setColor(transparente(ILUSION, (int) (90 + 50 * Math.sin(tiempo / 120.0))));
        g.fillOval((int) b.getPosX() - r, (int) b.getPosY() - r, r * 2, r * 2);

        Color[] colores = {new Color(225, 45, 60), Color.WHITE, new Color(45, 85, 200)};
        for (int i = 0; i < 6; i++) {
            double angulo = tiempo / 160.0 + Math.PI * i / 3;
            g.setColor(colores[i % 3]);
            g.fill(estrella((int) b.getPosX() + Math.cos(angulo) * (r + 3),
                    (int) b.getPosY() + Math.sin(angulo) * (r + 3), 7));
        }
    }

    /** Copos de nieve que giran alrededor de Rusia cuando tiene el poder listo. */
    private static void dibujarNevada(Graphics2D g, int x, int y, long tiempo) {
        double cx = x + Personaje.ANCHO / 2.0, cy = y + Personaje.ALTO / 2.0;
        for (int i = 0; i < 6; i++) {
            double angulo = tiempo / 700.0 + Math.PI * i / 3;
            dibujarCopo(g, cx + Math.cos(angulo) * 46, cy + Math.sin(angulo) * 52, 8, tiempo / 400.0 + i);
        }
    }

    /** Llamas que suben alrededor de Japón cuando tiene el poder listo. */
    private static void dibujarHoguera(Graphics2D g, int x, int y, long tiempo) {
        int base = y + Personaje.ALTO + 4;
        for (int i = 0; i < 7; i++) {
            double lx = x - 14 + i * (Personaje.ANCHO + 28) / 6.0;
            double alto = 34 + 16 * Math.sin(tiempo / 90.0 + i * 1.9) + (i % 2) * 14;
            dibujarLlama(g, lx, base, 15, alto, tiempo, i);
        }
    }

    /** Cristal que gira con el balón de Rusia, con copos que se van quedando atrás. */
    private static void dibujarBloqueDeHielo(Graphics2D g, Balon b, long tiempo) {
        double rapidez = Math.max(1, Math.hypot(b.getVelX(), b.getVelY()));
        for (int i = 4; i >= 1; i--) {
            double atrasX = b.getPosX() - b.getVelX() / rapidez * i * 22;
            double atrasY = b.getPosY() - b.getVelY() / rapidez * i * 22 + Math.sin(tiempo / 100.0 + i) * 6;
            dibujarCopo(g, atrasX, atrasY, 12 - i * 1.7, tiempo / 200.0 + i);
        }
        Shape cristal = poligono(b.getPosX(), b.getPosY(), b.getRadio() + 13, 6, tiempo / 130.0);
        g.setColor(new Color(HIELO.getRed(), HIELO.getGreen(), HIELO.getBlue(), 190));
        g.fill(cristal);
        g.setColor(new Color(70, 150, 205));
        g.setStroke(new BasicStroke(3));
        g.draw(cristal);
    }

    /** Cola de llamas que sigue al balón de Japón. */
    private static void dibujarOlaDeFuego(Graphics2D g, Balon b, long tiempo) {
        double rapidez = Math.max(1, Math.hypot(b.getVelX(), b.getVelY()));
        double dx = b.getVelX() / rapidez, dy = b.getVelY() / rapidez;
        for (int i = 7; i >= 1; i--) {
            double vaiven = Math.sin(tiempo / 60.0 + i) * i;
            double cx = b.getPosX() - dx * i * 14 - dy * vaiven;
            double cy = b.getPosY() - dy * i * 14 + dx * vaiven;
            double r = b.getRadio() + 8 - i * 2.4;
            // Cerca del balón es amarilla y hacia la punta se vuelve roja.
            g.setColor(new Color(255, Math.max(30, 205 - i * 25), 30, 225 - i * 22));
            g.fillOval((int) (cx - r), (int) (cy - r), (int) (r * 2), (int) (r * 2));
        }
        dibujarBrillo(g, b, FUEGO);
        for (int i = 0; i < 5; i++) {
            double lx = b.getPosX() + (i - 2) * 9;
            dibujarLlama(g, lx, b.getPosY() - 6, 12, 26 + 8 * Math.sin(tiempo / 70.0 + i * 2), tiempo, i);
        }
    }

    /** Bloque de hielo alrededor del cuerpo, con puntas arriba y un brillo que lo recorre. */
    private static void dibujarPrisionDeHielo(Graphics2D g, int x, int y, long tiempo) {
        int izquierda = x - 8, ancho = Personaje.ANCHO + 16, abajo = y + Personaje.ALTO + 2;
        int[] alturas = {6, -14, 2, -18, 0, -10, 6};
        Path2D bloque = new Path2D.Double();
        bloque.moveTo(izquierda, abajo);
        for (int i = 0; i < alturas.length; i++) {
            bloque.lineTo(izquierda + ancho * i / (double) (alturas.length - 1), y + alturas[i]);
        }
        bloque.lineTo(izquierda + ancho, abajo);
        bloque.closePath();

        g.setColor(new Color(HIELO.getRed(), HIELO.getGreen(), HIELO.getBlue(), 135));
        g.fill(bloque);
        g.setColor(new Color(235, 250, 255));
        g.setStroke(new BasicStroke(3));
        g.draw(bloque);

        int brillo = (int) (tiempo / 12 % (Personaje.ALTO - 20));
        g.drawLine(izquierda + 8, y + 22 + brillo, izquierda + 26, y + 4 + brillo);
        for (int i = 0; i < 3; i++) {
            double cx = izquierda + 8 + i * (ancho - 16) / 2.0;
            dibujarCopo(g, cx, y + 20 + i % 2 * 34, 6, tiempo / 500.0 + i);
        }
    }

    private static void dibujarCuerpoEnLlamas(Graphics2D g, int x, int y, long tiempo) {
        for (int i = 0; i < 6; i++) {
            double lx = x + 2 + i * (Personaje.ANCHO - 4) / 5.0;
            double base = y + Personaje.ALTO - (i % 2) * 22;
            dibujarLlama(g, lx, base, 13, 34 + 12 * Math.sin(tiempo / 80.0 + i * 1.7), tiempo, i);
        }
    }

    /** Copo de seis puntas. */
    private static void dibujarCopo(Graphics2D g, double cx, double cy, double r, double giro) {
        g.setStroke(new BasicStroke(2));
        for (int i = 0; i < 3; i++) {
            double angulo = giro + Math.PI * i / 3;
            int dx = (int) Math.round(Math.cos(angulo) * r), dy = (int) Math.round(Math.sin(angulo) * r);
            g.setColor(Color.WHITE);
            g.drawLine((int) cx - dx, (int) cy - dy, (int) cx + dx, (int) cy + dy);
        }
        g.setColor(new Color(70, 150, 205));
        g.fillOval((int) cx - 2, (int) cy - 2, 4, 4);
    }

    /** Llama naranja con el centro amarillo; la punta se mueve de lado a lado. */
    private static void dibujarLlama(Graphics2D g, double cx, double base, double ancho, double alto, long tiempo, int n) {
        double punta = Math.sin(tiempo / 110.0 + n * 1.3) * ancho * .5;
        g.setColor(new Color(FUEGO.getRed(), FUEGO.getGreen(), FUEGO.getBlue(), 190));
        g.fill(llama(cx, base, ancho, alto, punta));
        g.setColor(new Color(BRASA.getRed(), BRASA.getGreen(), BRASA.getBlue(), 210));
        g.fill(llama(cx, base, ancho * .5, alto * .55, punta * .5));
    }

    private static Shape llama(double cx, double base, double ancho, double alto, double punta) {
        Path2D f = new Path2D.Double();
        f.moveTo(cx, base);
        f.curveTo(cx - ancho, base - alto * .15, cx - ancho * .6, base - alto * .55, cx + punta, base - alto);
        f.curveTo(cx + ancho * .6, base - alto * .55, cx + ancho, base - alto * .15, cx, base);
        f.closePath();
        return f;
    }

    private static Shape poligono(double cx, double cy, double r, int lados, double giro) {
        Path2D figura = new Path2D.Double();
        for (int i = 0; i < lados; i++) {
            double angulo = giro + 2 * Math.PI * i / lados;
            double px = cx + Math.cos(angulo) * r, py = cy + Math.sin(angulo) * r;
            if (i == 0) figura.moveTo(px, py);
            else figura.lineTo(px, py);
        }
        figura.closePath();
        return figura;
    }

    /** Ondas de choque y líneas de velocidad sobre el rival empujado. */
    private static void dibujarEmpuje(Graphics2D g, int x, int y, long tiempo) {
        g.setStroke(new BasicStroke(3));
        for (int i = 0; i < 2; i++) {
            double fase = parte(tiempo, 400, i / 2.0);
            int margen = (int) (fase * 26);
            g.setColor(transparente(AZUL, (int) (230 * (1 - fase))));
            g.drawOval(x - margen, y - margen, Personaje.ANCHO + margen * 2, Personaje.ALTO + margen * 2);
        }
        g.setColor(AZUL);
        for (int i = 0; i < 4; i++) {
            int alto = y + 14 + i * 17, corrido = (int) (parte(tiempo, 250, i * .3) * 14);
            g.drawLine(x - 30 + corrido, alto, x - 10 + corrido, alto);
            g.drawLine(x + Personaje.ANCHO + 10 - corrido, alto, x + Personaje.ANCHO + 30 - corrido, alto);
        }
    }

    /** Rayos que caen sobre el rival y le recorren el cuerpo. */
    private static void dibujarDescarga(Graphics2D g, int x, int y, long tiempo) {
        long cambio = tiempo / 80;
        for (int i = 0; i < 3; i++) {
            double px = x + 6 + i * (Personaje.ANCHO - 12) / 2.0;
            dibujarRayo(g, px + ruido(cambio, i) * 18, y - 70, px, y + Personaje.ALTO - 6, 13, cambio + i * 3);
        }
        g.setColor(transparente(ELECTRICO, cambio % 2 == 0 ? 80 : 30));
        g.fillOval(x - 8, y - 4, Personaje.ANCHO + 16, Personaje.ALTO + 8);
    }

    /** Torbellino que envuelve al rival inconsciente y estrellas sobre su cabeza. */
    private static void dibujarTorbellino(Graphics2D g, int x, int y, long tiempo) {
        g.setStroke(new BasicStroke(4));
        for (int i = 0; i < 4; i++) {
            int ancho = Personaje.ANCHO - 8 + i * 12;
            g.setColor(transparente(VIENTO, 210));
            g.drawArc(x + Personaje.ANCHO / 2 - ancho / 2, y + Personaje.ALTO - 8 - i * 20, ancho, 16,
                    (int) (tiempo / 2 + i * 80) % 360, 230);
        }
        g.setColor(ELECTRICO);
        for (int i = 0; i < 4; i++) {
            double angulo = tiempo / 220.0 + Math.PI * i / 2;
            g.fill(estrella(x + Personaje.ANCHO / 2.0 + Math.cos(angulo) * 32, y - 4 + Math.sin(angulo) * 8, 8));
        }
    }

    /** Signos de pregunta que giran alrededor del rival confundido. */
    private static void dibujarDudas(Graphics2D g, int x, int y, long tiempo) {
        Font anterior = g.getFont();
        g.setFont(new Font("Arial", Font.BOLD, 24));
        for (int i = 0; i < 4; i++) {
            double angulo = tiempo / 300.0 + Math.PI * i / 2;
            int px = (int) (x + Personaje.ANCHO / 2.0 - 7 + Math.cos(angulo) * 44);
            int py = (int) (y + Personaje.ALTO / 2.0 + 8 + Math.sin(angulo) * 34);
            g.setColor(i % 2 == 0 ? ILUSION : AZUL);
            g.drawString("?", px, py);
        }
        g.setFont(anterior);
    }

    /** Línea quebrada amarilla con el centro blanco. La semilla hace que cambie de forma cada cierto tiempo. */
    private static void dibujarRayo(Graphics2D g, double x1, double y1, double x2, double y2, double desvio, long semilla) {
        int tramos = 6;
        double largo = Math.max(1, Math.hypot(x2 - x1, y2 - y1));
        double nx = -(y2 - y1) / largo, ny = (x2 - x1) / largo;
        Path2D rayo = new Path2D.Double();
        rayo.moveTo(x1, y1);
        for (int i = 1; i < tramos; i++) {
            double avance = i / (double) tramos, corrido = ruido(semilla, i) * desvio;
            rayo.lineTo(x1 + (x2 - x1) * avance + nx * corrido, y1 + (y2 - y1) * avance + ny * corrido);
        }
        rayo.lineTo(x2, y2);
        g.setColor(ELECTRICO);
        g.setStroke(new BasicStroke(4));
        g.draw(rayo);
        g.setColor(Color.WHITE);
        g.setStroke(new BasicStroke(1.5f));
        g.draw(rayo);
    }

    private static void dibujarHoja(Graphics2D g, double cx, double cy, double giro) {
        Graphics2D h = (Graphics2D) g.create();
        h.translate(cx, cy);
        h.rotate(giro * 2);
        h.setColor(HOJA);
        h.fillOval(-6, -3, 12, 6);
        h.dispose();
    }

    /** Valor entre -1 y 1 que parece al azar, pero es siempre el mismo para los mismos datos. */
    private static double ruido(long semilla, int n) {
        double v = Math.sin(semilla * 12.9898 + n * 78.233) * 43758.5453;
        return (v - Math.floor(v)) * 2 - 1;
    }

    /** Avance de 0 a 1 de una animación que se repite; el desfase evita que todas vayan a la vez. */
    private static double parte(long tiempo, int duracion, double desfase) {
        return (tiempo % duracion / (double) duracion + desfase) % 1;
    }

    private static Color transparente(Color color, int alfa) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, alfa)));
    }

    private static Shape estrella(double cx, double cy, double r) {
        Path2D e = new Path2D.Double();
        for (int i = 0; i < 10; i++) {
            double radio = i % 2 == 0 ? r : r * .45;
            double angulo = -Math.PI / 2 + Math.PI * i / 5;
            double px = cx + Math.cos(angulo) * radio, py = cy + Math.sin(angulo) * radio;
            if (i == 0) e.moveTo(px, py);
            else e.lineTo(px, py);
        }
        e.closePath();
        return e;
    }

    /** Escribe el nombre del efecto encima del personaje, con un fondo oscuro para que se lea bien. */
    private static void etiqueta(Graphics2D g, String texto, Color color, int x, int y) {
        Font anterior = g.getFont();
        g.setFont(new Font("Arial", Font.BOLD, 13));
        int ancho = g.getFontMetrics().stringWidth(texto);
        int tx = x + Personaje.ANCHO / 2 - ancho / 2;
        g.setColor(new Color(0, 0, 0, 170));
        g.fillRoundRect(tx - 6, y - 48, ancho + 12, 20, 10, 10);
        g.setColor(color);
        g.drawString(texto, tx, y - 33);
        g.setFont(anterior);
    }
}
