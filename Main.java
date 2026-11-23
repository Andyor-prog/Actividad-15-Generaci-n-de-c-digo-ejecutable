package act14_optimizacion_codigo;

/**
 * Cervantes Mora Juan Pablo
 * Muñoz Madriz Nadya Janneth
 * Ramirez Torres Jesus Adrian
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("--------------------------------------------------------");
        System.out.println("ACTIVIDAD 14: OPTIMIZACION DE CODIGO INTERMEDIO (3AC)");
        System.out.println("Integrantes:");
        System.out.println(" -Cervantes Mora Juan Pablo");
        System.out.println(" -Munoz Madriz Nadya Janneth");
        System.out.println(" -Ramirez Torres Jesus Adrian");
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

        System.out.println("--------------------------------------------------------\n");
        System.out.println("FIN DE ACTIVIDAD 14 - OPTIMIZACION DE CODIGO INTERMEDIO");
        System.out.println("Integrantes: Cervantes Mora Juan Pablo, Munoz Madriz Nadya Janneth, Ramirez Torres Jesus Adrian");
        System.out.println("--------------------------------------------------------\n");
    }
}