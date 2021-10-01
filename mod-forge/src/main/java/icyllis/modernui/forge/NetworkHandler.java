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
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fmllegacy.network.NetworkEvent;
import net.minecraftforge.fmllegacy.network.NetworkRegistry;
import net.minecraftforge.fmllegacy.network.event.EventNetworkChannel;
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
    private final ClientListener mClientListener;

    @Nullable
    private final ServerListener mServerListener;

    /**
     * Create a network handler of a mod. Note that this is a dist-sensitive operation,
     * you may consider the following example:
     *
     * <pre>
     * new NetworkHandler(MODID, "main_network", () -> NetworkMessages::handle, NetworkMessages::handle, null, false);
     * </pre>
     *
     * @param modid          the mod-id
     * @param name           the network channel name, for example, "network".
     * @param clientListener listener for server-to-client messages, the inner supplier must be in another class
     * @param serverListener listener for client-to-server messages
     * @param protocol       network protocol version, when null or empty it will request the same version of the mod
     * @param optional       when true it will accept if the channel absent on one side, or request same protocol
     * @see NetworkMessages
     */
    public NetworkHandler(@Nonnull String modid, @Nonnull String name,
                          @Nonnull Supplier<Supplier<ClientListener>> clientListener,
                          @Nullable ServerListener serverListener, @Nullable String protocol, boolean optional) {
        if (protocol == null || protocol.isEmpty()) {
            protocol = DigestUtils.md5Hex(ModList.get().getModFileById(modid).getMods().stream()
                    .map(iModInfo -> iModInfo.getVersion().getQualifier())
                    .collect(Collectors.joining(",")).getBytes(StandardCharsets.UTF_8));
        }
        mProtocol = protocol;
        mOptional = optional;
        EventNetworkChannel channel = NetworkRegistry.ChannelBuilder
                .named(mId = new ResourceLocation(modid, name))
                .networkProtocolVersion(this::getProtocolVersion)
                .clientAcceptedVersions(this::checkS2CProtocol)
                .serverAcceptedVersions(this::checkC2SProtocol)
                .eventNetworkChannel();
        if (FMLEnvironment.dist.isClient()) {
            mClientListener = clientListener.get().get();
            channel.addListener(this::onS2CMessageReceived);
        } else {
            mClientListener = null;
        }
        mServerListener = serverListener;
        channel.addListener(this::onC2SMessageReceived);
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
        return mOptional && serverProtocol.equals(NetworkRegistry.ABSENT) || serverProtocol.equals(mProtocol);
    }

    /**
     * This method will run on server to verify the remote client protocol that sent by handshake network channel
     *
     * @param clientProtocol the protocol of this channel sent from client side
     * @return {@code true} to accept the protocol, {@code false} otherwise
     */
    private boolean checkC2SProtocol(@Nonnull String clientProtocol) {
        return mOptional && clientProtocol.equals(NetworkRegistry.ABSENT) || clientProtocol.equals(mProtocol);
    }

    @OnlyIn(Dist.CLIENT)
    private void onS2CMessageReceived(@Nonnull NetworkEvent.ServerCustomPayloadEvent event) {
        // render thread
        if (mClientListener != null) {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null) {
                mClientListener.handle(event.getPayload().readShort(), event.getPayload(), player);
            }
        }
        // forge disabled this on client, see ClientPacketListener.handleCustomPayload() finally block
        event.getPayload().release();
        event.getSource().get().setPacketHandled(true);
    }

    private void onC2SMessageReceived(@Nonnull NetworkEvent.ClientCustomPayloadEvent event) {
        // server thread
        if (mServerListener != null) {
            ServerPlayer player = event.getSource().get().getSender();
            if (player != null) {
                mServerListener.handle(event.getPayload().readShort(), event.getPayload(), player);
            }
        }
        event.getSource().get().setPacketHandled(true);
    }

    /**
     * Allocates a heap buffer to write packet data with index. Once you done that,
     * pass the value returned here to {@link #getDispatcher(FriendlyByteBuf)}.
     * The message index is used to identify what type of message is it, which is
     * also determined by network protocol version.
     *
     * @param index the message index used on the reception side, ranged from 0 to 32767
     * @return a byte buf to write the packet data (message)
     * @see #getDispatcher(FriendlyByteBuf)
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
     *
     * @param data the packet data
     * @return a broadcaster to broadcast the packet
     * @see #buffer(int)
     * @see ClientListener
     */
    @Nonnull
    public PacketDispatcher getDispatcher(@Nonnull FriendlyByteBuf data) {
        return PacketDispatcher.obtain(mId, data);
    }

    /**
     * Send a message to server
     * <p>
     * This is the only method to be called on the client.
     *
     * @param data the packet data
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
         * Handle a server-to-client network message
         *
         * @param index   message index
         * @param payload packet data
         * @param player  the client player
         */
        void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull LocalPlayer player);
    }

    @FunctionalInterface
    public interface ServerListener {

        /**
         * Handle a client-to-server network message
         *
         * @param index   message index
         * @param payload packet data
         * @param player  the server player
         */
        void handle(short index, @Nonnull FriendlyByteBuf payload, @Nonnull ServerPlayer player);
    }
}
