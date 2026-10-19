package com.donggree.transcript.internal.infrastructure;

import com.donggree.transcript.internal.application.pdf.PdfTextReader;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.PDFTextStripperByArea;
import org.apache.pdfbox.text.TextPosition;
import org.springframework.stereotype.Component;

/** nDRIMS의 3열 표를 열 단위로 읽어 취득 내역과 수강신청 구역의 경계를 보존한다. */
@Component
public class PdfTextExtractor implements PdfTextReader {
    @Override
    public String extract(byte[] pdfBytes) throws IOException {
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            StringBuilder text = new StringBuilder();
            for (int pageNumber = 1; pageNumber <= doc.getNumberOfPages(); pageNumber++) {
                var detector = new TableHeaders();
                detector.setStartPage(pageNumber);
                detector.setEndPage(pageNumber);
                detector.setSortByPosition(true);
                String standard = detector.getText(doc);
                List<TextPosition> headers = detector.headers();
                if (headers.size() != 3) {
                    if (standard.contains("<<수강신청>>")) {
                        throw new IOException("수강신청이 포함된 표의 열 경계를 확인할 수 없습니다.");
                    }
                    text.append(standard).append('\n');
                    continue;
                }
                var page = doc.getPage(pageNumber - 1);
                float width = page.getCropBox().getWidth();
                float height = page.getCropBox().getHeight();
                float top = Math.max(0, headers.getFirst().getYDirAdj() - 12);
                var stripper = new PDFTextStripperByArea();
                stripper.setSortByPosition(true);
                stripper.addRegion("header", new Rectangle2D.Float(0, 0, width, top));
                for (int column = 0; column < 3; column++) {
                    float left = column == 0 ? 0 : headers.get(column).getXDirAdj() - 8;
                    float right = column == 2 ? width : headers.get(column + 1).getXDirAdj() - 8;
                    stripper.addRegion("column" + column, new Rectangle2D.Float(left, top, right - left, height - top));
                }
                stripper.extractRegions(page);
                text.append(stripper.getTextForRegion("header")).append('\n');
                for (int column = 0; column < 3; column++) {
                    text.append(stripper.getTextForRegion("column" + column)).append('\n');
                }
            }
            return text.toString();
        }
    }

    private static final class TableHeaders extends PDFTextStripper {
        private final List<TextPosition> positions = new ArrayList<>();

        private TableHeaders() throws IOException {}

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            positions.addAll(textPositions);
            super.writeString(text, textPositions);
        }

        private List<TextPosition> headers() {
            List<TextPosition> candidates = new ArrayList<>();
            for (int i = 0; i + 1 < positions.size(); i++) {
                TextPosition current = positions.get(i);
                TextPosition next = positions.get(i + 1);
                if ("년".equals(current.getUnicode())
                        && "학".equals(next.getUnicode())
                        && Math.abs(current.getYDirAdj() - next.getYDirAdj()) < 1
                        && Math.abs(current.getXDirAdj() - next.getXDirAdj()) <= current.getWidthDirAdj() + 1) {
                    candidates.add(current);
                }
            }
            if (candidates.isEmpty()) return List.of();
            float firstRow = candidates.stream()
                    .map(TextPosition::getYDirAdj)
                    .min(Float::compare)
                    .orElseThrow();
            return candidates.stream()
                    .filter(p -> Math.abs(p.getYDirAdj() - firstRow) < 1)
                    .sorted(Comparator.comparing(TextPosition::getXDirAdj))
                    .toList();
        }
    }
}
