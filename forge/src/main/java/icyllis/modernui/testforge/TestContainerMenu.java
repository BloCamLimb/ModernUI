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

package icyllis.modernui.testforge;

import icyllis.modernui.forge.MuiForgeApi;
import icyllis.modernui.forge.MuiRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import java.util.function.Consumer;

/**
 * Example.
 */
public class TestContainerMenu extends AbstractContainerMenu {

    private boolean mDiamond;

    /**
     * This constructor should be only used on client.
     * Indicates that this menu is only used for view layout without any network communication.
     */
    @OnlyIn(Dist.CLIENT)
    public TestContainerMenu() {
        super(null, 0);
    }

    /**
     * Constructor to create the container menu on client side.
     * <p>
     * This constructor is used with container menus with network communication.
     * The initiator must be server, and the client uses the same constructor and arguments.
     * <p>
     * The container ID generated by the server to avoid operating the dead menu in the
     * network packet due to network latency.
     * <p>
     * On the server, you should call {@link MuiForgeApi#openMenu(Player, MenuConstructor, Consumer)},
     * whose {@link MenuConstructor} provides you container id. On the client, you should
     * register the menu type via {@link net.minecraftforge.common.extensions.IForgeMenuType} along
     * with {@link net.minecraftforge.registries.RegisterEvent},
     * whose {@link net.minecraftforge.network.IContainerFactory} provides you container id and
     * the additional data sent by server.
     *
     * @param containerId the id representing the menu in communication model
     * @param inventory   player inventory (on client side)
     * @param data        additional data sent by server
     * @see MuiForgeApi#openMenu(Player, MenuConstructor, Consumer)
     */
    public TestContainerMenu(int containerId, @Nonnull Inventory inventory, @Nonnull FriendlyByteBuf data) {
        super(MuiRegistries.TEST_MENU.get(), containerId);
        mDiamond = data.readBoolean();
    }

    /**
     * Constructor to create the container menu on server side.
     * <p>
     * This constructor is used with container menus with network communication.
     * The initiator must be server, and the client uses the same constructor and arguments.
     * <p>
     * The container ID generated by the server to avoid operating the dead menu in the
     * network packet due to network latency.
     * <p>
     * On the server, you should call {@link MuiForgeApi#openMenu(Player, MenuConstructor, Consumer)},
     * whose {@link MenuConstructor} provides you container id. On the client, you should
     * register the menu type via {@link net.minecraftforge.common.extensions.IForgeMenuType} along
     * with {@link net.minecraftforge.registries.RegisterEvent},
     * whose {@link net.minecraftforge.network.IContainerFactory} provides you container id and
     * the additional data sent by server.
     *
     * @param containerId the id representing the menu in communication model
     * @param inventory   player inventory (on server side)
     * @param player      server player
     * @see MuiForgeApi#openMenu(Player, MenuConstructor, Consumer)
     */
    public TestContainerMenu(int containerId, @Nonnull Inventory inventory, @Nonnull Player player) {
        super(MuiRegistries.TEST_MENU.get(), containerId);
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

    /**
     * This method will be called every tick to determine
     * whether the menu should be closed intrinsically. This is a server-side logic.
     * Unless you use {@link #TestContainerMenu()}, you should implement this method.
     * Otherwise, it only depends on certain behaviors of the client.
     *
     * @param player the player using this menu (should be server player)
     * @return {@code false} to close this menu on server, also send a packet to client
     */
    @Override
    public boolean stillValid(@Nonnull Player player) {
        return true;
    }

    public boolean isDiamond() {
        return mDiamond;
    }

    @Deprecated
    @Nonnull
    @Override
    protected final DataSlot addDataSlot(@Nonnull DataSlot intValue) {
        throw new UnsupportedOperationException();
    }

    @Deprecated
    @Override
    protected final void addDataSlots(@Nonnull ContainerData array) {
        throw new UnsupportedOperationException();
    }

    @Nonnull
    @Override
    public ItemStack quickMoveStack(@Nonnull Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Deprecated
    @Override
    public final void setData(int id, int data) {
        throw new UnsupportedOperationException();
    }
}
