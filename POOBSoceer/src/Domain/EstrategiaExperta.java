package Domain;

import java.util.Set;

/** Decide según el estado del partido: ataca, defiende o esquiva de acuerdo con lo que más le conviene. */
public class EstrategiaExperta implements EstrategiaMaquina {

    private final EstrategiaMaquina ataque = new EstrategiaOfensiva();
    private final EstrategiaMaquina defensa = new EstrategiaDefensiva();

    @Override
    public Set<Accion> decidir(SituacionPartido situacion) {
        EstrategiaMaquina plan = convieneDefender(situacion) ? defensa : ataque;
        Set<Accion> acciones = plan.decidir(situacion);

        if (situacion.poderListo()) acciones.add(Accion.PODER);
        // Con poca vida prefiere saltar un tiro de poder antes que dejarse aturdir.
        if (situacion.tiroEnemigoEncima() && situacion.vidaBaja()) acciones.add(Accion.SALTAR);
        return acciones;
    }

    /** Cuida el arco si el balón viene hacia él o si va ganando y falta poco; con el rival indefenso, siempre ataca. */
    private boolean convieneDefender(SituacionPartido situacion) {
        if (situacion.rivalIndefenso()) return false;

        boolean arcoEnPeligro = situacion.balonEnMiMitad() && situacion.balonVaHaciaMiArco();
        boolean cuidarLaVentaja = situacion.voyGanando() && situacion.estaPorTerminar();
        return arcoEnPeligro || cuidarLaVentaja;
    }

    @Override
    public int cuadrosEntreDecisiones() {
        return 3;
    }

    @Override
    public String descripcion() {
        return "Decide según el estado del partido";
    }
}
