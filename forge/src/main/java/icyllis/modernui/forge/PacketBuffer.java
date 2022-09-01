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

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * The packet dispatcher to broadcast a network packet to various clients.
 *
 * @see NetworkHandler#buffer(int)
 */
public final class PacketBuffer extends FriendlyByteBuf {

    private final ResourceLocation mName;

    PacketBuffer(ResourceLocation name) {
        super(Unpooled.buffer());
        mName = name;
    }

    /**
     * Send the message to server.
     * <p>
     * This is the only method to be called on the client. Packet data cannot exceed 32,600 bytes.
     */
    @OnlyIn(Dist.CLIENT)
    public void sendToServer() {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(mName, this));
        } else {
            release(); // do not wait for GC
        }
    }

    /**
     * Send the message to a player.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore.
     *
     * @param player the player
     */
    public void sendToPlayer(@Nonnull Player player) {
        ((ServerPlayer) player).connection.send(new ClientboundCustomPayloadPacket(mName, this));
    }

    /**
     * Send the message to a player.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore.
     *
     * @param player the player
     */
    public void sendToPlayer(@Nonnull ServerPlayer player) {
        player.connection.send(new ClientboundCustomPayloadPacket(mName, this));
    }

    /**
     * Send the message to all specific players.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore.
     *
     * @param players players on server
     */
    public void sendToPlayers(@Nonnull Iterable<? extends Player> players) {
        Packet<?> packet = new ClientboundCustomPayloadPacket(mName, this);
        for (Player player : players) {
            ((ServerPlayer) player).connection.send(packet);
        }
    }

    /**
     * Send the message to all players on the server.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore.
     */
    public void sendToAll() {
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .broadcastAll(new ClientboundCustomPayloadPacket(mName, this));
    }

    /**
     * Send the message to all players in the specified dimension.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore.
     *
     * @param dimension dimension that players in
     */
    public void sendToDimension(@Nonnull ResourceKey<Level> dimension) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .broadcastAll(new ClientboundCustomPayloadPacket(mName, this), dimension);
    }

    /**
     * Send the message to all players nearby a point with specified radius in specified dimension.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore.
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
        ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcast(
                excluded, x, y, z, radius, dimension, new ClientboundCustomPayloadPacket(mName, this));
    }

    /**
     * Send the message to all players tracking the specified entity. If a chunk that player loaded
     * on the client contains the chunk where the entity is located, and then the player is
     * tracking the entity changes.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore.
     *
     * @param entity the entity is tracking
     */
    public void sendToTrackingEntity(@Nonnull Entity entity) {
        ((ServerLevel) entity.level).getChunkSource().broadcast(entity,
                new ClientboundCustomPayloadPacket(mName, this));
    }

    /**
     * Send the message to all players tracking the specified entity, and also send the message to
     * the entity if it is a player. If a chunk that player loaded on the client contains the
     * chunk where the entity is located, and then the player is tracking the entity changes.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore.
     *
     * @param entity the entity is tracking
     */
    public void sendToTrackingAndSelf(@Nonnull Entity entity) {
        ((ServerLevel) entity.level).getChunkSource().broadcastAndSend(entity,
                new ClientboundCustomPayloadPacket(mName, this));
    }

    /**
     * Send the message to all players who are tracking the specified chunk.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore.
     *
     * @param level the server level
     * @param pos   the block pos used to find the chunk
     */
    public void sendToTrackingChunk(@Nonnull Level level, @Nonnull BlockPos pos) {
        Packet<?> packet = new ClientboundCustomPayloadPacket(mName, this);
        ((ServerLevel) level).getChunkSource().chunkMap.getPlayers(
                level.getChunk(pos).getPos(), /* boundaryOnly */ false).forEach(p -> p.connection.send(packet));
    }

    /**
     * Send the message to all players who are tracking the specified chunk.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore.
     *
     * @param chunk the chunk that players in
     */
    public void sendToTrackingChunk(@Nonnull LevelChunk chunk) {
        Packet<?> packet = new ClientboundCustomPayloadPacket(mName, this);
        ((ServerLevel) chunk.getLevel()).getChunkSource().chunkMap.getPlayers(
                chunk.getPos(), /* boundaryOnly */ false).forEach(p -> p.connection.send(packet));
    }
}
