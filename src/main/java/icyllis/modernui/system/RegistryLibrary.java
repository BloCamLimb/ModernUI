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

package icyllis.modernui.system;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.registries.IForgeRegistry;

import javax.annotation.Nonnull;

/**
 * All variables are final, read-only
 */
public class RegistryLibrary {

    public static SoundEvent BUTTON_CLICK_1 = null;
    public static SoundEvent BUTTON_CLICK_2 = null;

    @OnlyIn(Dist.CLIENT)
    public static void registerSounds(IForgeRegistry<SoundEvent> registry) {
        BUTTON_CLICK_1 = registerSound(registry, "button1");
        BUTTON_CLICK_2 = registerSound(registry, "button2");
    }

    @Nonnull
    @OnlyIn(Dist.CLIENT)
    private static SoundEvent registerSound(@Nonnull IForgeRegistry<SoundEvent> registry, String soundName) {
        ResourceLocation soundID = new ResourceLocation(ModernUI.MODID, soundName);
        SoundEvent event = new SoundEvent(soundID).setRegistryName(soundID);
        registry.register(event);
        return event;
    }
}
