from pathlib import Path
import subprocess
import textwrap

from PIL import Image, ImageDraw, ImageFont
from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER, TA_LEFT
from reportlab.lib.pagesizes import letter
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import inch
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Image as PdfImage,
    PageBreak,
    Paragraph,
    Preformatted,
    SimpleDocTemplate,
    Spacer,
    Table,
    TableStyle,
)


ROOT = Path(__file__).resolve().parent
EVIDENCE = ROOT / "evidencias"
PDF_PATH = ROOT / (
    "CervantesMoraJuanPablo_MunozMadrizNadyaJanneth_"
    "RamirezTorresJesusAdrian_A15_GeneracionCodigoEjecutable.pdf"
)

pdfmetrics.registerFont(TTFont("Arial", r"C:\Windows\Fonts\arial.ttf"))
pdfmetrics.registerFont(TTFont("Arial-Bold", r"C:\Windows\Fonts\arialbd.ttf"))
pdfmetrics.registerFont(TTFont("Consolas", r"C:\Windows\Fonts\consola.ttf"))
FONT_REGULAR = ImageFont.truetype(r"C:\Windows\Fonts\consola.ttf", 17)
FONT_BOLD = ImageFont.truetype(r"C:\Windows\Fonts\arialbd.ttf", 18)


def build_capture(filename, heading, command, lines):
    width = 1440
    title_height = 46
    command_height = 38
    margin = 24
    line_height = 22
    height = title_height + command_height + margin * 2 + max(1, len(lines)) * line_height
    image = Image.new("RGB", (width, height), "#101820")
    draw = ImageDraw.Draw(image)
    draw.rectangle((0, 0, width, title_height), fill="#263746")
    draw.text((20, 12), heading, font=FONT_BOLD, fill="#ffffff")
    draw.rectangle((0, title_height, width, title_height + command_height), fill="#17232d")
    draw.text((20, title_height + 9), command, font=FONT_REGULAR, fill="#78dba9")
    y = title_height + command_height + margin
    for line in lines:
        draw.text((margin, y), line.rstrip(), font=FONT_REGULAR, fill="#e7edf2")
        y += line_height
    path = EVIDENCE / filename
    image.save(path)
    return path


def add_page_chrome(canvas, doc):
    canvas.saveState()
    page_width, page_height = letter
    canvas.setStrokeColor(colors.HexColor("#d5e0e8"))
    canvas.setLineWidth(0.6)
    canvas.line(0.65 * inch, 0.55 * inch, page_width - 0.65 * inch, 0.55 * inch)
    canvas.setFont("Arial", 8)
    canvas.setFillColor(colors.HexColor("#536575"))
    canvas.drawString(0.68 * inch, 0.38 * inch, "Actividad 15 | Traductores de Lenguajes I")
    canvas.drawRightString(page_width - 0.68 * inch, 0.38 * inch, str(doc.page))
    canvas.restoreState()


def make_styles():
    styles = getSampleStyleSheet()
    styles.add(ParagraphStyle(
        name="ReportTitle", fontName="Arial-Bold", fontSize=23, leading=29,
        alignment=TA_CENTER, textColor=colors.HexColor("#15324b"), spaceAfter=12))
    styles.add(ParagraphStyle(
        name="Section", fontName="Arial-Bold", fontSize=16, leading=20,
        textColor=colors.HexColor("#15324b"), spaceBefore=5, spaceAfter=10))
    styles.add(ParagraphStyle(
        name="Subsection", fontName="Arial-Bold", fontSize=11, leading=14,
        textColor=colors.HexColor("#1c5878"), spaceBefore=7, spaceAfter=5))
    styles.add(ParagraphStyle(
        name="BodyES", fontName="Arial", fontSize=9.6, leading=14,
        alignment=TA_LEFT, spaceAfter=7))
    styles.add(ParagraphStyle(
        name="SmallES", fontName="Arial", fontSize=8.5, leading=12,
        textColor=colors.HexColor("#33495b"), spaceAfter=5))
    styles.add(ParagraphStyle(
        name="CodeBlock", fontName="Consolas", fontSize=6.5, leading=8.1,
        textColor=colors.HexColor("#17232d"), leftIndent=3, rightIndent=3))
    styles.add(ParagraphStyle(
        name="CoverInfo", fontName="Arial", fontSize=12, leading=19,
        alignment=TA_CENTER, textColor=colors.HexColor("#263746")))
    return styles


def make_table(rows, widths, header=True):
    table = Table(rows, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
    commands = [
        ("GRID", (0, 0), (-1, -1), 0.45, colors.HexColor("#c8d5df")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]
    if header:
        commands.extend([
            ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#15324b")),
            ("TEXTCOLOR", (0, 0), (-1, 0), colors.white),
            ("FONTNAME", (0, 0), (-1, 0), "Arial-Bold"),
        ])
    commands.append(("FONTNAME", (0, 1 if header else 0), (-1, -1), "Arial"))
    commands.append(("FONTSIZE", (0, 0), (-1, -1), 8.5))
    table.setStyle(TableStyle(commands))
    return table


def add_source_pages(story, styles, filename):
    source = (ROOT / filename).read_text(encoding="utf-8").splitlines()
    display_lines = []
    for line in source:
        wrapped = textwrap.wrap(
            line, width=125, subsequent_indent="    ",
            replace_whitespace=False, drop_whitespace=False,
            break_long_words=True, break_on_hyphens=False)
        display_lines.extend(wrapped or [""])

    chunks = [display_lines[i:i + 57] for i in range(0, len(display_lines), 57)]
    for part, chunk in enumerate(chunks, 1):
        story.append(PageBreak())
        title = filename if len(chunks) == 1 else f"{filename} (parte {part}/{len(chunks)})"
        story.append(Paragraph("Apéndice de código", styles["Section"]))
        story.append(Paragraph(title, styles["Subsection"]))
        story.append(Preformatted("\n".join(chunk), styles["CodeBlock"]))


def main():
    EVIDENCE.mkdir(exist_ok=True)
    java_sources = sorted(
        str(path.relative_to(ROOT)) for path in (ROOT / "src").rglob("*.java"))
    compile_result = subprocess.run(
        ["javac", "-encoding", "UTF-8", "-d", "out", *java_sources],
        cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False)
    if compile_result.returncode != 0:
        raise RuntimeError(
            "Falló la compilación del proyecto.\n" +
            compile_result.stdout.decode("utf-8", errors="replace"))
    run = subprocess.run(
        ["java", "-Dfile.encoding=UTF-8", "-cp", "out", "act14_optimizacion_codigo.Main"],
        cwd=ROOT, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, check=False)
    output = run.stdout.decode("utf-8", errors="replace")
    if "\ufffd" in output:
        raise RuntimeError("La salida contiene caracteres inválidos; revise la codificación UTF-8.")
    (EVIDENCE / "salida_ejecucion.txt").write_text(output, encoding="utf-8")
    if run.returncode != 0:
        raise RuntimeError(f"La aplicación terminó con código {run.returncode}.\n{output}")

    valid_start = output.index("PRUEBAS VALIDAS (OPTIMIZABLES)")
    invalid_start = output.index("PRUEBAS INVALIDAS", valid_start)
    semantic_start = output.index("VALIDACION DE INTEGRIDAD SEMANTICA", invalid_start)
    valid_lines = output[valid_start:invalid_start].strip().splitlines()
    invalid_lines = output[invalid_start:semantic_start].strip().splitlines()
    if invalid_lines and invalid_lines[-1].strip() == ">>":
        invalid_lines.pop()
    result_lines = output[semantic_start:].strip().splitlines()

    captures = [
        build_capture(
            "captura_pruebas_validas.png",
            "Consola - Pruebas válidas de optimización",
            "javac -encoding UTF-8 -d out src\\act14_optimizacion_codigo\\*.java && java -Dfile.encoding=UTF-8 -cp out act14_optimizacion_codigo.Main",
            valid_lines),
        build_capture(
            "captura_pruebas_invalidas.png",
            "Consola - Controles negativos de optimización",
            "javac -encoding UTF-8 -d out src\\act14_optimizacion_codigo\\*.java && java -Dfile.encoding=UTF-8 -cp out act14_optimizacion_codigo.Main",
            invalid_lines),
        build_capture(
            "captura_generacion_ejecucion.png",
            "Consola - Código generado y ejecución JVM",
            "javac -encoding UTF-8 -d out src\\act14_optimizacion_codigo\\*.java && java -Dfile.encoding=UTF-8 -cp out act14_optimizacion_codigo.Main",
            result_lines),
    ]

    styles = make_styles()
    story = [
        Spacer(1, 0.55 * inch),
        Paragraph("UNIVERSIDAD DE GUADALAJARA", styles["CoverInfo"]),
        Spacer(1, 0.35 * inch),
        Paragraph("Traductores de Lenguajes I", styles["ReportTitle"]),
        Spacer(1, 0.1 * inch),
        Paragraph("ACTIVIDAD 15", styles["Section"]),
        Paragraph("Generación de código ejecutable", styles["ReportTitle"]),
        Spacer(1, 0.22 * inch),
        Paragraph("<b>Equipo 5</b>", styles["CoverInfo"]),
        Spacer(1, 0.25 * inch),
        Paragraph("<b>Integrantes</b>", styles["Subsection"]),
        Paragraph(
            "Cervantes Mora Juan Pablo &nbsp;|&nbsp; 223977265<br/>"
            "Muñoz Madriz Nadya Janneth &nbsp;|&nbsp; 220478101<br/>"
            "Ramirez Torres Jesus Adrian &nbsp;|&nbsp; 222348582",
            styles["CoverInfo"]),
        Spacer(1, 0.35 * inch),
        Paragraph("<b>Profesor:</b> José Navarro Rios", styles["CoverInfo"]),
        Spacer(1, 0.16 * inch),
        Paragraph("<b>Fecha:</b> 23 de noviembre de 2026", styles["CoverInfo"]),
        Spacer(1, 0.35 * inch),
        Paragraph(
            "<b>Video demostrativo:</b> Enlace público pendiente de agregar.",
            styles["SmallES"]),
        PageBreak(),
        Paragraph("1. Objetivo y alcance", styles["Section"]),
        Paragraph(
            "Transformar instrucciones de código intermedio de tres direcciones (3AC) "
            "en instrucciones de una ISA educativa y generar un programa ejecutable en "
            "la máquina virtual de Java (JVM). El programa generado se compila y ejecuta "
            "durante la demostración, y reporta los valores finales de las variables.",
            styles["BodyES"]),
        Paragraph("2. Proceso de generación", styles["Section"]),
        Paragraph(
            "El flujo inicia con el código 3AC, aplica el plegado de constantes "
            "implementado en la práctica anterior, determina variables, etiquetas y "
            "direcciones ilustrativas, traduce las operaciones a un listado de máquina "
            "educativo y genera ProgramaGenerado.java. El JDK compila esta fuente a "
            "bytecode JVM (.class) y el proceso ejecuta la clase resultante.",
            styles["BodyES"]),
        make_table([
            ["3AC optimizado", "Traducción educativa"],
            ["result = x + 15", "0x1000: LOAD R1, [0x2000]"],
            ["", "0x1004: ADDI R1, #15"],
            ["", "0x1008: STORE [0x2004], R1"],
        ], [2.05 * inch, 4.65 * inch]),
        Spacer(1, 8),
        Paragraph(
            "La ISA y las direcciones 0x1000/0x2000 son una representación didáctica; "
            "no son código nativo de un procesador real. El ejecutable obtenido y probado "
            "es bytecode JVM, dependiente de Java.",
            styles["SmallES"]),
        Paragraph("3. Direcciones y tabla de símbolos", styles["Section"]),
        make_table([
            ["Tipo", "Símbolo", "Dirección ilustrativa", "Uso"],
            ["Variable", "x", "0x2000", "Entrada inicial (10)"],
            ["Variable", "result", "0x2004", "Resultado calculado"],
            ["Etiqueta", "L1", "0x1018", "Destino del salto condicional"],
            ["Etiqueta", "L2", "0x1020", "Continuación del flujo"],
        ], [0.85 * inch, 1.05 * inch, 1.55 * inch, 3.25 * inch]),
        Paragraph("4. Pruebas y resultados", styles["Section"]),
        Paragraph(
            "Las pruebas válidas comprueban cinco optimizaciones. Los controles "
            "negativos verifican que no se eliminen variables necesarias, que no se "
            "fusionen expresiones diferentes y que se conserve el flujo condicional.",
            styles["BodyES"]),
        Paragraph(
            "Para la prueba aritmética, t1 = x + 5; t2 = t1 + 10; result = t2 se reduce "
            "a result = x + 15. Con x = 10, tanto el cálculo original como el optimizado "
            "dan 25. La prueba adicional toma la rama x > 5 y termina con result = 10.",
            styles["BodyES"]),
        Paragraph(
            "<b>Nota sobre evidencias:</b> las siguientes capturas muestran la salida "
            "real capturada al ejecutar el programa de consola con el JDK.",
            styles["SmallES"]),
    ]

    capture_titles = [
        "Evidencia 1. Pruebas válidas",
        "Evidencia 2. Pruebas negativas de control",
        "Evidencia 3. Generación y ejecución del código",
    ]
    for title, capture in zip(capture_titles, captures):
        story.extend([
            PageBreak(),
            Paragraph(title, styles["Section"]),
            Spacer(1, 5),
            PdfImage(str(capture), width=7.05 * inch, height=4.95 * inch, kind="proportional"),
        ])

    story.extend([
        PageBreak(),
        Paragraph("5. Ejecución y archivos generados", styles["Section"]),
        Paragraph("Requisitos: JDK 11 o posterior.", styles["BodyES"]),
        Preformatted(
            "javac -encoding UTF-8 -d out src\\act14_optimizacion_codigo\\*.java\n"
            "java -cp out act14_optimizacion_codigo.Main",
            styles["CodeBlock"]),
        Spacer(1, 8),
        Paragraph(
            "La ejecución genera ProgramaGenerado.java, ProgramaGenerado.class, "
            "codigo_maquina.txt y tabla_simbolos.txt dentro de salida-ejecutable/. "
            "El programa principal también genera una segunda salida en "
            "salida-ejecutable/flujo-condicional/ para la prueba de saltos.",
            styles["BodyES"]),
        Paragraph("Conclusión", styles["Subsection"]),
        Paragraph(
            "La práctica integra la optimización del 3AC, su traducción didáctica, la "
            "asignación de direcciones ilustrativas y la preparación y ejecución de un "
            "programa JVM. Los resultados observados fueron 25 para el caso aritmético "
            "y 10 para la prueba de salto condicional.",
            styles["BodyES"]),
        Paragraph(
            "<b>Pendiente antes de entregar:</b> agregar el enlace público del video "
            "descriptivo en la portada y en esta sección.",
            styles["SmallES"]),
    ])

    sources = [
        "src/act14_optimizacion_codigo/Main.java",
        "src/act14_optimizacion_codigo/GeneradorCodigoEjecutable.java",
        "src/act14_optimizacion_codigo/Instruccion3AC.java",
        "src/act14_optimizacion_codigo/Optimizador3AC.java",
        "src/act14_optimizacion_codigo/PruebasValidas.java",
        "src/act14_optimizacion_codigo/PruebasInvalidas.java",
    ]
    for filename in sources:
        add_source_pages(story, styles, filename)

    document = SimpleDocTemplate(
        str(PDF_PATH), pagesize=letter,
        rightMargin=0.68 * inch, leftMargin=0.68 * inch,
        topMargin=0.68 * inch, bottomMargin=0.72 * inch,
        title="Actividad 15: Generación de código ejecutable",
        author="Equipo 5 - Traductores de Lenguajes I",
        subject="Reporte de práctica con pruebas y código fuente")
    document.build(story, onFirstPage=add_page_chrome, onLaterPages=add_page_chrome)
    print(f"PDF: {PDF_PATH}")
    print(f"Capturas: {len(captures)}")
    print(f"Salida: {EVIDENCE / 'salida_ejecucion.txt'}")
    print(f"Bytes PDF: {PDF_PATH.stat().st_size}")


if __name__ == "__main__":
    main()
