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

package icyllis.modernui.gui.scroll.option;

import icyllis.modernui.gui.widget.KeyInputBox;
import icyllis.modernui.gui.window.SettingScrollWindow;
import net.minecraft.client.GameSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.util.InputMappings;
import net.minecraftforge.client.settings.KeyModifier;

import java.util.Locale;
import java.util.Objects;

public class KeyBindingEntry extends OptionEntry {

    private KeyBinding keyBinding;

    private KeyInputBox inputBox;

    public KeyBindingEntry(SettingScrollWindow window, KeyBinding keyBinding) {
        super(window, I18n.format(keyBinding.getKeyDescription()));
        this.keyBinding = keyBinding;
        this.inputBox = new KeyInputBox(window::setFocused, this::bindKey);
        updateKeyText();
    }

    @Override
    public void drawExtra(float centerX, float y, float currentTime) {
        inputBox.setPos(centerX + 70, y + 2);
        inputBox.draw(currentTime);
    }

    @Override
    public void mouseMoved(double deltaCenterX, double deltaY, double mouseX, double mouseY) {
        inputBox.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseClicked(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton) {
        if (inputBox.mouseClicked(mouseX, mouseY, mouseButton)) {
            return true;
        }
        return super.mouseClicked(deltaCenterX, deltaY, mouseX, mouseY, mouseButton);
    }

    private void updateKeyText() {
        String translateKey = keyBinding.getTranslationKey();
        String localizedName = I18n.format(translateKey);

        InputMappings.Input key = keyBinding.getKey();
        if (keyBinding.isInvalid()) {
            inputBox.setKeyText("");
            return;
        }

        int keyCode = key.getKeyCode();
        String glfwName = null;

        switch (key.getType()) {
            case KEYSYM:
                glfwName = InputMappings.getKeynameFromKeycode(keyCode);
                break;
            case SCANCODE:
                glfwName = InputMappings.getKeyNameFromScanCode(keyCode);
                break;
            case MOUSE:
                inputBox.setKeyText(Objects.equals(localizedName, translateKey) ? // if not translated, use default, keyCode 0 = Mouse 1 (Mouse Left)
                        I18n.format(InputMappings.Type.MOUSE.getName(), keyCode + 1) : localizedName);
                return;
        }

        KeyModifier modifier = keyBinding.getKeyModifier();
        String comboPrefix;
        // why not use forge localization? bcz there's no need actually, modifier key shouldn't be translated
        switch (modifier) {
            case CONTROL:
                comboPrefix = "Ctrl + ";
                break;
            case ALT:
                comboPrefix = "Alt + ";
                break;
            case SHIFT:
                comboPrefix = "Shift + ";
                break;
            default:
                comboPrefix = "";
                break;
        }

        if (glfwName != null) {
            char c = glfwName.charAt(0);
            if (Character.isLetter(c)) {
                glfwName = glfwName.toUpperCase(Locale.ROOT);
            }
            inputBox.setKeyText(comboPrefix + glfwName);
        } else {
            inputBox.setKeyText(comboPrefix + localizedName);
        }
    }

    private void bindKey(InputMappings.Input inputIn) {
        GameSettings gameSettings = Minecraft.getInstance().gameSettings;
        if (inputIn.getType() != InputMappings.Type.MOUSE) {
            keyBinding.setKeyModifierAndCode(KeyModifier.getActiveModifier(), inputIn);
        }
        gameSettings.setKeyBindingCode(keyBinding, inputIn);
        updateKeyText();
    }

    @Override
    protected void onMouseHoverOn() {
        super.onMouseHoverOn();
        inputBox.setTextGrayscale(titleGrayscale);
    }

    @Override
    protected void onMouseHoverOff() {
        super.onMouseHoverOff();
        inputBox.setTextGrayscale(titleGrayscale);
    }
}
