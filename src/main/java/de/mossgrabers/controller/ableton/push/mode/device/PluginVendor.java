// SPDX-License-Identifier: LGPL-3.0-only
package de.mossgrabers.controller.ableton.push.mode.device;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** The popup API exposes results but no vendor column. Resolve their VST class names locally. */
final class PluginVendor {
    static final String ALL_VENDORS = "すべての供給元";
    private static final Map<String,String> VENDORS = load();

    static String title(List<String> results) {
        String vendor = null;
        for (String name : results) {
            String next = VENDORS.get(name);
            if (next == null || next.isEmpty()) continue;
            if (vendor != null && !vendor.equals(next)) return ALL_VENDORS;
            vendor = next;
        }
        return vendor == null ? "供給元名を取得できません" : vendor;
    }

    private static Map<String,String> load() {
        Map<String,String> vendors = new HashMap<>();
        String local = System.getenv("LOCALAPPDATA");
        if (local == null) return vendors;
        Path cache = Path.of(local,"Bitwig Studio","cache");
        for (String filename : List.of("vst-metadata-27","vst3-metadata-11")) {
            try {
                Path path = cache.resolve(filename);
                if (Files.size(path) <= 16 * 1024 * 1024)
                    read(ByteBuffer.wrap(Files.readAllBytes(path)),vendors);
            } catch (IOException | SecurityException ignored) {
                // A plug-in format may have no installed devices.
            }
        }
        return vendors;
    }

    private static void read(ByteBuffer data, Map<String,String> vendors) {
        String name = null;
        for (int i=0; i<=data.limit()-9; i++) {
            int field = data.getInt(i);
            if ((field != 996 && field != 998 && field != 1003) || data.get(i+4) != 8) continue;
            int length = data.getInt(i+5);
            if (length <= 0 || length > 4096 || length > data.limit()-i-9) { name=null; continue; }
            String value = new String(data.array(),i+9,length,StandardCharsets.UTF_8);
            if (field == 996) name = value;
            else {
                if (field == 998 && name != null)
                    vendors.merge(name,value,(a,b) -> a.equals(b) ? a : "");
                name = null;
            }
            i += 8 + length;
        }
    }
}
