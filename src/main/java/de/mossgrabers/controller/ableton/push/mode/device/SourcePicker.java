// SPDX-License-Identifier: LGPL-3.0-only
package de.mossgrabers.controller.ableton.push.mode.device;

import de.mossgrabers.framework.daw.IBrowser;
import de.mossgrabers.framework.daw.IHost;
import de.mossgrabers.framework.daw.data.IBrowserColumn;

/** Scan the location bank independently of the navigation cursor. */
final class SourcePicker {
    private int generation;
    private Boolean pending;

    static boolean matches(String name, boolean plugins) {
        return plugins ? "Plug-ins".equalsIgnoreCase(name) || "プラグイン".equals(name)
            : "Bitwig Studio".equalsIgnoreCase(name);
    }

    void cancel() { generation++; pending=null; }

    void select(IBrowser browser, IHost host, boolean plugins) {
        var column = browser.getFilterColumn(0);
        if (Boolean.valueOf(plugins).equals(pending)) return;
        cancel();
        if (matches(column.getCursorName(),plugins)) return;
        pending=plugins;
        column.scrollEntriesToStart();
        int request=generation;
        host.scheduleTask(() -> scan(browser,host,column,plugins,request,-1,0),100);
    }

    private void scan(IBrowser browser,IHost host,IBrowserColumn column,boolean plugins,int request,int previous,int retries) {
        if(request!=generation) return;
        if(!browser.isActive()) { cancel(); return; }
        int page=column.entriesPosition();
        if(page!=previous) {
            for(var item:column.getColumnEntries()) {
                if(item.doesExist() && matches(item.getName(),plugins)) {
                    item.select();
                    pending=null;
                    return;
                }
            }
            if(column.nextEntriesPage()) {
                host.scheduleTask(() -> scan(browser,host,column,plugins,request,page,0),100);
                return;
            }
        } else if(retries<8) {
            host.scheduleTask(() -> scan(browser,host,column,plugins,request,previous,retries+1),100);
            return;
        }
        pending=null;
        host.println("Push2: requested browser location unavailable");
    }
}
