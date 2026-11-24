// SPDX-License-Identifier: LGPL-3.0-only
package de.mossgrabers.framework.graphics.canvas.component;

import de.mossgrabers.framework.controller.color.ColorEx;
import de.mossgrabers.framework.graphics.Align;
import de.mossgrabers.framework.graphics.IGraphicsInfo;

/** Push 2 empty clip page and transient User mode banner. */
public enum Push2ModeNotice implements IComponent
{
    CLIP, NOTE_CLIP, USER;

    @Override
    public void draw (final IGraphicsInfo info)
    {
        final var gc = info.getContext ();
        final double left = info.getBounds ().left ();
        final double width = info.getBounds ().width ();
        if (this == USER)
        {
            gc.fillRectangle (left, 0, width, 20, OrangePanel.BAND);
            gc.fillRectangle (left, 0, 3, 20, OrangePanel.ACCENT);
            gc.drawTextInBounds ("User   /   プロジェクト・トラックのリモートコントロール", left + 12, 0, width - 24, 20, Align.LEFT, ColorEx.WHITE, 12);
            return;
        }

        gc.fillRectangle (left, 0, width, 160, OrangePanel.BASE);
        gc.fillRectangle (left, 0, width, 17, OrangePanel.BAND);
        gc.drawTextInBounds (this == NOTE_CLIP ? "Clip   /   ピアノロール" : "Clip   /   クリップ設定", left + 12, 0, width - 24, 17, Align.LEFT, ColorEx.WHITE, 11);
        gc.fillRectangle (left + 16, 61, 3, 32, OrangePanel.ACCENT);
        gc.drawTextInBounds (this == NOTE_CLIP ? "ノートクリップを選択してください" : "クリップを選択してください", left + 32, 61, width - 48, 32, Align.LEFT, ColorEx.WHITE, 16);
        gc.fillRectangle (left, 143, width, 17, OrangePanel.BAND);
        gc.drawTextInBounds ("Clip ボタンで表示を切り替え", left + 12, 143, width - 24, 17, Align.LEFT, ColorEx.WHITE, 11);
    }
}
