// SPDX-License-Identifier: LGPL-3.0-only
package de.mossgrabers.controller.ableton.push.mode.device;

import java.awt.*;
import java.awt.geom.Arc2D;

record DeviceTile(String menu, boolean active, String context, String name, String value,
                  int position, int modulation, int maximum, boolean touched,
                  String footer, boolean selected, int trackColor) {
    private static final Color PANEL = new Color(36, 34, 33);
    private static final Color MUTED = new Color(203, 186, 172);
    private static final Font FONT = new Font("Meiryo", Font.PLAIN, 12);

    void paint(Graphics2D g, int width) {
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color accent = new Color(trackColor);
        g.setColor(touched ? new Color(70, 49, 36) : PANEL);
        g.fillRect(0, 0, width, 160);
        g.setColor(active ? Color.WHITE : MUTED);
        label(g, menu, 11, 15, width);
        g.setColor(accent);
        g.fillRect(3, 19, width - 6, active ? 3 : 1);
        g.setColor(MUTED);
        label(g, context, 11, 37, width);
        if (!name.isEmpty()) {
            g.setColor(Color.WHITE);
            label(g, name, 14, 58, width);
            int x = width / 2 - 17;
            g.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(Color.DARK_GRAY);
            g.draw(new Arc2D.Double(x, 67, 34, 34, 225, -270, Arc2D.OPEN));
            g.setColor(new Color(255, 132, 46));
            double amount = Math.clamp(position / (double)Math.max(1, maximum - 1), 0, 1);
            g.draw(new Arc2D.Double(x, 67, 34, 34, 225, -270 * amount, Arc2D.OPEN));
            g.setColor(Color.WHITE);
            label(g, value, 15, 122, width);
            g.setColor(Color.DARK_GRAY);
            g.fillRect(8, 129, width - 16, 3);
            g.setColor(accent);
            double bar = modulation < 0 ? amount : Math.clamp(modulation / (double)Math.max(1, maximum - 1), 0, 1);
            g.fillRect(8, 129, (int)Math.round((width - 16) * bar), 3);
        }
        if (selected) {
            g.setColor(accent);
            g.fillRect(0, 139, width, 21);
        }
        g.setColor(selected && accent.getRed()*.299 + accent.getGreen()*.587 + accent.getBlue()*.114 > 145 ? Color.BLACK : Color.WHITE);
        label(g, footer, 12, 155, width);
    }

    private static void label(Graphics2D g, String text, int size, int baseline, int width) {
        if (text == null || text.isEmpty()) return;
        g.setFont(FONT.deriveFont((float)size));
        FontMetrics metrics = g.getFontMetrics();
        String visible = text;
        if (metrics.stringWidth(text) > width - 6) {
            int end = text.length();
            do {
                end = text.offsetByCodePoints(end, -1);
                visible = text.substring(0, end) + "…";
            } while (end > 0 && metrics.stringWidth(visible) > width - 6);
        }
        g.drawString(visible, (width - metrics.stringWidth(visible)) / 2, baseline);
    }
}
