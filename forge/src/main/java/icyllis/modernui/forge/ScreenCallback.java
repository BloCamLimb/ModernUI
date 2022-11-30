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

package icyllis.modernui.forge;

import com.mojang.blaze3d.platform.InputConstants;
import icyllis.modernui.annotation.*;
import icyllis.modernui.fragment.Fragment;
import icyllis.modernui.view.KeyEvent;
import net.minecraft.client.Minecraft;
import net.minecraftforge.common.capabilities.AutoRegisterCapability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

import javax.annotation.Nonnull;

/**
 * Callback of a screen. Methods will be invoked from different threads.
 * Make your main {@link Fragment} subclass implement this interface,
 * or provide via {@link ICapabilityProvider}, or to use defaults.
 *
 * @see MenuScreenFactory
 * @see MuiForgeApi#openScreen(Fragment)
 */
@AutoRegisterCapability
public interface ScreenCallback {

    /**
     * Determine whether the key event is considered as a back key.
     * A back key will perform back stack operation and close the screen.
     * <p>
     * Call Frequency: a key pressed at least.
     * <p>
     * Default value: Escape for {@link MuiForgeApi#openScreen(Fragment)},
     * Escape and Inventory Key (default is E) for {@link MenuScreenFactory}.
     *
     * @param keyCode the key code, like {@link KeyEvent#KEY_E} (equivalent to GLFW)
     * @param event   the key event
     * @return whether the key event is considered as a back key
     */
    @UiThread
    default boolean isBackKey(int keyCode, @Nonnull KeyEvent event) {
        if (keyCode == KeyEvent.KEY_ESCAPE)
            return true;
        InputConstants.Key key = InputConstants.getKey(keyCode, event.getScanCode());
        return Minecraft.getInstance().options.keyInventory.isActiveAndMatches(key);
    }

    /**
     * Should the screen be closed by the user. Otherwise, it can only be closed programmatically.
     * <p>
     * Default value: true
     *
     * @return whether the screen should close
     */
    @MainThread
    default boolean shouldClose() {
        return true;
    }

    /**
     * Determine whether the screen should pause the game. This only works in a single-player
     * world without opening to LAN. Menu screen never pause game.
     * <p>
     * Call Frequency: each tick.
     * <p>
     * Default value: true for {@link MuiForgeApi#openScreen(Fragment)},
     * false for {@link MenuScreenFactory}.
     *
     * @return whether to pause game
     */
    @MainThread
    default boolean isPauseScreen() {
        return false;
    }

    /**
     * Determine whether the screen should draw a default background. The background
     * can be configured according to user preference.
     * <p>
     * Call Frequency: each frame.
     * <p>
     * Default value: true
     *
     * @return whether to draw a default background
     */
    @RenderThread
    default boolean hasDefaultBackground() {
        return true;
    }

    /**
     * Determine whether the screen should blur the game scene when opened. The blur
     * can be configured according to user preference.
     * <p>
     * Call Frequency: the screen is showing.
     * <p>
     * Default value: true
     *
     * @return whether the game world should be blurred
     */
    @RenderThread
    default boolean shouldBlurBackground() {
        return true;
    }
}
