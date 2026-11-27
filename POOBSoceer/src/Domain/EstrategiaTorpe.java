package Domain;

import java.util.EnumSet;
import java.util.Random;
import java.util.Set;

/** Escoge sus movimientos al azar, sin fijarse en lo que pasa en el partido. */
public class EstrategiaTorpe implements EstrategiaMaquina {

    private static final double PROBABILIDAD_SALTAR = .2, PROBABILIDAD_PATEAR = .4, PROBABILIDAD_PODER = .3;

    private final Random azar;

    public EstrategiaTorpe() {
        this(new Random());
    }

    /** Recibe el azar desde afuera para poder repetir los mismos movimientos en las pruebas. */
    public EstrategiaTorpe(Random azar) {
        this.azar = azar;
    }

    @Override
    public Set<Accion> decidir(SituacionPartido situacion) {
        Set<Accion> acciones = EnumSet.noneOf(Accion.class);

        // Puede ir a la izquierda, a la derecha o quedarse quieta.
        int camino = azar.nextInt(3);
        if (camino == 0) acciones.add(Accion.IZQUIERDA);
        if (camino == 1) acciones.add(Accion.DERECHA);

        if (azar.nextDouble() < PROBABILIDAD_SALTAR) acciones.add(Accion.SALTAR);
        if (azar.nextDouble() < PROBABILIDAD_PATEAR) acciones.add(Accion.PATEAR);
        if (azar.nextDouble() < PROBABILIDAD_PODER) acciones.add(Accion.PODER);
        return acciones;
    }

    @Override
    public int cuadrosEntreDecisiones() {
        return 20;
    }

    @Override
    public String descripcion() {
        return "Escoge al azar sus movimientos";
    }
}
