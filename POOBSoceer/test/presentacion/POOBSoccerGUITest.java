package presentacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static presentacion.ApoyoVisual.*;

/**
 * Pruebas del recorrido por las pantallas. La ventana se crea pero no se muestra.
 * En un equipo sin pantalla estas pruebas se omiten.
 */
public class POOBSoccerGUITest {

    private POOBSoccerGUI ventana;

    @BeforeEach
    public void preparar() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Se necesita una pantalla");
        SwingUtilities.invokeAndWait(() -> ventana = new POOBSoccerGUI());
    }

    @AfterEach
    public void cerrar() throws Exception {
        if (ventana == null) return;
        SwingUtilities.invokeAndWait(() -> {
            // Volver al menú detiene el reloj del juego antes de cerrar.
            if (visible() instanceof PantallaJuego) boton((Container) visible(), "MENÚ").doClick(0);
            ventana.dispose();
        });
    }

    /** Pantalla que se está mostrando en este momento. */
    private Component visible() {
        for (Component c : ventana.getContentPane().getComponents()) {
            if (c.isVisible()) return c;
        }
        return null;
    }

    private void presionar(String... botones) throws Exception {
        for (String texto : botones) {
            SwingUtilities.invokeAndWait(() -> boton((Container) visible(), texto).doClick(0));
        }
    }

    private void assertPantalla(Class<?> esperada) throws Exception {
        AtomicReference<Component> actual = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> actual.set(visible()));
        assertInstanceOf(esperada, actual.get());
    }

    @Test
    public void comienzaEnElInicio() throws Exception {
        assertPantalla(PantallaInicio.class);
    }

    @Test
    public void jugarLlevaAElegirElModo() throws Exception {
        presionar("PLAY");
        assertPantalla(PantallaModalidad.class);
    }

    @Test
    public void entreDosPersonasNoSePreguntaPorLaMaquina() throws Exception {
        presionar("PLAY", "JUGADOR VS JUGADOR");
        assertPantalla(PantallaTipos.class);
        presionar("VOLVER");
        assertPantalla(PantallaModalidad.class);
    }

    @Test
    public void contraLaMaquinaSeEligeSuPerfil() throws Exception {
        presionar("PLAY", "JUGADOR VS MÁQUINA");
        assertPantalla(PantallaMaquina.class);
        presionar("DEFENSIVA");
        assertPantalla(PantallaTipos.class);
        presionar("VOLVER");
        assertPantalla(PantallaMaquina.class);
    }

    @Test
    public void volverRegresaPasoAPaso() throws Exception {
        presionar("PLAY", "JUGADOR VS MÁQUINA", "TORPE", "PARTIDO POR GOLES");
        assertPantalla(PantallaSeleccion.class);
        presionar("VOLVER");
        assertPantalla(PantallaTipos.class);
        presionar("VOLVER", "VOLVER");
        assertPantalla(PantallaModalidad.class);
        presionar("VOLVER");
        assertPantalla(PantallaInicio.class);
    }

    @Test
    public void laSeleccionMuestraElRivalElegido() throws Exception {
        presionar("PLAY", "JUGADOR VS MÁQUINA", "EXPERTA", "PARTIDO POR TIEMPO");
        assertTrue(hayTexto((Container) visible(), "MÁQUINA EXPERTA"));
    }

    @Test
    public void siSeCambiaDeModoElRivalVuelveASerUnaPersona() throws Exception {
        presionar("PLAY", "JUGADOR VS MÁQUINA", "EXPERTA", "VOLVER", "VOLVER", "JUGADOR VS JUGADOR", "PARTIDO POR GOLES");
        assertTrue(hayTexto((Container) visible(), "JUGADOR 2"));
    }

    @Test
    public void sePuedeEmpezarUnPartidoEntreDosPersonas() throws Exception {
        presionar("PLAY", "JUGADOR VS JUGADOR", "PARTIDO POR TIEMPO", "JUGAR");
        assertPantalla(PantallaJuego.class);
    }

    @Test
    public void sePuedeEmpezarUnPartidoContraCadaMaquina() throws Exception {
        for (String perfil : new String[]{"TORPE", "OFENSIVA", "DEFENSIVA", "EXPERTA"}) {
            presionar("PLAY", "JUGADOR VS MÁQUINA", perfil, "PARTIDO POR GOLES", "JUGAR");
            assertPantalla(PantallaJuego.class);
            presionar("MENÚ");
            assertPantalla(PantallaInicio.class);
        }
    }

    @Test
    public void laVentanaSeAjustaALaCancha() throws Exception {
        presionar("PLAY", "JUGADOR VS JUGADOR", "PARTIDO POR GOLES", "JUGAR");
        assertEquals(1000, ventana.getWidth());
        assertEquals(745 + ventana.getJMenuBar().getPreferredSize().height, ventana.getHeight());
    }

    @Test
    public void elJuegoAvanzaSolo() throws Exception {
        presionar("PLAY", "JUGADOR VS MÁQUINA", "OFENSIVA", "PARTIDO POR GOLES", "JUGAR");
        PanelCancha cancha = buscar((Container) visible(), PanelCancha.class).get(0);
        java.awt.image.BufferedImage antes = fotoEnPantalla(cancha);
        Thread.sleep(400);
        assertTrue(diferencias(antes, fotoEnPantalla(cancha)) > 100, "El balón cae y la máquina se mueve");
    }

    private static java.awt.image.BufferedImage fotoEnPantalla(PanelCancha cancha) throws Exception {
        AtomicReference<java.awt.image.BufferedImage> imagen = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> imagen.set(foto(cancha, 1000, 650)));
        return imagen.get();
    }

    /** El botón de estadísticas del jugador 1 es el primero que aparece en la pantalla. */
    @Test
    public void lasEstadisticasSonDelPaisElegido() throws Exception {
        presionar("PLAY", "JUGADOR VS JUGADOR", "PARTIDO POR GOLES", ">>", "ESTADÍSTICAS");
        assertPantalla(PantallaEstadisticas.class);
        assertTrue(hayTexto((Container) visible(), "ESTADÍSTICAS DE CAMERÚN - JUGADOR 1"));
        presionar("VOLVER");
        assertPantalla(PantallaSeleccion.class);
    }

    @Test
    public void losPuntosDelPartidoAparecenEnLasEstadisticas() throws Exception {
        presionar("PLAY", "JUGADOR VS JUGADOR", "PARTIDO POR GOLES", "JUGAR", "TERMINAR PARTIDO", "MENÚ");
        presionar("PLAY", "JUGADOR VS JUGADOR", "PARTIDO POR GOLES", "ESTADÍSTICAS");
        assertTrue(hayTexto((Container) visible(), "Puntos disponibles: 25"), "Un empate da 25 puntos");
    }

    @Test
    public void elJugadorDosVeSusPropiasEstadisticas() throws Exception {
        presionar("PLAY", "JUGADOR VS JUGADOR", "PARTIDO POR GOLES");
        SwingUtilities.invokeAndWait(() -> {
            java.util.List<javax.swing.JButton> botones = buscar((Container) visible(), javax.swing.JButton.class);
            botones.removeIf(b -> !"ESTADÍSTICAS".equals(b.getText()));
            botones.get(1).doClick(0);
        });
        assertTrue(hayTexto((Container) visible(), "ESTADÍSTICAS DE CAMERÚN - JUGADOR 2"));
    }

    @Test
    public void perderElFocoNoCausaErrores() throws Exception {
        SwingUtilities.invokeAndWait(() ->
                ventana.dispatchEvent(new WindowEvent(ventana, WindowEvent.WINDOW_LOST_FOCUS)));
        assertPantalla(PantallaInicio.class);
    }

    @Test
    public void salirCierraLaVentana() throws Exception {
        presionar("SALIR");
        assertFalse(ventana.isDisplayable());
    }
}
