// SPDX-License-Identifier: LGPL-3.0-only
package de.mossgrabers.controller.ableton.push.mode.device;

import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.util.ArrayList;
import java.util.List;
import de.mossgrabers.framework.controller.color.ColorEx;
import de.mossgrabers.framework.daw.IBrowser;
import de.mossgrabers.framework.daw.data.IBrowserColumnItem;
import de.mossgrabers.framework.graphics.*;
import de.mossgrabers.framework.graphics.canvas.component.IComponent;

/** Immutable snapshot. Navigation and selection stay in DeviceBrowserMode. */
record OrangeBrowser(List<String> headings, List<String> filters, List<String> items,
                     int focus, int selected, String title, String context) implements IComponent {
    private static final ColorEx PANEL = ColorEx.fromRGB(36,34,33);
    private static final ColorEx BAND = ColorEx.fromRGB(56,48,44);
    private static final ColorEx INK = ColorEx.fromRGB(249,242,234);
    private static final ColorEx ORANGE = ColorEx.fromRGB(255,132,46);
    private static final List<String> LABELS = List.of("場所","ファイル種類","カテゴリ","タグ","制作者","機器種別","デバイス");
    private static final FontRenderContext METRICS = new FontRenderContext(null,true,true);
    private static final Font FONT = new Font("Meiryo",Font.PLAIN,12);

    static OrangeBrowser read(IBrowser browser, int focus, boolean pluginSource, int nativeKind, boolean previewSupported, List<String> recent, String recentSelection) {
        var headings = new ArrayList<String>();
        var filters = new ArrayList<String>();
        for (int i=0; i<6; i++) {
            boolean available = i < browser.getFilterColumnCount() && browser.getFilterColumn(i).doesExist();
            headings.add(available ? (i+1)+" "+LABELS.get(i)+"で絞る" : "");
            String value = "";
            if (available) {
                var column = browser.getFilterColumn(i);
                value = column.getCursorName();
                if (!column.doesCursorExist() || value == null || value.isBlank() || value.equals(column.getWildcard())) value="すべて";
            }
            filters.add(value);
        }
        headings.add("7 検索結果を選ぶ");
        filters.add(previewSupported ? (browser.isPreviewEnabled() ? "試聴 ON" : "試聴 OFF") : "");
        headings.add("8 最近使った項目");
        filters.add(recent.size()+" 件");
        if (focus == 7)
            return new OrangeBrowser(List.copyOf(headings),List.copyOf(filters),List.copyOf(recent),focus,
                recent.indexOf(recentSelection), recentSelection == null ? "履歴なし" : recentSelection,"");
        String title = browser.getSelectedResult();
        var items = new ArrayList<String>();
        int selected = -1;
        if (nativeKind >= 0) {
            var kinds = List.of("すべて","インストゥルメント","オーディオエフェクト","ノートエフェクト");
            headings.set(1,"2 種類を選ぶ");
            filters.set(1,"デバイス");
            headings.set(5,"6 機器種別で絞る");
            filters.set(5,kinds.get(nativeKind));
            if (focus == 5)
                return new OrangeBrowser(List.copyOf(headings),List.copyOf(filters),kinds,focus,nativeKind,kinds.get(nativeKind),"");
        }
        if (pluginSource) {
            headings.set(4,"5 供給元で絞る");
            var results = new ArrayList<String>();
            for (var result : browser.getResultColumnItems())
                if (result.doesExist()) results.add(result.getName());
            String vendor = PluginVendor.title(results);
            filters.set(4, PluginVendor.ALL_VENDORS.equals(vendor) ? "すべて" : vendor);
            if (focus == 4) {
                return new OrangeBrowser(List.copyOf(headings),List.copyOf(filters),List.copyOf(results),focus,-1,
                    vendor,"");
            }
        }
        {
            int row = nativeKind >= 0 ? 1 : pluginSource ? 2 : 0;
            var choices = List.of("任意の種類","デバイス","プラグイン");
            String label = choices.get(row);
            headings.set(1,"2 種類を選ぶ");
            filters.set(1, row == 0 ? "すべて" : label);
            if (focus == 1)
                return new OrangeBrowser(List.copyOf(headings),List.copyOf(filters),choices,focus,row,label,"");
        }
        if (focus >= 0) {
            IBrowserColumnItem[] entries = focus==6 ? browser.getResultColumnItems() : browser.getFilterColumn(focus).getItems();
            if (focus == 1) entries = FileKinds.visible (browser.getFilterColumn (1));
            if (focus == 0) entries = LocationOrder.visible (browser.getFilterColumn (0));
            for (int i=0; i<Math.min(48,entries.length); i++) {
                var item=entries[i];
                String name=item.doesExist() ? item.getName() : "";
                if (item.isSelected() && !name.isBlank()) { selected=i; title=name; }
                items.add(focus<6 && !name.isBlank() ? name+" ("+item.getHitCount()+")" : name);
            }
        }
        if (focus == 0 && !headings.get(0).isBlank()) headings.set(0,"Shift↑子 ↓親");
        boolean empty = focus>=0 && items.stream().allMatch(String::isBlank);
        if (empty) title="候補なし";
        if (focus == 1 && !empty && java.util.Arrays.stream(FileKinds.visible(browser.getFilterColumn(1)))
                .filter(IBrowserColumnItem::doesExist).allMatch(item -> item.getName().equals(browser.getFilterColumn(1).getWildcard())))
            title="形式指定なし　プラグインはノブ1「場所」へ";
        String context=String.join("  ",java.util.Objects.toString(browser.getInfoText(),""),java.util.Objects.toString(browser.getSelectedContentType(),"")).strip();
        return new OrangeBrowser(List.copyOf(headings),List.copyOf(filters),List.copyOf(items),focus,selected,
            title==null || title.isBlank() ? "未選択" : title,context);
    }

    OrangeBrowser scrollTitle(long elapsedMillis) {
        if (focus >= 0 && focus < 6 || FONT.deriveFont(16f).getStringBounds(title,METRICS).getWidth() <= 952)
            return this;
        int end = title.length();
        while (end > 0) {
            int prev = title.offsetByCodePoints(end,-1);
            if (FONT.deriveFont(16f).getStringBounds(title.substring(prev),METRICS).getWidth() > 952) break;
            end = prev;
        }
        int steps = title.codePointCount(0,end);
        long phase = elapsedMillis % (1800L + 160L * steps);
        int offset = Math.clamp((phase - 900) / 160, 0, steps);
        if (offset == 0) return this;
        return new OrangeBrowser(headings,filters,items,focus,selected,title.substring(title.offsetByCodePoints(0,offset)),context);
    }

    public void draw(IGraphicsInfo info) {
        var gc=info.getContext();
        double x=info.getBounds().left(), w=info.getBounds().width(), col=w/8;
        gc.fillRectangle(x,0,w,160,PANEL);
        gc.fillRectangle(x,0,w,17,BAND);
        gc.fillRectangle(x,143,w,17,BAND);
        for(int i=0;i<8;i++) {
            double left=x+i*col;
            if(i==focus) {
                gc.fillRectangle(left,0,col-1,17,ORANGE);
                gc.fillRectangle(left,143,col-1,17,ORANGE);
            }
            label(gc,headings.get(i),left,0,col,17,11,i==focus?PANEL:INK);
            label(gc,filters.get(i),left,143,col,17,11,i==focus?PANEL:INK);
        }
        label(gc,title,x,17,w,24,16,ORANGE);
        if(focus<0) {
            label(gc,context,x,52,w,25,14,INK);
            return;
        }
        for(int i=0;i<items.size();i++) {
            double left=x+(i/6)*col, top=41+(i%6)*17;
            if(i==selected) gc.fillRectangle(left,top,col-1,17,ORANGE);
            label(gc,items.get(i),left,top,col,17,12,i==selected?PANEL:INK);
        }
    }

    private static void label(IGraphicsContext gc,String value,double x,double y,double width,double height,int size,ColorEx ink) {
        if(value.isBlank()) return;
        Font font=FONT.deriveFont((float)size);
        double limit=width-8;
        String visible=value;
        if(font.getStringBounds(visible,METRICS).getWidth()>limit) {
            int end=value.length();
            do { end=value.offsetByCodePoints(end,-1); visible=value.substring(0,end)+"…"; }
            while(end>0 && font.getStringBounds(visible,METRICS).getWidth()>limit);
        }
        gc.drawTextInBounds(visible,x+4,y,limit,height,Align.LEFT,ink,size);
    }
}
