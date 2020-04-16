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
import icyllis.modernui.gui.draw.Background;
import icyllis.modernui.gui.draw.ConfirmWindowDraw;
import icyllis.modernui.gui.layout.WidgetLayout;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.gui.widget.TextFrameButton;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PopupConfirm extends Module {

    private final ConfirmWindowDraw bg;

    private WidgetLayout buttonLayout;

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
        addDrawable(new Background(this, 4));
        bg = new ConfirmWindowDraw(this);
        addDrawable(bg);
        List<IWidget> buttons = new ArrayList<>();
        if (seconds > 0) {
            buttons.add(new TextFrameButton.Countdown(this, confirmText, () -> callback.call(ConfirmCallback.CONFIRM), seconds));
        } else {
            buttons.add(new TextFrameButton(this, confirmText, () -> callback.call(ConfirmCallback.CONFIRM)));
        }
        if (alternative != null) {
            buttons.add(new TextFrameButton(this, alternative, () -> callback.call(ConfirmCallback.ALTERNATIVE)));
        }
        buttons.add(new TextFrameButton(this, cancelText, () -> callback.call(ConfirmCallback.CANCEL)));
        buttons.forEach(this::addWidget);
        buttonLayout = new WidgetLayout(buttons, WidgetLayout.Direction.HORIZONTAL_NEGATIVE, 6);
    }

    public PopupConfirm setFullTitle(String title) {
        bg.setTitle(title);
        return this;
    }

    public PopupConfirm setConfirmTitle(String operation) {
        bg.setTitle(I18n.format("gui.modernui.button.confirm", operation));
        return this;
    }

    public PopupConfirm setDescription(String description) {
        bg.setDesc(FontTools.splitStringToWidth(description, 164));
        return this;
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        buttonLayout.layout(width / 2f + 82, height / 2f + 20);
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
