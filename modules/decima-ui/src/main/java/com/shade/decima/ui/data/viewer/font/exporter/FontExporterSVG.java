package com.shade.decima.ui.data.viewer.font.exporter;

import com.shade.decima.model.rtti.types.java.HwFont;
import com.shade.decima.ui.data.viewer.font.FontExporter;
import com.shade.util.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.awt.geom.Path2D;
import java.awt.geom.PathIterator;
import java.io.OutputStream;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

public class FontExporterSVG implements FontExporter {
    @Override
    public void export(@NotNull HwFont font, @NotNull WritableByteChannel channel) throws Exception {
        final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        final DocumentBuilder builder = dbf.newDocumentBuilder();
        final Document document = builder.newDocument();

        final Element elemSvg = document.createElement("svg");
        elemSvg.setAttribute("version", "1.1");
        elemSvg.setAttribute("width", "100%");
        elemSvg.setAttribute("height", "100%");
        document.appendChild(elemSvg);

        final Element elemDefs = document.createElement("defs");
        elemSvg.appendChild(elemDefs);

        final Element elemFont = document.createElement("font");

        final Element elemFontFace = document.createElement("font-face");
        elemFontFace.setAttribute("font-family", font.getName());
        elemFontFace.setAttribute("units-per-em", String.valueOf(font.getHeight()));
        elemFontFace.setAttribute("ascent", String.valueOf(font.getAscent()));
        elemFontFace.setAttribute("descent", String.valueOf(font.getDescent()));
        elemFontFace.setAttribute("cap-height", String.valueOf(font.getEmHeight()));
        elemFontFace.setAttribute("vert-origin-y", "0");
        elemFont.appendChild(elemFontFace);

        for (int i = 0; i < font.getGlyphCount(); i++) {
            final HwFont.Glyph glyph = font.getGlyph(i);

            final Element elemGlyph = document.createElement("glyph");
            elemGlyph.setAttribute("unicode", Character.toString(glyph.getCodePoint()));
            elemGlyph.setAttribute("horiz-adv-x", String.valueOf(glyph.getAdvanceWidth()));
            elemFont.appendChild(elemGlyph);

            final Element elemPath = document.createElement("path");
            elemPath.setAttribute("d", getSvgPath(glyph.getPath(), font.getAscent()));
            elemGlyph.appendChild(elemPath);
        }

        elemSvg.appendChild(elemFont);

        try (OutputStream os = Channels.newOutputStream(channel)) {
            final TransformerFactory tf = TransformerFactory.newInstance();
            final Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.transform(new DOMSource(document), new StreamResult(os));
        }
    }

    @NotNull
    @Override
    public String getExtension() {
        return "svg";
    }

    @NotNull
    private static String getSvgPath(@NotNull Path2D path, float ascent) {
        final StringBuilder sb = new StringBuilder();
        final PathIterator it = path.getPathIterator(null);
        final float[] coords = new float[6];

        while (!it.isDone()) {
            switch (it.currentSegment(coords)) {
                case PathIterator.SEG_MOVETO -> sb.append("M%.3f,%.3f".formatted(
                    coords[0], ascent - coords[1]));
                case PathIterator.SEG_LINETO -> sb.append("L%.3f,%.3f".formatted(
                    coords[0], ascent - coords[1]));
                case PathIterator.SEG_CUBICTO -> sb.append("C%.3f,%.3f,%.3f,%.3f,%.3f,%.3f".formatted(
                    coords[0], ascent - coords[1],
                    coords[2], ascent - coords[3],
                    coords[4], ascent - coords[5]));
                case PathIterator.SEG_CLOSE -> sb.append('Z');
            }

            it.next();
        }

        return sb.toString();
    }
}
