package presentacion;

import Domain.CatalogoPersonajes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JSpinner;
import static org.junit.jupiter.api.Assertions.*;
import static presentacion.ApoyoVisual.*;

/** Pruebas de la pantalla donde se eligen los países y se configura la cancha. */
public class PantallaSeleccionTest {

    private PantallaSeleccion pantalla;
    private List<String> avisos;

    @BeforeEach
    public void preparar() {
        avisos = new ArrayList<>();
        pantalla = new PantallaSeleccion(() -> avisos.add("jugar"), () -> avisos.add("volver"),
                jugador -> avisos.add("estadísticas " + jugador));
        pantalla.prepararPara("Goles");
    }

    /** Los botones van en orden: primero los del jugador 1 y luego los del jugador 2. */
    private JButton flecha(String texto, int jugador) {
        List<JButton> flechas = buscar(pantalla, JButton.class).stream()
                .filter(b -> texto.equals(b.getText())).collect(Collectors.toList());
        return flechas.get(jugador - 1);
    }

    @Test
    public void comienzaConLosDosPrimerosPaises() {
        assertEquals(CatalogoPersonajes.paises().get(0), pantalla.getPais1());
        assertEquals(CatalogoPersonajes.paises().get(1), pantalla.getPais2());
    }

    @Test
    public void lasFlechasDelJugadorUnoSoloCambianSuPais() {
        String pais2 = pantalla.getPais2();
        flecha(">>", 1).doClick(0);
        assertEquals(CatalogoPersonajes.paises().get(1), pantalla.getPais1());
        flecha("<<", 1).doClick(0);
        assertEquals(CatalogoPersonajes.paises().get(0), pantalla.getPais1());
        assertEquals(pais2, pantalla.getPais2());
    }

    @Test
    public void lasFlechasDelJugadorDosSoloCambianSuPais() {
        String pais1 = pantalla.getPais1();
        flecha(">>", 2).doClick(0);
        assertEquals(CatalogoPersonajes.paises().get(2), pantalla.getPais2());
        flecha("<<", 2).doClick(0);
        assertEquals(CatalogoPersonajes.paises().get(1), pantalla.getPais2());
        assertEquals(pais1, pantalla.getPais1());
    }

    @Test
    public void laListaDePaisesDaLaVuelta() {
        List<String> paises = CatalogoPersonajes.paises();
        flecha("<<", 1).doClick(0);
        assertEquals(paises.get(paises.size() - 1), pantalla.getPais1(), "Antes del primero está el último");
        flecha(">>", 1).doClick(0);
        assertEquals(paises.get(0), pantalla.getPais1(), "Después del último está el primero");
    }

    @Test
    public void sePuedeElegirCualquierPais() {
        List<String> vistos = new ArrayList<>();
        for (int i = 0; i < CatalogoPersonajes.paises().size(); i++) {
            vistos.add(pantalla.getPais1());
            flecha(">>", 1).doClick(0);
        }
        assertEquals(CatalogoPersonajes.paises(), vistos);
    }

    @Test
    public void porTiempoPideMinutos() {
        pantalla.prepararPara("Tiempo");
        assertTrue(hayTexto(pantalla, "Duración (minutos):"));
        assertEquals(2, pantalla.getObjetivo());
    }

    @Test
    public void porGolesPideGoles() {
        assertTrue(hayTexto(pantalla, "Goles para ganar:"));
        assertEquals(5, pantalla.getObjetivo());
    }

    @Test
    public void entregaElObjetivoQueSeEscoge() {
        buscar(pantalla, JSpinner.class).get(0).setValue(8);
        assertEquals(8, pantalla.getObjetivo());
    }

    @Test
    public void laCanchaTieneTresTamanos() {
        JComboBox<?> combo = buscar(pantalla, JComboBox.class).get(0);
        int[][] esperados = {{800, 550}, {1000, 650}, {1200, 750}};
        assertEquals(3, combo.getItemCount());
        for (int i = 0; i < esperados.length; i++) {
            combo.setSelectedIndex(i);
            assertEquals(esperados[i][0], pantalla.getAnchoCancha());
            assertEquals(esperados[i][1], pantalla.getAltoCancha());
        }
    }

    @Test
    public void laCanchaMedianaEstaEscogidaAlComienzo() {
        assertEquals(1000, pantalla.getAnchoCancha());
        assertEquals(650, pantalla.getAltoCancha());
    }

    @Test
    public void muestraSiElRivalEsUnaMaquina() {
        assertTrue(hayTexto(pantalla, "JUGADOR 2"));
        pantalla.mostrarRival("Experta");
        assertTrue(hayTexto(pantalla, "MÁQUINA EXPERTA"));
        assertFalse(hayTexto(pantalla, "JUGADOR 2"));
        pantalla.mostrarRival(null);
        assertTrue(hayTexto(pantalla, "JUGADOR 2"), "Vuelve a ser una persona");
    }

    @Test
    public void losBotonesAvisanJugarYVolver() {
        boton(pantalla, "JUGAR").doClick(0);
        boton(pantalla, "VOLVER").doClick(0);
        assertEquals(Arrays.asList("jugar", "volver"), avisos);
    }

    @Test
    public void cadaJugadorTieneSuBotonDeEstadisticas() {
        flecha("ESTADÍSTICAS", 1).doClick(0);
        flecha("ESTADÍSTICAS", 2).doClick(0);
        assertEquals(Arrays.asList("estadísticas 1", "estadísticas 2"), avisos);
    }

    @Test
    public void laMaquinaNoTieneEstadisticasParaMejorar() {
        pantalla.mostrarRival("Torpe");
        assertTrue(flecha("ESTADÍSTICAS", 1).isVisible(), "La persona sí las tiene");
        assertFalse(flecha("ESTADÍSTICAS", 2).isVisible());
        pantalla.mostrarRival(null);
        assertTrue(flecha("ESTADÍSTICAS", 2).isVisible());
    }

    @Test
    public void cadaPaisSeMuestraConSuImagenOConSuNombre() {
        for (String pais : CatalogoPersonajes.paises()) {
            boolean conNombre = hayTexto(pantalla, pais);
            boolean conImagen = buscar(pantalla, javax.swing.JLabel.class).stream()
                    .anyMatch(l -> l.getIcon() != null && pais.equals(l.getToolTipText()));
            assertTrue(conNombre || conImagen, pais);
            flecha(">>", 1).doClick(0);
        }
    }

    @Test
    public void seDibujaSinErrores() {
        assertDoesNotThrow(() -> foto(pantalla, 1000, 650));
    }
}
