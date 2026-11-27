package persistencia;

import Domain.POOBSoccer;

/** Formato con el que se exportan y se importan las partidas: todo el contenido empaquetado en bits. */
public class ArchivoBits extends ArchivoPartida {

    public ArchivoBits() {
        super("bin", "Partida en bits (*.bin)", "DSBT");
    }

    @Override
    protected byte[] escribir(POOBSoccer juego) {
        SalidaBits bits = new SalidaBits();
        juego.guardar(bits);
        return bits.getContenido();
    }

    @Override
    protected void leer(POOBSoccer juego, byte[] contenido) {
        juego.restaurar(new EntradaBits(contenido));
    }
}
