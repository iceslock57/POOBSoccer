package Domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Estados Unidos: el balón se divide en ocho iguales y solo uno de ellos es el verdadero. */
public class PoderIlusion implements Poder {

    private static final double DANIO_BASE = 30;
    private static final double SEGUNDOS_CONFUSION = 2.5;
    // Altura hacia la que sale cada uno de los ocho balones.
    private static final List<Double> SALIDAS = Arrays.asList(-4.0, -3.0, -2.2, -1.5, -0.8, 0.0, 0.8, 1.6);

    private final Random azar;

    public PoderIlusion() {
        this(new Random());
    }

    /** Recibe el azar desde afuera para poder repetir el mismo reparto en las pruebas. */
    public PoderIlusion(Random azar) {
        this.azar = azar;
    }

    @Override
    public double velocidadDisparo() {
        return 14;
    }

    @Override
    public double duracionEfecto() {
        return SEGUNDOS_CONFUSION;
    }

    /** Reparte las ocho salidas al azar para que ninguna posición delate cuál es el balón real. */
    @Override
    public List<Balon> lanzar(Balon balon, int direccion, int propietario) {
        List<Double> salidas = new ArrayList<>(SALIDAS);
        Collections.shuffle(salidas, azar);
        double velX = direccion * velocidadDisparo();

        balon.aplicarDisparoEspecial(velX, salidas.get(0), propietario);

        List<Balon> falsos = new ArrayList<>();
        for (double velY : salidas.subList(1, salidas.size())) {
            Balon falso = new Balon(balon.getPosX(), balon.getPosY(), balon.getRadio());
            falso.aplicarDisparoEspecial(velX, velY, propietario);
            falsos.add(falso);
        }
        return falsos;
    }

    @Override
    public void aplicarEfecto(Personaje objetivo) {
        aplicarEfecto(objetivo, 1.0);
    }

    @Override
    public void aplicarEfecto(Personaje objetivo, double multiplicadorKick) {
        objetivo.recibirDanio(DANIO_BASE * multiplicadorKick);
        objetivo.aplicarConfusion(SEGUNDOS_CONFUSION);
    }
}
