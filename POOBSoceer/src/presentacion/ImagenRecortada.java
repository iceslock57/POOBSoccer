package presentacion;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * Una parte de una imagen de la carpeta recursos, lista para dibujarse en el partido.
 * Al cargarla se queda solo con la parte que interesa y la reduce, para que dibujarla
 * sesenta veces por segundo no sea pesado.
 *
 * Las posiciones se escriben en píxeles de la imagen original, tal como se ven al abrirla,
 * y esta clase se encarga de llevarlas al lugar donde queda dibujada.
 */
final class ImagenRecortada {

    private final BufferedImage imagen;
    private final Rectangle zona;

    private ImagenRecortada(BufferedImage imagen, Rectangle zona) {
        this.imagen = imagen;
        this.zona = zona;
    }

    /**
     * @param ruta        archivo de la imagen
     * @param zona        parte de la imagen original que se va a usar
     * @param anchoMaximo ancho con el que se guarda; si la zona es más ancha se reduce
     * @return la imagen lista, o null si el archivo no existe o no se puede leer
     */
    static ImagenRecortada cargar(String ruta, Rectangle zona, int anchoMaximo) {
        try {
            File archivo = new File(ruta);
            if (!archivo.isFile()) return null;
            BufferedImage original = ImageIO.read(archivo);
            if (original == null) return null;

            Rectangle dentro = zona.intersection(new Rectangle(original.getWidth(), original.getHeight()));
            double escala = Math.min(1, anchoMaximo / (double) dentro.width);
            int w = Math.max(1, (int) Math.round(dentro.width * escala));
            int h = Math.max(1, (int) Math.round(dentro.height * escala));

            BufferedImage reducida = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = reducida.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            Image parte = original.getSubimage(dentro.x, dentro.y, dentro.width, dentro.height)
                    .getScaledInstance(w, h, Image.SCALE_SMOOTH);
            g.drawImage(parte, 0, 0, null);
            g.dispose();
            return new ImagenRecortada(reducida, dentro);
        } catch (IOException | RuntimeException e) {
            return null;
        }
    }

    /** Proporción entre el alto y el ancho de la parte usada, para dibujarla sin deformarla. */
    double altoPorAncho() {
        return zona.height / (double) zona.width;
    }

    /** Dibuja la imagen ocupando el rectángulo indicado; si voltear es true se ve como en un espejo. */
    void dibujar(Graphics2D g, Rectangle2D destino, boolean voltear) {
        Graphics2D d = (Graphics2D) g.create();
        d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        d.translate(destino.getX(), destino.getY());
        d.scale(destino.getWidth() / imagen.getWidth(), destino.getHeight() / imagen.getHeight());
        if (voltear) {
            d.translate(imagen.getWidth(), 0);
            d.scale(-1, 1);
        }
        d.drawImage(imagen, 0, 0, null);
        d.dispose();
    }

    /**
     * Dice dónde queda una parte de la imagen original cuando la imagen se dibuja en destino.
     * Sirve para escribir los números justo dentro de las cajas y barras que trae el dibujo.
     */
    Rectangle2D ubicar(Rectangle parte, Rectangle2D destino) {
        double sx = destino.getWidth() / zona.width, sy = destino.getHeight() / zona.height;
        return new Rectangle2D.Double(destino.getX() + (parte.x - zona.x) * sx, destino.getY() + (parte.y - zona.y) * sy,
                parte.width * sx, parte.height * sy);
    }

    /**
     * Calcula dónde dibujar la imagen para que una parte de ella caiga exactamente sobre un rectángulo
     * del juego. Así los palos del arco coinciden con el lugar donde rebota el balón.
     * Si la imagen se va a dibujar volteada, la parte también se mide en el espejo.
     */
    Rectangle2D ajustarA(Rectangle parte, Rectangle2D lugar, boolean voltear) {
        if (voltear) parte = new Rectangle(2 * zona.x + zona.width - parte.x - parte.width, parte.y, parte.width, parte.height);
        double sx = lugar.getWidth() / parte.width, sy = lugar.getHeight() / parte.height;
        return new Rectangle2D.Double(lugar.getX() - (parte.x - zona.x) * sx, lugar.getY() - (parte.y - zona.y) * sy,
                zona.width * sx, zona.height * sy);
    }
}
