package presentacion;

import javax.swing.*;
import java.awt.*;

/** Estilo visual común de los menús: fondo, títulos, botones e imágenes. */
final class EstiloGUI {

    private static final Image IMAGEN_FONDO = cargarImagen("recursos/fondo_menu.png");
    private static final Image IMAGEN_PORTADA = cargarImagen("recursos/portada_poobsoccer.png");
    // En la portada (1920 x 1080) el césped empieza en y = 905.
    private static final double INICIO_CESPED_PORTADA = 905.0 / 1080;

    // Solo contiene métodos estáticos, así que no se crean objetos de esta clase.
    private EstiloGUI() {
    }

    /** Devuelve null si la imagen no existe, así el juego no se rompe. */
    static Image cargarImagen(String ruta) {
        return new java.io.File(ruta).exists() ? new ImageIcon(ruta).getImage() : null;
    }

    /** Panel con la imagen de fondo del menú, oscurecida para que se lea el texto (o un degradado si no está). */
    static JPanel fondo() {
        JPanel p = panelConImagen(new GridBagLayout(), IMAGEN_FONDO, true);
        p.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));
        return p;
    }

    /** true si existe la imagen de portada (recursos/portada_poobsoccer.png). */
    static boolean hayPortada() {
        return IMAGEN_PORTADA != null;
    }

    /**
     * Panel con la portada de POOBSoccer; ya trae el título dibujado.
     * La imagen se ajusta al ancho para que los cuatro personajes se vean completos,
     * y el espacio que sobre abajo se rellena con más césped.
     */
    static JPanel portada() {
        return new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D x = (Graphics2D) g;
                int w = getWidth(), h = getHeight();
                int altoImagen = altoPortadaAjustada(w);
                if (altoImagen > h) {
                    // Ventana más ancha que la imagen: se llena recortando arriba y abajo.
                    dibujarCubriendo(x, IMAGEN_PORTADA, w, h);
                    return;
                }
                Graphics2D d = (Graphics2D) x.create();
                d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                d.drawImage(IMAGEN_PORTADA, 0, 0, w, altoImagen, null);
                d.dispose();
                // Césped extra con las mismas franjas que el borde inferior de la portada.
                for (int i = 0; i < 12; i++) {
                    x.setColor(i % 2 == 0 ? new Color(19, 86, 35) : new Color(16, 75, 31));
                    x.fillRect(i * w / 12, altoImagen, w / 12 + 1, h - altoImagen);
                }
            }
        };
    }

    /** Alto con que se dibuja la portada si se ajusta al ancho w. */
    private static int altoPortadaAjustada(int w) {
        return (int) Math.round(w * IMAGEN_PORTADA.getHeight(null) / (double) IMAGEN_PORTADA.getWidth(null));
    }

    /** Alto de la franja de césped en la parte de abajo de la portada, para un panel de w x h. */
    static int altoCespedPortada(int w, int h) {
        int altoImagen = altoPortadaAjustada(w);
        // Si la imagen es más alta que el panel, se dibuja centrada verticalmente (ver portada()).
        double arriba = altoImagen > h ? (h - altoImagen) / 2.0 : 0;
        int inicioCesped = (int) (arriba + altoImagen * INICIO_CESPED_PORTADA);
        return Math.max(0, h - inicioCesped);
    }

    private static JPanel panelConImagen(LayoutManager layout, Image imagen, boolean oscurecer) {
        return new JPanel(layout) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D x = (Graphics2D) g;
                if (imagen != null) {
                    dibujarCubriendo(x, imagen, getWidth(), getHeight());
                    if (oscurecer) {
                        // Capa oscura semitransparente para que el texto blanco se lea bien.
                        x.setColor(new Color(0, 0, 0, 110));
                        x.fillRect(0, 0, getWidth(), getHeight());
                    }
                } else {
                    x.setPaint(new GradientPaint(0, 0, new Color(15, 34, 66), 0, getHeight(), new Color(26, 116, 93)));
                    x.fillRect(0, 0, getWidth(), getHeight());
                }
            }
        };
    }

    /**
     * Dibuja la imagen llenando todo el panel sin deformarla: se escala para cubrirlo
     * y se recorta lo que sobre a los lados (o arriba y abajo), dejándola centrada.
     */
    private static void dibujarCubriendo(Graphics2D g, Image img, int w, int h) {
        int iw = img.getWidth(null), ih = img.getHeight(null);
        if (iw <= 0 || ih <= 0) return;
        double escala = Math.max(w / (double) iw, h / (double) ih);
        int dw = (int) Math.ceil(iw * escala), dh = (int) Math.ceil(ih * escala);
        Graphics2D d = (Graphics2D) g.create();
        d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        d.drawImage(img, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
        d.dispose();
    }

    static JLabel titulo(String s, int size) {
        JLabel l = new JLabel(s, SwingConstants.CENTER);
        l.setForeground(Color.WHITE);
        l.setFont(new Font("Arial", Font.BOLD, size));
        return l;
    }

    static JButton boton(String s) {
        JButton b = new JButton(s);
        b.setFont(new Font("Arial", Font.BOLD, 20));
        b.setFocusPainted(false);
        b.setBackground(new Color(255, 205, 55));
        b.setForeground(new Color(25, 35, 55));
        b.setPreferredSize(new Dimension(250, 54));
        return b;
    }
}
