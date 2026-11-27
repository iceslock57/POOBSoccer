package presentacion;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import persistencia.ArchivoBits;
import persistencia.ArchivoDat;
import persistencia.ArchivoPartida;
import java.awt.Component;
import java.awt.Container;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;
import static presentacion.ApoyoVisual.*;

/** Pruebas del menú Archivo y de lo que hace la ventana al guardar y abrir partidas. */
public class MenuArchivoTest {

    @TempDir
    File carpeta;

    private POOBSoccerGUI ventana;
    private DialogosDePrueba dialogos;

    /** Responde siempre con el mismo archivo y anota los mensajes, sin mostrar ninguna ventana. */
    private static final class DialogosDePrueba implements Dialogos {
        private File archivo;
        private int preguntas;
        private final List<String> avisos = new ArrayList<>(), errores = new ArrayList<>();

        @Override
        public File paraGuardar(Component padre, ArchivoPartida formato) {
            preguntas++;
            return archivo;
        }

        @Override
        public File paraAbrir(Component padre, ArchivoPartida formato) {
            preguntas++;
            return archivo;
        }

        @Override
        public void informar(Component padre, String mensaje) {
            avisos.add(mensaje);
        }

        @Override
        public void error(Component padre, String mensaje) {
            errores.add(mensaje);
        }
    }

    @BeforeEach
    public void preparar() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Se necesita una pantalla");
        dialogos = new DialogosDePrueba();
        SwingUtilities.invokeAndWait(() -> ventana = new POOBSoccerGUI(dialogos));
    }

    @AfterEach
    public void cerrar() throws Exception {
        if (ventana == null) return;
        SwingUtilities.invokeAndWait(() -> {
            if (visible() instanceof PantallaJuego) boton((Container) visible(), "MENÚ").doClick(0);
            ventana.dispose();
        });
    }

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

    private void menu(String opcion) throws Exception {
        JMenu archivo = ventana.getJMenuBar().getMenu(0);
        for (int i = 0; i < archivo.getItemCount(); i++) {
            JMenuItem item = archivo.getItem(i);
            if (item != null && opcion.equals(item.getText())) {
                SwingUtilities.invokeAndWait(() -> item.doClick(0));
                return;
            }
        }
        fail("No está la opción " + opcion);
    }

    private void jugarContraLaMaquina() throws Exception {
        presionar("PLAY", "JUGADOR VS MÁQUINA", "EXPERTA", "PARTIDO POR GOLES", "JUGAR");
    }

    private BufferedImage cancha() throws Exception {
        BufferedImage[] imagen = new BufferedImage[1];
        SwingUtilities.invokeAndWait(() ->
                imagen[0] = foto(buscar((Container) visible(), PanelCancha.class).get(0), 1000, 650));
        return imagen[0];
    }

    /** Dice si el partido avanza; en pausa dos fotos seguidas son iguales. */
    private boolean estaDetenido() throws Exception {
        BufferedImage antes = cancha();
        Thread.sleep(200);
        return diferencias(antes, cancha()) == 0;
    }

    // Menú

    @Test
    public void ofreceLasCuatroOpciones() {
        JMenu archivo = ventana.getJMenuBar().getMenu(0);
        List<String> textos = new ArrayList<>();
        for (int i = 0; i < archivo.getItemCount(); i++) {
            if (archivo.getItem(i) != null) textos.add(archivo.getItem(i).getText());
        }
        assertEquals(Arrays.asList("Abrir", "Salvar", "Importar", "Exportar"), textos);
    }

    @Test
    public void cadaOpcionUsaSuFormato() {
        List<String> pedidos = new ArrayList<>();
        MenuArchivo menu = new MenuArchivo(f -> pedidos.add("guardar " + f.getExtension()),
                f -> pedidos.add("abrir " + f.getExtension()));
        JMenu archivo = menu.getMenu(0);
        for (int i = 0; i < archivo.getItemCount(); i++) {
            if (archivo.getItem(i) != null) archivo.getItem(i).doClick(0);
        }
        assertEquals(Arrays.asList("abrir dat", "guardar dat", "abrir bin", "guardar bin"), pedidos);
    }

    @Test
    public void lasTeclasDelMenuLlevanControl() {
        JMenu archivo = ventana.getJMenuBar().getMenu(0);
        for (int i = 0; i < archivo.getItemCount(); i++) {
            JMenuItem item = archivo.getItem(i);
            if (item != null) assertTrue(item.getAccelerator().toString().contains("ctrl"), item.getText());
        }
    }

    // Guardar

    @Test
    public void salvarCreaElArchivoYLoAvisa() throws Exception {
        dialogos.archivo = new File(carpeta, "mia.dat");
        jugarContraLaMaquina();
        menu("Salvar");
        assertTrue(dialogos.archivo.length() > 0);
        assertTrue(dialogos.avisos.get(0).contains("mia.dat"));
        assertTrue(dialogos.errores.isEmpty());
    }

    @Test
    public void exportarCreaElArchivoEnBits() throws Exception {
        dialogos.archivo = new File(carpeta, "mia.bin");
        jugarContraLaMaquina();
        menu("Exportar");
        assertDoesNotThrow(() -> new ArchivoBits().abrir(new Domain.POOBSoccer(), dialogos.archivo));
    }

    @Test
    public void sinPartidoSeGuardanLosPuntosYLasEstadisticas() throws Exception {
        for (String opcion : new String[]{"Salvar", "Exportar"}) {
            dialogos.archivo = new File(carpeta, opcion);
            menu(opcion);
            assertTrue(dialogos.archivo.length() > 0, opcion);
        }
        assertTrue(dialogos.errores.isEmpty());
        assertTrue(dialogos.avisos.get(0).contains("puntos y las estadísticas"));
        assertInstanceOf(PantallaInicio.class, visible(), "Sigue en la misma pantalla");
    }

    @Test
    public void losPuntosGuardadosVuelvenAlAbrir() throws Exception {
        dialogos.archivo = new File(carpeta, "puntos.dat");
        presionar("PLAY", "JUGADOR VS JUGADOR", "PARTIDO POR GOLES", "JUGAR", "TERMINAR PARTIDO");
        menu("Salvar");
        presionar("MENÚ", "PLAY", "JUGADOR VS JUGADOR", "PARTIDO POR GOLES", "JUGAR", "TERMINAR PARTIDO", "MENÚ");
        menu("Abrir");
        presionar("PLAY", "JUGADOR VS JUGADOR", "PARTIDO POR GOLES", "ESTADÍSTICAS");
        assertTrue(hayTexto((Container) visible(), "Puntos disponibles: 25"), "Los del primer partido, que se guardó");
    }

    @Test
    public void abrirSoloProgresoRetiraElPartidoYVuelveAlInicio() throws Exception {
        dialogos.archivo = new File(carpeta, "puntos.bin");
        menu("Exportar");
        jugarContraLaMaquina();
        menu("Importar");
        assertInstanceOf(PantallaInicio.class, visible());
        assertTrue(dialogos.avisos.get(1).contains("Se recuperaron"));
        assertTrue(dialogos.errores.isEmpty());
        jugarContraLaMaquina();
        assertFalse(estaDetenido(), "Se puede empezar otro partido");
    }

    @Test
    public void siSeCancelaNoPasaNada() throws Exception {
        jugarContraLaMaquina();
        menu("Salvar");
        menu("Abrir");
        assertEquals(2, dialogos.preguntas);
        assertTrue(dialogos.avisos.isEmpty() && dialogos.errores.isEmpty());
        assertInstanceOf(PantallaJuego.class, visible());
        assertFalse(estaDetenido(), "El partido sigue");
    }

    @Test
    public void despuesDeGuardarElPartidoSigue() throws Exception {
        dialogos.archivo = new File(carpeta, "mia.dat");
        jugarContraLaMaquina();
        menu("Salvar");
        assertFalse(estaDetenido());
    }

    @Test
    public void siEstabaEnPausaSigueEnPausa() throws Exception {
        dialogos.archivo = new File(carpeta, "mia.dat");
        jugarContraLaMaquina();
        presionar("PAUSAR / REANUDAR");
        menu("Salvar");
        assertTrue(estaDetenido());
    }

    @Test
    public void siNoSePuedeGuardarSeExplica() throws Exception {
        dialogos.archivo = new File(new File(carpeta, "no_existe"), "mia.dat");
        jugarContraLaMaquina();
        menu("Salvar");
        assertEquals(1, dialogos.errores.size());
        assertTrue(dialogos.avisos.isEmpty());
        assertFalse(estaDetenido(), "El partido sigue");
    }

    // Abrir

    @Test
    public void abrirMuestraElPartidoEnPausa() throws Exception {
        dialogos.archivo = new File(carpeta, "mia.dat");
        jugarContraLaMaquina();
        menu("Salvar");
        presionar("MENÚ");
        menu("Abrir");
        assertInstanceOf(PantallaJuego.class, visible());
        assertTrue(estaDetenido());
        presionar("PAUSAR / REANUDAR");
        assertFalse(estaDetenido(), "Al reanudar continúa");
    }

    @Test
    public void importarAbreLoQueSeExporto() throws Exception {
        dialogos.archivo = new File(carpeta, "mia.bin");
        jugarContraLaMaquina();
        menu("Exportar");
        presionar("MENÚ");
        menu("Importar");
        assertInstanceOf(PantallaJuego.class, visible());
        assertTrue(dialogos.errores.isEmpty());
    }

    @Test
    public void unArchivoDaniadoNoInterrumpeElPartido() throws Exception {
        dialogos.archivo = new File(carpeta, "mala.dat");
        Files.writeString(dialogos.archivo.toPath(), "hola");
        jugarContraLaMaquina();
        menu("Abrir");
        assertEquals(1, dialogos.errores.size());
        assertInstanceOf(PantallaJuego.class, visible());
        assertFalse(estaDetenido());
    }

    @Test
    public void laVentanaTomaElTamanoDeLaCanchaGuardada() throws Exception {
        dialogos.archivo = new File(carpeta, "grande.dat");
        Domain.POOBSoccer juego = new Domain.POOBSoccer();
        juego.iniciarPartido("Tiempo", "PvsP", Domain.CatalogoPersonajes.crear("Rusia"),
                Domain.CatalogoPersonajes.crear("Japón"), 2, 800, 550);
        new ArchivoDat().guardar(juego, dialogos.archivo);
        menu("Abrir");
        assertEquals(800, ventana.getWidth());
    }

    // Selector de archivos

    @Test
    public void elSelectorSoloMuestraArchivosDelFormato() throws Exception {
        for (ArchivoPartida formato : new ArchivoPartida[]{new ArchivoDat(), new ArchivoBits()}) {
            JFileChooser[] selector = new JFileChooser[1];
            SwingUtilities.invokeAndWait(() -> selector[0] = new DialogosSwing().crearSelector(formato));
            assertFalse(selector[0].isAcceptAllFileFilterUsed());
            assertTrue(selector[0].getFileFilter().accept(new File("uno." + formato.getExtension())));
            assertFalse(selector[0].getFileFilter().accept(new File("uno.txt")));
        }
    }
}
