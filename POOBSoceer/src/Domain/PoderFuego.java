package Domain;

import java.util.Collections;
import java.util.List;

/** Japón: una ola de fuego que avanza subiendo y bajando; si golpea, el rival se sigue quemando por unos segundos. */
public class PoderFuego implements Poder {

    private static final double DANIO_BASE = 25;
    private static final double DANIO_POR_SEGUNDO = 8;
    private static final double SEGUNDOS_QUEMADO = 3;
    // Qué tan rápido sube y baja el balón, y qué tan larga es cada curva.
    private static final double VAIVEN = 12, LARGO_CURVA = 60;

    @Override
    public double velocidadDisparo() {
        return 15;
    }

    @Override
    public double duracionEfecto() {
        return SEGUNDOS_QUEMADO;
    }

    @Override
    public List<Balon> lanzar(Balon balon, int direccion, int propietario) {
        balon.aplicarDisparoEspecial(direccion * velocidadDisparo(), -VAIVEN, propietario);
        return Collections.emptyList();
    }

    /** La altura depende de cuánto ha avanzado desde que salió; por eso empieza subiendo y dibuja una ola. */
    @Override
    public void avanzarDisparo(Balon balon, int suelo) {
        double avance = (balon.getPosX() - balon.getOrigenDisparo()) / LARGO_CURVA;
        balon.cambiarVelocidad(balon.getVelX(), -VAIVEN * Math.cos(avance));
    }

    @Override
    public void aplicarEfecto(Personaje objetivo) {
        aplicarEfecto(objetivo, 1.0);
    }

    @Override
    public void aplicarEfecto(Personaje objetivo, double multiplicadorKick) {
        objetivo.recibirDanio(DANIO_BASE * multiplicadorKick);
        objetivo.aplicarQuemadura(SEGUNDOS_QUEMADO, DANIO_POR_SEGUNDO * multiplicadorKick);
    }
}