package tests;

import Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.EnumSet;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de lo que una máquina puede ver del partido. */
public class SituacionPartidoTest {

    private static final double DELTA = 1e-6;

    private Jugador uno, dos;
    private Partido partido;

    @BeforeEach
    public void preparar() {
        uno = new JugadorMaquina("Uno", CatalogoPersonajes.crear("Estados Unidos"), "Experta", new EstrategiaDefensiva());
        dos = new JugadorMaquina("Dos", CatalogoPersonajes.crear("Japón"), "Experta", new EstrategiaDefensiva());
        partido = new PartidoPorGoles(uno, dos, 3);
    }

    private SituacionPartido vistaDe(Jugador jugador) {
        return new SituacionPartido(jugador, partido);
    }

    @Test
    public void cadaUnoAtacaHaciaElArcoContrario() {
        assertEquals(1, vistaDe(uno).direccionDeAtaque());
        assertEquals(-1, vistaDe(dos).direccionDeAtaque());
    }

    @Test
    public void cadaUnoCuidaSuPropioArco() {
        // Justo delante de la boca de su arco, a un cuerpo de distancia.
        assertEquals(Cancha.ANCHO_ARCO + Personaje.ANCHO, vistaDe(uno).puestoDeGuardia(), DELTA);
        assertEquals(1000 - Cancha.ANCHO_ARCO - Personaje.ANCHO, vistaDe(dos).puestoDeGuardia(), DELTA);
    }

    @Test
    public void laMitadPropiaDependeDelLado() {
        partido.getBalon().resetearPosicion(200, 300);
        assertTrue(vistaDe(uno).balonEnMiMitad());
        assertFalse(vistaDe(dos).balonEnMiMitad());
    }

    @Test
    public void elBalonVaHaciaElArcoDeUnoSolo() {
        partido.getBalon().aplicarFuerza(5, 0);
        assertFalse(vistaDe(uno).balonVaHaciaMiArco());
        assertTrue(vistaDe(dos).balonVaHaciaMiArco());
    }

    @Test
    public void caminaHastaLlegarYAhiSeDetiene() {
        double centro = uno.getPersonaje().getPosX() + Personaje.ANCHO / 2.0;
        assertEquals(EnumSet.of(Accion.DERECHA), vistaDe(uno).caminarHacia(centro + 50));
        assertEquals(EnumSet.of(Accion.IZQUIERDA), vistaDe(uno).caminarHacia(centro - 50));
        assertTrue(vistaDe(uno).caminarHacia(centro + 2).isEmpty());
    }

    @Test
    public void laPrediccionDelBalonNoSeSaleDeLaCancha() {
        partido.getBalon().resetearPosicion(900, 300);
        partido.getBalon().aplicarFuerza(20, 0);
        assertEquals(1000, vistaDe(uno).balonXDentroDe(60), DELTA);
        partido.getBalon().aplicarFuerza(-20, 0);
        assertEquals(800, vistaDe(uno).balonXDentroDe(5), DELTA);
        assertEquals(0, vistaDe(uno).balonXDentroDe(600), DELTA);
    }

    @Test
    public void conBalonesFalsosSigueElMasCercano() {
        uno.cargarPoder(100);
        uno.realizarAccion("PODER", partido);
        partido.getBalon().resetearPosicion(uno.getPersonaje().getPosX() + 20, uno.getPersonaje().getPosY() + 20);
        uno.realizarAccion("PATEAR", partido);
        partido.actualizarEstado();
        assertEquals(7, partido.getBalonesFalsos().size(), "Precondición: hay balones falsos");

        // El real se deja al otro lado de la cancha; la máquina no sabe que es ese.
        partido.getBalon().resetearPosicion(20, 60);
        assertTrue(vistaDe(dos).balonXDentroDe(0) > 100, "Sigue uno falso porque lo tiene más cerca");
    }

    @Test
    public void reconoceUnTiroEnemigoPeroNoElPropio() {
        partido.getBalon().resetearPosicion(650, 500);
        partido.getBalon().aplicarDisparoEspecial(12, 0, 1);
        assertTrue(vistaDe(dos).tiroEnemigoEncima());
        partido.getBalon().aplicarDisparoEspecial(12, 0, 2);
        assertFalse(vistaDe(dos).tiroEnemigoEncima(), "El suyo no la amenaza");
        partido.getBalon().resetearPosicion(300, 500);
        partido.getBalon().aplicarDisparoEspecial(12, 0, 1);
        assertFalse(vistaDe(dos).tiroEnemigoEncima(), "Todavía está lejos");
    }

    @Test
    public void elRivalIndefensoNoPuedeJugar() {
        assertFalse(vistaDe(uno).rivalIndefenso());
        dos.getPersonaje().congelar(1);
        assertTrue(vistaDe(uno).rivalIndefenso(), "Congelado");
        dos.getPersonaje().aplicarInconsciencia(1);
        assertTrue(vistaDe(uno).rivalIndefenso(), "Inconsciente");
        dos.getPersonaje().aplicarConfusion(1);
        assertFalse(vistaDe(uno).rivalIndefenso(), "Confundido sí puede jugar");
        dos.getPersonaje().aturdir(1);
        assertTrue(vistaDe(uno).rivalIndefenso(), "Aturdido");
    }

    @Test
    public void conElAuraPuestaElPoderYaNoEstaListo() {
        uno.cargarPoder(100);
        assertTrue(vistaDe(uno).poderListo());
        uno.activarPoder();
        uno.cargarPoder(100);
        assertFalse(vistaDe(uno).poderListo());
    }

    @Test
    public void laVidaBajaEsCercaDeUnTercio() {
        uno.getPersonaje().recibirDanio(64);
        assertFalse(vistaDe(uno).vidaBaja(), "Con 36 todavía no");
        uno.getPersonaje().recibirDanio(1);
        assertTrue(vistaDe(uno).vidaBaja(), "Con 35 sí");
    }

    @Test
    public void sabeQuienVaGanando() {
        assertFalse(vistaDe(uno).voyGanando(), "Empate");
        uno.anotarGol();
        assertTrue(vistaDe(uno).voyGanando());
        assertFalse(vistaDe(dos).voyGanando());
    }

    @Test
    public void enUnPartidoPorGolesNuncaSeAcabaElTiempo() {
        assertFalse(vistaDe(uno).estaPorTerminar());
        partido.getMarcador()[0] = 2;
        assertFalse(vistaDe(uno).estaPorTerminar());
    }

    @Test
    public void elBalonEnTodoElCentroEsDeLosDos() {
        partido.getBalon().resetearPosicion(500, 534);
        assertTrue(vistaDe(uno).balonEnMiMitad());
        assertTrue(vistaDe(dos).balonEnMiMitad());
    }

    @Test
    public void elBalonEstaCercaSiSeLoPuedenDisputar() {
        partido.getBalon().resetearPosicion(uno.getPersonaje().getPosX() + 100, 500);
        assertTrue(vistaDe(uno).balonCerca());
        assertFalse(vistaDe(dos).balonCerca());
    }

    @Test
    public void elPartidoPorTiempoEstaPorTerminarEnLosUltimosSegundos() {
        partido = new PartidoPorTiempo(uno, dos, 1);
        for (int i = 0; i < 39 * 60; i++) partido.actualizarEstado();
        assertFalse(partido.estaPorTerminar(), "Faltan 21 segundos");
        for (int i = 0; i < 60; i++) partido.actualizarEstado();
        assertTrue(partido.estaPorTerminar(), "Faltan 20 segundos");
    }

    @Test
    public void elBalonPorEncimaDebeEstarAlAlcanceDelSalto() {
        double centro = uno.getPersonaje().getPosX() + Personaje.ANCHO / 2.0;
        partido.getBalon().resetearPosicion(centro, 400);
        assertTrue(vistaDe(uno).balonPorEncima());
        partido.getBalon().resetearPosicion(centro, 200);
        assertFalse(vistaDe(uno).balonPorEncima(), "Muy alto");
        partido.getBalon().resetearPosicion(centro + 200, 400);
        assertFalse(vistaDe(uno).balonPorEncima(), "Muy lejos");
        partido.getBalon().resetearPosicion(centro, 500);
        assertFalse(vistaDe(uno).balonPorEncima(), "A la altura del cuerpo");
    }
}
