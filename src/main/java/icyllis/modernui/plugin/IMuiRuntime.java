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

package icyllis.modernui.plugin;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.container.IContainerProvider;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.network.IContainerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * Modern UI system service, process server logic and send relevant packets
 * to client.
 */
public interface IMuiRuntime {

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @throws ClassCastException this method is not called on server thread
     * @see #openGui(PlayerEntity, IContainerProvider, Consumer)
     */
    void openGui(@Nonnull PlayerEntity player, @Nonnull IContainerProvider constructor);

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @param pos         a block pos as the additional data, this will
     *                    be passed to the menu supplier that registered on client
     * @throws ClassCastException this method is not called on server thread
     * @see #openGui(PlayerEntity, IContainerProvider, Consumer)
     * @see net.minecraftforge.common.extensions.IForgeContainerType#create(IContainerFactory)
     */
    void openGui(@Nonnull PlayerEntity player, @Nonnull IContainerProvider constructor, @Nonnull BlockPos pos);

    /**
     * Open a container menu on server, generate an id represents the next screen.
     * Then send a packet to player to request to open an application screen on client.
     *
     * @param player      the player to open the screen for
     * @param constructor a constructor to create a menu on server side
     * @param dataWriter  a data writer to send additional data to client, this will
     *                    be passed to the menu supplier that registered on client
     * @throws ClassCastException this method is not called on server thread
     * @see net.minecraftforge.common.extensions.IForgeContainerType#create(IContainerFactory)
     */
    void openGui(@Nonnull PlayerEntity player, @Nonnull IContainerProvider constructor, @Nullable Consumer<PacketBuffer> dataWriter);
}
