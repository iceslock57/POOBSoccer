package tests;

import Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas del rebote del balón en la cabeza de los personajes. */
public class ReboteCabezaTest {

    private Partido partido;
    private Personaje p1, p2;
    private Balon balon;

    @BeforeEach
    public void preparar() {
        Jugador j1 = new JugadorHumano("Uno", CatalogoPersonajes.crear("Rusia"), new String[]{"A", "D", "W", "S", "Q"});
        Jugador j2 = new JugadorHumano("Dos", CatalogoPersonajes.crear("Japón"), new String[]{"LEFT", "RIGHT", "UP", "M", "N"});
        partido = new PartidoPorGoles(j1, j2, 5);
        p1 = j1.getPersonaje();
        p2 = j2.getPersonaje();
        balon = partido.getBalon();
    }

    private double centro(Personaje p) {
        return p.getPosX() + Personaje.ANCHO / 2.0;
    }

    /** Deja caer el balón y avanza hasta que empiece a subir. */
    private void caerHastaRebotar() {
        for (int i = 0; i < 120 && balon.getVelY() >= 0; i++) partido.actualizarEstado();
        assertTrue(balon.getVelY() < 0, "El balón debe rebotar");
    }

    /** Mayor cantidad de cuadros seguidos que el balón pasa tocando la cabeza. */
    private int cuadrosPegado(int cuadros) {
        int seguidos = 0, maximo = 0;
        for (int i = 0; i < cuadros; i++) {
            partido.actualizarEstado();
            double abajo = balon.getPosY() + balon.getRadio();
            boolean sobreAlguien = abajo > p1.getPosY() - 6 && abajo < p1.getPosY() + 30
                    && balon.getPosX() > p1.getPosX() - 20 && balon.getPosX() < p2.getPosX() + Personaje.ANCHO + 20;
            seguidos = sobreAlguien ? seguidos + 1 : 0;
            maximo = Math.max(maximo, seguidos);
        }
        return maximo;
    }

    @Test
    public void elBalonRebotaHaciaArribaEnLaCabeza() {
        balon.resetearPosicion(centro(p1), 250);
        caerHastaRebotar();
        assertTrue(balon.getPosY() <= p1.getPosY(), "Rebota sobre la cabeza y no en el suelo");
        double alturaDelGolpe = balon.getPosY();
        for (int i = 0; i < 15; i++) partido.actualizarEstado();
        assertTrue(balon.getPosY() < alturaDelGolpe - 60, "Sube de verdad");
    }

    @Test
    public void aunqueCaigaDespacioElReboteSeNota() {
        balon.resetearPosicion(centro(p1), p1.getPosY() - balon.getRadio() - 2);
        caerHastaRebotar();
        assertTrue(balon.getVelY() <= -6);
    }

    @Test
    public void unBalonMuyRapidoNoSaleDisparado() {
        balon.resetearPosicion(centro(p1), 250);
        balon.aplicarFuerza(0, 30);
        caerHastaRebotar();
        assertTrue(balon.getVelY() >= -13);
    }

    @Test
    public void noSeQuedaPegadoALaCabeza() {
        balon.resetearPosicion(centro(p1), 250);
        p2.resetearPosicion(900, 470);
        assertTrue(cuadrosPegado(120) <= 8);
    }

    @Test
    public void noSeQuedaPegadoEntreLosDosJugadores() {
        p2.resetearPosicion(p1.getPosX() + Personaje.ANCHO, 470);
        balon.resetearPosicion(p1.getPosX() + Personaje.ANCHO - 2, 250);
        assertTrue(cuadrosPegado(300) <= 10);
    }

    @Test
    public void noSeQuedaPegadoJuntoAlBorde() {
        p1.resetearPosicion(0, 470);
        balon.resetearPosicion(20, 250);
        p2.resetearPosicion(900, 470);
        assertTrue(cuadrosPegado(120) <= 8);
    }

    @Test
    public void seDesviaHaciaElLadoDondePega() {
        balon.resetearPosicion(centro(p1) + 20, 250);
        caerHastaRebotar();
        assertTrue(balon.getVelX() > 0, "Pegó a la derecha");
        balon.resetearPosicion(centro(p1) - 20, 250);
        caerHastaRebotar();
        assertTrue(balon.getVelX() < 0, "Pegó a la izquierda");
    }

    @Test
    public void enTodoElCentroSigueHaciaElArcoContrario() {
        balon.resetearPosicion(centro(p1), 250);
        caerHastaRebotar();
        assertTrue(balon.getVelX() > 0, "El jugador 1 ataca a la derecha");
        balon.resetearPosicion(centro(p2), 250);
        caerHastaRebotar();
        assertTrue(balon.getVelX() < 0, "El jugador 2 ataca a la izquierda");
    }

    @Test
    public void saltandoElCabezazoSaleMasFuerte() {
        Balon quieto = new Balon(centro(p1), p1.getPosY() - 10, 16);
        quieto.aplicarFuerza(0, 5);
        quieto.rebotarEnCabeza(p1, 1);

        p1.saltar();
        p1.actualizarEstado();
        Balon saltando = new Balon(centro(p1), p1.getPosY() - 10, 16);
        saltando.aplicarFuerza(0, 5);
        saltando.rebotarEnCabeza(p1, 1);
        assertTrue(saltando.getVelY() < quieto.getVelY());
    }

    @Test
    public void elLadoDelCuerpoNoEsLaCabeza() {
        Balon alLado = new Balon(p1.getPosX() + Personaje.ANCHO + 5, p1.getPosY() + 50, 16);
        assertFalse(alLado.caeSobre(p1));
        Balon arriba = new Balon(centro(p1), p1.getPosY() - 10, 16);
        assertTrue(arriba.caeSobre(p1));
    }

    @Test
    public void conLaPatadaSoloSaleDisparadoLoQueTocaElPie() {
        balon.resetearPosicion(p1.getPosX() + Personaje.ANCHO + 15, p1.getPosY() + 60);
        partido.getJugador1().realizarAccion("S", partido);
        partido.actualizarEstado();
        assertEquals(12, balon.getVelX(), 1e-6, "Delante del pie sale como una patada");

        p1.consumirPatada();
        balon.resetearPosicion(centro(p1), p1.getPosY() - 10);
        partido.getJugador1().realizarAccion("S", partido);
        partido.actualizarEstado();
        assertTrue(balon.getVelY() < 0 && balon.getVelX() < 12, "En la cabeza es un cabezazo, aunque esté pateando");
    }

    @Test
    public void unTiroDePoderPropioNoRebotaEnSuDuenio() {
        balon.resetearPosicion(centro(p1), p1.getPosY() - 10);
        balon.aplicarDisparoEspecial(0, 2, 1);
        partido.actualizarEstado();
        assertTrue(balon.isDisparoEspecial());
        assertTrue(balon.getVelY() > 0, "Sigue su camino hacia abajo");
    }
}
