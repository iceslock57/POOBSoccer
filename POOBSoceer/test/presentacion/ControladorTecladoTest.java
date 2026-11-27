package presentacion;

import Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.event.KeyEvent;
import javax.swing.JPanel;
import static org.junit.jupiter.api.Assertions.*;
import static presentacion.ApoyoVisual.tecla;

/** Pruebas de las teclas: que cada una le llegue al jugador correcto y en el momento correcto. */
public class ControladorTecladoTest {

    private static final double DELTA = 1e-6;

    private POOBSoccer juego;
    private JPanel raiz;
    private ControladorTeclado teclado;
    private Personaje p1, p2;
    private int repintadas;

    @BeforeEach
    public void preparar() {
        repintadas = 0;
        raiz = new JPanel();
        juego = ApoyoVisual.partido("Rusia", "Japón");
        teclado = new ControladorTeclado(juego, raiz, () -> repintadas++);
        p1 = juego.getPartidoActual().getJugador1().getPersonaje();
        p2 = juego.getPartidoActual().getJugador2().getPersonaje();
    }

    private void presionar(int codigo) {
        tecla(raiz, codigo, false);
    }

    private void soltar(int codigo) {
        tecla(raiz, codigo, true);
    }

    @Test
    public void mientrasSeMantieneLaTeclaElJugadorUnoCamina() {
        double x = p1.getPosX();
        presionar(KeyEvent.VK_D);
        for (int i = 0; i < 3; i++) teclado.procesarTeclasContinuas();
        assertEquals(x + 3 * Personaje.PASO_BASE, p1.getPosX(), DELTA);
        soltar(KeyEvent.VK_D);
        teclado.procesarTeclasContinuas();
        assertEquals(x + 3 * Personaje.PASO_BASE, p1.getPosX(), DELTA, "Al soltar se detiene");
    }

    @Test
    public void elJugadorUnoTambienVaALaIzquierda() {
        double x = p1.getPosX();
        presionar(KeyEvent.VK_A);
        teclado.procesarTeclasContinuas();
        assertEquals(x - Personaje.PASO_BASE, p1.getPosX(), DELTA);
    }

    @Test
    public void elJugadorDosCaminaConLasFlechas() {
        double x = p2.getPosX();
        presionar(KeyEvent.VK_LEFT);
        teclado.procesarTeclasContinuas();
        assertEquals(x - Personaje.PASO_BASE, p2.getPosX(), DELTA);
        soltar(KeyEvent.VK_LEFT);
        presionar(KeyEvent.VK_RIGHT);
        teclado.procesarTeclasContinuas();
        assertEquals(x, p2.getPosX(), DELTA);
    }

    @Test
    public void losDosPuedenMoverseAlMismoTiempo() {
        double x1 = p1.getPosX(), x2 = p2.getPosX();
        presionar(KeyEvent.VK_D);
        presionar(KeyEvent.VK_LEFT);
        teclado.procesarTeclasContinuas();
        assertTrue(p1.getPosX() > x1);
        assertTrue(p2.getPosX() < x2);
    }

    @Test
    public void cadaUnoSaltaConSuTecla() {
        double y = p1.getPosY();
        presionar(KeyEvent.VK_W);
        juego.actualizar();
        assertTrue(p1.getPosY() < y);
        assertEquals(y, p2.getPosY(), DELTA, "El otro sigue en el suelo");
        presionar(KeyEvent.VK_UP);
        juego.actualizar();
        assertTrue(p2.getPosY() < y);
    }

    @Test
    public void cadaUnoPateaConSuTecla() {
        presionar(KeyEvent.VK_S);
        assertTrue(p1.isPateando());
        assertFalse(p2.isPateando());
        presionar(KeyEvent.VK_M);
        assertTrue(p2.isPateando());
    }

    @Test
    public void cadaUnoPreparaSuPoderConSuTecla() {
        Jugador j1 = juego.getPartidoActual().getJugador1(), j2 = juego.getPartidoActual().getJugador2();
        j1.cargarPoder(100);
        j2.cargarPoder(100);
        presionar(KeyEvent.VK_Q);
        assertTrue(j1.tienePoderPreparado());
        assertFalse(j2.tienePoderPreparado());
        presionar(KeyEvent.VK_N);
        assertTrue(j2.tienePoderPreparado());
    }

    @Test
    public void lasTeclasDeUnaVezRepintanLaCancha() {
        presionar(KeyEvent.VK_S);
        assertEquals(1, repintadas);
    }

    @Test
    public void enPausaNoSePuedePatear() {
        juego.pausarJuego();
        presionar(KeyEvent.VK_S);
        assertFalse(p1.isPateando());
    }

    @Test
    public void conElPartidoTerminadoNoSePuedePatear() {
        juego.getPartidoActual().terminar();
        presionar(KeyEvent.VK_S);
        assertFalse(p1.isPateando());
    }

    @Test
    public void reiniciarOlvidaLasTeclasPresionadas() {
        double x = p1.getPosX();
        presionar(KeyEvent.VK_D);
        teclado.reiniciar();
        teclado.procesarTeclasContinuas();
        assertEquals(x, p1.getPosX(), DELTA);
    }

    @Test
    public void sinPartidoLasTeclasNoHacenNada() {
        JPanel otra = new JPanel();
        ControladorTeclado sinPartido = new ControladorTeclado(new POOBSoccer(), otra, () -> repintadas++);
        assertDoesNotThrow(() -> {
            tecla(otra, KeyEvent.VK_S, false);
            tecla(otra, KeyEvent.VK_D, false);
            sinPartido.procesarTeclasContinuas();
        });
        assertEquals(0, repintadas);
    }

    @Test
    public void lasTeclasDelJugadorDosNoMuevenALaMaquina() {
        juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_MAQUINA, CatalogoPersonajes.crear("Rusia"),
                CatalogoPersonajes.crear("Japón"), 5, 1000, 650, "Experta");
        Personaje maquina = juego.getPartidoActual().getJugador2().getPersonaje();
        double x = maquina.getPosX();
        presionar(KeyEvent.VK_RIGHT);
        teclado.procesarTeclasContinuas();
        presionar(KeyEvent.VK_M);
        assertEquals(x, maquina.getPosX(), DELTA);
        assertFalse(maquina.isPateando());
    }
}
