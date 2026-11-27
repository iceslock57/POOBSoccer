package Domain;

/** Camerún: el tiro sale recto y, si golpea, el rival no puede saltar y se mueve a la mitad de su velocidad. */
public class PoderRayo implements Poder {

    private static final double DANIO_BASE = 60;
    private static final double SEGUNDOS_ELECTROCUTADO = 3;

    @Override
    public double duracionEfecto() {
        return SEGUNDOS_ELECTROCUTADO;
    }

    @Override
    public void aplicarEfecto(Personaje objetivo) {
        aplicarEfecto(objetivo, 1.0);
    }

    @Override
    public void aplicarEfecto(Personaje objetivo, double multiplicadorKick) {
        objetivo.recibirDanio(DANIO_BASE * multiplicadorKick);
        objetivo.aplicarElectrocucion(SEGUNDOS_ELECTROCUTADO);
    }
}
