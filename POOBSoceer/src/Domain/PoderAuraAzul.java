package Domain;

/** Corea del Sur: el tiro sale recto a gran velocidad y lanza al rival hacia su propio arco. */
public class PoderAuraAzul implements Poder {

    private static final double DANIO_BASE = 40;
    private static final double SEGUNDOS_EMPUJE = 1.5;

    @Override
    public double velocidadDisparo() {
        return 24;
    }

    @Override
    public double duracionEfecto() {
        return SEGUNDOS_EMPUJE;
    }

    @Override
    public void aplicarEfecto(Personaje objetivo) {
        aplicarEfecto(objetivo, 1.0);
    }

    @Override
    public void aplicarEfecto(Personaje objetivo, double multiplicadorKick) {
        aplicarImpacto(objetivo, multiplicadorKick, 1);
    }

    /** El tiro viaja hacia el arco del rival, así que empujarlo en la misma dirección lo lleva hasta allí. */
    @Override
    public void aplicarImpacto(Personaje rival, double multiplicadorKick, int direccion) {
        rival.recibirDanio(DANIO_BASE * multiplicadorKick);
        rival.aplicarEmpuje(direccion, SEGUNDOS_EMPUJE);
    }
}
