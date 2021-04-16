/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.Paint;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelLoadingScreen.class)
public class MixinLevelLoadingScreen extends Screen {

    @Shadow
    @Final
    private StoringChunkProgressListener progressListener;

    private float mSweep;
    private float mTime;

    protected MixinLevelLoadingScreen(Component titleIn) {
        super(titleIn);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void render(PoseStack matrixStack, int scaledMouseX, int scaledMouseY, float deltaTick, CallbackInfo ci) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        Paint paint = Paint.take();
        paint.reset();
        paint.setStyle(Paint.Style.STROKE);
        paint.setFeatherRadius(2.0f);
        mTime += deltaTick;
        float amp = Math.abs((mTime % 32) - 16) * 0.125f;
        paint.setStrokeWidth(4.0f + amp);
        mSweep = Math.min(progressListener.getProgress() * 3.6f, mSweep + deltaTick * 12.0f);
        amp *= 0.5f;
        paint.setRGBA(64, 64, 64, 128);
        Canvas.getInstance().drawCircle(width / 2.0f, height / 2.0f - 36.0f, 15 + amp, paint);
        paint.setRGBA(255, 255, 255, 255);
        Canvas.getInstance().drawArc(width / 2.0f, height / 2.0f - 36.0f, 15 + amp, -90, mSweep, paint);
    }

    @Redirect(method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/gui/screens/LevelLoadingScreen;drawCenteredString(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/gui/Font;Ljava/lang/String;III)V"
            )
    )
    private void drawP(PoseStack matrixStack, Font fontRenderer, String progress, int x, int y, int color) {
        GuiComponent.drawCenteredString(matrixStack, fontRenderer, progress, x, y - 6, color);
    }
}
