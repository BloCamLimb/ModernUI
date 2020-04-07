/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.gui.popup;

import icyllis.modernui.font.FontTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.background.Background;
import icyllis.modernui.gui.background.ConfirmWindowBG;
import icyllis.modernui.gui.layout.WidgetLayout;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.widget.TextFrameButton;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PopupConfirm extends Module {

    private IFontRenderer fontRenderer = FontTools.FONT_RENDERER;

    private String title = "";

    private String[] desc = new String[0];

    private WidgetLayout buttonLayout;

    private float x, y;

    public PopupConfirm(ConfirmCallback callback) {
        this(callback, 0);
    }

    public PopupConfirm(ConfirmCallback callback, int seconds) {
        this(callback, seconds, I18n.format("gui.yes"), I18n.format("gui.no"), null);
    }

    /**
     * Main constructor to build popup confirm window
     * @param callback operation when left clicked button
     * @param seconds countdown to make confirm button clickable
     * @param confirmText confirm button text
     * @param cancelText cancel button text
     * @param alternative third button between confirm button and cancel button to perform another operation
     */
    public PopupConfirm(ConfirmCallback callback, int seconds, String confirmText, String cancelText, @Nullable String alternative) {
        addBackground(new Background(4));
        addBackground(new ConfirmWindowBG());
        List<IWidget> buttons = new ArrayList<>();
        if (seconds > 0) {
            buttons.add(new TextFrameButton.Countdown(confirmText, () -> callback.call(ConfirmCallback.CONFIRM), seconds));
        } else {
            buttons.add(new TextFrameButton(confirmText, () -> callback.call(ConfirmCallback.CONFIRM)));
        }
        if (alternative != null) {
            buttons.add(new TextFrameButton(alternative, () -> callback.call(ConfirmCallback.ALTERNATIVE)));
        }
        buttons.add(new TextFrameButton(cancelText, () -> callback.call(ConfirmCallback.CANCEL)));
        buttons.forEach(this::addWidget);
        buttonLayout = new WidgetLayout(buttons, WidgetLayout.Direction.HORIZONTAL_NEGATIVE, 6);
    }

    public PopupConfirm setFullTitle(String title) {
        this.title = title;
        return this;
    }

    public PopupConfirm setConfirmTitle(String operation) {
        this.title = I18n.format("gui.modernui.button.confirm", operation);
        return this;
    }

    public PopupConfirm setDescription(String description) {
        this.desc = FontTools.splitStringToWidth(description, 244);
        return this;
    }

    @Override
    public void draw(float time) {
        super.draw(time);
        fontRenderer.drawString(title, x + 130, y + 4, TextAlign.CENTER);
        int i = 0;
        for (String t : desc) {
            fontRenderer.drawString(t, x + 8, y + 24 + i++ * 12);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        this.x = width / 2f - 130;
        this.y = height / 2f - 40;
        buttonLayout.layout(width / 2f + 130, height / 2f + 20);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (super.mouseClicked(mouseX, mouseY, mouseButton)) {
            GlobalModuleManager.INSTANCE.closePopup();
            return true;
        }
        return false;
    }
}
