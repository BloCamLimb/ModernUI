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

package icyllis.modernui.gui.widget;

import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.master.*;
import icyllis.modernui.gui.math.Color3f;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

public class KeyInputBox extends Widget implements IKeyboardListener {

    private String keyText;

    private float backAlpha = 0.063f;

    private float textBrightness = 0.85f;

    private boolean editing = false;

    /**
     * Pressing a modifier key
     */
    @Nullable
    private InputMappings.Input pressing = null;

    private Consumer<InputMappings.Input> keyBinder;

    public KeyInputBox(Module module, Consumer<InputMappings.Input> keyBinder) {
        super(module, 84, 16);
        this.keyBinder = keyBinder;
    }

    @Override
    public void draw(@Nonnull Canvas canvas, float time) {
        canvas.setRGBA(0.377f, 0.377f, 0.377f, backAlpha);
        canvas.drawRect(x1, y1, x2, y2);
        if (editing) {
            canvas.setLineAntiAliasing(true);
            canvas.setColor(Color3f.BLUE_C, 0.863f);
            canvas.drawRectLines(x1, y1, x2, y2);
            canvas.setLineAntiAliasing(false);
        }
        canvas.setTextAlign(TextAlign.CENTER);
        canvas.setRGBA(textBrightness, textBrightness, textBrightness, 1.0f);
        canvas.drawText(keyText, x1 + 42, y1 + 4);
    }

    /*@Override
    public void draw(float time) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x1, y2, 0.0D).color(96, 96, 96, backAlpha).endVertex();
        bufferBuilder.pos(x2, y2, 0.0D).color(96, 96, 96, backAlpha).endVertex();
        bufferBuilder.pos(x2, y1, 0.0D).color(96, 96, 96, backAlpha).endVertex();
        bufferBuilder.pos(x1, y1, 0.0D).color(96, 96, 96, backAlpha).endVertex();
        tessellator.draw();

        if (editing) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            GL11.glLineWidth(1.0F);
            bufferBuilder.pos(x1, y2, 0.0D).color(153, 220, 240, 220).endVertex();
            bufferBuilder.pos(x2, y2, 0.0D).color(153, 220, 240, 220).endVertex();
            bufferBuilder.pos(x2, y1, 0.0D).color(153, 220, 240, 220).endVertex();
            bufferBuilder.pos(x1, y1, 0.0D).color(153, 220, 240, 220).endVertex();
            tessellator.draw();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }
        RenderSystem.enableTexture();

        fontRenderer.drawString(keyText, x1 + 42, y1 + 4, textBrightness, TextAlign.CENTER);
    }*/

    public void setKeyText(String keyText) {
        this.keyText = keyText;
    }

    public void setTextColor(int tier) {
        if (keyText.indexOf('\u00a7') != -1) {
            keyText = keyText.substring(2);
        }
        switch (tier) {
            case 1:
                keyText = TextFormatting.GOLD + keyText;
                break;
            case 2:
                keyText = TextFormatting.RED + keyText;
                break;
        }
    }

    @Override
    protected void onMouseHoverEnter() {
        super.onMouseHoverEnter();
        MouseTools.useIBeamCursor();
        backAlpha = 0.25f;
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
        MouseTools.useDefaultCursor();
        if (!editing) {
            backAlpha = 0.063f;
        }
    }

    public void setTextBrightness(float s) {
        this.textBrightness = s;
    }

    @Override
    public void stopKeyboardListening() {
        editing = false;
        if (!mouseHovered) {
            MouseTools.useDefaultCursor();
            backAlpha = 0.063f;
        }
        pressing = null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (editing) {
            keyBinder.accept(InputMappings.Type.MOUSE.getOrMakeInput(mouseButton));
            module.setKeyboardListener(null);
            return true;
        }
        if (mouseButton == 0) {
            editing = true;
            module.setKeyboardListener(this);
            backAlpha = 0.25f;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                keyBinder.accept(InputMappings.INPUT_INVALID);
                module.setKeyboardListener(null);
                return true;
            }
            InputMappings.Input input = InputMappings.getInputByCode(keyCode, scanCode);
            if (!KeyModifier.isKeyCodeModifier(input)) {
                // a combo key or a single non-modifier key
                keyBinder.accept(input);
                module.setKeyboardListener(null);
            } else {
                if (pressing == null) {
                    // this is the modifier key that has already pressed, and can't be changed
                    keyText = I18n.format(input.getTranslationKey());
                    pressing = input;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean keyReleased(int keyCode, int scanCode, int modifiers) {
        if (editing) {
            InputMappings.Input input = InputMappings.getInputByCode(keyCode, scanCode);
            // this used for single modifier key input, not for combo key
            if (input.equals(pressing)) {
                keyBinder.accept(input);
                module.setKeyboardListener(null);
                return true;
            }
        }
        return false;
    }

    public String getKeyText() {
        return keyText;
    }
}
