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

package icyllis.modernui.ui.widget;

import icyllis.modernui.graphics.font.TextAlign;
import icyllis.modernui.ui.master.*;
import icyllis.modernui.graphics.math.Color3i;
import icyllis.modernui.ui.test.IHost;
import icyllis.modernui.ui.test.IKeyboardListener;
import icyllis.modernui.ui.test.Widget;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Go'in my way
 */
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

    public KeyInputBox(IHost host, Consumer<InputMappings.Input> keyBinder) {
        super(host, new TextField.Builder().setWidth(84));
        this.keyBinder = keyBinder;
    }

    @Override
    public void onDraw(@Nonnull Canvas canvas, float time) {
        canvas.setRGBA(0.377f, 0.377f, 0.377f, backAlpha);
        canvas.drawRect(x1, y1, x2, y2);
        if (editing) {
            canvas.setLineAntiAliasing(true);
            canvas.setColor(Color3i.BLUE_C, 0.863f);
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
        keyText = TextFormatting.getTextWithoutFormattingCodes(keyText);

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
    public void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        UITools.useIBeamCursor();
        backAlpha = 0.25f;
    }

    @Override
    public void onMouseHoverExit() {
        super.onMouseHoverExit();
        UITools.useDefaultCursor();
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
        if (!isMouseHovered()) {
            UITools.useDefaultCursor();
            backAlpha = 0.063f;
        }
        pressing = null;
    }

    @Override
    protected boolean dispatchMouseClick(double mouseX, double mouseY, int mouseButton) {
        if (editing) {
            keyBinder.accept(InputMappings.Type.MOUSE.getOrMakeInput(mouseButton));
            getParent().setKeyboardListener(null);
            return true;
        }
        return super.dispatchMouseClick(mouseX, mouseY, mouseButton);
    }

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        editing = true;
        getParent().setKeyboardListener(this);
        backAlpha = 0.25f;
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                keyBinder.accept(InputMappings.INPUT_INVALID);
                getParent().setKeyboardListener(null);
                return true;
            }
            InputMappings.Input input = InputMappings.getInputByCode(keyCode, scanCode);
            if (!KeyModifier.isKeyCodeModifier(input)) {
                // a combo key or a single non-modifier key
                keyBinder.accept(input);
                getParent().setKeyboardListener(null);
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
                getParent().setKeyboardListener(null);
                return true;
            }
        }
        return false;
    }

    public String getKeyText() {
        return keyText;
    }
}
