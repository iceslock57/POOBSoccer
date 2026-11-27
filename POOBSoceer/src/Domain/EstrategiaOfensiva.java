package Domain;

import java.util.Set;

/** Su prioridad es anotar: persigue el balón por toda la cancha y patea apenas lo alcanza. */
public class EstrategiaOfensiva implements EstrategiaMaquina {

    // Cuadros que se adelanta al movimiento del balón.
    private static final int ANTICIPACION = 8;

    @Override
    public Set<Accion> decidir(SituacionPartido situacion) {
        Set<Accion> acciones = situacion.caminarHacia(situacion.lugarParaPatear(ANTICIPACION));

        if (situacion.poderListo()) acciones.add(Accion.PODER);
        // Solo le pega al rival si el balón está lejos; si no, guarda la patada para el balón.
        boolean golpearAlRival = situacion.rivalAlAlcance() && !situacion.balonCerca();
        if (situacion.balonAlAlcance() || golpearAlRival) acciones.add(Accion.PATEAR);
        if (situacion.balonPorEncima()) acciones.add(Accion.SALTAR);
        return acciones;
    }

    @Override
    public int cuadrosEntreDecisiones() {
        return 8;
    }

    @Override
    public String descripcion() {
        return "Su prioridad es anotar";
    }
}
