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

package icyllis.modernui.network;

import icyllis.modernui.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.network.play.ClientPlayNetHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCustomPayloadPacket;
import net.minecraft.network.play.server.SCustomPayloadPlayPacket;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkInstance;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Modern UI does not require a network channel, you can copy this class at your disposal
 */
@SuppressWarnings("unused")
public enum NetworkHandler {
    INSTANCE(ModernUI.MODID, "main_network");

    private final NetworkInstance network;

    private final String protocol;

    NetworkHandler(@Nonnull String modid, @Nonnull String name) {
        // get protocol first
        protocol = UUID.nameUUIDFromBytes(ModList.get().getModFileById(modid).getMods().stream()
                .map(iModInfo -> iModInfo.getVersion().getQualifier())
                .collect(Collectors.joining(",")).getBytes(StandardCharsets.UTF_8)).toString();
        NetworkRegistry.ChannelBuilder builder = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(modid, name))
                .networkProtocolVersion(this::getProtocolVersion)
                .clientAcceptedVersions(this::verifyServerProtocol)
                .serverAcceptedVersions(this::verifyClientProtocol);
        try {
            network = (NetworkInstance) ObfuscationReflectionHelper.findMethod(
                    NetworkRegistry.ChannelBuilder.class,
                    "createNetworkInstance").invoke(builder);
        } catch (IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Add network event listeners
     */
    public void addListeners() {
        network.addListener(this::onS2CMessageReceived);
        network.addListener(this::onC2SMessageReceived);
    }

    /**
     * Get the protocol version of this channel on current side
     *
     * @return the protocol
     */
    public String getProtocolVersion() {
        return protocol;
    }

    /**
     * This method will run on client to verify the server protocol that sent by handshake network channel
     *
     * @param serverProtocol the protocol of this channel sent from server side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    private boolean verifyServerProtocol(@Nonnull String serverProtocol) {
        return serverProtocol.equals(protocol) || serverProtocol.equals(NetworkRegistry.ABSENT);
    }

    /**
     * This method will run on server to verify the remote client protocol that sent by handshake network channel
     *
     * @param clientProtocol the protocol of this channel sent from client side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    private boolean verifyClientProtocol(@Nonnull String clientProtocol) {
        return clientProtocol.equals(protocol) || clientProtocol.equals(NetworkRegistry.ABSENT)
                || clientProtocol.equals(NetworkRegistry.ACCEPTVANILLA);
    }

    private void onS2CMessageReceived(NetworkEvent.ServerCustomPayloadEvent event) {
        // received on main thread of effective side
        try {
            S2CMsgHandler.CONSUMERS[event.getPayload().readUnsignedByte()]
                    .handle(event.getPayload(), Minecraft.getInstance().player);
        } catch (RuntimeException e) {
            ModernUI.LOGGER.warn("An error occurred while handling server-to-client message", e);
        }
        event.getPayload().release();
        event.getSource().get().setPacketHandled(true);
    }

    private void onC2SMessageReceived(NetworkEvent.ClientCustomPayloadEvent event) {
        // received on main thread of effective side
        try {
            C2SMsgHandler.CONSUMERS[event.getPayload().readUnsignedByte()]
                    .handle(event.getPayload(), event.getSource().get().getSender());
        } catch (RuntimeException e) {
            ModernUI.LOGGER.warn("An error occurred while handling client-to-server message", e);
        }
        event.getSource().get().setPacketHandled(true);
    }

    /**
     * Send a message to server, call this on client side
     *
     * @param buffer message to send
     */
    @OnlyIn(Dist.CLIENT)
    public void sendToServer(PacketBuffer buffer) {
        ClientPlayNetHandler connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendPacket(new CCustomPayloadPacket(network.getChannelName(), buffer));
        }
    }

    /**
     * Send a message to a player
     *
     * @param buffer message to send
     * @param player player entity on server
     */
    public void sendToPlayer(PacketBuffer buffer, @Nonnull PlayerEntity player) {
        ((ServerPlayerEntity) player).connection.sendPacket(new SCustomPayloadPlayPacket(network.getChannelName(), buffer));
    }

    /**
     * Send a message to all specific players
     *
     * @param buffer  message to send
     * @param players players on server
     */
    public void sendToPlayers(PacketBuffer buffer, @Nonnull Iterable<? extends PlayerEntity> players) {
        final IPacket<?> packet = new SCustomPayloadPlayPacket(network.getChannelName(), buffer);
        for (PlayerEntity player : players) {
            ((ServerPlayerEntity) player).connection.sendPacket(packet);
        }
    }

    /**
     * Send a message to all players on the server
     *
     * @param buffer message to send
     */
    public void sendToAll(PacketBuffer buffer) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .sendPacketToAllPlayers(new SCustomPayloadPlayPacket(network.getChannelName(), buffer));
    }

    /**
     * Send a message to all players in specified dimension
     *
     * @param buffer    message to send
     * @param dimension dimension that players in
     */
    public void sendToDimension(PacketBuffer buffer, @Nonnull RegistryKey<World> dimension) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .func_232642_a_(new SCustomPayloadPlayPacket(network.getChannelName(), buffer), dimension);
    }

    /**
     * Send a message to all players nearby a point with specified radius in specified dimension
     *
     * @param buffer    message to send
     * @param excluded  excluded player to send the packet
     * @param x         target point x
     * @param y         target point y
     * @param z         target point z
     * @param radius    radius to target point
     * @param dimension dimension that players in
     */
    public void sendToAllNear(PacketBuffer buffer, @Nullable ServerPlayerEntity excluded,
                              double x, double y, double z, double radius,
                              @Nonnull RegistryKey<World> dimension) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().sendToAllNearExcept(excluded,
                x, y, z, radius, dimension, new SCustomPayloadPlayPacket(network.getChannelName(), buffer));
    }

    /**
     * Send a message to all players tracking the specified entity. If a chunk that player loaded
     * on the client contains the chunk where the entity is located, and then the player is
     * tracking the entity.
     *
     * @param buffer message to send
     * @param entity entity is tracking
     */
    public void sendToTrackingEntity(PacketBuffer buffer, @Nonnull Entity entity) {
        ((ServerWorld) entity.getEntityWorld()).getChunkProvider().sendToAllTracking(
                entity, new SCustomPayloadPlayPacket(network.getChannelName(), buffer));
    }

    /**
     * Send a message to all players tracking the specified entity, and also send the message to
     * the entity if it is a player. If a chunk that player loaded on the client contains the
     * chunk where the entity is located, and then the player is tracking the entity.
     *
     * @param buffer message to send
     * @param entity entity is tracking
     */
    public void sendToTrackingAndSelf(PacketBuffer buffer, @Nonnull Entity entity) {
        ((ServerWorld) entity.getEntityWorld()).getChunkProvider().sendToTrackingAndSelf(
                entity, new SCustomPayloadPlayPacket(network.getChannelName(), buffer));
    }

    /**
     * Send a message to all players who loaded the specified chunk
     *
     * @param buffer message to send
     * @param chunk  chunk that players in
     */
    public void sendToTrackingChunk(PacketBuffer buffer, @Nonnull Chunk chunk) {
        final IPacket<?> packet = new SCustomPayloadPlayPacket(network.getChannelName(), buffer);
        ((ServerWorld) chunk.getWorld()).getChunkProvider().chunkManager.getTrackingPlayers(
                chunk.getPos(), false).forEach(player -> player.connection.sendPacket(packet));
    }
}
