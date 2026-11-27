package Domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Registro de personajes disponibles: pais -> poder. */
public final class CatalogoPersonajes {

    private static final Map<String, Supplier<Poder>> PODERES = new LinkedHashMap<>();

    static {
        PODERES.put("Corea del Sur", PoderAuraAzul::new);
        PODERES.put("Camerún", PoderRayo::new);
        PODERES.put("Nigeria", PoderTornado::new);
        PODERES.put("Estados Unidos", PoderIlusion::new);
        // Rusia y Japón no están en el anexo; son personajes propios con poderes propios.
        PODERES.put("Rusia", PoderHielo::new);
        PODERES.put("Japón", PoderFuego::new);
    }

    private CatalogoPersonajes() {
    }

    public static List<String> paises() {
        return new ArrayList<>(PODERES.keySet());
    }

    public static boolean existe(String pais) {
        return PODERES.containsKey(pais);
    }

    public static Personaje crear(String pais) {
        return crear(pais, null);
    }

    /**
     * Crea el personaje con las estadísticas que el usuario le ha comprado a ese país.
     * Si todavía no hay perfil asociado, todas las estadísticas comienzan en uno.
     */
    public static Personaje crear(String pais, PerfilJugador perfil) {
        return crearConEstadisticas(pais, perfil == null ? new EstadisticasPersonaje() : perfil.getEstadisticas(pais));
    }

    /** Crea el personaje con unos niveles ya definidos. */
    static Personaje crearConEstadisticas(String pais, EstadisticasPersonaje e) {
        Supplier<Poder> fabrica = PODERES.get(pais);

        if (fabrica == null) {
            throw new IllegalArgumentException("País sin poder definido: " + pais);
        }

        return new Personaje(pais, pais,
                e.getNivel(PerfilJugador.Estadistica.SPEED),
                e.getNivel(PerfilJugador.Estadistica.JUMP),
                e.getNivel(PerfilJugador.Estadistica.KICK),
                e.getNivel(PerfilJugador.Estadistica.VIDA),
                fabrica.get());
    }
}