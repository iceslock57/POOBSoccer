package Domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Registro de los perfiles de máquina. Un perfil nuevo solo necesita una línea aquí. */
public final class CatalogoMaquinas {

    private static final Map<String, Supplier<EstrategiaMaquina>> PERFILES = new LinkedHashMap<>();

    static {
        PERFILES.put("Torpe", EstrategiaTorpe::new);
        PERFILES.put("Ofensiva", EstrategiaOfensiva::new);
        PERFILES.put("Defensiva", EstrategiaDefensiva::new);
        PERFILES.put("Experta", EstrategiaExperta::new);
    }

    private CatalogoMaquinas() {
    }

    public static List<String> perfiles() {
        return new ArrayList<>(PERFILES.keySet());
    }

    public static EstrategiaMaquina crear(String perfil) {
        Supplier<EstrategiaMaquina> fabrica = PERFILES.get(perfil);
        if (fabrica == null) throw new ExcepcionJuego("Perfil de máquina no válido: " + perfil);
        return fabrica.get();
    }
}
