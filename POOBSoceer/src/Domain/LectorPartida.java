package Domain;

/** Entrega los datos de una partida guardada, en el mismo orden en que se escribieron. */
public interface LectorPartida {

    int entero(int bits);

    long largo();

    double decimal();

    boolean logico();

    String texto();

    /** Comprueba que ya se leyó todo; si sobran datos, el archivo no es lo que parece. */
    void terminar();
}
