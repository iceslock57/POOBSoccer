package presentacion;

import persistencia.ArchivoPartida;
import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.Component;
import java.io.File;

/** Diálogos normales de Swing. Recuerda la última carpeta que se usó. */
class DialogosSwing implements Dialogos {

    private File carpeta = new File(".");

    @Override
    public File paraGuardar(Component padre, ArchivoPartida formato) {
        JFileChooser selector = crearSelector(formato);
        if (selector.showSaveDialog(padre) != JFileChooser.APPROVE_OPTION) return null;

        File archivo = formato.conExtension(elegido(selector));
        boolean reemplazar = !archivo.exists() || JOptionPane.showConfirmDialog(padre,
                archivo.getName() + " ya existe. ¿Quieres reemplazarlo?", "Guardar partida",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        return reemplazar ? archivo : null;
    }

    @Override
    public File paraAbrir(Component padre, ArchivoPartida formato) {
        JFileChooser selector = crearSelector(formato);
        return selector.showOpenDialog(padre) == JFileChooser.APPROVE_OPTION ? elegido(selector) : null;
    }

    private File elegido(JFileChooser selector) {
        carpeta = selector.getCurrentDirectory();
        return selector.getSelectedFile();
    }

    /** Selector que solo muestra los archivos de ese formato. */
    JFileChooser crearSelector(ArchivoPartida formato) {
        JFileChooser selector = new JFileChooser(carpeta);
        selector.setAcceptAllFileFilterUsed(false);
        selector.setFileFilter(new FileNameExtensionFilter(formato.getDescripcion(), formato.getExtension()));
        return selector;
    }

    @Override
    public void informar(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "DOPOSoccer", JOptionPane.INFORMATION_MESSAGE);
    }

    @Override
    public void error(Component padre, String mensaje) {
        JOptionPane.showMessageDialog(padre, mensaje, "DOPOSoccer", JOptionPane.ERROR_MESSAGE);
    }
}
