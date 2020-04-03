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

package icyllis.modernui.gui.option;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.font.TextAlign;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.popup.PopupMenu;
import icyllis.modernui.gui.widget.DropDownMenu;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
import icyllis.modernui.system.ConstantsLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.List;
import java.util.function.Consumer;

public class DropdownOptionEntry extends AbstractOptionEntry {

    private TextureManager textureManager = Minecraft.getInstance().getTextureManager();

    public int originalOptionIndex;
    public int currentOptionIndex;

    public List<String> optionNames;

    private String optionText;
    private float textLength;

    private float optionBrightness = 0.85f;

    protected boolean drawOptionFrame = false;
    private int frameAlpha = 0; // 0~255

    protected Consumer<Integer> saveOption;

    protected boolean available = true;

    public DropdownOptionEntry(SettingScrollWindow window, String optionTitle, List<String> optionNames, int originalIndex, Consumer<Integer> saveOption) {
        super(window, optionTitle);
        this.currentOptionIndex = this.originalOptionIndex = originalIndex;
        this.optionNames = optionNames;
        this.saveOption = saveOption;
        optionText = optionNames.get(originalIndex);
        textLength = fontRenderer.getStringWidth(optionText) + 3;
    }

    public void setAvailable(boolean b) {
        available = b;
        if (b) {
            optionBrightness = mouseHovered ? 1.0f : 0.85f;
        } else {
            optionBrightness = 0.5f;
        }
    }

    @Override
    public void drawExtra(float centerX, float y, float currentTime) {
        if (frameAlpha > 0) {
            Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();
            RenderSystem.disableTexture();
            bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
            float bl = centerX + 150 - textLength;
            bufferBuilder.pos(bl, y + 18, 0.0D).color(96, 96, 96, frameAlpha).endVertex();
            bufferBuilder.pos(centerX + 160, y + 18, 0.0D).color(96, 96, 96, frameAlpha).endVertex();
            bufferBuilder.pos(centerX + 160, y + 2, 0.0D).color(96, 96, 96, frameAlpha).endVertex();
            bufferBuilder.pos(bl, y + 2, 0.0D).color(96, 96, 96, frameAlpha).endVertex();
            tessellator.draw();
            RenderSystem.enableTexture();
        }
        fontRenderer.drawString(optionText, centerX + 150, y + 6, optionBrightness, 1, TextAlign.RIGHT);
        RenderSystem.pushMatrix();
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.scalef(0.25f, 0.25f, 1);
        RenderSystem.color3f(optionBrightness, optionBrightness, optionBrightness);
        textureManager.bindTexture(ConstantsLibrary.ICONS);
        DrawTools.blit(centerX * 4 + 606, y * 4 + 28, 64, 32, 32, 32);
        RenderSystem.popMatrix();
    }

    public void setFrameAlpha(float a) {
        if (!drawOptionFrame) {
            frameAlpha = 0;
            return;
        }
        frameAlpha = (int) a;
    }

    @Override
    public void mouseMoved(double deltaCenterX, double deltaY, double mouseX, double mouseY) {
        if (available && !drawOptionFrame && optionNames.size() > 1) {
            if (mouseInOption(deltaCenterX, deltaY)) {
                drawOptionFrame = true;
                GlobalModuleManager.INSTANCE.addAnimation(new Animation(2)
                        .applyTo(new Applier(64, this::setFrameAlpha)));
            }
        } else if (!mouseInOption(deltaCenterX, deltaY)) {
            GlobalModuleManager.INSTANCE.addAnimation(new Animation(2)
                    .applyTo(new Applier(64, 0, this::setFrameAlpha))
                    .onFinish(() -> drawOptionFrame = false));
        }
    }

    private boolean mouseInOption(double deltaCenterX, double deltaY) {
        return deltaCenterX >= 150 - textLength && deltaCenterX <= 156 && deltaY >= 2 && deltaY <= 18;
    }

    @Override
    public boolean mouseClicked(double deltaCenterX, double deltaY, double mouseX, double mouseY, int mouseButton) {
        if (drawOptionFrame && mouseButton == 0) {
            DropDownMenu menu = new DropDownMenu(optionNames, currentOptionIndex, 16, this::onValueChanged);
            menu.setPos((float) (mouseX - deltaCenterX + 156), (float) (mouseY - deltaY + 18), GlobalModuleManager.INSTANCE.getWindowHeight());
            GlobalModuleManager.INSTANCE.openPopup(new PopupMenu(menu), false);
            return true;
        }
        return super.mouseClicked(deltaCenterX, deltaY, mouseX, mouseY, mouseButton);
    }

    @Override
    protected void onMouseHoverOn() {
        super.onMouseHoverOn();
        if (available) {
            optionBrightness = 1.0f;
        }
    }

    @Override
    protected void onMouseHoverOff() {
        super.onMouseHoverOff();
        drawOptionFrame = false;
        frameAlpha = 0;
        if (available) {
            optionBrightness = 0.85f;
        }
    }

    public void onValueChanged(int index) {
        updateValue(index);
        saveOption();
    }

    protected void updateValue(int index) {
        currentOptionIndex = index;
        optionText = optionNames.get(index);
        textLength = fontRenderer.getStringWidth(optionText) + 3;
    }

    public void saveOption() {
        saveOption.accept(currentOptionIndex);
    }
}
