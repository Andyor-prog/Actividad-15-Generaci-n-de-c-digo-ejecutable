package act14_optimizacion_codigo;

import java.util.Arrays;
import java.util.List;

/**
 * Cervantes Mora Juan Pablo
 * Muñoz Madriz Nadya Janneth
 * Ramirez Torres Jesus Adrian
 */
public class PruebasInvalidas {

    public void ejecutar(Optimizador3AC opt) {
        System.out.println("\n--------------------------------------------------------");
        System.out.println("PRUEBAS INVALIDAS");
        System.out.println("--------------------------------------------------------\n");

        // 1. Caso invalido para eliminar variables: todas si se usan en el resultado
        List<Instruccion3AC> invalida1 = Arrays.asList(
            new Instruccion3AC("a = 4", "ASIGNACION", "a", "4", null, null),
            new Instruccion3AC("b = a + 6", "OPERACION", "b", "a", "+", "6"),
            new Instruccion3AC("result = b", "ASIGNACION", "result", "b", null, null)
        );
        System.out.println("   [CONTROL - Caso Invalido para Eliminar] Todas las variables son necesarias para 'result'.");
        opt.imprimirComparativa("Invalida 1: Intento de poda donde todas las variables son activas", invalida1, opt.eliminarVariablesNoUsadas(invalida1, "result"));

        // 2. Caso invalido para subexpresion comun: los operandos cambian entre lineas
        List<Instruccion3AC> invalida2 = Arrays.asList(
            new Instruccion3AC("t1 = a + b", "OPERACION", "t1", "a", "+", "b"),
            new Instruccion3AC("t2 = a + c", "OPERACION", "t2", "a", "+", "c"),
            new Instruccion3AC("result = t2", "ASIGNACION", "result", "t2", null, null)
        );
        System.out.println("   [CONTROL - Caso Invalido para Fusionar] 'a + c' no equivale a 'a + b'.");
        opt.imprimirComparativa("Invalida 2: Expresiones distintas no deben eliminarse", invalida2, opt.eliminarCalculosRepetidos(invalida2));

        // 3. Caso invalido para plegado directo de constantes: hay multiplicacion/ramificacion
        List<Instruccion3AC> invalida3 = Arrays.asList(
            new Instruccion3AC("if a > 10", "CONDICIONAL", null, "a", ">", "10"),
            new Instruccion3AC("b = a * 2", "OPERACION", "b", "a", "*", "2"),
            new Instruccion3AC("c = b + 1", "OPERACION", "c", "b", "+", "1")
        );
        System.out.println("   [CONTROL - Flujo Condicional] Se conserva intacta la estructura condicional.");
        opt.imprimirComparativa("Invalida 3: Estructura condicional preservada sin alterar logica", invalida3, opt.simplificarConstantesEncadenadas(invalida3));
    }
}