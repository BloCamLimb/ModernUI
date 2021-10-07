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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.RunningOnDifferentThreadException;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * For handling a network channel more faster, you can create an instance for your mod.
 */
public class NetworkHandler {

    private static final Map<String, NetworkHandler> sNetworks = new HashMap<>();

    private final ResourceLocation mId;

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
     * @param scl      listener for S->C messages, the inner supplier must be in another non-anonymous class
     * @param csl      listener for C->S messages, it is on logical server side
     * @param protocol network protocol, when empty it will request the same version of mod(s)
     * @param optional when true it will accept if the channel absent on one side, or request same protocol
     * @throws IllegalArgumentException invalid mod-id
     */
    public NetworkHandler(@Nonnull String modid, @Nullable Supplier<Supplier<ClientListener>> scl,
                          @Nullable ServerListener csl, @Nullable String protocol, boolean optional) {
        // modid only starts with [a-z]
        if (!modid.startsWith("_") && ModList.get().getModFileById(modid) == null) {
            throw new IllegalArgumentException("No mod found that given by modid " + modid);
        }
        if (protocol == null) {
            protocol = "default";
        } else if (protocol.isEmpty()) {
            protocol = ModList.get().getModFileById(modid).getMods().stream()
                    .map(iModInfo -> iModInfo.getVersion().getQualifier())
                    .collect(Collectors.joining(","));
        }
        mProtocol = protocol;
        mOptional = optional;
        NetworkRegistry.ChannelBuilder
                .named(mId = new ResourceLocation(ModernUI.ID, modid))
                .networkProtocolVersion(this::getProtocol)
                .clientAcceptedVersions(this::checkS2CProtocol)
                .serverAcceptedVersions(this::checkC2SProtocol)
                .eventNetworkChannel();
        if (scl != null && FMLEnvironment.dist.isClient()) {
            mClientListener = scl.get().get();
        } else {
            mClientListener = null;
        }
        mServerListener = csl;
        // NetworkRegistry has duplication detection
        sNetworks.put(modid, this);
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
    private boolean checkS2CProtocol(@Nonnull String protocol) {
        return mOptional && protocol.equals(NetworkRegistry.ABSENT) || protocol.equals(mProtocol);
    }

    /**
     * This method will run on server to verify the remote client protocol that sent by handshake network channel.
     *
     * @param protocol the protocol of this channel sent from client side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    private boolean checkC2SProtocol(@Nonnull String protocol) {
        return mOptional && protocol.equals(NetworkRegistry.ABSENT) || protocol.equals(mProtocol);
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
     * Allocates a heap buffer to write indexed packet data. Once you done that,
     * pass the value returned here to {@link #dispatcher(FriendlyByteBuf)} or
     * {@link #sendToServer(FriendlyByteBuf)}. The message index is used to identify
     * what type of message is, which is also determined by your network protocol.
     *
     * @param index the message index used on the reception side, ranged from 0 to 32767
     * @return a byte buf to write the packet data (message body)
     * @see #dispatcher(FriendlyByteBuf)
     * @see #sendToServer(FriendlyByteBuf)
     */
    @Nonnull
    public static FriendlyByteBuf buffer(int index) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        buffer.writeShort(index);
        return buffer;
    }

    /**
     * Creates the packet with a broadcaster from a message. The packet must by dispatched
     * right after calling this, for example {@link PacketDispatcher#sendToPlayer(Player)}.
     * Packet data cannot exceed 1,043,200 bytes.
     *
     * @param data the packet data (message body)
     * @return a broadcaster to broadcast the packet
     * @see #buffer(int)
     * @see ClientListener
     */
    @Nonnull
    public PacketDispatcher dispatcher(@Nonnull FriendlyByteBuf data) {
        return PacketDispatcher.obtain(mId, data);
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
            connection.send(new ServerboundCustomPayloadPacket(mId, data));
        } else {
            data.release();
        }
    }

    @FunctionalInterface
    public interface ClientListener {

        /**
         * Handle a client-to-server network message.
         * <p>
         * This method is invoked on the Netty-IO thread, you need to consume or retain
         * the payload and then process it further through thread scheduling. In addition
         * to retain, you can throw {@link RunningOnDifferentThreadException}
         * to prevent the payload from being released after this method call.
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
