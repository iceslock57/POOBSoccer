package Domain;

import java.util.Set;

/** Su prioridad es defender el arco: lo cuida de cerca y solo sale cuando el balón entra en su mitad. */
public class EstrategiaDefensiva implements EstrategiaMaquina {

    // Cuadros que se adelanta al movimiento del balón.
    private static final int ANTICIPACION = 8;

    @Override
    public Set<Accion> decidir(SituacionPartido situacion) {
        boolean hayPeligro = situacion.balonEnMiMitad();
        double destino = hayPeligro ? situacion.lugarParaPatear(ANTICIPACION) : situacion.puestoDeGuardia();
        Set<Accion> acciones = situacion.caminarHacia(destino);

        // Guarda el poder para cuando tenga que despejar.
        if (hayPeligro && situacion.poderListo()) acciones.add(Accion.PODER);
        if (situacion.balonAlAlcance()) acciones.add(Accion.PATEAR);
        if (situacion.balonPorEncima()) acciones.add(Accion.SALTAR);
        return acciones;
    }

    @Override
    public int cuadrosEntreDecisiones() {
        return 8;
    }

    @Override
    public String descripcion() {
        return "Su prioridad es defender el arco";
    }
}
