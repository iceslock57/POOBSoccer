package Domain;

/** Cancha configurable. El suelo está 90 px por encima del borde inferior. */
public class Cancha {

    /** Medidas de cada arco. La imagen del arco se estira para que sus palos queden justo en estos bordes. */
    public static final int ALTO_ARCO = 140, ANCHO_ARCO = 115;

    private final int ancho, alto, suelo;
    private final Arco arcoIzquierdo, arcoDerecho;

    public Cancha(int ancho, int alto) {
        if (ancho < 700 || alto < 500) throw new IllegalArgumentException("La cancha debe medir al menos 700x500");

        this.ancho = ancho;
        this.alto = alto;
        this.suelo = alto - 100;

        int altoArco = ALTO_ARCO, anchoArco = ANCHO_ARCO, y = suelo - altoArco;

        arcoIzquierdo = new Arco(0, y, anchoArco, altoArco, "Jugador 1", true);
        arcoDerecho = new Arco(ancho - anchoArco, y, anchoArco, altoArco, "Jugador 2", false);
    }

    public Arco esGol(Balon b) {
        // Gol al cruzar el plano interior de la porteria; usa la geometria real.
        double y = b.getPosY();
        int r = b.getRadio();

        double arriba = arcoIzquierdo.getPosY();
        double abajo = arriba + arcoIzquierdo.getAlto();

        boolean dentro = y - r >= arriba && y + r <= abajo;

        if (!dentro) return null;

        double planoIzq = arcoIzquierdo.getPosX() + arcoIzquierdo.getAncho();
        double planoDer = arcoDerecho.getPosX();

        if (b.getVelX() < 0 && b.getPosX() - r <= planoIzq) return arcoIzquierdo;
        if (b.getVelX() > 0 && b.getPosX() + r >= planoDer) return arcoDerecho;

        return null;
    }

    public int getAncho() {
        return ancho;
    }

    public int getAlto() {
        return alto;
    }

    public int getSuelo() {
        return suelo;
    }

    public Arco getArcoIzquierdo() {
        return arcoIzquierdo;
    }

    public Arco getArcoDerecho() {
        return arcoDerecho;
    }
}
