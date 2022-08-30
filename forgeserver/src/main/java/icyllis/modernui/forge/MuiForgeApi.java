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

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuConstructor;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.PlayerContainerEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Public APIs for Minecraft Forge mods to Modern UI.
 *
 * @since 3.3
 */
public final class MuiForgeApi {

    private MuiForgeApi() {
    }

    /**
     * Get the lifecycle of current server. At most one server instance exists
     * at the same time, which may be integrated or dedicated.
     *
     * @return {@code true} if server started
     */
    public static boolean isServerStarted() {
        return ServerHandler.INSTANCE.mStarted;
    }

    /**
     * Open a container menu on server, generating a container id represents the next screen
     * (due to network latency). Then send a packet to the player to request the application
     * user interface on client. This method must be called from server thread.
     * <p>
     * This is served as a client/server interaction model, there must be a running server.
     *
     * @param player   the server player to open the screen for
     * @param provider a provider to create a menu on server side
     * @see #openMenu(Player, MenuConstructor, Consumer)
     * @see net.minecraftforge.common.extensions.IForgeMenuType#create(net.minecraftforge.network.IContainerFactory)
     */
    public static void openMenu(@Nonnull Player player, @Nonnull MenuConstructor provider) {
        openMenu(player, provider, (Consumer<FriendlyByteBuf>) null);
    }

    /**
     * Open a container menu on server, generating a container id represents the next screen
     * (due to network latency). Then send a packet to the player to request the application
     * user interface on client. This method must be called from server thread.
     * <p>
     * This is served as a client/server interaction model, there must be a running server.
     *
     * @param player   the server player to open the screen for
     * @param provider a provider to create a menu on server side
     * @param pos      a block pos to send to client, this will be passed to
     *                 the menu supplier that registered on client
     * @see #openMenu(Player, MenuConstructor, Consumer)
     * @see net.minecraftforge.common.extensions.IForgeMenuType#create(net.minecraftforge.network.IContainerFactory)
     */
    public static void openMenu(@Nonnull Player player, @Nonnull MenuConstructor provider, @Nonnull BlockPos pos) {
        openMenu(player, provider, buf -> buf.writeBlockPos(pos));
    }

    /**
     * Open a container menu on server, generating a container id represents the next screen
     * (due to network latency). Then send a packet to the player to request the application
     * user interface on client. This method must be called from server thread.
     * <p>
     * This is served as a client/server interaction model, there must be a running server.
     *
     * @param player   the server player to open the screen for
     * @param provider a provider to create a menu on server side
     * @param writer   a data writer to send additional data to client, this will be passed
     *                 to the menu supplier (IContainerFactory) that registered on client
     * @see net.minecraftforge.common.extensions.IForgeMenuType#create(net.minecraftforge.network.IContainerFactory)
     */
    public static void openMenu(@Nonnull Player player, @Nonnull MenuConstructor provider,
                                @Nullable Consumer<FriendlyByteBuf> writer) {
        if (!(player instanceof ServerPlayer p)) {
            ModernUIForge.LOGGER.warn(ModernUIForge.MARKER, "openMenu() is not called from logical server",
                    new Exception().fillInStackTrace());
            return;
        }
        // do the same thing as ServerPlayer.openMenu()
        if (p.containerMenu != p.inventoryMenu) {
            p.closeContainer();
        }
        p.nextContainerCounter();
        AbstractContainerMenu menu = provider.createMenu(p.containerCounter, p.getInventory(), p);
        if (menu == null) {
            return;
        }
        NetworkMessages.openMenu(menu, writer).sendToPlayer(p);
        p.initMenu(menu);
        p.containerMenu = menu;
        MinecraftForge.EVENT_BUS.post(new PlayerContainerEvent.Open(p, menu));
    }
}
