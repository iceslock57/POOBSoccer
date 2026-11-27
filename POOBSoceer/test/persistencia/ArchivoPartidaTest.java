package persistencia;

import Domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

/** Pruebas de los archivos: nombres, formatos y lo que pasa cuando algo sale mal. */
public class ArchivoPartidaTest {

    private static final ArchivoPartida[] FORMATOS = {new ArchivoDat(), new ArchivoBits()};

    @TempDir
    File carpeta;

    private POOBSoccer juego;

    @BeforeEach
    public void preparar() {
        juego = new POOBSoccer();
        juego.iniciarPartido("Goles", POOBSoccer.JUGADOR_VS_JUGADOR,
                CatalogoPersonajes.crear("Rusia"), CatalogoPersonajes.crear("Japón"));
    }

    private File guardada(ArchivoPartida formato) {
        File archivo = new File(carpeta, "partida." + formato.getExtension());
        formato.guardar(juego, archivo);
        return archivo;
    }

    /** Intenta abrir un archivo que no sirve y comprueba que el juego no cambie. */
    private String assertNoSeAbre(ArchivoPartida formato, File archivo) {
        Partido antes = juego.getPartidoActual();
        juego.getPerfil(1).agregarPuntos(10);
        long puntos = juego.getPerfil(1).getPuntos();
        ExcepcionJuego error = assertThrows(ExcepcionJuego.class, () -> formato.abrir(juego, archivo));
        assertSame(antes, juego.getPartidoActual(), "Se sigue con el mismo partido");
        assertEquals(puntos, juego.getPerfil(1).getPuntos(), "Con los mismos puntos");
        assertFalse(juego.isPausa());
        return error.getMessage();
    }

    @Test
    public void cadaFormatoTieneSuExtension() {
        assertEquals("dat", new ArchivoDat().getExtension());
        assertEquals("bin", new ArchivoBits().getExtension());
        for (ArchivoPartida formato : FORMATOS) {
            assertTrue(formato.getDescripcion().contains("*." + formato.getExtension()));
        }
    }

    @Test
    public void agregaLaExtensionSoloSiFalta() {
        ArchivoDat dat = new ArchivoDat();
        assertEquals("uno.dat", dat.conExtension(new File("uno")).getName());
        assertEquals("uno.dat", dat.conExtension(new File("uno.dat")).getName());
        assertEquals("uno.DAT", dat.conExtension(new File("uno.DAT")).getName());
        assertEquals("uno.txt.dat", dat.conExtension(new File("uno.txt")).getName());
    }

    @Test
    public void enBitsOcupaMenosQueEnDat() {
        assertTrue(guardada(new ArchivoBits()).length() < guardada(new ArchivoDat()).length());
    }

    @Test
    public void guardarDosVecesReemplazaElArchivo() {
        for (ArchivoPartida formato : FORMATOS) {
            File archivo = guardada(formato);
            juego.getPerfil(2).agregarPuntos(80);
            formato.guardar(juego, archivo);
            POOBSoccer otro = new POOBSoccer();
            formato.abrir(otro, archivo);
            assertEquals(juego.getPerfil(2).getPuntos(), otro.getPerfil(2).getPuntos(), "Queda lo último que se guardó");
        }
    }

    @Test
    public void noDejaArchivosTemporales() {
        for (ArchivoPartida formato : FORMATOS) guardada(formato);
        assertEquals(2, carpeta.list().length);
    }

    @Test
    public void siSobranDatosNoSeAbre() throws Exception {
        juego = new POOBSoccer();
        SalidaBits bits = new SalidaBits();
        juego.guardar(bits);
        byte[] justo = bits.getContenido();
        new ArchivoBits().abrir(juego, conContenido(new ArchivoBits(), "DSBT", justo));
        assertNoSeAbre(new ArchivoBits(), conContenido(new ArchivoBits(), "DSBT", Arrays.copyOf(justo, justo.length + 1)));

        byte[] dat = new ArchivoDat().escribir(juego);
        new ArchivoDat().abrir(juego, conContenido(new ArchivoDat(), "DSDT", dat));
        assertNoSeAbre(new ArchivoDat(), conContenido(new ArchivoDat(), "DSDT", Arrays.copyOf(dat, dat.length + 1)));
    }

    @Test
    public void guardarEnUnaCarpetaQueNoExisteAvisa() {
        for (ArchivoPartida formato : FORMATOS) {
            File archivo = new File(new File(carpeta, "no_existe"), "partida");
            ExcepcionJuego error = assertThrows(ExcepcionJuego.class, () -> formato.guardar(juego, archivo));
            assertNotNull(error.getCause(), "Conserva la causa para el registro");
        }
    }

    @Test
    public void unArchivoQueNoExisteAvisa() {
        for (ArchivoPartida formato : FORMATOS) {
            assertTrue(assertNoSeAbre(formato, new File(carpeta, "nada.dat")).contains("nada.dat"));
            assertNoSeAbre(formato, carpeta);
        }
    }

    @Test
    public void unArchivoCualquieraNoEsUnaPartida() throws Exception {
        for (ArchivoPartida formato : FORMATOS) {
            File archivo = new File(carpeta, "carta.txt");
            Files.writeString(archivo.toPath(), "Esto no es una partida de fútbol");
            assertNoSeAbre(formato, archivo);
            Files.write(archivo.toPath(), new byte[0]);
            assertNoSeAbre(formato, archivo);
        }
    }

    @Test
    public void unArchivoDemasiadoGrandeNoSeLee() throws Exception {
        File archivo = new File(carpeta, "grande.dat");
        Files.write(archivo.toPath(), new byte[(1 << 20) + 1]);
        for (ArchivoPartida formato : FORMATOS) assertNoSeAbre(formato, archivo);
    }

    @Test
    public void losFormatosNoSeConfunden() {
        File dat = guardada(new ArchivoDat()), bits = guardada(new ArchivoBits());
        assertNoSeAbre(new ArchivoBits(), dat);
        assertNoSeAbre(new ArchivoDat(), bits);
    }

    @Test
    public void unArchivoRecortadoNoSeAbre() throws Exception {
        for (ArchivoPartida formato : FORMATOS) {
            File archivo = guardada(formato);
            byte[] bytes = Files.readAllBytes(archivo.toPath());
            for (int largo : new int[]{3, 4, 8, 13, bytes.length / 2, bytes.length - 1}) {
                Files.write(archivo.toPath(), Arrays.copyOf(bytes, largo));
                assertNoSeAbre(formato, archivo);
            }
        }
    }

    @Test
    public void unSoloBitCambiadoSeNota() throws Exception {
        for (ArchivoPartida formato : FORMATOS) {
            File archivo = guardada(formato);
            byte[] bytes = Files.readAllBytes(archivo.toPath());
            for (int i = 0; i < bytes.length; i += 7) {
                byte[] daniado = bytes.clone();
                daniado[i] ^= 1;
                Files.write(archivo.toPath(), daniado);
                assertNoSeAbre(formato, archivo);
            }
        }
    }

    @Test
    public void otraVersionSeReconoce() throws Exception {
        for (ArchivoPartida formato : FORMATOS) {
            File archivo = guardada(formato);
            byte[] bytes = Files.readAllBytes(archivo.toPath());
            bytes[4] = 9;
            Files.write(archivo.toPath(), bytes);
            assertTrue(assertNoSeAbre(formato, archivo).contains("versión"));
        }
    }

    /** Escribe un contenido inventado con el código de revisión correcto. */
    private File conContenido(ArchivoPartida formato, String marca, byte[] contenido) throws Exception {
        File archivo = new File(carpeta, "inventada." + formato.getExtension());
        java.util.zip.CRC32 codigo = new java.util.zip.CRC32();
        codigo.update(contenido);
        try (java.io.DataOutputStream datos = new java.io.DataOutputStream(Files.newOutputStream(archivo.toPath()))) {
            datos.writeBytes(marca);
            datos.writeByte(1);
            datos.writeLong(codigo.getValue());
            datos.write(contenido);
        }
        return archivo;
    }

    @Test
    public void unContenidoSinSentidoNoDaniaElJuego() throws Exception {
        byte[] unos = new byte[600];
        Arrays.fill(unos, (byte) 0xFF);
        for (byte[] contenido : new byte[][]{new byte[0], new byte[40], new byte[600], unos}) {
            assertNoSeAbre(new ArchivoBits(), conContenido(new ArchivoBits(), "DSBT", contenido));
            assertNoSeAbre(new ArchivoDat(), conContenido(new ArchivoDat(), "DSDT", contenido));
        }
    }

    @Test
    public void unPersonajeDesconocidoNoSeAbre() throws Exception {
        SalidaBits bits = new SalidaBits();
        for (int i = 0; i < 2; i++) {
            bits.texto("Uno");
            bits.largo(0);
            bits.entero(0, 8);
        }
        bits.texto("Goles");
        bits.texto("PvsP");
        bits.texto("");
        bits.entero(5, 16);
        bits.entero(1000, 16);
        bits.entero(650, 16);
        bits.texto("Atlántida");
        bits.entero(0, 16);
        assertNoSeAbre(new ArchivoBits(), conContenido(new ArchivoBits(), "DSBT", bits.getContenido()));
    }
}
