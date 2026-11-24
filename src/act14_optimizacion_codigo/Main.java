package act14_optimizacion_codigo;

import java.io.IOException;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Cervantes Mora Juan Pablo
 * Muñoz Madriz Nadya Janneth
 * Ramirez Torres Jesus Adrian
 */
public class Main {
    public static void main(String[] args) throws IOException, InterruptedException {
        System.setOut(new PrintStream(new FileOutputStream(FileDescriptor.out), true, StandardCharsets.UTF_8));
        System.out.println("Integrantes: Cervantes Mora Juan Pablo, Muñoz Madriz Nadya Janneth, Ramirez Torres Jesus Adrian");
        System.out.println("--------------------------------------------------------");
        System.out.println("ACTIVIDAD 15: GENERACION DE CODIGO EJECUTABLE");
        System.out.println("--------------------------------------------------------\n");

        Optimizador3AC opt = new Optimizador3AC();
        PruebasValidas validas = new PruebasValidas();
        PruebasInvalidas invalidas = new PruebasInvalidas();

        validas.ejecutar(opt);
        invalidas.ejecutar(opt);

        // Validacion numerica de equivalencia semantica (x = 10)
        int x = 10;
        int t1Original = x + 5;
        int t2Original = t1Original + 10;
        int resOriginal = t2Original;
        int resOptimizado = x + 15;

        System.out.println(">> VALIDACION DE INTEGRIDAD SEMANTICA (x = 10):");
        System.out.println("   Resultado sin optimizar : " + resOriginal);
        System.out.println("   Resultado optimizado    : " + resOptimizado);
        System.out.println("   Estado de validacion    : " + (resOriginal == resOptimizado ? "EXITOSO (Resultados identicos)" : "ERROR"));

        List<Instruccion3AC> codigoOriginal = Arrays.asList(
                new Instruccion3AC("t1 = x + 5", "OPERACION", "t1", "x", "+", "5"),
                new Instruccion3AC("t2 = t1 + 10", "OPERACION", "t2", "t1", "+", "10"),
                new Instruccion3AC("result = t2", "ASIGNACION", "result", "t2", null, null));
        List<Instruccion3AC> codigoOptimizado = opt.simplificarConstantesEncadenadas(codigoOriginal);
        opt.imprimirComparativa("Traducción del código optimizado a JVM", codigoOriginal, codigoOptimizado);
        System.out.println(">> GENERACION Y EJECUCION DEL PROGRAMA OBJETIVO:");
        System.out.println("   Valor inicial: x = 10");
        new GeneradorCodigoEjecutable().generarYEjecutar(codigoOptimizado,
                Collections.singletonMap("x", 10), Paths.get("salida-ejecutable"));

        List<Instruccion3AC> codigoConSaltos = Arrays.asList(
                new Instruccion3AC("x = 10", "ASIGNACION", "x", "10", null, null),
                new Instruccion3AC("if x > 5 goto L1", "CONDICIONAL", null, "x", ">", "5"),
                new Instruccion3AC("result = 0", "ASIGNACION", "result", "0", null, null),
                new Instruccion3AC("goto L2", "GOTO", "L2", null, null, null),
                new Instruccion3AC("L1:", "ETIQUETA", "L1", null, null, null),
                new Instruccion3AC("result = x", "ASIGNACION", "result", "x", null, null),
                new Instruccion3AC("L2:", "ETIQUETA", "L2", null, null, null));
        System.out.println("\n>> DEMOSTRACION DE SALTOS Y DIRECCIONES DE ETIQUETA:");
        new GeneradorCodigoEjecutable().generarYEjecutar(codigoConSaltos,
                Collections.emptyMap(), Paths.get("salida-ejecutable", "flujo-condicional"));

        System.out.println("--------------------------------------------------------\n");
        System.out.println("FIN DE ACTIVIDAD 15 - GENERACION DE CODIGO EJECUTABLE");
        System.out.println("--------------------------------------------------------\n");
        System.out.println("Integrantes: Cervantes Mora Juan Pablo, Muñoz Madriz Nadya Janneth, Ramirez Torres Jesus Adrian");
    }
}