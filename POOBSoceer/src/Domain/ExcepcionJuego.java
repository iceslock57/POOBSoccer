package Domain;
/** Error de negocio controlado del videojuego. */
public class ExcepcionJuego extends RuntimeException {
    public ExcepcionJuego(String mensaje){super(mensaje);}
    public ExcepcionJuego(String mensaje,Throwable causa){super(mensaje,causa);}
}
