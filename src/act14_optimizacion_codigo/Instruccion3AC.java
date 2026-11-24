package act14_optimizacion_codigo;

/**
 * Cervantes Mora Juan Pablo
 * Muñoz Madriz Nadya Janneth
 * Ramirez Torres Jesus Adrian
 */
public class Instruccion3AC {
    public String textoOriginal;
    public String tipo; 
    public String destino;
    public String op1;
    public String operador;
    public String op2;

    public Instruccion3AC(String textoOriginal, String tipo, String destino, String op1, String operador, String op2) {
        this.textoOriginal = textoOriginal;
        this.tipo = tipo;
        this.destino = destino;
        this.op1 = op1;
        this.operador = operador;
        this.op2 = op2;
    }

    @Override
    public String toString() {
        return textoOriginal;
    }
}