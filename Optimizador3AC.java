package act14_optimizacion_codigo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cervantes Mora Juan Pablo
 * Muñoz Madriz Nadya Janneth
 * Ramirez Torres Jesus Adrian
 */
public class Optimizador3AC {

    // REGLA 1A: Eliminación de instrucciones inalcanzables después de un goto incondicional
    public List<Instruccion3AC> eliminarCodigoInalcanzable(List<Instruccion3AC> bloque) {
        List<Instruccion3AC> optimizado = new ArrayList<>();
        boolean enZonaMuerta = false;
        String etiquetaObjetivo = "";

        for (Instruccion3AC ins : bloque) {
            if (enZonaMuerta) {
                if (ins.tipo.equals("ETIQUETA") && ins.destino.equals(etiquetaObjetivo)) {
                    enZonaMuerta = false;
                    optimizado.add(ins);
                } else {
                    System.out.println("   [OPTIMIZACIÓN - Código Inalcanzable] Eliminada: " + ins.textoOriginal);
                }
            } else {
                optimizado.add(ins);
                if (ins.tipo.equals("GOTO")) {
                    enZonaMuerta = true;
                    etiquetaObjetivo = ins.destino;
                }
            }
        }
        return optimizado;
    }

    // REGLA 1B: Eliminación de variables inicializadas pero nunca utilizadas
    public List<Instruccion3AC> eliminarVariablesNoUsadas(List<Instruccion3AC> bloque, String variableFinal) {
        Set<String> variablesUsadas = new HashSet<>();
        variablesUsadas.add(variableFinal);

        // Análisis hacia atrás (Liveness Analysis) para detectar qué variables realmente influyen
        for (int i = bloque.size() - 1; i >= 0; i--) {
            Instruccion3AC ins = bloque.get(i);
            if (ins.destino != null && variablesUsadas.contains(ins.destino)) {
                if (ins.op1 != null && !esNumero(ins.op1)) variablesUsadas.add(ins.op1);
                if (ins.op2 != null && !esNumero(ins.op2)) variablesUsadas.add(ins.op2);
            }
        }

        List<Instruccion3AC> optimizado = new ArrayList<>();
        for (Instruccion3AC ins : bloque) {
            if (ins.destino != null && !variablesUsadas.contains(ins.destino)) {
                System.out.println("   [OPTIMIZACIÓN - Variable No Usada] Eliminada: " + ins.textoOriginal);
            } else {
                optimizado.add(ins);
            }
        }
        return optimizado;
    }

    // REGLA 2A: Eliminación de cálculos repetidos (Subexpresiones comunes)
    public List<Instruccion3AC> eliminarCalculosRepetidos(List<Instruccion3AC> bloque) {
        List<Instruccion3AC> optimizado = new ArrayList<>();
        Map<String, String> expresionesCalculadas = new HashMap<>();
        Map<String, String> reemplazos = new HashMap<>();

        for (Instruccion3AC ins : bloque) {
            String op1Actual = reemplazos.getOrDefault(ins.op1, ins.op1);
            String op2Actual = reemplazos.getOrDefault(ins.op2, ins.op2);

            if (ins.tipo.equals("OPERACION")) {
                String firmaExpresion = op1Actual + " " + ins.operador + " " + op2Actual;
                if (expresionesCalculadas.containsKey(firmaExpresion)) {
                    String tempPrevio = expresionesCalculadas.get(firmaExpresion);
                    reemplazos.put(ins.destino, tempPrevio);
                    System.out.println("   [OPTIMIZACIÓN - Cálculo Repetido] Eliminada: " + ins.textoOriginal + " (Reutiliza " + tempPrevio + ")");
                } else {
                    expresionesCalculadas.put(firmaExpresion, ins.destino);
                    optimizado.add(new Instruccion3AC(ins.destino + " = " + firmaExpresion, ins.tipo, ins.destino, op1Actual, ins.operador, op2Actual));
                }
            } else if (ins.tipo.equals("ASIGNACION")) {
                optimizado.add(new Instruccion3AC(ins.destino + " = " + op1Actual, ins.tipo, ins.destino, op1Actual, null, null));
            } else {
                optimizado.add(ins);
            }
        }
        return optimizado;
    }

    // REGLA 2B: Eliminación de asignaciones innecesarias (sobrescritas inmediatamente)
    public List<Instruccion3AC> eliminarAsignacionesSobrescritas(List<Instruccion3AC> bloque) {
        List<Instruccion3AC> optimizado = new ArrayList<>();
        Map<String, String> copiaDirecta = new HashMap<>();

        for (int i = 0; i < bloque.size(); i++) {
            Instruccion3AC actual = bloque.get(i);
            if (i + 1 < bloque.size()) {
                Instruccion3AC siguiente = bloque.get(i + 1);
                if (actual.tipo.equals("ASIGNACION") && siguiente.tipo.equals("ASIGNACION")
                        && actual.destino.equals(siguiente.destino)) {
                    System.out.println("   [OPTIMIZACIÓN - Asignación Sobrescrita] Eliminada: " + actual.textoOriginal);
                    continue;
                }
            }

            // Propagación simple de copia si es una variable temporal intermedia
            if (actual.tipo.equals("ASIGNACION") && actual.destino.startsWith("temp")) {
                copiaDirecta.put(actual.destino, actual.op1);
                System.out.println("   [OPTIMIZACIÓN - Propagación de Copia] Simplificada: " + actual.textoOriginal);
            } else {
                String valorFinal = copiaDirecta.getOrDefault(actual.op1, actual.op1);
                optimizado.add(new Instruccion3AC(actual.destino + " = " + valorFinal, actual.tipo, actual.destino, valorFinal, null, null));
            }
        }
        return optimizado;
    }

    // REGLA 4: Simplificación algebraica / Plegado de constantes encadenadas
    public List<Instruccion3AC> simplificarConstantesEncadenadas(List<Instruccion3AC> bloque) {
        List<Instruccion3AC> optimizado = new ArrayList<>();
        if (bloque.size() == 3
                && bloque.get(0).tipo.equals("OPERACION") && bloque.get(0).operador.equals("+")
                && bloque.get(1).tipo.equals("OPERACION") && bloque.get(1).operador.equals("+")
                && bloque.get(1).op1.equals(bloque.get(0).destino)
                && esNumero(bloque.get(0).op2) && esNumero(bloque.get(1).op2)) {

            int sumaConstantes = Integer.parseInt(bloque.get(0).op2) + Integer.parseInt(bloque.get(1).op2);
            String varBase = bloque.get(0).op1;
            String varFinal = bloque.get(2).destino;

            System.out.println("   [OPTIMIZACIÓN - Plegado de Constantes] Se fusionan sumas constantes (" + bloque.get(0).op2 + " + " + bloque.get(1).op2 + " = " + sumaConstantes + ")");
            optimizado.add(new Instruccion3AC(varFinal + " = " + varBase + " + " + sumaConstantes, "OPERACION", varFinal, varBase, "+", String.valueOf(sumaConstantes)));
            return optimizado;
        }
        return bloque;
    }

    private boolean esNumero(String str) {
        if (str == null) return false;
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public void imprimirComparativa(String titulo, List<Instruccion3AC> antes, List<Instruccion3AC> despues) {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("PRUEBA: " + titulo);
        System.out.println("--------------------------------------------------------------------------");
        System.out.println(">> CÓDIGO INTERMEDIO ORIGINAL:");
        for (Instruccion3AC ins : antes) {
            System.out.println("   " + ins.textoOriginal);
        }
        System.out.println("\n>> CÓDIGO INTERMEDIO OPTIMIZADO:");
        for (Instruccion3AC ins : despues) {
            System.out.println("   " + ins.textoOriginal);
        }
        System.out.println("--------------------------------------------------------------------------\n");
    }
}