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

package icyllis.modernui.ui.example;

import icyllis.modernui.system.RegistryLib;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

// container should be not in UI package
public class ContainerTest extends Container {

    // client
    public ContainerTest(int windowId, PlayerInventory inv, PacketBuffer buf) {
        super(RegistryLib.TEST_CONTAINER, windowId);
    }

    // server
    public ContainerTest(int windowId, PlayerInventory inv, PlayerEntity player) {
        super(RegistryLib.TEST_CONTAINER, windowId);
    }

    @Override
    public void onContainerClosed(PlayerEntity playerIn) {
        super.onContainerClosed(playerIn);
        //ModernUI.LOGGER.debug("Test Container - Closed on {}", EffectiveSide.get());
    }

    @Override
    public boolean canInteractWith(@Nonnull PlayerEntity playerIn) {
        return true;
    }

    public static class Provider implements INamedContainerProvider {

        @Nonnull
        @Override
        public ITextComponent getDisplayName() {
            return new StringTextComponent("test");
        }

        @Nullable
        @Override
        public Container createMenu(int windowId, @Nonnull PlayerInventory playerInventory, @Nonnull PlayerEntity playerEntity) {
            return new ContainerTest(windowId, playerInventory, playerEntity);
        }
    }
}
