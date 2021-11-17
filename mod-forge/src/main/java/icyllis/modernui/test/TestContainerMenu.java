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

package icyllis.modernui.test;

import icyllis.modernui.forge.MuiForgeBridge;
import icyllis.modernui.forge.MuiRegistries;
import icyllis.modernui.mcgui.ContainerMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuConstructor;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

public class TestContainerMenu extends ContainerMenu {

    /**
     * Constructor to create the container menu on client side.
     *
     * @param containerId the id representing the menu in communication model
     * @param inventory   player inventory (on client side)
     * @param data        additional data sent by server
     * @see MuiForgeBridge#openMenu(Player, MenuConstructor, Consumer)
     */
    public TestContainerMenu(int containerId, @Nonnull Inventory inventory, @Nonnull FriendlyByteBuf data) {
        super(MuiRegistries.TEST_MENU, containerId);
    }

    /**
     * Constructor to create the container menu on server side.
     *
     * @param containerId the id representing the menu in communication model
     * @param inventory   player inventory (on server side)
     * @param player      server player
     * @see MuiForgeBridge#openMenu(Player, MenuConstructor, Consumer)
     */
    public TestContainerMenu(int containerId, @Nonnull Inventory inventory, @Nonnull Player player) {
        super(MuiRegistries.TEST_MENU, containerId);
    }

    /**
     * Called when the container menu is closed, on both side.
     *
     * @param player the player using this menu
     */
    @Override
    public void removed(@Nonnull Player player) {
        super.removed(player);
    }
}
