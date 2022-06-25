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

package icyllis.modernui.forge.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.ModernUI;
import icyllis.modernui.core.Core;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.Configuration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Objects;
import java.util.function.LongSupplier;

@Mixin(RenderSystem.class)
public class MixinRenderSystem {

    @Inject(method = "initBackendSystem", at = @At("HEAD"))
    private static void onInitBackendSystem(CallbackInfoReturnable<LongSupplier> ci) {
        RenderSystem.assertInInitPhase();
        String name = Configuration.OPENGL_LIBRARY_NAME.get();
        if (name != null) {
            // non-system library should load before window creation
            ModernUI.LOGGER.info(ModernUI.MARKER, "OpenGL library: {}", name);
            Objects.requireNonNull(GL.getFunctionProvider(), "Implicit loading is required");
        }
    }

    @Inject(method = "initRenderer", at = @At("TAIL"))
    private static void onInitRenderer(int debugLevel, boolean debugSync, CallbackInfo ci) {
        Core.initMainThread();
        Core.initOpenGL();
    }
}
