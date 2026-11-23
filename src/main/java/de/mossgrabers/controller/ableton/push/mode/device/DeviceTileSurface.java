// SPDX-License-Identifier: LGPL-3.0-only
package de.mossgrabers.controller.ableton.push.mode.device;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.nio.ByteOrder;
import de.mossgrabers.bitwig.framework.graphics.BitmapImpl;
import de.mossgrabers.bitwig.framework.graphics.ImageImpl;
import de.mossgrabers.framework.daw.IHost;
import de.mossgrabers.framework.graphics.IGraphicsInfo;
import de.mossgrabers.framework.graphics.canvas.component.IComponent;

final class DeviceTileSurface {
    private final BufferedImage pixels;
    private final BitmapImpl bitmap;
    private final ImageImpl image;
    private DeviceTile previous;

    record Column(DeviceTileSurface surface, DeviceTile tile) implements IComponent {
        public void draw(IGraphicsInfo info) {
            surface.draw(tile, info);
        }
    }

    DeviceTileSurface(IHost host, int width) {
        pixels = new BufferedImage(width, 160, BufferedImage.TYPE_INT_ARGB);
        bitmap = (BitmapImpl)host.createBitmap(width, 160);
        image = new ImageImpl(bitmap.bitmap());
    }

    private void draw(DeviceTile tile, IGraphicsInfo info) {
        int width = pixels.getWidth();
        if (!tile.equals(previous)) {
            Graphics2D graphics = pixels.createGraphics();
            try { tile.paint(graphics, width); }
            finally { graphics.dispose(); }
            int[] data = ((DataBufferInt)pixels.getRaster().getDataBuffer()).getData();
            bitmap.encode((buffer, w, h) -> buffer.duplicate().order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(data));
            previous = tile;
        }
        info.getContext().drawImage(image, info.getBounds().left(), info.getBounds().top());
    }
}
