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

import icyllis.modernui.ModernUI;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
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
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;
import net.minecraftforge.fml.server.ServerLifecycleHooks;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * For handling a network channel more faster, you can create an instance for your mod
 */
public class NetworkHandler {

    public static final Marker MARKER = MarkerManager.getMarker("Network");

    private final ResourceLocation mId;

    private final String mProtocol;
    private final boolean mOptional;

    @Nullable
    private final S2CMsgHandler mClientHandler;
    @Nullable
    private final C2SMsgHandler mServerHandler;

    protected final Pool<Broadcaster> mPool = Pools.concurrent(3);

    /**
     * Create a network handler of a mod. Note that this is a dist-sensitive operation,
     * you may consider the following example:
     *
     * <pre>
     * network = new NetworkHandler(MODID, "main_network", () -> NetMessages::handle, NetMessages::handle, null, false);
     * </pre>
     *
     * @param modid         the mod id
     * @param name          the network channel name, should be short
     * @param clientHandler a handler to handle server-to-client messages, the inner supplier must be in another class
     * @param serverHandler a handler to handle client-to-server messages
     * @param protocol      network protocol version, when null or empty it will request the same version of the mod
     * @param optional      when true it will pass if the mod missing on one side, or request same protocol
     * @see NetworkMessages
     */
    public NetworkHandler(@Nonnull String modid, @Nonnull String name,
                          @Nonnull Supplier<Supplier<S2CMsgHandler>> clientHandler,
                          @Nullable C2SMsgHandler serverHandler, @Nullable String protocol, boolean optional) {
        if (protocol == null || protocol.isEmpty())
            protocol = DigestUtils.md5Hex(ModList.get().getModFileById(modid).getMods().stream()
                    .map(iModInfo -> iModInfo.getVersion().getQualifier())
                    .collect(Collectors.joining(",")).getBytes(StandardCharsets.UTF_8));
        mProtocol = protocol;
        mOptional = optional;
        mClientHandler = FMLEnvironment.dist.isClient() ? clientHandler.get().get() : null;
        mServerHandler = serverHandler;
        EventNetworkChannel network = NetworkRegistry.ChannelBuilder
                .named(mId = new ResourceLocation(modid, name))
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
     * Get the protocol version of this channel on current side
     *
     * @return the protocol
     */
    public String getProtocolVersion() {
        return mProtocol;
    }

    /**
     * This method will run on client to verify the server protocol that sent by handshake network channel
     *
     * @param serverProtocol the protocol of this channel sent from server side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    private boolean checkS2CProtocol(@Nonnull String serverProtocol) {
        boolean allowAbsent = mOptional && serverProtocol.equals(NetworkRegistry.ABSENT);
        if (allowAbsent) {
            ModernUI.LOGGER.debug(MARKER, "Connecting to a server that does not have {} channel available", mId);
        }
        return allowAbsent || serverProtocol.equals(mProtocol);
    }

    /**
     * This method will run on server to verify the remote client protocol that sent by handshake network channel
     *
     * @param clientProtocol the protocol of this channel sent from client side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    private boolean checkC2SProtocol(@Nonnull String clientProtocol) {
        if (clientProtocol.equals(NetworkRegistry.ACCEPTVANILLA)) {
            return false;
        }
        return clientProtocol.equals(mProtocol) || mOptional && clientProtocol.equals(NetworkRegistry.ABSENT);
    }

    @OnlyIn(Dist.CLIENT)
    private void onS2CMessageReceived(@Nonnull NetworkEvent.ServerCustomPayloadEvent event) {
        // received on main thread of effective side
        if (mClientHandler != null) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                mClientHandler.handle(event.getPayload().readShort(), event.getPayload(), player);
            }
        }
        event.getPayload().release(); // forge disabled this on client, see ClientPacketListener.handleCustomPayload() finally {}
        event.getSource().get().setPacketHandled(true);
    }

    private void onC2SMessageReceived(@Nonnull NetworkEvent.ClientCustomPayloadEvent event) {
        // received on main thread of effective side
        if (mServerHandler != null) {
            ServerPlayer player = event.getSource().get().getSender();
            if (player != null) {
                mServerHandler.handle(event.getPayload().readShort(), event.getPayload(), player);
            }
        }
        event.getSource().get().setPacketHandled(true);
    }

    /**
     * Allocate a buffer to write packet data with index. Once you done that,
     * pass the value returned here to {@link #getBroadcaster(FriendlyByteBuf)}
     *
     * @param index The message index used on the opposite side, range from 0 to 32767
     * @return a byte buf to write the packet data (message)
     * @see S2CMsgHandler
     * @see C2SMsgHandler
     */
    @Nonnull
    public static FriendlyByteBuf buffer(int index) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeShort(index);
        return buffer;
    }

    /**
     * Wraps the packet with a broadcaster for sending, you must dispatch
     * the packet right after calling this, for example {@link Broadcaster#sendToPlayer(Player)}
     *
     * @param data the packet data
     * @return a broadcaster to broadcast the packet
     * @see #buffer(int)
     */
    @Nonnull
    public Broadcaster getBroadcaster(@Nonnull FriendlyByteBuf data) {
        Broadcaster b = mPool.acquire();
        if (b == null) b = new Broadcaster();
        b.mData = data;
        return b;
    }

    /**
     * Send a message to server
     * <p>
     * This is the only method to be called on the client, the rest needs
     * to be called on the server side
     */
    @OnlyIn(Dist.CLIENT)
    public void sendToServer(@Nonnull FriendlyByteBuf data) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null)
            connection.send(new ServerboundCustomPayloadPacket(mId, data));
        data.release();
    }

    public class Broadcaster {

        private FriendlyByteBuf mData;

        protected void recycle() {
            mData = null;
            mPool.release(this);
        }

        /**
         * Send a message to a player
         *
         * @param player the server player
         */
        public void sendToPlayer(@Nonnull Player player) {
            ((ServerPlayer) player).connection.send(new ClientboundCustomPayloadPacket(mId, mData));
            recycle();
        }

        /**
         * Send a message to a player
         *
         * @param player the server player
         */
        public void sendToPlayer(@Nonnull ServerPlayer player) {
            player.connection.send(new ClientboundCustomPayloadPacket(mId, mData));
            recycle();
        }

        /**
         * Send a message to all specific players
         *
         * @param players players on server
         */
        public void sendToPlayers(@Nonnull Iterable<? extends Player> players) {
            final Packet<?> packet = new ClientboundCustomPayloadPacket(mId, mData);
            for (Player player : players)
                ((ServerPlayer) player).connection.send(packet);
            recycle();
        }

        /**
         * Send a message to all players on the server
         */
        public void sendToAll() {
            ServerLifecycleHooks.getCurrentServer().getPlayerList()
                    .broadcastAll(new ClientboundCustomPayloadPacket(mId, mData));
            recycle();
        }

        /**
         * Send a message to all players in specified dimension
         *
         * @param dimension dimension that players in
         */
        public void sendToDimension(@Nonnull ResourceKey<Level> dimension) {
            ServerLifecycleHooks.getCurrentServer().getPlayerList()
                    .broadcastAll(new ClientboundCustomPayloadPacket(mId, mData), dimension);
            recycle();
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
        public void sendToNearby(@Nullable ServerPlayer excluded,
                                 double x, double y, double z, double radius,
                                 @Nonnull ResourceKey<Level> dimension) {
            ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcast(excluded,
                    x, y, z, radius, dimension, new ClientboundCustomPayloadPacket(mId, mData));
            recycle();
        }

        /**
         * Send a message to all players tracking the specified entity. If a chunk that player loaded
         * on the client contains the chunk where the entity is located, and then the player is
         * tracking the entity changes.
         *
         * @param entity entity is tracking
         */
        public void sendToTrackingEntity(@Nonnull Entity entity) {
            ((ServerLevel) entity.level).getChunkSource().broadcast(
                    entity, new ClientboundCustomPayloadPacket(mId, mData));
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
            ((ServerLevel) entity.level).getChunkSource().broadcastAndSend(
                    entity, new ClientboundCustomPayloadPacket(mId, mData));
            recycle();
        }

        /**
         * Send a message to all players who loaded the specified chunk
         *
         * @param chunk the chunk that players in
         */
        public void sendToTrackingChunk(@Nonnull LevelChunk chunk) {
            final Packet<?> packet = new ClientboundCustomPayloadPacket(mId, mData);
            ((ServerLevel) chunk.getLevel()).getChunkSource().chunkMap.getPlayers(
                    chunk.getPos(), false).forEach(player -> player.connection.send(packet));
            recycle();
        }
    }

    @FunctionalInterface
    public interface S2CMsgHandler {

        /**
         * Handle a server-to-client network message
         *
         * @param index   message index
         * @param payload packet payload
         * @param player  the local player
         */
        void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull LocalPlayer player);
    }

    @FunctionalInterface
    public interface C2SMsgHandler {

        /**
         * Handle a client-to-server network message
         *
         * @param index   message index
         * @param payload packet payload
         * @param player  the server player
         */
        void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull ServerPlayer player);
    }
}
