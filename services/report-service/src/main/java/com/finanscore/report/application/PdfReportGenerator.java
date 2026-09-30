package com.finanscore.report.application;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Genera un reporte académico de la evaluación. No representa una aprobación bancaria definitiva. */
@Component
public class PdfReportGenerator {
    public byte[] generate(Map<String, Object> payload) throws Exception {
        try (var document = new PDDocument(); var output = new ByteArrayOutputStream()) {
            var page = new PDPage();
            document.addPage(page);

            try (var content = new PDPageContentStream(document, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 16);
                content.newLineAtOffset(50, 750);
                content.showText("REPORTE DE EVALUACION DE RIESGO CREDITICIO");
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 10);

                List<String> lines = new ArrayList<>();
                lines.add("");
                lines.add("Solicitud: " + payload.get("requestId"));
                lines.add("Producto: " + payload.get("productCode"));
                lines.add("Monto solicitado: " + payload.get("amount") + " " + payload.get("currency"));
                lines.add("Plazo: " + payload.get("termMonths") + " meses");
                lines.add("Finalidad: " + payload.get("purpose"));
                lines.add("");
                lines.add("RESULTADO DE LA EVALUACION");
                lines.add("Puntaje: " + payload.get("score") + " / 1000");
                lines.add("Recomendacion: " + payload.get("recommendation"));
                lines.add("Modelo utilizado: " + payload.get("modelVersion"));
                lines.add("");
                lines.add("FACTORES EVALUADOS");

                Object factors = payload.get("factors");
                if (factors instanceof List<?> list && !list.isEmpty()) {
                    for (Object item : list) {
                        if (item instanceof Map<?, ?> factor) {
                            String name = String.valueOf(factor.get("factor"));
                            String score = String.valueOf(factor.get("puntajeObtenido"));
                            String observation = factor.get("observacion") == null ? "" : String.valueOf(factor.get("observacion"));
                            lines.add("- " + name + ": " + score + " puntos" + (observation.isBlank() ? "" : " - " + observation));
                        } else {
                            lines.add("- " + String.valueOf(item));
                        }
                    }
                } else {
                    lines.add("- Detalle disponible en la trazabilidad de la evaluacion.");
                }

                lines.add("");
                lines.add("La recomendacion es resultado del motor de scoring para esta solicitud.");
                lines.add("No constituye desembolso ni aprobacion bancaria definitiva.");

                int written = 0;
                for (String line : lines) {
                    if (written >= 30) {
                        content.newLineAtOffset(0, -18);
                        content.showText("(Detalle adicional conservado en la evaluacion y auditoria.)");
                        break;
                    }
                    content.newLineAtOffset(0, -18);
                    content.showText(safe(line, 105));
                    written++;
                }
                content.endText();
            }

            document.save(output);
            return output.toByteArray();
        }
    }

    private String safe(String value, int max) {
        String normalized = value == null ? "" : value.replace('\n', ' ').replace('\r', ' ');
        return normalized.length() <= max ? normalized : normalized.substring(0, max - 3) + "...";
    }
}
