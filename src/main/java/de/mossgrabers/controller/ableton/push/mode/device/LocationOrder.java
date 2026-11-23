// SPDX-License-Identifier: LGPL-3.0-only
package de.mossgrabers.controller.ableton.push.mode.device;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Locale;
import de.mossgrabers.framework.daw.data.IBrowserColumn;
import de.mossgrabers.framework.daw.data.IBrowserColumnItem;

final class LocationOrder {
    private LocationOrder() {}

    private static int priority(String name, String wildcard) {
        if (name.equals(wildcard)) return 0;
        String key = name.toLowerCase(Locale.ROOT).replace("-", "").replace(" ", "");
        if (key.equals("plugins") || key.equals("プラグイン")) return 1;
        if (key.startsWith("vst") || key.equals("clap")) return 2;
        return 3;
    }

    static IBrowserColumnItem[] visible(IBrowserColumn column) {
        var items = column.getItems();
        // Keep native navigation across page boundaries in long folders.
        if (items.length == 0 || items[items.length - 1].doesExist()) return items;
        var present = Arrays.stream(items).filter(IBrowserColumnItem::doesExist).toArray(IBrowserColumnItem[]::new);
        if (Arrays.stream(present).noneMatch(i -> {
            int rank = priority(i.getName(), column.getWildcard());
            return rank == 1 || rank == 2;
        })) return items;
        Arrays.sort(present, Comparator.comparingInt(i -> priority(i.getName(), column.getWildcard())));
        return present;
    }

    static boolean step(IBrowserColumn column, int delta) {
        var items = visible(column);
        if (items == column.getItems()) return false;
        for (int i=0; i<items.length; i++) {
            if (items[i].isSelected()) {
                items[Math.clamp(i+delta,0,items.length-1)].select();
                return true;
            }
        }
        return false;
    }
}
