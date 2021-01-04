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
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.player.ClientPlayerEntity;
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
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * For handling a network channel more faster, you can create an instance for your mod
 */
@SuppressWarnings("unused")
public class NetworkHandler {

    private final ResourceLocation channel;
    private final String protocol;

    // internal value
    private boolean optional;

    @Nullable
    private final IClientMsgHandler clientHandler;
    @Nullable
    private final IServerMsgHandler serverHandler;

    // a ByteBuf wrapper for write data more friendly
    private PacketBuffer buffer;

    /**
     * Create a network handler of a mod. This is a dist-sensitive operation,
     * you may consider the following example:
     *
     * <pre>
     * network = new NetworkHandler(ModernUI.MODID, "main_network", DistExecutor.safeRunForDist(() -> NetMessages::handle, () -> NetMessages::ignore), NetMessages::handle);
     * </pre>
     *
     * @param modid         mod id
     * @param name          network channel name
     * @param clientHandler a handler to handle server-to-client messages
     * @param serverHandler a handler to handle client-to-server messages
     * @see net.minecraftforge.fml.DistExecutor
     * @see icyllis.modernui.system.NetMessages
     */
    public NetworkHandler(@Nonnull String modid, @Nonnull String name,
                          @Nullable IClientMsgHandler clientHandler, @Nullable IServerMsgHandler serverHandler) {
        protocol = UUID.nameUUIDFromBytes(ModList.get().getModFileById(modid).getMods().stream()
                .map(iModInfo -> iModInfo.getVersion().getQualifier())
                .collect(Collectors.joining(",")).getBytes(StandardCharsets.UTF_8)).toString();
        this.clientHandler = clientHandler;
        this.serverHandler = serverHandler;
        EventNetworkChannel network = NetworkRegistry.ChannelBuilder
                .named(channel = new ResourceLocation(modid, name))
                .networkProtocolVersion(this::getProtocolVersion)
                .clientAcceptedVersions(this::checkS2CProtocol)
                .serverAcceptedVersions(this::checkC2SProtocol)
                .eventNetworkChannel();
        if (FMLEnvironment.dist.isClient()) {
            network.addListener(this::onS2CMessageReceived);
        }
        network.addListener(this::onC2SMessageReceived);
    }

    /**
     * Allocate a buffer to write packet data with index.  The packet must
     * be dispatched later, for example {@link #sendToPlayer(PlayerEntity)}
     *
     * @param index The message index used on the opposite side, range from 0 to 32767
     * @return A byte buf to write the packet data (message)
     * @see IClientMsgHandler
     * @see IServerMsgHandler
     */
    @Nonnull
    public PacketBuffer allocBuf(int index) {
        if (buffer != null) {
            throw new IllegalStateException("Previous payload was not dispatched");
        }
        buffer = new PacketBuffer(Unpooled.buffer());
        buffer.writeShort(index);
        return buffer;
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
    public boolean checkS2CProtocol(@Nonnull String serverProtocol) {
        boolean allowAbsent = optional && serverProtocol.equals(NetworkRegistry.ABSENT);
        if (allowAbsent) {
            ModernUI.LOGGER.debug(ModernUI.MARKER, "Connecting to a server that does not have {} channel available", channel);
        }
        return allowAbsent || serverProtocol.equals(protocol);
    }

    /**
     * This method will run on server to verify the remote client protocol that sent by handshake network channel
     *
     * @param clientProtocol the protocol of this channel sent from client side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    public boolean checkC2SProtocol(@Nonnull String clientProtocol) {
        if (clientProtocol.equals(NetworkRegistry.ACCEPTVANILLA)) {
            return false;
        }
        return clientProtocol.equals(protocol) || optional && clientProtocol.equals(NetworkRegistry.ABSENT);
    }

    @OnlyIn(Dist.CLIENT)
    private void onS2CMessageReceived(NetworkEvent.ServerCustomPayloadEvent event) {
        // received on main thread of effective side
        if (clientHandler != null) {
            try {
                clientHandler.handle(event.getPayload().readShort(), event.getPayload(), Minecraft.getInstance().player);
            } catch (Exception e) {
                ModernUI.LOGGER.error(ModernUI.MARKER, "An error occurred while handling server-to-client message", e);
            }
        }
        event.getPayload().release(); // forge disabled this on client
        event.getSource().get().setPacketHandled(true);
    }

    private void onC2SMessageReceived(NetworkEvent.ClientCustomPayloadEvent event) {
        // received on main thread of effective side
        if (serverHandler != null) {
            try {
                serverHandler.handle(event.getPayload().readShort(), event.getPayload(), event.getSource().get().getSender());
            } catch (Exception e) {
                ModernUI.LOGGER.error(ModernUI.MARKER, "An error occurred while handling client-to-server message", e);
            }
        }
        event.getSource().get().setPacketHandled(true);
    }

    /**
     * Send a message to server
     * <p>
     * This is the only method to be called on the client, the rest needs
     * to be called on the server side
     */
    @OnlyIn(Dist.CLIENT)
    public void sendToServer() {
        ClientPlayNetHandler connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.sendPacket(new CCustomPayloadPacket(channel, buffer));
        }
        buffer = null;
    }

    /**
     * Send a message to a player
     *
     * @param player the server player
     */
    public void sendToPlayer(@Nonnull PlayerEntity player) {
        ((ServerPlayerEntity) player).connection.sendPacket(new SCustomPayloadPlayPacket(channel, buffer));
        buffer = null;
    }

    /**
     * Send a message to a player
     *
     * @param player the server player
     */
    public void sendToPlayer(@Nonnull ServerPlayerEntity player) {
        player.connection.sendPacket(new SCustomPayloadPlayPacket(channel, buffer));
        buffer = null;
    }

    /**
     * Send a message to all specific players
     *
     * @param players players on server
     */
    public void sendToPlayers(@Nonnull Iterable<? extends PlayerEntity> players) {
        final IPacket<?> packet = new SCustomPayloadPlayPacket(channel, buffer);
        for (PlayerEntity player : players) {
            ((ServerPlayerEntity) player).connection.sendPacket(packet);
        }
        buffer = null;
    }

    /**
     * Send a message to all players on the server
     */
    public void sendToAll() {
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .sendPacketToAllPlayers(new SCustomPayloadPlayPacket(channel, buffer));
        buffer = null;
    }

    /**
     * Send a message to all players in specified dimension
     *
     * @param dimension dimension that players in
     */
    public void sendToDimension(@Nonnull RegistryKey<World> dimension) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .func_232642_a_(new SCustomPayloadPlayPacket(channel, buffer), dimension);
        buffer = null;
    }

    /**
     * Send a message to all players nearby a point with specified radius in specified dimension
     *
     * @param excluded  the player that will not be sent the packet
     * @param x         target point x
     * @param y         target point y
     * @param z         target point z
     * @param radius    radius to target point
     * @param dimension dimension that players in
     */
    public void sendToAllNear(@Nullable ServerPlayerEntity excluded,
                              double x, double y, double z, double radius,
                              @Nonnull RegistryKey<World> dimension) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().sendToAllNearExcept(excluded,
                x, y, z, radius, dimension, new SCustomPayloadPlayPacket(channel, buffer));
        buffer = null;
    }

    /**
     * Send a message to all players tracking the specified entity. If a chunk that player loaded
     * on the client contains the chunk where the entity is located, and then the player is
     * tracking the entity changes.
     *
     * @param entity entity is tracking
     */
    public void sendToTrackingEntity(@Nonnull Entity entity) {
        ((ServerWorld) entity.getEntityWorld()).getChunkProvider().sendToAllTracking(
                entity, new SCustomPayloadPlayPacket(channel, buffer));
        buffer = null;
    }

    /**
     * Send a message to all players tracking the specified entity, and also send the message to
     * the entity if it is a player. If a chunk that player loaded on the client contains the
     * chunk where the entity is located, and then the player is tracking the entity changes.
     *
     * @param entity the entity is tracking
     */
    public void sendToTrackingAndSelf(@Nonnull Entity entity) {
        ((ServerWorld) entity.getEntityWorld()).getChunkProvider().sendToTrackingAndSelf(
                entity, new SCustomPayloadPlayPacket(channel, buffer));
        buffer = null;
    }

    /**
     * Send a message to all players who loaded the specified chunk
     *
     * @param chunk the chunk that players in
     */
    public void sendToTrackingChunk(@Nonnull Chunk chunk) {
        final IPacket<?> packet = new SCustomPayloadPlayPacket(channel, buffer);
        ((ServerWorld) chunk.getWorld()).getChunkProvider().chunkManager.getTrackingPlayers(
                chunk.getPos(), false).forEach(player -> player.connection.sendPacket(packet));
        buffer = null;
    }

    @FunctionalInterface
    public interface IClientMsgHandler {

        /**
         * Handle a server-to-client network message
         *
         * @param index   message index
         * @param payload packet payload
         * @param player  the client player, when you receive the packet, it should
         *                not be null, so you have to check if it's non-null
         */
        void handle(short index, @Nonnull PacketBuffer payload, @Nullable ClientPlayerEntity player);
    }

    @FunctionalInterface
    public interface IServerMsgHandler {

        /**
         * Handle a client-to-server network message
         *
         * @param index   message index
         * @param payload packet payload
         * @param player  the server player, when you receive the packet, it should
         *                not be null, so you have to check if it's non-null
         */
        void handle(short index, @Nonnull PacketBuffer payload, @Nullable ServerPlayerEntity player);
    }
}
