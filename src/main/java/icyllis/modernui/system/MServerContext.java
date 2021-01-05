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

package icyllis.modernui.system;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.registry.Registry;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;
import net.minecraftforge.fml.network.IContainerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Modern UI server service, both for dedicated server or integrated server
 */
public final class MServerContext {

    /**
     * Get the lifecycle of current server.
     *
     * @return {@code true} if server started
     */
    public static boolean isServerStarted() {
        return ServerHandler.INSTANCE.serverStarted;
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @throws ClassCastException this method is not called on server thread
     * @see #openMenu(PlayerEntity, IContainerProvider, Consumer)
     */
    public static void openMenu(@Nonnull PlayerEntity player, @Nonnull IContainerProvider constructor) {
        openMenu((ServerPlayerEntity) player, constructor, (Consumer<PacketBuffer>) null);
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @param pos         a data writer to send a block pos to client, this will be passed to
     *                    the menu supplier (IContainerFactory) that registered on client
     * @throws ClassCastException this method is not called on server thread
     * @see #openMenu(PlayerEntity, IContainerProvider, Consumer)
     * @see net.minecraftforge.common.extensions.IForgeContainerType#create(IContainerFactory)
     */
    public static void openMenu(@Nonnull PlayerEntity player, @Nonnull IContainerProvider constructor, @Nonnull BlockPos pos) {
        openMenu((ServerPlayerEntity) player, constructor, buf -> buf.writeBlockPos(pos));
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @param writer      a data writer to send additional data to client, this will be passed
     *                    to the menu supplier (IContainerFactory) that registered on client
     * @throws ClassCastException this method is not called on server thread
     * @see net.minecraftforge.common.extensions.IForgeContainerType#create(IContainerFactory)
     */
    public static void openMenu(@Nonnull PlayerEntity player, @Nonnull IContainerProvider constructor, @Nullable Consumer<PacketBuffer> writer) {
        openMenu((ServerPlayerEntity) player, constructor, writer);
    }

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the server player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @param writer      a data writer to send additional data to client, this will be passed
     *                    to the menu supplier (IContainerFactory) that registered on client
     * @see net.minecraftforge.common.extensions.IForgeContainerType#create(IContainerFactory)
     */
    public static void openMenu(@Nonnull ServerPlayerEntity player, @Nonnull IContainerProvider constructor, @Nullable Consumer<PacketBuffer> writer) {
        // do the same thing as ServerPlayer.openMenu()
        if (player.openContainer != player.container) {
            player.closeScreen();
        }
        player.getNextWindowId();
        int containerId = player.currentWindowId;
        Container menu = constructor.createMenu(containerId, player.inventory, player);
        if (menu == null) {
            return;
        }
        NetMessages.menu(containerId, Registry.MENU.getId(menu.getType()), writer).sendToPlayer(player);
        menu.addListener(player);
        player.openContainer = menu;
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(player, menu));
    }
}
