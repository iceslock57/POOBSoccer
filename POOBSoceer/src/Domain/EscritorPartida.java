package Domain;

/**
 * Recibe, uno por uno, los datos de una partida que se está guardando.
 * El dominio no sabe en qué formato quedan; eso lo decide quien implemente esta interfaz.
 */
public interface EscritorPartida {

    /** Guarda un entero que no es negativo y que cabe en esa cantidad de bits. */
    void entero(int valor, int bits);

    void largo(long valor);

    void decimal(double valor);

    void logico(boolean valor);

    void texto(String valor);
}
