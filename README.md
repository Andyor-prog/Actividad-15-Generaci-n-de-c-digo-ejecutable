# Actividad 15: Generación de código ejecutable

El proyecto extiende el optimizador de código intermedio de tres direcciones (3AC) con un backend para una ISA educativa y la generación de un programa ejecutable en la máquina virtual de Java (JVM).

## Requisitos

- JDK 11 o posterior (se utiliza `Path.of` y la API `JavaCompiler`).

## Compilar y ejecutar

Desde la raíz del proyecto:

```text
javac -encoding UTF-8 -d out src\act14_optimizacion_codigo\*.java
java -cp out act14_optimizacion_codigo.Main
```

Las clases están en `src/act14_optimizacion_codigo/` y la configuración de `.vscode/` registra `src` como carpeta de código fuente. Abre la carpeta raíz del proyecto en VS Code y espera a que termine la importación de Java.

El archivo `pom.xml` permite que VS Code importe el código como un proyecto Maven. Si tienes Maven instalado, también puedes compilar con `mvn clean package` y ejecutar con `java -cp target\classes act14_optimizacion_codigo.Main`.

La demostración optimiza `t1 = x + 5; t2 = t1 + 10; result = t2`, asigna `x = 10`, y ejecuta el código resultante. Debe mostrar `result = 25`. También ejecuta una prueba de saltos condicionales y muestra las direcciones de sus etiquetas.

El programa genera en `salida-ejecutable/`:

- `ProgramaGenerado.java`: fuente del programa objetivo.
- `ProgramaGenerado.class`: bytecode JVM compilado y ejecutado.
- `codigo_maquina.txt`: traducción a instrucciones de una ISA educativa con direcciones ilustrativas.
- `tabla_simbolos.txt`: variables, etiquetas y direcciones ilustrativas.

La ISA educativa y las direcciones mostradas son una representación didáctica; el archivo ejecutable generado es bytecode JVM y requiere una instalación de Java para ejecutarse.

## Instrucciones 3AC del backend

Se admiten asignaciones, operaciones enteras `+`, `-`, `*`, `/`, `%`, etiquetas, saltos incondicionales `goto L1` y condicionales con el formato `if a > b goto L1`. Las variables sin valor inicial se inicializan en cero. Los operandos deben ser identificadores o enteros decimales.
