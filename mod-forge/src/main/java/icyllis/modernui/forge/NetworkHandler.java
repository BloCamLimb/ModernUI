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
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * For handling a network channel faster, you can create an instance for your mod.
 */
public class NetworkHandler {

    private static final HashMap<String, NetworkHandler> sNetworks = new HashMap<>();

    private final ResourceLocation mName;

    private final String mProtocol;
    private final boolean mOptional;

    @Nullable
    private final ClientListener mClientListener;

    @Nullable
    private final ServerListener mServerListener;

    /**
     * Create a network handler of a mod. Note that this is a distribution-sensitive operation,
     * you must be careful with the security of class loading.
     *
     * @param modid    the mod-id
     * @param cli      listener for S->C messages, the inner supplier must be in another non-anonymous class
     * @param sli      listener for C->S messages, it is on logical server side
     * @param protocol network protocol, leaving empty will request the same version of mod(s)
     * @param optional when true it will accept if the channel absent on one side, or request same protocol
     * @throws IllegalArgumentException invalid mod-id
     */
    public NetworkHandler(@Nonnull String modid, @Nullable Supplier<Supplier<ClientListener>> cli,
                          @Nullable ServerListener sli, @Nonnull String protocol, boolean optional) {
        // modid only starts with [a-z]
        if (!modid.startsWith("_") && ModList.get().getModFileById(modid) == null) {
            throw new IllegalArgumentException("No mod found that given by modid " + modid);
        }
        if (protocol.isEmpty()) {
            protocol = ModList.get().getModFileById(modid).getMods().stream()
                    .map(iModInfo -> iModInfo.getVersion().getQualifier())
                    .collect(Collectors.joining(","));
        }
        mProtocol = protocol;
        mOptional = optional;

        // Just register it to FML, we handle messages from a mixin hook
        NetworkRegistry.ChannelBuilder
                .named(mName = new ResourceLocation(ModernUI.ID, modid))
                .networkProtocolVersion(this::getProtocol)
                .clientAcceptedVersions(this::testServerProtocolOnClient)
                .serverAcceptedVersions(this::testClientProtocolOnServer)
                .eventNetworkChannel();
        if (cli != null && FMLEnvironment.dist.isClient()) {
            mClientListener = cli.get().get();
        } else {
            mClientListener = null;
        }
        mServerListener = sli;

        synchronized (sNetworks) {
            // NetworkRegistry has duplication detection
            sNetworks.put(modid, this);
        }
    }

    /**
     * Get the protocol string of this channel on current side.
     *
     * @return the protocol
     */
    public String getProtocol() {
        return mProtocol;
    }

    /**
     * This method will run on client to verify the server protocol that sent by handshake network channel.
     *
     * @param protocol the protocol of this channel sent from server side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    private boolean testServerProtocolOnClient(@Nonnull String protocol) {
        return mOptional && protocol.equals(NetworkRegistry.ABSENT) || mProtocol.equals(protocol);
    }

    /**
     * This method will run on server to verify the remote client protocol that sent by handshake network channel.
     *
     * @param protocol the protocol of this channel sent from client side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    private boolean testClientProtocolOnServer(@Nonnull String protocol) {
        return mOptional && protocol.equals(NetworkRegistry.ABSENT) || mProtocol.equals(protocol);
    }

    @OnlyIn(Dist.CLIENT)
    public static void onCustomPayload(@Nonnull ClientboundCustomPayloadPacket packet,
                                       @Nonnull Supplier<LocalPlayer> player) {
        ResourceLocation id = packet.getIdentifier();
        if (id.getNamespace().equals(ModernUI.ID)) {
            FriendlyByteBuf payload = packet.getInternalData();
            NetworkHandler it = sNetworks.get(id.getPath());
            if (it != null && it.mClientListener != null) {
                it.mClientListener.handle(payload.readShort(), payload, player);
            }
            payload.release();
            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
        }
    }

    public static void onCustomPayload(@Nonnull ServerboundCustomPayloadPacket packet,
                                       @Nonnull Supplier<ServerPlayer> player) {
        ResourceLocation id = packet.getIdentifier();
        if (id.getNamespace().equals(ModernUI.ID)) {
            FriendlyByteBuf payload = packet.getInternalData();
            NetworkHandler it = sNetworks.get(id.getPath());
            if (it != null && it.mServerListener != null) {
                it.mServerListener.handle(payload.readShort(), payload, player);
            }
            payload.release();
            throw RunningOnDifferentThreadException.RUNNING_ON_DIFFERENT_THREAD;
        }
    }

    /**
     * Allocates a heap buffer to write indexed packet data. Once you're done that,
     * pass the value returned here to {@link #dispatch(FriendlyByteBuf)} or
     * {@link #sendToServer(FriendlyByteBuf)}. The message index is used to identify
     * what type of message is, which is also determined by your network protocol.
     *
     * @param index the message index used on the reception side, ranged from 0 to 32767
     * @return a byte buf to write the packet data (message body)
     * @see #sendToServer(FriendlyByteBuf)
     * @see #dispatch(FriendlyByteBuf)
     */
    @Nonnull
    public static FriendlyByteBuf buffer(int index) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeShort(index);
        return buffer;
    }

    /**
     * Send a message to server.
     * <p>
     * This is the only method to be called on the client. Packet data cannot exceed 32,600 bytes.
     *
     * @param data the packet data (message body)
     * @see #buffer(int)
     * @see ServerListener
     */
    @OnlyIn(Dist.CLIENT)
    public void sendToServer(@Nonnull FriendlyByteBuf data) {
        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            connection.send(new ServerboundCustomPayloadPacket(mName, data));
        } else {
            data.release();
        }
    }

    /**
     * Send a message to a player.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore. It is recommended to use {@link #dispatch(FriendlyByteBuf)} that is
     * chaining and safe.
     *
     * @param data   the packet data (message body)
     * @param player the player
     * @see #dispatch(FriendlyByteBuf)
     */
    public void sendToPlayer(@Nonnull FriendlyByteBuf data, @Nonnull Player player) {
        ((ServerPlayer) player).connection.send(new ClientboundCustomPayloadPacket(mName, data));
    }

    /**
     * Send a message to a player.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore. It is recommended to use {@link #dispatch(FriendlyByteBuf)} that is
     * chaining and safe.
     *
     * @param data   the packet data (message body)
     * @param player the player
     * @see #dispatch(FriendlyByteBuf)
     */
    public void sendToPlayer(@Nonnull FriendlyByteBuf data, @Nonnull ServerPlayer player) {
        player.connection.send(new ClientboundCustomPayloadPacket(mName, data));
    }

    /**
     * Send a message to all specific players.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore. It is recommended to use {@link #dispatch(FriendlyByteBuf)} that is
     * chaining and safe.
     *
     * @param data    the packet data (message body)
     * @param players players on server
     * @see #dispatch(FriendlyByteBuf)
     */
    public void sendToPlayers(@Nonnull FriendlyByteBuf data, @Nonnull Iterable<? extends Player> players) {
        for (Player player : players)
            ((ServerPlayer) player).connection.send(new ClientboundCustomPayloadPacket(mName, data));
    }

    /**
     * Send a message to all players on the server.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore. It is recommended to use {@link #dispatch(FriendlyByteBuf)} that is
     * chaining and safe.
     *
     * @param data the packet data (message body)
     * @see #dispatch(FriendlyByteBuf)
     */
    public void sendToAll(@Nonnull FriendlyByteBuf data) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .broadcastAll(new ClientboundCustomPayloadPacket(mName, data));
    }

    /**
     * Send a message to all players in the specified dimension.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore. It is recommended to use {@link #dispatch(FriendlyByteBuf)} that is
     * chaining and safe.
     *
     * @param data      the packet data (message body)
     * @param dimension dimension that players in
     * @see #dispatch(FriendlyByteBuf)
     */
    public void sendToDimension(@Nonnull FriendlyByteBuf data, @Nonnull ResourceKey<Level> dimension) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList()
                .broadcastAll(new ClientboundCustomPayloadPacket(mName, data), dimension);
    }

    /**
     * Send a message to all players nearby a point with specified radius in specified dimension.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore. It is recommended to use {@link #dispatch(FriendlyByteBuf)} that is
     * chaining and safe.
     *
     * @param data      the packet data (message body)
     * @param excluded  the player excluded from broadcasting
     * @param x         target point x
     * @param y         target point y
     * @param z         target point z
     * @param radius    radius to target point
     * @param dimension dimension that target players in
     * @see #dispatch(FriendlyByteBuf)
     */
    public void sendToNear(@Nonnull FriendlyByteBuf data, @Nullable Player excluded,
                           double x, double y, double z, double radius,
                           @Nonnull ResourceKey<Level> dimension) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().broadcast(
                excluded, x, y, z, radius, dimension, new ClientboundCustomPayloadPacket(mName, data));
    }

    /**
     * Send a message to all players tracking the specified entity. If a chunk that player loaded
     * on the client contains the chunk where the entity is located, and then the player is
     * tracking the entity changes.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore. It is recommended to use {@link #dispatch(FriendlyByteBuf)} that is
     * chaining and safe.
     *
     * @param data   the packet data (message body)
     * @param entity the entity is tracking
     * @see #dispatch(FriendlyByteBuf)
     */
    public void sendToTrackingEntity(@Nonnull FriendlyByteBuf data, @Nonnull Entity entity) {
        ((ServerLevel) entity.level).getChunkSource().broadcast(entity,
                new ClientboundCustomPayloadPacket(mName, data));
    }

    /**
     * Send a message to all players tracking the specified entity, and also send the message to
     * the entity if it is a player. If a chunk that player loaded on the client contains the
     * chunk where the entity is located, and then the player is tracking the entity changes.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore. It is recommended to use {@link #dispatch(FriendlyByteBuf)} that is
     * chaining and safe.
     *
     * @param data   the packet data (message body)
     * @param entity the entity is tracking
     * @see #dispatch(FriendlyByteBuf)
     */
    public void sendToTrackingAndSelf(@Nonnull FriendlyByteBuf data, @Nonnull Entity entity) {
        ((ServerLevel) entity.level).getChunkSource().broadcastAndSend(entity,
                new ClientboundCustomPayloadPacket(mName, data));
    }

    /**
     * Returns a broadcaster with the data buffer as the message. The packet must be dispatched
     * right after calling this, for example {@link PacketDispatcher#sendToPlayer(Player)}.
     * <p>
     * Packet data cannot exceed 1,043,200 bytes. After calling this method, you should not touch
     * the data buffer anymore. After dispatching, you should not touch the PacketDispatcher object
     * anymore.
     *
     * @param data the packet data (message body)
     * @return a broadcaster to broadcast the packet
     * @see #buffer(int)
     * @see ClientListener
     */
    @Nonnull
    public PacketDispatcher dispatch(@Nonnull FriendlyByteBuf data) {
        return PacketDispatcher.obtain(mName, data);
    }

    @FunctionalInterface
    public interface ClientListener {

        /**
         * Handle a server-to-client network message.
         * <p>
         * This method is invoked on the Netty-IO thread, you need to consume or retain
         * the payload and then process it further through thread scheduling. In addition
         * to retain, you can throw {@link RunningOnDifferentThreadException}
         * to prevent the payload from being released after this method call.
         * In the latter two cases, you must manually release the payload.
         * <p>
         * Note that the player supplier may return null if the connection is interrupted.
         * In this case, the message handling should be ignored.
         *
         * @param index   message index
         * @param payload message body
         * @param player  the supplier to get the current client player, may return null
         */
        void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull Supplier<LocalPlayer> player);
    }

    @FunctionalInterface
    public interface ServerListener {

        /**
         * Handle a client-to-server network message.
         * <p>
         * This method is invoked on the Netty-IO thread, you need to consume or retain
         * the payload and then process it further through thread scheduling. In addition
         * to retain, you can throw {@link RunningOnDifferentThreadException}
         * to prevent the payload from being released after this method call.
         * In the latter two cases, you must manually release the payload.
         * <p>
         * Note that the player supplier may return null if the connection is interrupted.
         * In this case, the message handling should be ignored.
         *
         * @param index   message index
         * @param payload message body
         * @param player  the supplier to get the current server player, may return null
         */
        void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull Supplier<ServerPlayer> player);
    }
}
