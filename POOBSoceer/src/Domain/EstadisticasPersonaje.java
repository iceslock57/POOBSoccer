package Domain;

import java.util.EnumMap;
import java.util.Map;

/** Niveles que un jugador ha comprado para uno de sus personajes. Todos empiezan en uno y llegan hasta diez. */
public class EstadisticasPersonaje {

    public static final int NIVEL_MAXIMO = 10;
    private static final long PRECIO_INICIAL = 500;

    private final Map<PerfilJugador.Estadistica, Integer> niveles = new EnumMap<>(PerfilJugador.Estadistica.class);

    public EstadisticasPersonaje() {
        for (PerfilJugador.Estadistica e : PerfilJugador.Estadistica.values()) niveles.put(e, 1);
    }

    public int getNivel(PerfilJugador.Estadistica estadistica) {
        return niveles.get(estadistica);
    }

    /** Guarda un nivel recuperado del archivo sin dejar que se salga del rango permitido. */
    void setNivel(PerfilJugador.Estadistica estadistica, int nivel) {
        niveles.put(estadistica, Math.max(1, Math.min(NIVEL_MAXIMO, nivel)));
    }

    public boolean puedeSubir(PerfilJugador.Estadistica estadistica) {
        return getNivel(estadistica) < NIVEL_MAXIMO;
    }

    void subir(PerfilJugador.Estadistica estadistica) {
        setNivel(estadistica, getNivel(estadistica) + 1);
    }

    void guardar(EscritorPartida e) {
        for (PerfilJugador.Estadistica estadistica : PerfilJugador.Estadistica.values()) {
            e.entero(getNivel(estadistica), 4);
        }
    }

    static EstadisticasPersonaje leer(LectorPartida l) {
        EstadisticasPersonaje leidas = new EstadisticasPersonaje();
        for (PerfilJugador.Estadistica estadistica : PerfilJugador.Estadistica.values()) {
            leidas.setNivel(estadistica, l.entero(4));
        }
        return leidas;
    }

    /** Cantidad de mejoras que ya tiene este personaje. */
    public int getMejoras() {
        int suma = 0;
        for (int nivel : niveles.values()) suma += nivel - 1;
        return suma;
    }

    /** Empieza en 500 y se duplica con cada mejora de este mismo personaje. */
    public long getCostoSiguienteMejora() {
        return PRECIO_INICIAL << getMejoras();
    }
}
