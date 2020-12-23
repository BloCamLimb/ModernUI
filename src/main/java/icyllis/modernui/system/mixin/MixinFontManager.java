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

import icyllis.modernui.font.ModernFontRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.fonts.Font;
import net.minecraft.client.gui.fonts.FontResourceManager;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Function;

@Mixin(FontResourceManager.class)
public class MixinFontManager {

    @Redirect(method = "func_238548_a_",
            at = @At(value = "NEW", target = "net/minecraft/client/gui/FontRenderer"))
    private FontRenderer createFontRenderer(Function<ResourceLocation, Font> fonts) {
        return ModernFontRenderer.create(fonts);
    }
}
