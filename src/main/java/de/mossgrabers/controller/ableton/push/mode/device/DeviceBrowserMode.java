// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2025
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.ableton.push.mode.device;

import de.mossgrabers.controller.ableton.push.command.continuous.IPush3Encoder;
import de.mossgrabers.controller.ableton.push.controller.Push1Display;
import de.mossgrabers.controller.ableton.push.controller.PushColorManager;
import de.mossgrabers.controller.ableton.push.controller.PushControlSurface;
import de.mossgrabers.controller.ableton.push.mode.BaseMode;
import de.mossgrabers.framework.controller.ButtonID;
import de.mossgrabers.framework.controller.color.ColorEx;
import de.mossgrabers.framework.controller.display.IGraphicDisplay;
import de.mossgrabers.framework.controller.display.ITextDisplay;
import de.mossgrabers.framework.daw.IBrowser;
import de.mossgrabers.framework.daw.IModel;
import de.mossgrabers.framework.daw.constants.Capability;
import de.mossgrabers.framework.daw.data.IBrowserColumn;
import de.mossgrabers.framework.daw.data.IBrowserColumnItem;
import de.mossgrabers.framework.daw.data.IItem;
import de.mossgrabers.framework.featuregroup.AbstractFeatureGroup;
import de.mossgrabers.framework.featuregroup.AbstractMode;
import de.mossgrabers.framework.featuregroup.ViewManager;
import de.mossgrabers.framework.utils.ButtonEvent;
import de.mossgrabers.framework.utils.StringUtils;
import de.mossgrabers.framework.view.Views;
import de.mossgrabers.framework.view.sequencer.AbstractDrumView;

import java.util.Optional;


/**
 * Mode for navigating the browser.
 *
 * @author Jürgen Moßgraber
 */
public class DeviceBrowserMode extends BaseMode<IItem> implements IPush3Encoder, de.mossgrabers.framework.command.trigger.BrowserCommand.SelectionGuard
{
    private static final int SELECTION_OFF    = 0;
    private static final int SELECTION_PRESET = 1;
    private static final int SELECTION_FILTER = 2;

    private final SourcePicker sourcePicker = new SourcePicker ();
    private final java.util.List<String> recent = new java.util.ArrayList<> ();
    private String recentSelection;
    private boolean browsingRecent;
    private String lastTitle = "";
    private long titleSince;
    private boolean          pluginSource;
    private int              nativeKind = -1;
    private int              sourceRevision;
    private boolean          firstTypeTurn;
    private int              selectionMode;
    private int              filterColumn;


    /**
     * Constructor.
     *
     * @param surface The control surface
     * @param model The model
     */
    public DeviceBrowserMode (final PushControlSurface surface, final IModel model)
    {
        super ("Browser", surface, model);

        this.selectionMode = SELECTION_OFF;
        this.filterColumn = -1;
    }


    /** {@inheritDoc} */
    @Override
    public void onDeactivate ()
    {
        this.browsingRecent = false;
        this.recentSelection = null;
        this.sourcePicker.cancel ();
        this.pluginSource = false;
        this.nativeKind = -1;
        this.sourceRevision++;
        super.onDeactivate ();

        this.model.getBrowser ().stopBrowsing (true);
        this.selectionMode = SELECTION_OFF;
        this.filterColumn = -1;
    }


    /**
     * Change the value of the last selected column.
     *
     * @param value The change value
     */
    public void changeSelectedColumnValue (final int value)
    {
        final int index = this.filterColumn == -1 ? (this.isPush2Browser () ? 6 : 7) : this.filterColumn;
        this.changeValue (index, value);
    }


    /**
     * Set the last selected column to the selection column.
     */
    public void resetFilterColumn ()
    {
        this.filterColumn = -1;
    }


    /** {@inheritDoc} */
    @Override
    public void onKnobValue (final int index, final int value)
    {
        if (!this.isKnobTouched (index))
            return;

        if ((index == 1 || index == 5 && this.nativeKind >= 0) && this.firstTypeTurn)
        {
            this.firstTypeTurn = false;
            this.changeValue (index, value);
        }
        else if (this.increaseKnobMovement ())
            this.changeValue (index, value);
    }


    /** {@inheritDoc} */
    @Override
    public void onKnobTouch (final int index, final boolean isTouched)
    {
        // Make sure that only 1 knob gets changed in browse mode to prevent weird behavior
        if (this.isAnyKnobTouched () && !this.isKnobTouched (index))
            return;
        this.setTouchedKnob (index, isTouched);

        if (isTouched && this.isPush2Browser ())
        {
            this.browsingRecent = index == 7;
            if (index >= 6)
            {
                this.filterColumn = index;
                this.selectionMode = SELECTION_PRESET;
                if (this.browsingRecent)
                    this.chooseRecent (0);
                return;
            }
        }

        if (isTouched && index == 5 && this.nativeKind >= 0)
        {
            this.firstTypeTurn = true;
            this.filterColumn = 5;
            this.selectionMode = SELECTION_FILTER;
            if (this.surface.isDeletePressed ())
            {
                this.surface.setTriggerConsumed (ButtonID.DELETE);
                this.selectNativeKind (0);
            }
            return;
        }

        if (isTouched && index == 4 && this.pluginSource)
        {
            this.filterColumn = 4;
            this.selectionMode = SELECTION_FILTER;
            this.model.getApplication ().invokeAction ("focus_vendor_column");
            if (this.surface.isDeletePressed ())
            {
                this.surface.setTriggerConsumed (ButtonID.DELETE);
                this.model.getApplication ().invokeAction ("clear_focused_filter");
            }
            return;
        }

        Optional<IBrowserColumn> fc;
        if (isTouched)
        {
            if (this.surface.isDeletePressed ())
            {
                this.surface.setTriggerConsumed (ButtonID.DELETE);
                fc = this.getFilterColumn (index);
                if (fc.isPresent () && fc.get ().doesExist ())
                    this.model.getBrowser ().resetFilterColumn (fc.get ().getIndex ());
                return;
            }
        }
        else
        {
            this.selectionMode = DeviceBrowserMode.SELECTION_OFF;
            return;
        }

        if (index == 1 && this.isPush2Browser ())
        {
            this.firstTypeTurn = true;
            this.filterColumn = 1;
            this.selectionMode = SELECTION_FILTER;
            return;
        }
        if (index == 7)
        {
            this.selectionMode = DeviceBrowserMode.SELECTION_PRESET;
            this.filterColumn = -1;
        }
        else
        {
            fc = this.getFilterColumn (index);
            if (fc.isPresent () && fc.get ().doesExist ())
            {
                this.selectionMode = DeviceBrowserMode.SELECTION_FILTER;
                this.filterColumn = fc.get ().getIndex ();
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    public void onFirstRow (final int index, final ButtonEvent event)
    {
        if (event != ButtonEvent.DOWN)
            return;
        if (index == 0 && this.surface.isShiftPressed () && this.isPush2Browser ())
        {
            this.getFilterColumn (0).filter (IBrowserColumn::doesExist).ifPresent (column -> {
                column.selectFirstChild ();
                this.filterColumn = 0;
                this.selectionMode = SELECTION_FILTER;
            });
            return;
        }


        if (this.surface.isShiftPressed () && index == (this.isPush2Browser () ? 6 : 7))
        {
            this.model.getBrowser ().togglePreviewEnabled ();
            return;
        }

        if (this.isPushModern)
            this.selectNext (index, 1);
        else
            this.selectPrevious (index, 1);
    }


    /** {@inheritDoc} */
    @Override
    public void onSecondRow (final int index, final ButtonEvent event)
    {
        if (event != ButtonEvent.DOWN)
            return;
        if (index == 0 && this.surface.isShiftPressed () && this.isPush2Browser ())
        {
            this.getFilterColumn (0).filter (IBrowserColumn::doesExist).ifPresent (column -> {
                column.selectParent ();
                this.filterColumn = 0;
                this.selectionMode = SELECTION_FILTER;
            });
            return;
        }

        if (this.isPushModern)
            this.selectPrevious (index, 1);
        else
            this.selectNext (index, 1);
    }


    /** {@inheritDoc} */
    @Override
    public void updateDisplay1 (final ITextDisplay display)
    {
        final IBrowser browser = this.model.getBrowser ();
        if (!browser.isActive ())
            return;

        switch (this.selectionMode)
        {
            case DeviceBrowserMode.SELECTION_OFF:
                String selectedContentType = browser.getSelectedContentType ();
                if (this.filterColumn == -1)
                    selectedContentType = Push1Display.SELECT_ARROW + selectedContentType;
                display.setCell (0, 7, selectedContentType);

                display.setBlock (2, 2, "Selection:");
                String selectedResult = browser.getSelectedResult ();
                selectedResult = selectedResult == null || selectedResult.isBlank () ? "None" : StringUtils.shortenAndFixASCII (selectedResult, 18);

                String [] infoText = browser.getInfoText ().split (":");
                infoText = new String []
                {
                    infoText[0] + ":",
                    infoText.length == 1 ? "" : infoText[1].trim ()
                };

                String info1 = infoText[0].length () > 17 ? infoText[0].substring (0, 17) : infoText[0];
                String info2 = infoText[0].length () > 17 ? infoText[0].substring (17, infoText[0].length ()) : "";
                display.setBlock (2, 0, info1).setBlock (2, 1, info2);

                info1 = infoText[1].length () > 17 ? infoText[1].substring (0, 17) : infoText[1];
                info2 = infoText[1].length () > 17 ? infoText[1].substring (17, infoText[1].length ()) : "";
                display.setBlock (3, 0, info1).setBlock (3, 1, info2);

                final String sel1 = selectedResult.length () > 17 ? selectedResult.substring (0, 17) : selectedResult;
                final String sel2 = selectedResult.length () > 17 ? selectedResult.substring (17, selectedResult.length ()) : "";
                display.setBlock (3, 2, sel1).setCell (3, 6, sel2);

                if (this.model.getHost ().supports (Capability.HAS_BROWSER_PREVIEW))
                {
                    display.setCell (2, 7, "Preview:");
                    display.setCell (3, 7, browser.isPreviewEnabled () ? "On" : "Off");
                }

                for (int i = 0; i < 7; i++)
                {
                    final Optional<IBrowserColumn> column = this.getFilterColumn (i);
                    String name = column.isEmpty () ? "" : StringUtils.shortenAndFixASCII (column.get ().getName (), 8);
                    if (i == this.filterColumn)
                        name = Push1Display.SELECT_ARROW + name;
                    display.setCell (0, i, name).setCell (1, i, getColumnName (column));
                }
                break;

            case DeviceBrowserMode.SELECTION_PRESET:
                final IBrowserColumnItem [] results = browser.getResultColumnItems ();

                if (!results[0].doesExist ())
                {
                    display.clear ().setBlock (1, 1, "       No results").setBlock (1, 2, "available...").allDone ();
                    return;
                }

                for (int i = 0; i < 16; i++)
                {
                    if (i < results.length)
                        display.setBlock (i % 4, i / 4, (results[i].isSelected () ? Push1Display.SELECT_ARROW : " ") + StringUtils.shortenAndFixASCII (results[i].getName (), 16));
                    else
                        display.setBlock (i % 4, i / 4, "");
                }
                break;

            case DeviceBrowserMode.SELECTION_FILTER:
                final IBrowserColumnItem [] items = browser.getFilterColumn (this.filterColumn).getItems ();
                for (int i = 0; i < 16; i++)
                {
                    String text = (items[i].isSelected () ? Push1Display.SELECT_ARROW : " ") + StringUtils.shortenAndFixASCII (items[i].getName (), 18) + "                ";
                    if (!items[i].getName ().isEmpty ())
                    {
                        final String hitStr = "(" + items[i].getHitCount () + ")";
                        text = text.substring (0, 17 - hitStr.length ()) + hitStr;
                    }
                    display.setBlock (i % 4, i / 4, text);
                }
                break;

            default:
                // Not used
                break;
        }
    }


    /** {@inheritDoc} */
    @Override
    public void updateDisplay2 (final IGraphicDisplay display)
    {
        final IBrowser browser = this.model.getBrowser ();
        if (!browser.isActive ())
            return;
        if (this.surface.getConfiguration ().getPushVersion () == de.mossgrabers.controller.ableton.push.PushVersion.VERSION_2)
        {
            int focus = this.selectionMode == SELECTION_OFF ? -1 : this.filterColumn;
            OrangeBrowser frame = OrangeBrowser.read (browser, focus, this.pluginSource, this.nativeKind,
                this.model.getHost ().supports (Capability.HAS_BROWSER_PREVIEW), this.recent, this.recentSelection);
            if (!frame.title ().equals (this.lastTitle))
            {
                this.lastTitle = frame.title ();
                this.titleSince = System.nanoTime ();
            }
            display.addElement (frame.scrollTitle ((System.nanoTime () - this.titleSince) / 1_000_000));
            return;
        }


        switch (this.selectionMode)
        {
            case DeviceBrowserMode.SELECTION_OFF:
                String selectedResult = browser.getSelectedResult ();
                selectedResult = selectedResult == null || selectedResult.isBlank () ? "Selection: None" : "Selection: " + selectedResult;
                for (int i = 0; i < 7; i++)
                {
                    final Optional<IBrowserColumn> column = this.getFilterColumn (i);
                    final String headerTopName = i == 0 ? browser.getInfoText () : "";
                    final String headerBottomName = i == 0 ? selectedResult : "";
                    final String menuBottomName = getColumnName (column);
                    display.addOptionElement (headerTopName, column.isEmpty () ? "" : column.get ().getName (), i == this.filterColumn, headerBottomName, menuBottomName, !menuBottomName.equals (" "), false);
                }

                final boolean supportsPreview = this.model.getHost ().supports (Capability.HAS_BROWSER_PREVIEW);
                final String bottomMenu = supportsPreview ? "Preview" : "";
                final ColorEx menuBottomColor = browser.isPreviewEnabled () ? ColorEx.ORANGE : ColorEx.GRAY;
                display.addOptionElement ("", browser.getSelectedContentType (), this.filterColumn == -1, null, "", bottomMenu, false, supportsPreview ? menuBottomColor : null, false);
                break;

            case DeviceBrowserMode.SELECTION_PRESET:
                final IBrowserColumnItem [] results = browser.getResultColumnItems ();

                if (!results[0].doesExist ())
                {
                    for (int i = 0; i < 8; i++)
                        display.addOptionElement (i == 3 ? "No results available..." : "", "", false, "", "", false, false);
                    return;
                }

                for (int i = 0; i < 8; i++)
                {
                    final String [] items = new String [6];
                    final boolean [] selected = new boolean [6];
                    for (int item = 0; item < 6; item++)
                    {
                        final int pos = i * 6 + item;
                        items[item] = pos < results.length ? results[pos].getName (14) : "";
                        selected[item] = pos < results.length && results[pos].isSelected ();
                    }
                    display.addListElement (items, selected);
                }
                break;

            case DeviceBrowserMode.SELECTION_FILTER:
                final IBrowserColumnItem [] item = browser.getFilterColumn (this.filterColumn).getItems ();
                for (int i = 0; i < 8; i++)
                {
                    final String [] items = new String [6];
                    final boolean [] selected = new boolean [6];
                    for (int itemIndex = 0; itemIndex < 6; itemIndex++)
                    {
                        final int pos = i * 6 + itemIndex;
                        final String hitText = " (" + item[pos].getHitCount () + ")";
                        String text = item[pos].getName (12 - hitText.length ());
                        if (!text.isEmpty ())
                            text = text + hitText;
                        items[itemIndex] = text;
                        selected[itemIndex] = item[pos].isSelected ();
                    }
                    display.addListElement (items, selected);
                }
                break;

            default:
                // Not used
                break;
        }
    }


    /** {@inheritDoc} */
    @Override
    public String getButtonColorID (final ButtonID buttonID)
    {
        int index = this.isButtonRow (0, buttonID);
        if (index >= 0)
        {
            if (index == 7)
            {
                if (this.surface.isShiftPressed () && this.model.getHost ().supports (Capability.HAS_BROWSER_PREVIEW))
                    return this.model.getBrowser ().isPreviewEnabled () ? PushColorManager.PUSH_ORANGE_HI : PushColorManager.PUSH_ORANGE_LO;
                return AbstractFeatureGroup.BUTTON_COLOR_ON;
            }
            final Optional<IBrowserColumn> col = this.getFilterColumn (index);
            return col.isPresent () && col.get ().doesExist () ? AbstractFeatureGroup.BUTTON_COLOR_ON : AbstractFeatureGroup.BUTTON_COLOR_OFF;
        }

        index = this.isButtonRow (1, buttonID);
        if (index >= 0)
        {
            if (index == 7)
                return AbstractMode.BUTTON_COLOR2_ON;
            final Optional<IBrowserColumn> col = this.getFilterColumn (index);
            return col.isPresent () && col.get ().doesExist () ? AbstractMode.BUTTON_COLOR2_ON : AbstractFeatureGroup.BUTTON_COLOR_OFF;
        }

        return AbstractFeatureGroup.BUTTON_COLOR_OFF;
    }


    /** {@inheritDoc} */
    @Override
    public void selectPreviousItemPage ()
    {
        this.resetFilterColumn ();
        this.model.getBrowser ().previousContentType ();
    }


    /** {@inheritDoc} */
    @Override
    public void selectNextItemPage ()
    {
        this.resetFilterColumn ();
        this.model.getBrowser ().nextContentType ();
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasPreviousItem ()
    {
        return this.model.getBrowser ().hasPreviousContentType ();
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasNextItem ()
    {
        return this.model.getBrowser ().hasNextContentType ();
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasPreviousItemPage ()
    {
        return this.hasPreviousItem ();
    }


    /** {@inheritDoc} */
    @Override
    public boolean hasNextItemPage ()
    {
        return this.hasNextItem ();
    }


    private Optional<IBrowserColumn> getFilterColumn (final int index)
    {
        final IBrowser browser = this.model.getBrowser ();
        int column = -1;
        for (int i = 0; i < browser.getFilterColumnCount (); i++)
        {
            column++;
            if (column == index)
                return Optional.of (browser.getFilterColumn (i));
        }
        return Optional.empty ();
    }


    private void chooseRecent (final int delta)
    {
        if (this.recent.isEmpty ()) return;
        int current = this.recent.indexOf (this.recentSelection);
        int next = Math.clamp (current < 0 ? 0 : current + delta, 0, this.recent.size () - 1);
        this.recentSelection = this.recent.get (next);
        var item = this.uniqueRecentMatch ();
        if (item != null) item.select ();
    }

    private IBrowserColumnItem uniqueRecentMatch ()
    {
        IBrowserColumnItem match = null;
        for (var item : this.model.getBrowser ().getResultColumnItems ())
        {
            if (!item.doesExist () || !item.getName ().equals (this.recentSelection)) continue;
            if (match != null) return null;
            match = item;
        }
        return match;
    }

    @Override
    public boolean prepareBrowserCommit ()
    {
        if (!this.isPush2Browser ()) return true;
        var browser = this.model.getBrowser ();
        String name = browser.getSelectedResult ();
        if (this.browsingRecent)
        {
            var item = this.uniqueRecentMatch ();
            if (item == null || !item.isSelected () || !item.getName ().equals (name))
            {
                this.surface.getDisplay ().notify ("履歴の候補を特定できません。ノブ7で選択してください");
                return false;
            }
        }
        if (name != null && !name.isBlank ())
        {
            this.recent.remove (name);
            this.recent.addFirst (name);
            if (this.recent.size () > 24) this.recent.removeLast ();
        }
        return true;
    }

    private boolean isPush2Browser ()
    {
        return this.surface.getConfiguration ().getPushVersion () == de.mossgrabers.controller.ableton.push.PushVersion.VERSION_2;
    }

    private void selectNext (final int index, final int count)
    {
        final IBrowser browser = this.model.getBrowser ();
        if (this.isPush2Browser () && index >= 6)
        {
            this.filterColumn = index;
            this.browsingRecent = index == 7;
            if (this.browsingRecent)
                this.chooseRecent (count);
            else
                for (int i = 0; i < count; i++)
                    browser.selectNextResult ();
            return;
        }
        if (index < 7)
        {
            final Optional<IBrowserColumn> fc = this.getFilterColumn (index);
            if (fc.isPresent () && fc.get ().doesExist ())
            {
                final int fi = fc.get ().getIndex ();
                if (fi < 0)
                    return;
                this.filterColumn = fi;
                if (fi == 1 && this.isPush2Browser () && FileKinds.step (fc.get (), count))
                    return;
                if (fi == 0 && this.isPush2Browser () && LocationOrder.step (fc.get (), count))
                    return;
                for (int i = 0; i < count; i++)
                    browser.selectNextFilterItem (this.filterColumn);
            }
        }
        else
        {
            for (int i = 0; i < count; i++)
                browser.selectNextResult ();
        }
    }


    private void selectPrevious (final int index, final int count)
    {
        final IBrowser browser = this.model.getBrowser ();
        if (this.isPush2Browser () && index >= 6)
        {
            this.filterColumn = index;
            this.browsingRecent = index == 7;
            if (this.browsingRecent)
                this.chooseRecent (-count);
            else
                for (int i = 0; i < count; i++)
                    browser.selectPreviousResult ();
            return;
        }
        if (index < 7)
        {
            final Optional<IBrowserColumn> fc = this.getFilterColumn (index);
            if (fc.isPresent () && fc.get ().doesExist ())
            {
                final int fi = fc.get ().getIndex ();
                if (fi < 0)
                    return;
                this.filterColumn = fi;
                if (fi == 1 && this.isPush2Browser () && FileKinds.step (fc.get (), -count))
                    return;
                if (fi == 0 && this.isPush2Browser () && LocationOrder.step (fc.get (), -count))
                    return;
                for (int j = 0; j < count; j++)
                    browser.selectPreviousFilterItem (this.filterColumn);
            }
        }
        else
        {
            for (int j = 0; j < count; j++)
                browser.selectPreviousResult ();
        }
    }


    private void selectNativeKind (final int kind)
    {
        final var browser = this.model.getBrowser ();
        for (var item : browser.getFilterColumn (5).getItems ())
        {
            if (item.doesExist () && item.getIndex () == kind)
            {
                item.select ();
                this.nativeKind = kind;
                return;
            }
        }
    }

    private void changeValue (final int index, final int value)
    {
        if (index < 7 && this.isPush2Browser ()) this.browsingRecent = false;
        int speed = this.model.getValueChanger ().calcSteppedKnobChange (value);
        if (index == 5 && this.nativeKind >= 0)
        {
            final int next = Math.clamp (this.nativeKind + Integer.signum (speed), 0, 3);
            if (next != this.nativeKind)
                this.selectNativeKind (next);
            return;
        }
        if (index == 4 && this.pluginSource && this.model.getBrowser ().isActive ())
        {
            if (speed > 0)
                this.model.getApplication ().arrowKeyDown ();
            else if (speed < 0)
                this.model.getApplication ().arrowKeyUp ();
            return;
        }
        if (index == 1 && this.isPush2Browser ())
        {
            final int current = this.pluginSource ? 2 : this.nativeKind >= 0 ? 1 : 0;
            final int next = Math.clamp (current + Integer.signum (speed), 0, 2);
            if (next == current && next != 0)
                return;
            final int revision = ++this.sourceRevision;
            this.pluginSource = next == 2;
            this.nativeKind = next == 1 ? 0 : -1;
            this.model.getApplication ().invokeAction (this.pluginSource ? "select_quick_source4" : "select_everything");
            if (!this.pluginSource)
                this.model.getHost ().scheduleTask (() -> {
                    if (revision != this.sourceRevision || !this.model.getBrowser ().isActive ())
                        return;
                    this.model.getApplication ().invokeAction ("focus_file_kind_or_type_column");
                    this.model.getApplication ().invokeAction ("clear_focused_filter");
                    if (next == 1)
                        this.model.getApplication ().arrowKeyDown ();
                    this.model.getBrowser ().resetFilterColumn (5);
                }, 150);
            this.filterColumn = 1;
            return;
        }
        final boolean direction = speed > 0;
        if (this.surface.isShiftPressed ())
            speed = speed * 4;

        speed = Math.abs (speed);
        if (direction)
            this.selectNext (index, speed);
        else
            this.selectPrevious (index, speed);
    }


    private static String getColumnName (final Optional<IBrowserColumn> column)
    {
        if (column.isEmpty () || !column.get ().doesCursorExist ())
            return "";
        final IBrowserColumn browserColumn = column.get ();
        return browserColumn.getCursorName ().equals (browserColumn.getWildcard ()) ? " " : browserColumn.getCursorName (12);
    }


    /** {@inheritDoc} */
    @Override
    public void encoderTurn (final int value)
    {
        this.changeSelectedColumnValue (value);
    }


    /** {@inheritDoc} */
    @Override
    public void encoderPress (final ButtonEvent event)
    {
        if (event != ButtonEvent.DOWN)
            return;

        final boolean commit = !this.surface.isShiftPressed ();

        if (commit && !this.prepareBrowserCommit ()) return;
        this.model.getBrowser ().stopBrowsing (commit);

        if (!commit)
            return;

        // Workaround for drum page scroll bug
        final ViewManager viewManager = this.surface.getViewManager ();
        if (viewManager.isActive (Views.DRUM))
            AbstractDrumView.class.cast (viewManager.get (Views.DRUM)).repositionBankPage ();
    }


    /** {@inheritDoc} */
    @Override
    public void encoderLeft (final ButtonEvent event)
    {
        if (event != ButtonEvent.DOWN || this.filterColumn == 0)
            return;

        final IBrowser browser = this.model.getBrowser ();
        if (this.filterColumn == -1)
            this.filterColumn = browser.getFilterColumnCount () - 1;
        else
            this.filterColumn--;
        while (this.filterColumn > 0 && !browser.getFilterColumn (this.filterColumn).doesExist ())
            this.filterColumn--;
    }


    /** {@inheritDoc} */
    @Override
    public void encoderRight (final ButtonEvent event)
    {
        if (event != ButtonEvent.DOWN)
            return;

        final IBrowser browser = this.model.getBrowser ();
        final int max = browser.getFilterColumnCount () - 1;

        do
        {
            if (this.filterColumn >= max)
            {
                this.filterColumn = -1;
                break;
            }
            this.filterColumn++;
        } while (this.filterColumn >= max || !browser.getFilterColumn (this.filterColumn).doesExist ());
    }


    /** {@inheritDoc} */
    @Override
    public void arrowCenter (final ButtonEvent event)
    {
        if (event == ButtonEvent.DOWN)
            this.model.getBrowser ().toggleInsertionPoint ();
    }
}