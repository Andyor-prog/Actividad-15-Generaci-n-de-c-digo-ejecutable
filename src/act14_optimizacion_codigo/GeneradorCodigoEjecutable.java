package act14_optimizacion_codigo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;

/**
 * Cervantes Mora Juan Pablo
 * Muñoz Madriz Nadya Janneth
 * Ramirez Torres Jesus Adrian
 */
public class GeneradorCodigoEjecutable {
    private static final int DIRECCION_INICIO_CODIGO = 0x1000;
    private static final int DIRECCION_INICIO_DATOS = 0x2000;
    private static final Pattern IDENTIFICADOR = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");
    private static final Pattern CONDICIONAL = Pattern.compile(
            "^\\s*if\\s+(\\S+)\\s*(==|!=|<=|>=|<|>)\\s*(\\S+)\\s+goto\\s+(\\S+)\\s*$");

    public void generarYEjecutar(List<Instruccion3AC> instrucciones,
                                 Map<String, Integer> valoresIniciales,
                                 Path directorioSalida) throws IOException, InterruptedException {
        if (instrucciones == null || instrucciones.isEmpty()) {
            throw new IllegalArgumentException("El código intermedio no puede estar vacío.");
        }

        Map<String, Integer> etiquetas = obtenerEtiquetas(instrucciones);
        Map<String, Integer> simbolos = obtenerSimbolos(instrucciones, valoresIniciales);
        Map<Integer, Integer> direccionesInstrucciones = obtenerDirecciones(instrucciones);
        List<String> codigoMaquina = traducir(instrucciones, etiquetas, simbolos, direccionesInstrucciones);
        String codigoJava = generarCodigoJava(instrucciones, etiquetas, simbolos, valoresIniciales);

        Files.createDirectories(directorioSalida);
        Path archivoJava = directorioSalida.resolve("ProgramaGenerado.java");
        Files.write(archivoJava, codigoJava.getBytes(StandardCharsets.UTF_8));
        Files.write(directorioSalida.resolve("codigo_maquina.txt"),
                codigoMaquina, StandardCharsets.UTF_8);
        Files.write(directorioSalida.resolve("tabla_simbolos.txt"),
                generarTablaSimbolos(simbolos, etiquetas, direccionesInstrucciones), StandardCharsets.UTF_8);

        System.out.println("\n>> CÓDIGO MÁQUINA (ISA educativa; direcciones ilustrativas):");
        for (String instruccion : codigoMaquina) {
            System.out.println("   " + instruccion);
        }
        System.out.println("\n>> TABLA DE SÍMBOLOS Y ETIQUETAS (direcciones ilustrativas):");
        for (Map.Entry<String, Integer> simbolo : simbolos.entrySet()) {
            System.out.println("   Variable " + simbolo.getKey() + " -> " +
                    direccionDato(simbolo.getValue()));
        }
        for (Map.Entry<String, Integer> etiqueta : etiquetas.entrySet()) {
            System.out.println("   Etiqueta " + etiqueta.getKey() + " -> " +
                    formatearDireccion(direccionesInstrucciones.get(etiqueta.getValue())));
        }

        compilarYEjecutar(archivoJava, directorioSalida);
    }

    private Map<String, Integer> obtenerEtiquetas(List<Instruccion3AC> instrucciones) {
        Map<String, Integer> etiquetas = new LinkedHashMap<>();
        for (int i = 0; i < instrucciones.size(); i++) {
            Instruccion3AC instruccion = instrucciones.get(i);
            if ("ETIQUETA".equals(instruccion.tipo)) {
                validarIdentificador(instruccion.destino, "etiqueta");
                if (etiquetas.put(instruccion.destino, i) != null) {
                    throw new IllegalArgumentException("Etiqueta duplicada: " + instruccion.destino);
                }
            }
        }
        return etiquetas;
    }

    private Map<String, Integer> obtenerSimbolos(List<Instruccion3AC> instrucciones,
                                                   Map<String, Integer> valoresIniciales) {
        Map<String, Integer> simbolos = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entrada : valoresIniciales.entrySet()) {
            validarIdentificador(entrada.getKey(), "variable inicial");
            agregarSimbolo(simbolos, entrada.getKey());
        }

        for (Instruccion3AC instruccion : instrucciones) {
            if ("ETIQUETA".equals(instruccion.tipo) || "GOTO".equals(instruccion.tipo)) {
                continue;
            }
            if (instruccion.destino != null) {
                validarIdentificador(instruccion.destino, "destino");
                agregarSimbolo(simbolos, instruccion.destino);
            }
            agregarOperando(simbolos, instruccion.op1);
            agregarOperando(simbolos, instruccion.op2);
        }
        return simbolos;
    }

    private void agregarOperando(Map<String, Integer> simbolos, String operando) {
        if (operando != null && !esNumero(operando)) {
            validarIdentificador(operando, "operando");
            agregarSimbolo(simbolos, operando);
        }
    }

    private void agregarSimbolo(Map<String, Integer> simbolos, String nombre) {
        if (!simbolos.containsKey(nombre)) {
            simbolos.put(nombre, simbolos.size());
        }
    }

    private Map<Integer, Integer> obtenerDirecciones(List<Instruccion3AC> instrucciones) {
        Map<Integer, Integer> direcciones = new LinkedHashMap<>();
        int direccion = DIRECCION_INICIO_CODIGO;
        for (int i = 0; i < instrucciones.size(); i++) {
            direcciones.put(i, direccion);
            String tipo = instrucciones.get(i).tipo;
            if ("ASIGNACION".equals(tipo)) {
                direccion += 8;
            } else if ("OPERACION".equals(tipo)) {
                direccion += 12;
            } else if ("GOTO".equals(tipo) || "CONDICIONAL".equals(tipo)) {
                direccion += 4;
            } else if (!"ETIQUETA".equals(tipo)) {
                throw new IllegalArgumentException("Tipo de instrucción no soportado: " + tipo);
            }
        }
        return direcciones;
    }

    private List<String> traducir(List<Instruccion3AC> instrucciones,
                                  Map<String, Integer> etiquetas,
                                  Map<String, Integer> simbolos,
                                  Map<Integer, Integer> direcciones) {
        List<String> codigo = new ArrayList<>();
        for (int i = 0; i < instrucciones.size(); i++) {
            Instruccion3AC instruccion = instrucciones.get(i);
            int direccion = direcciones.get(i);
            switch (instruccion.tipo) {
                case "ETIQUETA":
                    codigo.add(formatearDireccion(direccion) + ": " + instruccion.destino + ":");
                    break;
                case "ASIGNACION":
                    codigo.add(formatearDireccion(direccion) + ": " + cargar("R1", instruccion.op1, simbolos));
                    codigo.add(formatearDireccion(direccion + 4) + ": STORE [" +
                            direccionDato(simbolos.get(instruccion.destino)) + "], R1");
                    break;
                case "OPERACION":
                    validarOperadorAritmetico(instruccion.operador);
                    codigo.add(formatearDireccion(direccion) + ": " + cargar("R1", instruccion.op1, simbolos));
                    codigo.add(formatearDireccion(direccion + 4) + ": " +
                            instruccionOperacion(instruccion.operador, instruccion.op2, simbolos));
                    codigo.add(formatearDireccion(direccion + 8) + ": STORE [" +
                            direccionDato(simbolos.get(instruccion.destino)) + "], R1");
                    break;
                case "GOTO":
                    validarEtiqueta(instruccion.destino, etiquetas);
                    codigo.add(formatearDireccion(direccion) + ": JMP " +
                            formatearDireccion(direcciones.get(etiquetas.get(instruccion.destino))));
                    break;
                case "CONDICIONAL":
                    Condicion condicion = obtenerCondicion(instruccion, etiquetas);
                    codigo.add(formatearDireccion(direccion) + ": J" +
                            sufijoSalto(condicion.operador) + " [" +
                            ubicacion(condicion.op1, simbolos) + "], [" +
                            ubicacion(condicion.op2, simbolos) + "], " +
                            formatearDireccion(direcciones.get(etiquetas.get(condicion.etiqueta))));
                    break;
                default:
                    throw new IllegalArgumentException("Tipo de instrucción no soportado: " + instruccion.tipo);
            }
        }
        return codigo;
    }

    private String generarCodigoJava(List<Instruccion3AC> instrucciones,
                                     Map<String, Integer> etiquetas,
                                     Map<String, Integer> simbolos,
                                     Map<String, Integer> valoresIniciales) {
        StringBuilder fuente = new StringBuilder();
        fuente.append("public class ProgramaGenerado {\n")
                .append("  public static void main(String[] args) {\n")
                .append("    System.setOut(new java.io.PrintStream(new java.io.FileOutputStream(java.io.FileDescriptor.out), true, java.nio.charset.StandardCharsets.UTF_8));\n")
                .append("    int[] memoria = new int[").append(simbolos.size()).append("];\n");
        for (Map.Entry<String, Integer> inicial : valoresIniciales.entrySet()) {
            fuente.append("    memoria[").append(simbolos.get(inicial.getKey())).append("] = ")
                    .append(inicial.getValue()).append(";\n");
        }
        fuente.append("    int pc = 0;\n")
                .append("    while (pc >= 0) {\n")
                .append("      switch (pc) {\n");

        for (int i = 0; i < instrucciones.size(); i++) {
            Instruccion3AC instruccion = instrucciones.get(i);
            fuente.append("        case ").append(i).append(": {\n");
            switch (instruccion.tipo) {
                case "ETIQUETA":
                    fuente.append("          pc = ").append(i + 1).append(";\n");
                    break;
                case "ASIGNACION":
                    fuente.append("          memoria[").append(simbolos.get(instruccion.destino))
                            .append("] = ").append(valorJava(instruccion.op1, simbolos)).append(";\n")
                            .append("          pc = ").append(i + 1).append(";\n");
                    break;
                case "OPERACION":
                    validarOperadorAritmetico(instruccion.operador);
                    fuente.append("          memoria[").append(simbolos.get(instruccion.destino))
                            .append("] = ").append(valorJava(instruccion.op1, simbolos))
                            .append(" ").append(instruccion.operador).append(" ")
                            .append(valorJava(instruccion.op2, simbolos)).append(";\n")
                            .append("          pc = ").append(i + 1).append(";\n");
                    break;
                case "GOTO":
                    validarEtiqueta(instruccion.destino, etiquetas);
                    fuente.append("          pc = ").append(etiquetas.get(instruccion.destino)).append(";\n");
                    break;
                case "CONDICIONAL":
                    Condicion condicion = obtenerCondicion(instruccion, etiquetas);
                    fuente.append("          pc = (").append(valorJava(condicion.op1, simbolos))
                            .append(" ").append(condicion.operador).append(" ")
                            .append(valorJava(condicion.op2, simbolos)).append(") ? ")
                            .append(etiquetas.get(condicion.etiqueta)).append(" : ").append(i + 1).append(";\n");
                    break;
                default:
                    throw new IllegalArgumentException("Tipo de instrucción no soportado: " + instruccion.tipo);
            }
            fuente.append("          break;\n        }\n");
        }

        fuente.append("        default: pc = -1;\n")
                .append("      }\n")
                .append("    }\n")
                .append("    System.out.println(\"\\nResultado de la ejecución:\");\n");
        for (Map.Entry<String, Integer> simbolo : simbolos.entrySet()) {
            fuente.append("    System.out.println(\"  ").append(simbolo.getKey()).append(" = \" + memoria[")
                    .append(simbolo.getValue()).append("]);\n");
        }
        fuente.append("  }\n}\n");
        return fuente.toString();
    }

    private Condicion obtenerCondicion(Instruccion3AC instruccion, Map<String, Integer> etiquetas) {
        Matcher coincidencia = CONDICIONAL.matcher(instruccion.textoOriginal);
        if (!coincidencia.matches()) {
            throw new IllegalArgumentException(
                    "Condicional inválido; se esperaba: if operando operador operando goto etiqueta. Recibido: " +
                            instruccion.textoOriginal);
        }
        Condicion condicion = new Condicion(
                coincidencia.group(1), coincidencia.group(2), coincidencia.group(3), coincidencia.group(4));
        if (!condicion.operador.equals(instruccion.operador)
                || !condicion.op1.equals(instruccion.op1)
                || !condicion.op2.equals(instruccion.op2)) {
            throw new IllegalArgumentException("Los operandos del condicional no coinciden con la instrucción 3AC.");
        }
        validarEtiqueta(condicion.etiqueta, etiquetas);
        return condicion;
    }

    private void validarEtiqueta(String nombre, Map<String, Integer> etiquetas) {
        if (!etiquetas.containsKey(nombre)) {
            throw new IllegalArgumentException("Etiqueta no definida: " + nombre);
        }
    }

    private void validarIdentificador(String nombre, String contexto) {
        if (nombre == null || !IDENTIFICADOR.matcher(nombre).matches()) {
            throw new IllegalArgumentException("Identificador inválido para " + contexto + ": " + nombre);
        }
    }

    private void validarOperadorAritmetico(String operador) {
        if (!"+".equals(operador) && !"-".equals(operador) && !"*".equals(operador)
                && !"/".equals(operador) && !"%".equals(operador)) {
            throw new IllegalArgumentException("Operador aritmético no soportado: " + operador);
        }
    }

    private boolean esNumero(String valor) {
        if (valor == null) {
            return false;
        }
        try {
            Integer.parseInt(valor);
            return true;
        } catch (NumberFormatException excepcion) {
            return false;
        }
    }

    private String valorJava(String operando, Map<String, Integer> simbolos) {
        if (esNumero(operando)) {
            return operando;
        }
        Integer indice = simbolos.get(operando);
        if (indice == null) {
            throw new IllegalArgumentException("Variable no definida: " + operando);
        }
        return "memoria[" + indice + "]";
    }

    private String cargar(String registro, String operando, Map<String, Integer> simbolos) {
        return esNumero(operando)
                ? "LOADI " + registro + ", #" + operando
                : "LOAD " + registro + ", [" + direccionDato(simbolos.get(operando)) + "]";
    }

    private String instruccionOperacion(String operador, String operando,
                                        Map<String, Integer> simbolos) {
        String sufijo;
        switch (operador) {
            case "+": sufijo = "ADD"; break;
            case "-": sufijo = "SUB"; break;
            case "*": sufijo = "MUL"; break;
            case "/": sufijo = "DIV"; break;
            case "%": sufijo = "MOD"; break;
            default: throw new IllegalArgumentException("Operador aritmético no soportado: " + operador);
        }
        return sufijo + (esNumero(operando) ? "I R1, #" + operando : " R1, [" +
                direccionDato(simbolos.get(operando)) + "]");
    }

    private String ubicacion(String operando, Map<String, Integer> simbolos) {
        return esNumero(operando) ? "#" + operando : direccionDato(simbolos.get(operando));
    }

    private String sufijoSalto(String operador) {
        switch (operador) {
            case "==": return "EQ";
            case "!=": return "NE";
            case "<": return "LT";
            case "<=": return "LE";
            case ">": return "GT";
            case ">=": return "GE";
            default: throw new IllegalArgumentException("Operador relacional no soportado: " + operador);
        }
    }

    private String direccionDato(int indice) {
        return formatearDireccion(DIRECCION_INICIO_DATOS + indice * 4);
    }

    private String formatearDireccion(int direccion) {
        return String.format(Locale.ROOT, "0x%04X", direccion);
    }

    private List<String> generarTablaSimbolos(Map<String, Integer> simbolos,
                                               Map<String, Integer> etiquetas,
                                               Map<Integer, Integer> direcciones) {
        List<String> lineas = new ArrayList<>();
        lineas.add("Tipo | Símbolo | Dirección");
        lineas.add("-----|---------|----------");
        for (Map.Entry<String, Integer> simbolo : simbolos.entrySet()) {
            lineas.add("Variable | " + simbolo.getKey() + " | " +
                    direccionDato(simbolo.getValue()));
        }
        for (Map.Entry<String, Integer> etiqueta : etiquetas.entrySet()) {
            lineas.add("Etiqueta | " + etiqueta.getKey() + " | " +
                    formatearDireccion(direcciones.get(etiqueta.getValue())));
        }
        return lineas;
    }

    private void compilarYEjecutar(Path archivoJava, Path directorioSalida)
            throws IOException, InterruptedException {
        JavaCompiler compilador = ToolProvider.getSystemJavaCompiler();
        if (compilador == null) {
            throw new IllegalStateException("Se requiere un JDK para compilar el programa generado.");
        }
        int resultadoCompilacion = compilador.run(null, System.out, System.err,
                "-encoding", "UTF-8", "-d",
                directorioSalida.toAbsolutePath().toString(), archivoJava.toAbsolutePath().toString());
        if (resultadoCompilacion != 0) {
            throw new IllegalStateException("Falló la compilación del código generado (código " +
                    resultadoCompilacion + ").");
        }

        String ejecutableJava = Path.of(System.getProperty("java.home"), "bin",
                System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("win") ? "java.exe" : "java")
                .toString();
        Process proceso = new ProcessBuilder(ejecutableJava, "-cp",
                directorioSalida.toAbsolutePath().toString(), "ProgramaGenerado")
                .inheritIO()
                .start();
        int resultadoEjecucion = proceso.waitFor();
        if (resultadoEjecucion != 0) {
            throw new IllegalStateException("Falló la ejecución del programa generado (código " +
                    resultadoEjecucion + ").");
        }
        System.out.println("\nEjecutable JVM generado y ejecutado desde: " +
                directorioSalida.toAbsolutePath());
    }

    private static final class Condicion {
        private final String op1;
        private final String operador;
        private final String op2;
        private final String etiqueta;

        private Condicion(String op1, String operador, String op2, String etiqueta) {
            this.op1 = op1;
            this.operador = operador;
            this.op2 = op2;
            this.etiqueta = etiqueta;
        }
    }
}
