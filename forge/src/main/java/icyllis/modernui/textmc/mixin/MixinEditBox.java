/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.textmc.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Matrix4f;
import icyllis.modernui.textmc.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.renderer.*;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.BiFunction;

/**
 * This mostly fixes text advance shift and decreases dynamic layout overhead,
 * but it cannot be truly internationalized due to Minecraft design defects.
 */
@Mixin(EditBox.class)
public abstract class MixinEditBox extends AbstractWidget {

    @Shadow
    @Final
    private static String CURSOR_APPEND_CHARACTER;

    @Shadow
    @Final
    private static int BORDER_COLOR_FOCUSED;

    @Shadow
    @Final
    private static int BORDER_COLOR;

    @Shadow
    @Final
    private static int BACKGROUND_COLOR;

    @Shadow
    private boolean isEditable;

    @Shadow
    private int textColor;

    @Shadow
    private int textColorUneditable;

    @Shadow
    private int cursorPos;

    @Shadow
    private int displayPos;

    @Shadow
    private int highlightPos;

    @Shadow
    private String value;

    @Shadow
    private int frame;

    @Shadow
    private boolean bordered;

    @Shadow
    @Nullable
    private String suggestion;

    @Shadow
    private BiFunction<String, Integer, FormattedCharSequence> formatter;

    public MixinEditBox(int x, int y, int w, int h, Component title) {
        super(x, y, w, h, title);
    }

    @Inject(method = "<init>(Lnet/minecraft/client/gui/Font;IIIILnet/minecraft/client/gui/components/EditBox;" +
            "Lnet/minecraft/network/chat/Component;)V",
            at = @At("RETURN"))
    public void E_EditBox(Font font, int x, int y, int w, int h, @Nullable EditBox src, Component title,
                          CallbackInfo ci) {
        // fast path
        formatter = (s, i) -> null;
    }

    @Shadow
    public abstract boolean isVisible();

    @Shadow
    public abstract int getInnerWidth();

    @Shadow
    protected abstract int getMaxLength();

    /**
     * @author BloCamLimb
     * @reason Modern Text Engine
     */
    @Override
    @Overwrite
    public void renderButton(@Nonnull PoseStack poseStack, int mouseX, int mouseY, float deltaTicks) {
        if (!isVisible()) {
            return;
        }
        if (bordered) {
            int color = isFocused() ? BORDER_COLOR_FOCUSED : BORDER_COLOR;
            fill(poseStack, x - 1, y - 1, x + width + 1, y + height + 1, color);
            fill(poseStack, x, y, x + width, y + height, BACKGROUND_COLOR);
        }
        final int color = isEditable ? textColor : textColorUneditable;

        final String viewText =
                ModernStringSplitter.headByWidth(value.substring(displayPos), getInnerWidth(), Style.EMPTY);
        final int viewCursorPos = cursorPos - displayPos;
        final int clampedViewHighlightPos = Mth.clamp(highlightPos - displayPos, 0, viewText.length());

        final boolean cursorInRange = viewCursorPos >= 0 && viewCursorPos <= viewText.length();
        final boolean cursorVisible = isFocused() && ((frame / 10) & 1) == 0 && cursorInRange;

        final int baseX = bordered ? x + 4 : x;
        final int baseY = bordered ? y + (height - 8) / 2 : y;
        float seqX = baseX;

        final Matrix4f matrix = poseStack.last().pose();
        final MultiBufferSource.BufferSource bufferSource =
                MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        final boolean separate;
        if (!viewText.isEmpty()) {
            String subText = cursorInRange ? viewText.substring(0, viewCursorPos) : viewText;
            FormattedCharSequence subSequence = formatter.apply(subText, displayPos);
            if (subSequence != null) {
                separate = true;
                seqX = ModernTextRenderer.drawText(subSequence, seqX, baseY, color, true,
                        matrix, bufferSource, false, 0, LightTexture.FULL_BRIGHT);
            } else {
                separate = false;
                seqX = ModernTextRenderer.drawText(viewText, seqX, baseY, color, true,
                        matrix, bufferSource, false, 0, LightTexture.FULL_BRIGHT);
            }
        } else {
            separate = false;
        }

        final boolean cursorNotAtEnd = cursorPos < value.length() || value.length() >= getMaxLength();

        // XXX: BiDi is not supported here
        final float cursorX;
        if (cursorInRange) {
            if (!separate && !viewText.isEmpty()) {
                TextRenderNode node = TextLayoutEngine.getInstance().lookupVanillaNode(viewText);
                float accAdv = 0;
                for (int i = 0; i < viewCursorPos; i++) {
                    accAdv += node.getAdvances()[i];
                }
                cursorX = baseX + accAdv;
            } else {
                cursorX = seqX;
            }
        } else {
            cursorX = viewCursorPos > 0 ? baseX + width : baseX;
        }

        if (!viewText.isEmpty() && cursorInRange && viewCursorPos < viewText.length() && separate) {
            String subText = viewText.substring(viewCursorPos);
            FormattedCharSequence subSequence = formatter.apply(subText, cursorPos);
            if (subSequence != null) {
                ModernTextRenderer.drawText(subSequence, seqX, baseY, color, true,
                        matrix, bufferSource, false, 0, LightTexture.FULL_BRIGHT);
            } else {
                ModernTextRenderer.drawText(subText, seqX, baseY, color, true,
                        matrix, bufferSource, false, 0, LightTexture.FULL_BRIGHT);
            }
        }

        if (!cursorNotAtEnd && suggestion != null) {
            ModernTextRenderer.drawText(suggestion, cursorX, baseY, 0xFF808080, true,
                    matrix, bufferSource, false, 0, LightTexture.FULL_BRIGHT);
        }

        if (viewCursorPos != clampedViewHighlightPos) {
            bufferSource.endBatch();

            TextRenderNode node = TextLayoutEngine.getInstance().lookupVanillaNode(viewText);
            float startX = baseX;
            float endX = cursorX;
            for (int i = 0; i < clampedViewHighlightPos; i++) {
                startX += node.getAdvances()[i];
            }

            if (endX < startX) {
                float temp = startX;
                startX = endX;
                endX = temp;
            }
            if (startX > x + width) {
                startX = x + width;
            }
            if (endX > x + width) {
                endX = x + width;
            }

            BufferBuilder builder = Tesselator.getInstance().getBuilder();
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.disableTexture();
            RenderSystem.disableDepthTest();
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();
            builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            builder.vertex(matrix, startX, baseY + 10, 0)
                    .color(51, 181, 229, 102).endVertex();
            builder.vertex(matrix, endX, baseY + 10, 0)
                    .color(51, 181, 229, 102).endVertex();
            builder.vertex(matrix, endX, baseY - 1, 0)
                    .color(51, 181, 229, 102).endVertex();
            builder.vertex(matrix, startX, baseY - 1, 0)
                    .color(51, 181, 229, 102).endVertex();
            builder.end();
            BufferUploader.end(builder);
            RenderSystem.enableTexture();
            RenderSystem.enableDepthTest();
        } else if (cursorVisible) {
            if (cursorNotAtEnd) {
                bufferSource.endBatch();

                BufferBuilder builder = Tesselator.getInstance().getBuilder();
                RenderSystem.setShader(GameRenderer::getPositionColorShader);
                RenderSystem.disableTexture();
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                builder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
                builder.vertex(matrix, cursorX - 0.5f, baseY + 10, 0)
                        .color(208, 208, 208, 255).endVertex();
                builder.vertex(matrix, cursorX + 0.5f, baseY + 10, 0)
                        .color(208, 208, 208, 255).endVertex();
                builder.vertex(matrix, cursorX + 0.5f, baseY - 1, 0)
                        .color(208, 208, 208, 255).endVertex();
                builder.vertex(matrix, cursorX - 0.5f, baseY - 1, 0)
                        .color(208, 208, 208, 255).endVertex();
                builder.end();
                BufferUploader.end(builder);
                RenderSystem.enableTexture();
            } else {
                ModernTextRenderer.drawText(CURSOR_APPEND_CHARACTER, cursorX, baseY, color, true,
                        matrix, bufferSource, false, 0, LightTexture.FULL_BRIGHT);

                bufferSource.endBatch();
            }
        } else {
            bufferSource.endBatch();
        }
    }

    @Inject(method = "setCursorPosition", at = @At("RETURN"))
    public void E_setCursorPosition(int pos, CallbackInfo ci) {
        frame = 0;
    }
}
