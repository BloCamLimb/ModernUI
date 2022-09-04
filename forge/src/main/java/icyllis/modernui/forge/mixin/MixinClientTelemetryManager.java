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

import com.mojang.authlib.minecraft.TelemetrySession;
import com.mojang.authlib.minecraft.UserApiService;
import icyllis.modernui.forge.ModernUIForge;
import net.minecraft.client.ClientTelemetryManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.concurrent.Executor;

@Mixin(ClientTelemetryManager.class)
public class MixinClientTelemetryManager {

    @Redirect(method = "<init>",
            at = @At(value = "INVOKE",
                    target = "Lcom/mojang/authlib/minecraft/UserApiService;newTelemetrySession" +
                            "(Ljava/util/concurrent/Executor;)Lcom/mojang/authlib/minecraft/TelemetrySession;",
                    remap = false))
    private TelemetrySession onCreateTelemetrySession(UserApiService service, Executor executor) {
        if (ModernUIForge.sRemoveTelemetrySession) {
            return TelemetrySession.DISABLED;
        } else {
            return service.newTelemetrySession(executor);
        }
    }
}
