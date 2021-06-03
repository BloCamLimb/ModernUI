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

import icyllis.modernui.forge.MuiRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

public class TestMenu extends AbstractContainerMenu {

    // client
    public TestMenu(int windowId, Inventory inventory, FriendlyByteBuf buf) {
        super(MuiRegistries.TEST_MENU, windowId);
    }

    // server
    public TestMenu(int windowId, Inventory inventory, Player player) {
        super(MuiRegistries.TEST_MENU, windowId);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }
}
