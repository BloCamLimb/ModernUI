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

package icyllis.modernui.mcimpl.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.player.LocalPlayer;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinIngameGui {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Shadow
    private int screenWidth;

    @Shadow
    private int screenHeight;

    @Shadow
    public abstract Font getFont();

    @Redirect(
            method = "renderExperienceBar",
            at = @At(value = "FIELD", target = "net/minecraft/client/player/LocalPlayer.experienceLevel:I", opcode = Opcodes.GETFIELD)
    )
    private int pass(LocalPlayer player) {
        return 0;
    }

    @Inject(method = "renderExperienceBar", at = @At("TAIL"))
    private void exp(PoseStack matrix, int i, CallbackInfo ci) {
        LocalPlayer player = minecraft.player;
        if (player != null && player.experienceLevel > 0) {
            String s = Integer.toString(player.experienceLevel);
            Font font = getFont();
            int x = (screenWidth - font.width(s)) / 2;
            int y = screenHeight - 31 - 4;
            font.draw(matrix, s, x + 0.5f, y, 0);
            font.draw(matrix, s, x - 0.5f, y, 0);
            font.draw(matrix, s, x, y + 0.5f, 0);
            font.draw(matrix, s, x, y - 0.5f, 0);
            font.draw(matrix, s, x, y, 0xff80ff20);
        }
    }
}
