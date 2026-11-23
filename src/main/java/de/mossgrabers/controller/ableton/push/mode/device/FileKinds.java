// SPDX-License-Identifier: LGPL-3.0-only
package de.mossgrabers.controller.ableton.push.mode.device;

import java.util.Arrays;
import java.util.stream.Collectors;
import de.mossgrabers.framework.daw.data.IBrowserColumn;
import de.mossgrabers.framework.daw.data.IBrowserColumnItem;

final class FileKinds {
    private FileKinds() {}

    static String names(IBrowserColumnItem[] items) {
        return Arrays.stream(items).filter(IBrowserColumnItem::doesExist)
            .map(IBrowserColumnItem::getName).collect(Collectors.joining(", "));
    }

    private static boolean hasChoice(IBrowserColumnItem[] items, String wildcard) {
        return Arrays.stream(items).anyMatch(item -> item.doesExist()
            && !item.getName().isBlank() && !item.getName().equals(wildcard));
    }

    static IBrowserColumnItem[] visible(IBrowserColumn column) {
        var siblings = column.getItems();
        var entries = column.getColumnEntries();
        if (entries == null || hasChoice(siblings,column.getWildcard()) || !hasChoice(entries,column.getWildcard()))
            return siblings;
        return entries;
    }

    static boolean step(IBrowserColumn column,int delta) {
        var items = visible(column);
        if (items == column.getItems()) return false;
        var candidates = Arrays.stream(items).filter(IBrowserColumnItem::doesExist).toList();
        if (candidates.isEmpty() || delta == 0) return true;
        int selected = -1;
        for(int i=0;i<candidates.size();i++)
            if(candidates.get(i).isSelected()) { selected=i; break; }
        candidates.get(Math.clamp(selected+delta,0,candidates.size()-1)).select();
        return true;
    }
}
