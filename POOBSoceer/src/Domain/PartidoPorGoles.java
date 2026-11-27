package Domain;

/**
 * Representa un partido que se define por una cantidad de goles.
 * No tiene límite de tiempo y termina cuando un jugador alcanza la meta.
 */
public class PartidoPorGoles extends Partido {

    private int golesObjetivo;

    /**
     * Constructor para el partido por goles.
     *
     * @param jugador1 El primer jugador.
     * @param jugador2 El segundo jugador.
     * @param golesObjetivo La cantidad de goles para ganar (por ejemplo: 3, 5 o 7).
     */
    public PartidoPorGoles(Jugador jugador1, Jugador jugador2, int golesObjetivo) {
        super(jugador1, jugador2);

        // El objetivo de goles para ganar el partido.
        if (golesObjetivo < 1) throw new IllegalArgumentException("La meta de goles debe ser positiva");

        this.golesObjetivo = golesObjetivo;
    }

    public PartidoPorGoles(Jugador a, Jugador b, int goles, int ancho, int alto) {
        super(a, b, ancho, alto);

        if (goles < 1) throw new IllegalArgumentException("La meta de goles debe ser positiva");

        golesObjetivo = goles;
    }

    @Override
    public boolean esFinDePartido() {
        // Gana el primero que llegue a la cantidad de goles objetivo, sin límite de tiempo.
        // Retorna true automáticamente si cualquiera de los dos alcanza la meta.
        return marcador[0] >= golesObjetivo || marcador[1] >= golesObjetivo;
    }

    public int getGolesObjetivo() {
        return this.golesObjetivo;
    }
}