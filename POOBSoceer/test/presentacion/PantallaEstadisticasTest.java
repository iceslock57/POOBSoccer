package presentacion;

import Domain.PerfilJugador;
import Domain.PerfilJugador.Estadistica;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JProgressBar;
import static org.junit.jupiter.api.Assertions.*;
import static presentacion.ApoyoVisual.*;

/** Pruebas de la pantalla donde se mejoran los personajes. */
public class PantallaEstadisticasTest {

    private PantallaEstadisticas pantalla;
    private PerfilJugador perfil;
    private int vueltas;

    @BeforeEach
    public void preparar() {
        vueltas = 0;
        perfil = new PerfilJugador("jugador1", "Uno");
        pantalla = new PantallaEstadisticas(() -> vueltas++);
    }

    private void mostrar(String pais, long puntos) {
        perfil.agregarPuntos(puntos);
        pantalla.mostrar("JUGADOR 1", perfil, pais);
    }

    /** Botones para mejorar, en el orden vida, velocidad, salto y patada. */
    private List<JButton> mejoras() {
        return buscar(pantalla, JButton.class).stream()
                .filter(b -> !"VOLVER".equals(b.getText())).collect(Collectors.toList());
    }

    private JButton mejora(Estadistica e) {
        return mejoras().get(e.ordinal());
    }

    private int barra(Estadistica e) {
        return buscar(pantalla, JProgressBar.class).get(e.ordinal()).getValue();
    }

    @Test
    public void diceDeQuienSonLasEstadisticas() {
        mostrar("Rusia", 0);
        assertTrue(hayTexto(pantalla, "ESTADÍSTICAS DE RUSIA - JUGADOR 1"));
    }

    @Test
    public void muestraLosPuntosDisponibles() {
        mostrar("Rusia", 725);
        assertTrue(hayTexto(pantalla, "Puntos disponibles: 725"));
    }

    @Test
    public void tieneUnaFilaPorCadaEstadistica() {
        mostrar("Rusia", 0);
        assertEquals(4, mejoras().size());
        for (String nombre : new String[]{"Vida", "Velocidad", "Salto", "Patada"}) {
            assertTrue(hayTexto(pantalla, nombre), nombre);
        }
    }

    @Test
    public void sinPuntosLosBotonesEstanApagados() {
        mostrar("Rusia", 499);
        for (JButton b : mejoras()) assertFalse(b.isEnabled());
    }

    @Test
    public void conPuntosSePuedeMejorar() {
        mostrar("Rusia", 500);
        for (JButton b : mejoras()) assertTrue(b.isEnabled());
    }

    @Test
    public void cadaBotonMejoraSuPropiaEstadistica() {
        mostrar("Rusia", 100000);
        for (Estadistica e : Estadistica.values()) {
            mejora(e).doClick(0);
            assertEquals(2, perfil.getNivel("Rusia", e), e.name());
            assertEquals(2, barra(e), e.name());
        }
    }

    @Test
    public void alMejorarSeActualizanLosPuntosYElPrecio() {
        mostrar("Rusia", 600);
        mejora(Estadistica.SPEED).doClick(0);
        assertTrue(hayTexto(pantalla, "Puntos disponibles: 100"));
        assertEquals("MEJORAR (1000)", mejora(Estadistica.SPEED).getText());
        assertFalse(mejora(Estadistica.SPEED).isEnabled(), "Ya no le alcanza");
        assertTrue(hayTexto(pantalla, "Velocidad de Rusia subió al nivel 2"));
    }

    @Test
    public void muestraLoQueSeGanaConElNivel() {
        mostrar("Rusia", 100000);
        assertTrue(hayTexto(pantalla, "100 HP"));
        mejora(Estadistica.VIDA).doClick(0);
        mejora(Estadistica.KICK).doClick(0);
        assertTrue(hayTexto(pantalla, "110 HP"));
        assertTrue(hayTexto(pantalla, "+10 %"));
    }

    @Test
    public void alLlegarAlMaximoYaNoSeVende() {
        mostrar("Rusia", Long.MAX_VALUE / 2);
        for (int i = 0; i < 9; i++) mejora(Estadistica.JUMP).doClick(0);
        assertEquals("MÁXIMO", mejora(Estadistica.JUMP).getText());
        assertFalse(mejora(Estadistica.JUMP).isEnabled());
        assertTrue(mejora(Estadistica.SPEED).isEnabled(), "Las demás siguen disponibles");
    }

    @Test
    public void alCambiarDePersonajeSeVenSusPropiosNiveles() {
        mostrar("Rusia", 500);
        mejora(Estadistica.SPEED).doClick(0);
        mostrar("Japón", 0);
        assertEquals(1, barra(Estadistica.SPEED), "Japón no recibió la mejora");
        assertEquals("MEJORAR (500)", mejora(Estadistica.SPEED).getText());
        assertFalse(hayTexto(pantalla, "Velocidad de Rusia subió al nivel 2"), "El mensaje anterior se borra");
        mostrar("Rusia", 0);
        assertEquals(2, barra(Estadistica.SPEED), "Rusia la conserva");
    }

    @Test
    public void elOtroJugadorTieneOtrasEstadisticas() {
        mostrar("Rusia", 500);
        mejora(Estadistica.SPEED).doClick(0);
        pantalla.mostrar("JUGADOR 2", new PerfilJugador("jugador2", "Dos"), "Rusia");
        assertEquals(1, barra(Estadistica.SPEED));
    }

    @Test
    public void elBotonVolverAvisa() {
        mostrar("Rusia", 0);
        boton(pantalla, "VOLVER").doClick(0);
        assertEquals(1, vueltas);
    }

    @Test
    public void seDibujaSinErrores() {
        mostrar("Estados Unidos", 2500);
        assertDoesNotThrow(() -> foto(pantalla, 1000, 650));
    }
}
