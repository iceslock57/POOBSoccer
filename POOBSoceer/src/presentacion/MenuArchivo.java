package presentacion;

import persistencia.ArchivoBits;
import persistencia.ArchivoDat;
import persistencia.ArchivoPartida;
import javax.swing.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

/** Menú con las cuatro opciones de archivo: abrir y salvar en .dat, importar y exportar en bits. */
class MenuArchivo extends JMenuBar {

    /**
     * @param alGuardar acción que recibe el formato en el que se quiere guardar
     * @param alAbrir   acción que recibe el formato del que se quiere abrir
     */
    MenuArchivo(Consumer<ArchivoPartida> alGuardar, Consumer<ArchivoPartida> alAbrir) {
        ArchivoPartida dat = new ArchivoDat(), bits = new ArchivoBits();
        JMenu archivo = new JMenu("Archivo");
        archivo.add(opcion("Abrir", KeyEvent.VK_O, () -> alAbrir.accept(dat)));
        archivo.add(opcion("Salvar", KeyEvent.VK_G, () -> alGuardar.accept(dat)));
        archivo.addSeparator();
        archivo.add(opcion("Importar", KeyEvent.VK_I, () -> alAbrir.accept(bits)));
        archivo.add(opcion("Exportar", KeyEvent.VK_E, () -> alGuardar.accept(bits)));
        add(archivo);
    }

    /** Cada opción también se puede usar con Ctrl y su tecla. */
    private static JMenuItem opcion(String texto, int tecla, Runnable accion) {
        JMenuItem item = new JMenuItem(texto);
        item.setAccelerator(KeyStroke.getKeyStroke(tecla, InputEvent.CTRL_DOWN_MASK));
        item.addActionListener(e -> accion.run());
        return item;
    }
}
