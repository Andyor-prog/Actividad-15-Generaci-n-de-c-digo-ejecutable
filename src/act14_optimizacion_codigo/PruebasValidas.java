package act14_optimizacion_codigo;

import java.util.Arrays;
import java.util.List;

/**
 * Cervantes Mora Juan Pablo
 * Muñoz Madriz Nadya Janneth
 * Ramirez Torres Jesus Adrian
 */
public class PruebasValidas {

    public void ejecutar(Optimizador3AC opt) {
        System.out.println("\n--------------------------------------------------------");
        System.out.println(" PRUEBAS VALIDAS (OPTIMIZABLES)");
        System.out.println("--------------------------------------------------------\n");

        // 1. Codigo muerto por salto incondicional (goto)
        List<Instruccion3AC> caso1 = Arrays.asList(
            new Instruccion3AC("L1:", "ETIQUETA", "L1", null, null, null),
            new Instruccion3AC("x = 10", "ASIGNACION", "x", "10", null, null),
            new Instruccion3AC("goto L2", "GOTO", "L2", null, null, null),
            new Instruccion3AC("y = 20", "ASIGNACION", "y", "20", null, null),
            new Instruccion3AC("L2:", "ETIQUETA", "L2", null, null, null),
            new Instruccion3AC("z = x + 10", "OPERACION", "z", "x", "+", "10")
        );
        opt.imprimirComparativa("Valida 1: Instruccion inalcanzable tras goto", caso1, opt.eliminarCodigoInalcanzable(caso1));

        // 2. Variable asignada pero nunca utilizada
        List<Instruccion3AC> caso2 = Arrays.asList(
            new Instruccion3AC("a = 5", "ASIGNACION", "a", "5", null, null),
            new Instruccion3AC("b = 10", "ASIGNACION", "b", "10", null, null),
            new Instruccion3AC("c = b + 2", "OPERACION", "c", "b", "+", "2"),
            new Instruccion3AC("result = c", "ASIGNACION", "result", "c", null, null)
        );
        opt.imprimirComparativa("Valida 2: Eliminacion de variable no usada", caso2, opt.eliminarVariablesNoUsadas(caso2, "result"));

        // 3. Calculo repetido
        List<Instruccion3AC> caso3 = Arrays.asList(
            new Instruccion3AC("t1 = a + b", "OPERACION", "t1", "a", "+", "b"),
            new Instruccion3AC("t2 = a + b", "OPERACION", "t2", "a", "+", "b"),
            new Instruccion3AC("result = t1", "ASIGNACION", "result", "t1", null, null)
        );
        opt.imprimirComparativa("Valida 3: Eliminacion de calculo repetido", caso3, opt.eliminarCalculosRepetidos(caso3));

        // 4. Asignacion sobrescrita inmediatamente
        List<Instruccion3AC> caso4 = Arrays.asList(
            new Instruccion3AC("temp = a", "ASIGNACION", "temp", "a", null, null),
            new Instruccion3AC("temp = b", "ASIGNACION", "temp", "b", null, null),
            new Instruccion3AC("result = temp", "ASIGNACION", "result", "temp", null, null)
        );
        opt.imprimirComparativa("Valida 4: Eliminacion de asignacion sobrescrita", caso4, opt.eliminarAsignacionesSobrescritas(caso4));

        // 5. Simplificacion de constantes encadenadas
        List<Instruccion3AC> caso5 = Arrays.asList(
            new Instruccion3AC("t1 = x + 5", "OPERACION", "t1", "x", "+", "5"),
            new Instruccion3AC("t2 = t1 + 10", "OPERACION", "t2", "t1", "+", "10"),
            new Instruccion3AC("result = t2", "ASIGNACION", "result", "t2", null, null)
        );
        opt.imprimirComparativa("Valida 5: Plegado de constantes encadenadas", caso5, opt.simplificarConstantesEncadenadas(caso5));
    }
}