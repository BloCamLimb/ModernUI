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

package icyllis.modernui.core.forge;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class MuiRegistries {

    /**
     * Sounds
     */
    public static SoundEvent BUTTON_CLICK_1;
    public static SoundEvent BUTTON_CLICK_2;

    /**
     * Container Menus (Debug Only)
     */
    public static MenuType<?> TEST_MENU;

    /**
     * Items (Production Only)
     */
    public static Item PROJECT_BUILDER_ITEM;
}
