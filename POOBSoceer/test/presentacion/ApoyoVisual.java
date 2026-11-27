package presentacion;

import Domain.CatalogoPersonajes;
import Domain.POOBSoccer;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.swing.*;
import static org.junit.jupiter.api.Assertions.*;

/** Ayudas que comparten las pruebas de presentación: buscar componentes, presionar teclas y tomar fotos. */
final class ApoyoVisual {

    private ApoyoVisual() {
    }

    /** Busca en toda la pantalla los componentes de una clase. */
    static <T extends Component> List<T> buscar(Container raiz, Class<T> clase) {
        List<T> encontrados = new ArrayList<>();
        for (Component c : raiz.getComponents()) {
            if (clase.isInstance(c)) encontrados.add(clase.cast(c));
            if (c instanceof Container) encontrados.addAll(buscar((Container) c, clase));
        }
        return encontrados;
    }

    static JButton boton(Container raiz, String texto) {
        for (JButton b : buscar(raiz, JButton.class)) {
            if (texto.equals(b.getText())) return b;
        }
        return fail("No se encontró el botón " + texto);
    }

    static boolean hayTexto(Container raiz, String texto) {
        for (JLabel l : buscar(raiz, JLabel.class)) {
            if (texto.equals(l.getText())) return true;
        }
        return false;
    }

    /** Presiona o suelta una tecla de la misma forma en que lo haría la ventana. */
    static void tecla(JComponent raiz, int codigo, boolean alSoltar) {
        Object nombre = raiz.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).get(KeyStroke.getKeyStroke(codigo, 0, alSoltar));
        assertNotNull(nombre, "Tecla sin acción: " + KeyEvent.getKeyText(codigo));
        raiz.getActionMap().get(nombre).actionPerformed(null);
    }

    static POOBSoccer partido(String pais1, String pais2) {
        POOBSoccer juego = new POOBSoccer();
        juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_JUGADOR,
                CatalogoPersonajes.crear(pais1), CatalogoPersonajes.crear(pais2), 5, 1000, 650);
        return juego;
    }

    /** Foto de un componente con el tamaño indicado. */
    static BufferedImage foto(JComponent componente, int ancho, int alto) {
        componente.setSize(ancho, alto);
        acomodar(componente);
        return dibujo(ancho, alto, componente::paint);
    }

    private static void acomodar(Container c) {
        c.doLayout();
        for (Component hijo : c.getComponents()) {
            if (hijo instanceof Container) acomodar((Container) hijo);
        }
    }

    /** Imagen vacía sobre la que se hace un dibujo. */
    static BufferedImage dibujo(int ancho, int alto, Consumer<Graphics2D> dibujo) {
        BufferedImage imagen = new BufferedImage(ancho, alto, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = imagen.createGraphics();
        dibujo.accept(g);
        g.dispose();
        return imagen;
    }

    /** Cantidad de puntos que cambian entre dos imágenes del mismo tamaño. */
    static int diferencias(BufferedImage a, BufferedImage b) {
        return diferencias(a, b, new Rectangle(0, 0, a.getWidth(), a.getHeight()));
    }

    static int diferencias(BufferedImage a, BufferedImage b, Rectangle zona) {
        int distintos = 0;
        for (int x = zona.x; x < zona.x + zona.width; x++) {
            for (int y = zona.y; y < zona.y + zona.height; y++) {
                if (a.getRGB(x, y) != b.getRGB(x, y)) distintos++;
            }
        }
        return distintos;
    }
}
