package Domain;

/** Partido cronometrado; si acaba empatado entra en gol de oro. */
public class PartidoPorTiempo extends Partido {

    private static final int SEGUNDOS_FINALES = 20;

    private int tiempoRestante, contadorFrames;
    private boolean esGolDeOro;

    public PartidoPorTiempo(Jugador a, Jugador b) {
        this(a, b, 2);
    }

    public PartidoPorTiempo(Jugador a, Jugador b, int minutos) {
        super(a, b);

        if (minutos < 1) throw new IllegalArgumentException("La duración debe ser positiva");

        tiempoRestante = minutos * 60;
    }

    public PartidoPorTiempo(Jugador a, Jugador b, int minutos, int ancho, int alto) {
        super(a, b, ancho, alto);

        if (minutos < 1) throw new IllegalArgumentException("La duración debe ser positiva");

        tiempoRestante = minutos * 60;
    }

    @Override
    public void actualizarEstado() {
        if (terminado) return;

        super.actualizarEstado();

        if (terminado) return;

        if (!esGolDeOro) {
            contadorFrames++;

            if (contadorFrames >= 60) {
                contadorFrames = 0;

                if (tiempoRestante > 0) tiempoRestante--;

                if (tiempoRestante == 0 && marcador[0] == marcador[1]) {
                    esGolDeOro = true;
                }
            }
        }

        finalizarSiCorresponde();
    }

    @Override
    public boolean esFinDePartido() {
        return tiempoRestante <= 0
            && (!esGolDeOro ? marcador[0] != marcador[1] : marcador[0] != marcador[1]);
    }

    /** Quedan los últimos segundos o ya se está jugando el gol de oro. */
    @Override
    public boolean estaPorTerminar() {
        return tiempoRestante <= SEGUNDOS_FINALES;
    }

    @Override
    void guardar(EscritorPartida e) {
        super.guardar(e);
        e.entero(tiempoRestante, 16);
        e.entero(contadorFrames, 8);
        e.logico(esGolDeOro);
    }

    @Override
    void restaurar(LectorPartida l) {
        super.restaurar(l);
        tiempoRestante = l.entero(16);
        contadorFrames = l.entero(8);
        esGolDeOro = l.logico();
    }

    public int getTiempoRestante() {
        return tiempoRestante;
    }

    public boolean isEsGolDeOro() {
        return esGolDeOro;
    }
}