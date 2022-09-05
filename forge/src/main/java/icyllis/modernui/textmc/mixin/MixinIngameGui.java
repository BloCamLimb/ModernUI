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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import icyllis.modernui.textmc.ModernStringSplitter;
import icyllis.modernui.textmc.ModernTextRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinIngameGui {

    @Shadow
    @Final
    protected Minecraft minecraft;

    @Shadow
    protected int screenWidth;

    @Shadow
    protected int screenHeight;

    @Shadow
    public abstract Font getFont();

    @Redirect(
            method = "renderExperienceBar",
            at = @At(value = "FIELD", target = "net/minecraft/client/player/LocalPlayer.experienceLevel:I", opcode =
                    Opcodes.GETFIELD)
    )
    private int fakeExperience(LocalPlayer player) {
        return 0;
    }

    @Inject(method = "renderExperienceBar", at = @At("TAIL"))
    private void drawExperience(PoseStack matrix, int i, CallbackInfo ci) {
        LocalPlayer player = minecraft.player;
        if (player != null && player.experienceLevel > 0) {
            Component s = Component.literal(Integer.toString(player.experienceLevel));
            float w = ModernStringSplitter.measureText(s);
            float x = (screenWidth - w) / 2;
            int y = screenHeight - 31 - 4;
            MultiBufferSource.BufferSource source = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
            ModernTextRenderer.drawText8xOutline(s, x, y, 0xff80ff20, 0xff000000, matrix.last().pose(), source);
            source.endBatch();
        }
    }
}
