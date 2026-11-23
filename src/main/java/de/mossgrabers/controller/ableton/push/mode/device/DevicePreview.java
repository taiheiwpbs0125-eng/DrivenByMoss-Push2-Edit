// SPDX-License-Identifier: LGPL-3.0-only
package de.mossgrabers.controller.ableton.push.mode.device;

import java.awt.*;
import javax.swing.*;

public final class DevicePreview {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Push 2 • Device Mode Demo");
            JLabel status = new JLabel();
            JPanel panel = new JPanel() {
                long since = System.nanoTime(), cost;
                int frames;
                final String[] names = {"音色", "Cutoff", "共鳴", "Drive", "残響 Mix", "Pan", "出力", ""};
                final String[] menus = {"On","Parameters","Expanded","Chains","Banks","Pin Device","Window","Up"};
                protected void paintComponent(Graphics g) {
                    super.paintComponent(g);
                    long start = System.nanoTime();
                    for (int i = 0; i < 8; i++) {
                        int v = (int)(50 + 49 * Math.sin(start / 1e9 + i));
                        DeviceTile tile = new DeviceTile(menus[i], i == 0, i == 0 ? "Phase-4" : i == 1 ? "音色調整" : "",
                            names[i], v + " %", v, -1, 101, i == 0, i == 0 ? "Phase-4" : "", i == 0, 0x9861d4);
                        Graphics2D cell = (Graphics2D)g.create(i*120, 0, 120, 160);
                        try { tile.paint(cell, 120); } finally { cell.dispose(); }
                    }
                    cost += System.nanoTime() - start;
                    frames++;
                    if (start - since >= 1000000000L) {
                        status.setText(String.format("Demo   %.1f FPS    %.2f ms/frame", frames*1e9/(start-since), cost/1e6/frames));
                        since = start; frames = 0; cost = 0;
                    }
                }
            };
            panel.setPreferredSize(new Dimension(960,160));
            frame.add(panel); frame.add(status, BorderLayout.SOUTH);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.pack(); frame.setVisible(true);
            new Timer(33, e -> panel.repaint()).start();
        });
    }
}
