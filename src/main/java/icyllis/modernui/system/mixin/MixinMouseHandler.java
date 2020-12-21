/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
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

package icyllis.modernui.system.mixin;

import icyllis.modernui.view.UIManager;
import net.minecraft.client.MouseHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHelper.class)
public class MixinMouseHandler {

    /*@Inject(method = "mouseButtonCallback",
            at = @At(value = "JUMP",
                    opcode = Opcodes.IFEQ,
                    shift = At.Shift.BY,
                    by = -2,
                    ordinal = 0
            ),
            slice = @Slice(from = @At(value = "INVOKE",
                    target = "Lnet/minecraft/client/MainWindow;getHeight()I")
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private void onMouseButtonEvent(long handle, int button, int action, int mods, CallbackInfo ci,
                                    boolean flag, int i, boolean[] aboolean, double d0, double d1) {
    }*/

    private final UIManager.InputEventReceiver mInputEventReceiver = MixinHooks.C.inputEventReceiver;

    /**
     * Capture the horizontal scroll offset
     */
    @Inject(method = "scrollCallback", at = @At("HEAD"))
    private void onScrollCallback(long handle, double xoffset, double yoffset, CallbackInfo ci) {
        mInputEventReceiver.onScrollCallback(xoffset, yoffset);
    }
}
