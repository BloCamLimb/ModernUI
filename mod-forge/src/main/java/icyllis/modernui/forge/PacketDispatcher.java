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

package icyllis.modernui.forge;

import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.function.Consumer;

/**
 * The packet dispatcher to broadcast a network packet to various clients.
 *
 * @see NetworkHandler#dispatch(FriendlyByteBuf)
 */
public final class PacketDispatcher {

    private static final Pool<PacketDispatcher> sPool = Pools.concurrent(5);

    private ClientboundCustomPayloadPacket mPacket;
    private final Consumer<ServerPlayer> mDispatcher = p -> p.connection.send(mPacket);

    private PacketDispatcher() {
    }

    @Nonnull
    static PacketDispatcher obtain(@Nonnull ResourceLocation id, @Nonnull FriendlyByteBuf data) {
        PacketDispatcher b = sPool.acquire();
        if (b == null) {
            b = new PacketDispatcher();
        } else if (b.mPacket != null) {
            throw new IllegalStateException("A previous packet was not dispatched: " + b.mPacket.getIdentifier());
        }
        b.mPacket = new ClientboundCustomPayloadPacket(id, data);
        return b;
    }

    private void check() {
        if (mPacket == null) {
            throw new IllegalStateException("The packet was already dispatched");
        }
    }

    private void recycle() {
        mPacket = null;
        sPool.release(this);
    }

    /**
     * Send a message to a player.
     *
     * @param player the player
     */
    public void sendToPlayer(@Nonnull Player player) {
        check();
        ((ServerPlayer) player).connection.send(mPacket);
        recycle();
    }

    /**
     * Send a message to a player.
     *
     * @param player the player
     */
    public void sendToPlayer(@Nonnull ServerPlayer player) {
        check();
        player.connection.send(mPacket);
        recycle();
    }

    /**
     * Send a message to all specific players.
     *
     * @param players players on server
     */
    public void sendToPlayers(@Nonnull Iterable<? extends Player> players) {
        check();
        for (Player player : players)
            ((ServerPlayer) player).connection.send(mPacket);
        recycle();
    }

    /**
     * Send a message to all players on the server.
     */
    public void sendToAll() {
        check();
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .broadcastAll(mPacket);
        recycle();
    }

    /**
     * Send a message to all players in the specified dimension.
     *
     * @param dimension dimension that players in
     */
    public void sendToDimension(@Nonnull ResourceKey<Level> dimension) {
        check();
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .broadcastAll(mPacket, dimension);
        recycle();
    }

    /**
     * Send a message to all players nearby a point with specified radius in specified dimension.
     *
     * @param excluded  the player excluded from broadcasting
     * @param x         target point x
     * @param y         target point y
     * @param z         target point z
     * @param radius    radius to target point
     * @param dimension dimension that target players in
     */
    public void sendToNear(@Nullable Player excluded, double x, double y, double z, double radius,
                           @Nonnull ResourceKey<Level> dimension) {
        check();
        ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcast(
                excluded, x, y, z, radius, dimension, mPacket);
        recycle();
    }

    /**
     * Send a message to all players tracking the specified entity. If a chunk that player loaded
     * on the client contains the chunk where the entity is located, and then the player is
     * tracking the entity changes.
     *
     * @param entity the entity is tracking
     */
    public void sendToTrackingEntity(@Nonnull Entity entity) {
        check();
        ((ServerLevel) entity.level).getChunkSource().broadcast(entity, mPacket);
        recycle();
    }

    /**
     * Send a message to all players tracking the specified entity, and also send the message to
     * the entity if it is a player. If a chunk that player loaded on the client contains the
     * chunk where the entity is located, and then the player is tracking the entity changes.
     *
     * @param entity the entity is tracking
     */
    public void sendToTrackingAndSelf(@Nonnull Entity entity) {
        check();
        ((ServerLevel) entity.level).getChunkSource().broadcastAndSend(entity, mPacket);
        recycle();
    }

    /**
     * Send a message to all players who are tracking the specified chunk.
     *
     * @param level the server level
     * @param pos   the block pos used to find the chunk
     */
    public void sendToTrackingChunk(@Nonnull Level level, @Nonnull BlockPos pos) {
        check();
        ((ServerLevel) level).getChunkSource().chunkMap.getPlayers(
                level.getChunk(pos).getPos(), /* boundaryOnly */ false).forEach(mDispatcher);
        recycle();
    }

    /**
     * Send a message to all players who are tracking the specified chunk.
     *
     * @param chunk the chunk that players in
     */
    public void sendToTrackingChunk(@Nonnull LevelChunk chunk) {
        check();
        ((ServerLevel) chunk.getLevel()).getChunkSource().chunkMap.getPlayers(
                chunk.getPos(), /* boundaryOnly */ false).forEach(mDispatcher);
        recycle();
    }
}
