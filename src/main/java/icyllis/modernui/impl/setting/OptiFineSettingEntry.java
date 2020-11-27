/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.impl.setting;

import icyllis.modernui.font.text.TextAlign;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.graphics.renderer.Icon;
import icyllis.modernui.view.UITools;
import net.minecraft.client.AbstractOption;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.optifine.config.IteratableOptionOF;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OptiFineSettingEntry extends SettingEntry {

    private static Method getter;
    private static Method setter;

    private static final Icon ICON = new Icon(UITools.ICONS, 0.5f, 0.125f, 0.625f, 0.25f, true);

    static {
        try {
            getter = GameSettings.class.getDeclaredMethod("getKeyBindingOF", AbstractOption.class);
            setter = GameSettings.class.getDeclaredMethod("setOptionValueOF", AbstractOption.class, int.class);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
    }

    private final IteratableOptionOF option;

    private final GameSettings gameSettings;

    private String optionText = "";
    private float  textLength;

    protected boolean drawOptionFrame = false;

    public OptiFineSettingEntry(SettingScrollWindow window, String title, IteratableOptionOF option) {
        super(window, title);
        gameSettings = Minecraft.getInstance().gameSettings;
        this.option = option;
        updateText();
    }

    @Override
    protected void drawExtra(@Nonnull Canvas canvas, float time) {
        if (drawOptionFrame) {
            float bl = x2 - 10 - textLength;
            //canvas.setColor(0.377f, 0.377f, 0.377f, 0.25f);
            canvas.drawRect(bl, y1 + 2, x2, y1 + 18);
        }
        canvas.setTextAlign(TextAlign.RIGHT);
        //canvas.setColor(titleBrightness, titleBrightness, titleBrightness, 1);
        canvas.drawText(optionText, x2 - 10, y1 + 6);
        canvas.drawIcon(ICON, x2 - 8, y1 + 6, x2, y1 + 14);
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            if (!drawOptionFrame) {
                if (mouseInOption(mouseX, mouseY)) {
                    drawOptionFrame = true;
                }
            } else if (!mouseInOption(mouseX, mouseY)) {
                drawOptionFrame = false;
            }
            return true;
        }
        return false;
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        if (drawOptionFrame) {
            try {
                setter.invoke(gameSettings, option, 1);
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
            updateText();
            drawOptionFrame = mouseInOption(mouseX, mouseY);
            return true;
        }
        return super.onMouseLeftClick(mouseX, mouseY);
    }

    private boolean mouseInOption(double mouseX, double mouseY) {
        return mouseX >= x2 - 10 - textLength && mouseX <= x2 - 4 && mouseY >= y1 + 2 && mouseY <= y1 + 18;
    }

    @Override
    public void onMouseHoverExit() {
        super.onMouseHoverExit();
        drawOptionFrame = false;
    }

    private void updateText() {
        try {
            optionText = (String) getter.invoke(gameSettings, option);
            optionText = optionText.split(": ")[1];
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        textLength = UITools.getTextWidth(optionText) + 3;
    }
}
