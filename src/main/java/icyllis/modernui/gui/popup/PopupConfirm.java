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
import icyllis.modernui.gui.layout.WidgetLayout;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.master.IModule;
import icyllis.modernui.gui.master.IWidget;
import icyllis.modernui.gui.widget.TextFrameButton;
import net.minecraft.client.resources.I18n;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

public class PopupConfirm implements IModule {

    private IFontRenderer fontRenderer = FontTools.FONT_RENDERER;

    private String title = "";

    private String[] desc = new String[0];

    private List<IWidget> buttons = new ArrayList<>();

    private WidgetLayout buttonLayout;

    private float x, y;

    private float frameSizeHOffset = 16;

    public PopupConfirm(ConfirmCallback callback) {
        this(callback, 0);
    }

    public PopupConfirm(ConfirmCallback callback, int seconds) {
        this(callback, seconds, null);
    }

    public PopupConfirm(ConfirmCallback callback, int seconds, @Nullable String alternative) {
        if (seconds > 0) {
            buttons.add(new TextFrameButton.Countdown(I18n.format("gui.yes"), () -> callback.call(ConfirmCallback.CONFIRM), seconds));
        } else {
            buttons.add(new TextFrameButton(I18n.format("gui.yes"), () -> callback.call(ConfirmCallback.CONFIRM)));
        }
        if (alternative != null) {
            buttons.add(new TextFrameButton(alternative, () -> callback.call(ConfirmCallback.ALTERNATIVE)));
        }
        buttons.add(new TextFrameButton(I18n.format("gui.no"), () -> callback.call(ConfirmCallback.CANCEL)));
        buttonLayout = new WidgetLayout(buttons, WidgetLayout.Direction.HORIZONTAL_NEGATIVE, 6);

        GlobalModuleManager manager = GlobalModuleManager.INSTANCE;
        manager.addAnimation(new Animation(3, true)
                .applyTo(new Applier(frameSizeHOffset, 80, value -> frameSizeHOffset = value)));
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
        this.desc = FontTools.splitStringToWidth(description, 164);
        return this;
    }

    @Override
    public void draw(float time) {
        DrawTools.fillRectWithFrame(x, y, x + 180, y + frameSizeHOffset, 0.51f, 0x101010, 0.7f, 0x404040, 1);
        DrawTools.fillRectWithColor(x, y, x + 180, y + 16, 0x080808, 0.85f);
        fontRenderer.drawString(title, x + 90, y + 4, TextAlign.CENTER);
        int i = 0;
        for (String t : desc) {
            fontRenderer.drawString(t, x + 8, y + 24 + i++ * 12);
        }
        buttons.forEach(e -> e.draw(time));
    }

    @Override
    public void resize(int width, int height) {
        this.x = width / 2f - 90;
        this.y = height / 2f - 40;
        buttonLayout.layout(width / 2f + 80, height / 2f + 20);
    }

    @Override
    public void tick(int ticks) {
        buttons.forEach(e -> e.tick(ticks));
    }

    @Override
    public boolean mouseMoved(double mouseX, double mouseY) {
        for (IWidget widget : buttons) {
            if (widget.updateMouseHover(mouseX, mouseY)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        for (IWidget widget : buttons) {
            if (widget.isMouseHovered() && widget.mouseClicked(mouseButton)) {
                GlobalModuleManager.INSTANCE.closePopup();
                return true;
            }
        }
        return false;
    }
}
