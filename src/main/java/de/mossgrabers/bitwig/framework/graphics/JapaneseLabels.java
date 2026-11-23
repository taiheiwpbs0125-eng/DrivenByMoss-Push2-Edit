// SPDX-License-Identifier: LGPL-3.0-only
package de.mossgrabers.bitwig.framework.graphics;

import com.bitwig.extension.api.graphics.GraphicsOutput;
import java.awt.Font;
import java.awt.Shape;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.PathIterator;
import java.util.LinkedHashMap;
import java.util.Map;

/** Per-display cache of Japanese label outlines in baseline coordinates. */
final class JapaneseLabels {
    private static final Font FONT = new Font("Meiryo", Font.PLAIN, 12);
    private static final FontRenderContext METRICS = new FontRenderContext(null, true, true);
    private record Key(String text, float size) {}
    record Label(double width, double ascent, double descent, Shape outline) {
        void paint(GraphicsOutput gc, double x, double baseline) {
            gc.save();
            try {
                gc.translate(x, baseline);
                gc.newPath();
                double[] p = new double[6];
                double lastX = 0, lastY = 0;
                for (PathIterator it = outline.getPathIterator(null); !it.isDone(); it.next()) {
                    switch (it.currentSegment(p)) {
                        case PathIterator.SEG_MOVETO -> { gc.moveTo(p[0], p[1]); lastX=p[0]; lastY=p[1]; }
                        case PathIterator.SEG_LINETO -> { gc.lineTo(p[0], p[1]); lastX=p[0]; lastY=p[1]; }
                        case PathIterator.SEG_QUADTO -> {
                            gc.curveTo((lastX+2*p[0])/3, (lastY+2*p[1])/3,
                                (p[2]+2*p[0])/3, (p[3]+2*p[1])/3, p[2], p[3]);
                            lastX=p[2]; lastY=p[3];
                        }
                        case PathIterator.SEG_CUBICTO -> { gc.curveTo(p[0],p[1],p[2],p[3],p[4],p[5]); lastX=p[4]; lastY=p[5]; }
                        case PathIterator.SEG_CLOSE -> gc.closePath();
                    }
                }
                gc.fill();
            } finally { gc.restore(); }
        }
    }
    private final Map<Key, Label> cache = new LinkedHashMap<>(128, .75f, true) {
        protected boolean removeEldestEntry(Map.Entry<Key, Label> eldest) { return size() > 256; }
    };

    boolean matches(String text) {
        if (text == null) return false;
        for (int i=0; i<text.length();) {
            int c=text.codePointAt(i);
            if (c >= 0x3000 && c <= 0x30ff || c >= 0xff00 && c <= 0xffef ||
                c >= 0x31f0 && c <= 0x31ff || c >= 0x3400 && Character.UnicodeScript.of(c)==Character.UnicodeScript.HAN)
                return true;
            i += Character.charCount(c);
        }
        return false;
    }

    Label get(String text, double size) {
        return cache.computeIfAbsent(new Key(text, (float)Math.max(1,size)), key -> {
            TextLayout line = new TextLayout(key.text(), FONT.deriveFont(key.size()), METRICS);
            return new Label(line.getAdvance(), line.getAscent(), line.getDescent(), line.getOutline(null));
        });
    }
}
