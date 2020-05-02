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

package icyllis.modernui.impl.setting;

import icyllis.modernui.font.FontTools;
import icyllis.modernui.gui.math.Align3H;
import icyllis.modernui.gui.animation.Animation;
import icyllis.modernui.gui.animation.Applier;
import icyllis.modernui.gui.master.Canvas;
import icyllis.modernui.gui.master.GlobalModuleManager;
import icyllis.modernui.gui.master.Icon;
import icyllis.modernui.gui.math.Align9D;
import icyllis.modernui.gui.popup.PopupMenu;
import icyllis.modernui.gui.scroll.SettingScrollWindow;
import icyllis.modernui.gui.popup.DropDownMenu;
import icyllis.modernui.system.ConstantsLibrary;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.function.Consumer;

public class DropdownSettingEntry extends SettingEntry {

    public int lastOptionIndex;
    public int currentOptionIndex;

    public final List<String> optionNames;

    private String optionText;
    private float textLength;

    private float optionBrightness = 0.85f;

    protected boolean drawOptionFrame = false;
    private float frameAlpha = 0;

    protected Consumer<Integer> saveOption;

    protected boolean available = true;

    private final Animation frameAnimation;

    private final Icon icon = new Icon(ConstantsLibrary.ICONS, 0.25f, 0.125f, 0.375f, 0.25f, true);

    public DropdownSettingEntry(SettingScrollWindow window, String optionTitle, @Nonnull List<String> optionNames, int originalIndex, Consumer<Integer> saveOption) {
        super(window, optionTitle);
        this.currentOptionIndex = this.lastOptionIndex = originalIndex;
        this.optionNames = optionNames;
        this.saveOption = saveOption;
        optionText = optionNames.get(originalIndex);
        textLength = FontTools.getStringWidth(optionText) + 3;

        frameAnimation = new Animation(100)
                .addAppliers(new Applier(0, 0.25f, () -> frameAlpha, this::setFrameAlpha))
                .addListener(new Animation.IAnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation, boolean isReverse) {
                        if (isReverse) {
                            drawOptionFrame = false;
                        }
                    }
                });
    }

    public void setAvailable(boolean b) {
        available = b;
        if (b) {
            optionBrightness = isMouseHovered() ? 1.0f : 0.85f;
        } else {
            optionBrightness = 0.5f;
        }
    }

    @Override
    public void drawExtra(Canvas canvas, float time) {
        if (frameAlpha > 0) {
            /*Tessellator tessellator = Tessellator.getInstance();
            BufferBuilder bufferBuilder = tessellator.getBuffer();
            RenderSystem.disableTexture();
            bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);*/
            float bl = x2 - 10 - textLength;
            /*bufferBuilder.pos(bl, y1 + 18, 0.0D).color(96, 96, 96, frameAlpha).endVertex();
            bufferBuilder.pos(x2, y1 + 18, 0.0D).color(96, 96, 96, frameAlpha).endVertex();
            bufferBuilder.pos(x2, y1 + 2, 0.0D).color(96, 96, 96, frameAlpha).endVertex();
            bufferBuilder.pos(bl, y1 + 2, 0.0D).color(96, 96, 96, frameAlpha).endVertex();
            tessellator.draw();
            RenderSystem.enableTexture();*/
            canvas.setRGBA(0.377f, 0.377f, 0.377f, frameAlpha);
            canvas.drawRect(bl, y1 + 2, x2, y1 + 18);
        }
        canvas.setTextAlign(Align3H.RIGHT);
        canvas.setRGBA(optionBrightness, optionBrightness, optionBrightness, 1);
        canvas.drawText(optionText, x2 - 10, y1 + 6);
        //fontRenderer.drawString(optionText, x2 - 10, y1 + 6, optionBrightness, 1, TextAlign.RIGHT);
        canvas.drawIcon(icon, x2 - 8, y1 + 7, x2, y1 + 15);
        /*RenderSystem.pushMatrix();
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        RenderSystem.scalef(0.25f, 0.25f, 1);
        RenderSystem.color3f(optionBrightness, optionBrightness, optionBrightness);
        textureManager.bindTexture(ConstantsLibrary.ICONS);
        DrawTools.blit(x2 * 4 - 34, y1 * 4 + 28, 64, 32, 32, 32);
        RenderSystem.popMatrix();*/
    }

    public void setFrameAlpha(float a) {
        if (!drawOptionFrame) {
            frameAlpha = 0;
            return;
        }
        frameAlpha = a;
    }

    @Override
    public boolean updateMouseHover(double mouseX, double mouseY) {
        if (super.updateMouseHover(mouseX, mouseY)) {
            if (available && !drawOptionFrame && optionNames.size() > 1) {
                if (mouseInOption(mouseX, mouseY)) {
                    drawOptionFrame = true;
                    /*getModule().addAnimation(new Animation(2)
                            .addAppliers(new Applier(0.25f, this::setFrameAlpha)));*/
                    frameAnimation.start();
                }
            } else if (!mouseInOption(mouseX, mouseY)) {
                /*getModule().addAnimation(new Animation(2)
                        .addAppliers(new Applier(0.25f, 0, getter, this::setFrameAlpha))
                        .onFinish(() -> drawOptionFrame = false));*/
                frameAnimation.invert();
            }
            return true;
        }
        return false;
    }

    private boolean mouseInOption(double mouseX, double mouseY) {
        return mouseX >= x2 - 10 - textLength && mouseX <= x2 - 4 && mouseY >= y1 + 2 && mouseY <= y1 + 18;
    }

    /*@Override
    public boolean mouseClicked(double mouseX, double mouseY, int mouseButton) {
        if (drawOptionFrame && mouseButton == 0) {
            DropDownMenu menu = new DropDownMenu(window.getModule(), optionNames, currentOptionIndex, 16, this::onValueChanged, DropDownMenu.Align.RIGHT);
            menu.locate(x2 - 4, y1 + 18 - window.getVisibleOffset());
            GlobalModuleManager.INSTANCE.openPopup(new PopupMenu(menu), false);
            return true;
        }
        return false;
    }*/

    @Override
    protected boolean onMouseLeftClick(double mouseX, double mouseY) {
        if (drawOptionFrame) {
            DropDownMenu menu = new DropDownMenu.Builder(optionNames, currentOptionIndex)
                    .setAlign(Align9D.TOP_RIGHT)
                    .build(window)
                    .buildCallback(this::onValueChanged);
            menu.locate(window.toAbsoluteX(x2 - 4), window.toAbsoluteY(y1 + 18));
            GlobalModuleManager.INSTANCE.openPopup(new PopupMenu(menu), false);
            return true;
        }
        return super.onMouseLeftClick(mouseX, mouseY);
    }

    @Override
    protected void onMouseHoverEnter(double mouseX, double mouseY) {
        super.onMouseHoverEnter(mouseX, mouseY);
        if (available) {
            optionBrightness = 1.0f;
        }
    }

    @Override
    protected void onMouseHoverExit() {
        super.onMouseHoverExit();
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
        textLength = FontTools.getStringWidth(optionText) + 3;
    }

    public void saveOption() {
        saveOption.accept(currentOptionIndex);
    }
}
