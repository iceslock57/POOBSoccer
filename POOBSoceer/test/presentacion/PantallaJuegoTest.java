package presentacion;

import Domain.POOBSoccer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static presentacion.ApoyoVisual.*;

/** Pruebas de la pantalla del partido y de sus botones. */
public class PantallaJuegoTest {

    private POOBSoccer juego;
    private PantallaJuego pantalla;
    private int idasAlMenu;

    @BeforeEach
    public void preparar() {
        idasAlMenu = 0;
        juego = ApoyoVisual.partido("Rusia", "Japón");
        pantalla = new PantallaJuego(juego, () -> idasAlMenu++);
    }

    @Test
    public void contieneLaCancha() {
        assertEquals(1, buscar(pantalla, PanelCancha.class).size());
    }

    @Test
    public void elMismoBotonPausaYReanuda() {
        boton(pantalla, "PAUSAR / REANUDAR").doClick(0);
        assertTrue(juego.isPausa());
        boton(pantalla, "PAUSAR / REANUDAR").doClick(0);
        assertFalse(juego.isPausa());
    }

    @Test
    public void sePuedeTerminarElPartido() {
        boton(pantalla, "TERMINAR PARTIDO").doClick(0);
        assertTrue(juego.getPartidoActual().isTerminado());
    }

    @Test
    public void elBotonMenuAvisa() {
        boton(pantalla, "MENÚ").doClick(0);
        assertEquals(1, idasAlMenu);
    }

    @Test
    public void laCanchaPuedeRecibirElFoco() {
        assertTrue(buscar(pantalla, PanelCancha.class).get(0).isFocusable());
        assertDoesNotThrow(pantalla::enfocar);
    }

    @Test
    public void seDibujaYSeRepintaSinErrores() {
        assertDoesNotThrow(() -> foto(pantalla, 1000, 745));
        assertDoesNotThrow(pantalla::repintar);
    }
}
