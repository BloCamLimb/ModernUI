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

package icyllis.modernui.gui.component.option;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.component.DropDownList;
import icyllis.modernui.gui.master.DrawTools;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.window.SettingScrollWindow;
import icyllis.modernui.system.ReferenceLibrary;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class SelectiveOptionEntry extends OptionEntry {

    private TextureManager textureManager = Minecraft.getInstance().textureManager;

    public final int originalOptionIndex;

    public int currentOptionIndex;

    public List<String> options;

    private String optionText;

    private float textLength;

    private boolean drawOptionFrame = false;

    private int frameAlpha = 0;

    public SelectiveOptionEntry(SettingScrollWindow window, String optionTitle, List<String> options, int originalIndex) {
        super(window, optionTitle);
        this.currentOptionIndex = this.originalOptionIndex = originalIndex;
        this.options = options;
        onOptionChanged(originalIndex);
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
        fontRenderer.drawString(optionText, centerX + 150, y + 6, textBrightness, textBrightness, textBrightness, 1, 0.5f);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.pushMatrix();
        RenderSystem.scalef(0.25f, 0.25f, 1);
        RenderSystem.color3f(textBrightness, textBrightness, textBrightness);
        textureManager.bindTexture(ReferenceLibrary.ICONS);
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
        if (!drawOptionFrame) {
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
        if (drawOptionFrame && mouseButton == 0 && !window.hasDropDownList()) {
            DropDownList list = new DropDownList(options, currentOptionIndex, 16, this::onOptionChanged);
            list.setPos((float) (mouseX - deltaCenterX + 156), (float) (mouseY - deltaY + 18), GlobalModuleManager.INSTANCE.getWindowHeight());
            window.setDropDownList(list);
            return true;
        }
        return super.mouseClicked(deltaCenterX, deltaY, mouseX, mouseY, mouseButton);
    }

    @Override
    protected void onMouseHoverOff() {
        super.onMouseHoverOff();
        drawOptionFrame = false;
        frameAlpha = 0;
    }

    public void onOptionChanged(int index) {
        this.currentOptionIndex = index;
        optionText = options.get(index);
        textLength = fontRenderer.getStringWidth(optionText) + 3;
    }
}
