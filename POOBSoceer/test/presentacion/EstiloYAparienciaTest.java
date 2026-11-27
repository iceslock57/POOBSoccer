package presentacion;

import org.junit.jupiter.api.Test;
import java.awt.Color;
import java.awt.image.BufferedImage;
import javax.swing.JButton;
import javax.swing.JLabel;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static presentacion.ApoyoVisual.*;

/** Pruebas del estilo de los menús y de las imágenes de los personajes. */
public class EstiloYAparienciaTest {

    // Estilo

    @Test
    public void losBotonesCompartenTamanoYColor() {
        JButton uno = EstiloGUI.boton("UNO"), dos = EstiloGUI.boton("DOS");
        assertEquals("UNO", uno.getText());
        assertEquals(uno.getPreferredSize(), dos.getPreferredSize());
        assertEquals(uno.getBackground(), dos.getBackground());
    }

    @Test
    public void elTituloUsaElTamanoPedidoYLetraBlanca() {
        JLabel titulo = EstiloGUI.titulo("HOLA", 31);
        assertEquals("HOLA", titulo.getText());
        assertEquals(31, titulo.getFont().getSize());
        assertEquals(Color.WHITE, titulo.getForeground());
    }

    @Test
    public void unaImagenQueNoExisteNoRompeElJuego() {
        assertNull(EstiloGUI.cargarImagen("recursos/no_existe.png"));
    }

    @Test
    public void cargaLasImagenesQueSiExisten() {
        assertNotNull(EstiloGUI.cargarImagen("recursos/fondo_menu.png"));
    }

    @Test
    public void elFondoCubreTodoElPanel() {
        BufferedImage imagen = foto(EstiloGUI.fondo(), 400, 300);
        int negro = Color.BLACK.getRGB();
        assertNotEquals(negro, imagen.getRGB(200, 150));
    }

    @Test
    public void elCespedDeLaPortadaCreceConLaVentana() {
        assumeTrue(EstiloGUI.hayPortada());
        assertTrue(EstiloGUI.altoCespedPortada(1000, 800) > EstiloGUI.altoCespedPortada(1000, 650));
        assertTrue(EstiloGUI.altoCespedPortada(2000, 300) >= 0, "Nunca es negativo");
    }

    @Test
    public void laPortadaSeDibujaAnchaYAlta() {
        assumeTrue(EstiloGUI.hayPortada());
        assertDoesNotThrow(() -> foto(EstiloGUI.portada(), 1200, 400));
        assertDoesNotThrow(() -> foto(EstiloGUI.portada(), 500, 800));
    }

    // Apariencia de los personajes

    @Test
    public void unPaisSinImagenesNoTieneApariencia() {
        assertNull(AparienciaPersonaje.de("Sin imagen"));
    }

    @Test
    public void losPaisesConImagenesTienenApariencia() {
        for (String pais : new String[]{"Corea del Sur", "Camerún", "Rusia", "Japón"}) {
            assertNotNull(AparienciaPersonaje.de(pais), pais);
        }
    }

    private static BufferedImage personaje(int direccion, double patada, boolean atenuado) {
        return dibujo(160, 160, g -> {
            g.setColor(Color.GREEN);
            g.fillRect(0, 0, 160, 160);
            AparienciaPersonaje.de("Rusia").dibujar(g, 50, 40, direccion, patada, atenuado);
        });
    }

    @Test
    public void elPersonajeSeDibujaDentroDeSuZona() {
        BufferedImage vacia = dibujo(160, 160, g -> {
            g.setColor(Color.GREEN);
            g.fillRect(0, 0, 160, 160);
        });
        assertTrue(diferencias(vacia, personaje(1, 0, false)) > 1000);
        assertEquals(Color.GREEN.getRGB(), personaje(1, 0, false).getRGB(5, 5), "Lejos no pinta nada");
    }

    @Test
    public void cadaJugadorMiraHaciaSuLado() {
        assertTrue(diferencias(personaje(1, 0, false), personaje(-1, 0, false)) > 100);
    }

    @Test
    public void laPatadaCambiaElDibujo() {
        assertTrue(diferencias(personaje(1, 0, false), personaje(1, .5, false)) > 100);
    }

    @Test
    public void aturdidoSeVeMasTransparente() {
        assertTrue(diferencias(personaje(1, 0, false), personaje(1, 0, true)) > 1000);
    }
}
