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

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.font.FontTools;
import icyllis.modernui.font.IFontRenderer;
import icyllis.modernui.gui.element.IElement;
import icyllis.modernui.system.MouseTools;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.InputMappings;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.settings.KeyModifier;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nullable;
import java.util.function.Consumer;

public class KeyInputBox implements IElement, IGuiEventListener {

    private IFontRenderer fontRenderer = FontTools.FONT_RENDERER;

    private float x, y;

    private float width = 84;

    private float height = 16;

    private String keyText;

    private boolean mouseHovered;

    private int backAlpha = 16;

    private float textGrayscale = 0.85f;

    private boolean editing = false;

    // pressing a modifier key
    @Nullable
    private InputMappings.Input pressing = null;

    // focus this make keyPressed keyReleased can be called
    private Consumer<IGuiEventListener> focuser;

    private Consumer<InputMappings.Input> keyBinder;

    public KeyInputBox(Consumer<IGuiEventListener> focuser, Consumer<InputMappings.Input> keyBinder) {
        this.focuser = focuser;
        this.keyBinder = keyBinder;
    }

    @Override
    public void draw(float currentTime) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder bufferBuilder = tessellator.getBuffer();
        RenderSystem.disableTexture();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        bufferBuilder.pos(x, y + height, 0.0D).color(96, 96, 96, backAlpha).endVertex();
        bufferBuilder.pos(x + width, y + height, 0.0D).color(96, 96, 96, backAlpha).endVertex();
        bufferBuilder.pos(x + width, y, 0.0D).color(96, 96, 96, backAlpha).endVertex();
        bufferBuilder.pos(x, y, 0.0D).color(96, 96, 96, backAlpha).endVertex();
        tessellator.draw();

        if (editing) {
            GL11.glEnable(GL11.GL_LINE_SMOOTH);
            GL11.glHint(GL11.GL_LINE_SMOOTH_HINT, GL11.GL_NICEST);
            bufferBuilder.begin(GL11.GL_LINE_LOOP, DefaultVertexFormats.POSITION_COLOR);
            GL11.glLineWidth(1.0F);
            bufferBuilder.pos(x, y + height, 0.0D).color(153, 220, 240, 220).endVertex();
            bufferBuilder.pos(x + width, y + height, 0.0D).color(153, 220, 240, 220).endVertex();
            bufferBuilder.pos(x + width, y, 0.0D).color(153, 220, 240, 220).endVertex();
            bufferBuilder.pos(x, y, 0.0D).color(153, 220, 240, 220).endVertex();
            tessellator.draw();
            GL11.glDisable(GL11.GL_LINE_SMOOTH);
        }
        RenderSystem.enableTexture();

        fontRenderer.drawString(keyText, x + 42, y + 4, textGrayscale, textGrayscale, textGrayscale, 1, 0.25f);
    }

    public void setPos(float x, float y) {
        this.x = x;
        this.y = y;
    }

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
    public void mouseMoved(double mouseX, double mouseY) {
        boolean prev = mouseHovered;
        mouseHovered = isMouseOver(mouseX, mouseY);
        if (prev != mouseHovered) {
            if (mouseHovered) {
                MouseTools.useIBeamCursor();
                backAlpha = 64;
            } else {
                MouseTools.useDefaultCursor();
                if (!editing)
                    backAlpha = 16;
            }
        }
    }

    public void setTextGrayscale(float s) {
        this.textGrayscale = s;
    }

    private void startEditing() {
        editing = true;
        focuser.accept(this);
        backAlpha = 64;
    }

    private void stopEditing() {
        editing = false;
        focuser.accept(null);
        if (!mouseHovered) {
            MouseTools.useDefaultCursor();
            backAlpha = 16;
        }
        pressing = null;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (editing && mouseHovered) {
            keyBinder.accept(InputMappings.Type.MOUSE.getOrMakeInput(mouseButton));
            stopEditing();
            return true;
        }
        if (!editing && mouseHovered && mouseButton == 0) {
            startEditing();
            return true;
        }
        if (editing) {
            stopEditing();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (editing) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                keyBinder.accept(InputMappings.INPUT_INVALID);
                stopEditing();
                return true;
            }
            InputMappings.Input input = InputMappings.getInputByCode(keyCode, scanCode);
            if (!KeyModifier.isKeyCodeModifier(input)) {
                // a combo key or a single non-modifier key
                keyBinder.accept(input);
                stopEditing();
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
                stopEditing();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isMouseOver(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }
}
