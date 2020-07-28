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
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.RegistryKey;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.network.NetworkDirection;
import net.minecraftforge.fml.network.NetworkEvent;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.fml.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Supplier;

@SuppressWarnings("unused")
public class NetworkHandler {

    /**
     * Main network channel, lazy-loading.
     */
    public static final NetworkHandler INSTANCE = new NetworkHandler(ModernUI.MODID, "main_network");

    protected SimpleChannel channel;

    protected String protocol;

    private int index = 0;

    public NetworkHandler(@Nonnull String modid, @Nonnull String name) {
        channel = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(modid, name))
                .networkProtocolVersion(this::getProtocolVersion)
                .clientAcceptedVersions(this::checkProtocolOnClient)
                .serverAcceptedVersions(this::checkProtocolOnServer)
                .simpleChannel();
        protocol = ModList.get().getModFileById(modid).getMods().get(0).getVersion().getQualifier();
    }

    public NetworkHandler(@Nonnull SimpleChannel channel) {
        this.channel = channel;
    }

    public NetworkHandler() {

    }

    public String getProtocolVersion() {
        return protocol;
    }

    public boolean checkProtocolOnClient(@Nonnull String serverProtocol) {
        return serverProtocol.equals(NetworkRegistry.ABSENT) || serverProtocol.equals(protocol);
    }

    public boolean checkProtocolOnServer(@Nonnull String clientProtocol) {
        return clientProtocol.equals(protocol);
    }

    /**
     * Register a network message
     *
     * @param type      message class
     * @param factory   factory to create default new instance every time, should be an empty constructor
     * @param direction message direction, either {@code null} for bi-directional message,
     *                  {@link NetworkDirection#PLAY_TO_CLIENT} or {@link NetworkDirection#PLAY_TO_SERVER}
     * @param <MSG>     message type
     */
    public <MSG extends IMessage> void registerMessage(@Nonnull Class<MSG> type, @Nonnull Supplier<? extends MSG> factory,
                                                       @Nullable NetworkDirection direction) {
        /*CHANNEL.messageBuilder(type, ++index, direction)
                .encoder(IMessage::encode)
                .decoder(buf -> decode(factory, buf))
                .consumer((BiConsumer<MSG, Supplier<NetworkEvent.Context>>) this::handle)
                .add();*/
        synchronized (this) {
            channel.registerMessage(index++, type, IMessage::encode, buf -> decode(factory, buf),
                    NetworkHandler::handle, Optional.ofNullable(direction));
        }
    }

    @Nonnull
    private static <MSG extends IMessage> MSG decode(@Nonnull Supplier<MSG> factory, PacketBuffer buf) {
        MSG msg = factory.get();
        msg.decode(buf);
        return msg;
    }

    private static <MSG extends IMessage> void handle(@Nonnull MSG message, @Nonnull Supplier<NetworkEvent.Context> ctx) {
        message.handle(ctx.get());
        ctx.get().setPacketHandled(true);
    }

    @Nullable
    public static PlayerEntity getPlayer(@Nonnull NetworkEvent.Context context) {
        if (context.getDirection().getOriginationSide().isClient()) {
            return context.getSender();
        } else {
            return Minecraft.getInstance().player;
        }
    }

    /**
     * Reply a message depend on network context
     *
     * @param message message to reply
     * @param context network context
     * @param <MSG>   message type
     */
    public <MSG extends IMessage> void reply(MSG message, NetworkEvent.Context context) {
        channel.reply(message, context);
    }

    /**
     * Send a message to server, call this on client side
     *
     * @param message message to send
     * @param <MSG>   message type
     */
    @OnlyIn(Dist.CLIENT)
    public <MSG extends IMessage> void sendToServer(MSG message) {
        channel.sendToServer(message);
    }

    /**
     * A helper version of {@link #sendToPlayer(IMessage, ServerPlayerEntity)}
     * to cast player entity to server player entity
     *
     * @param message message to send
     * @param player  player entity on server
     * @param <MSG>   message type
     */
    public <MSG extends IMessage> void sendToPlayer(MSG message, PlayerEntity player) {
        sendToPlayer(message, (ServerPlayerEntity) player);
    }

    /**
     * Send a message to a client player, call this on server side
     *
     * @param message  message to send
     * @param playerMP player entity on server
     * @param <MSG>    message type
     */
    public <MSG extends IMessage> void sendToPlayer(MSG message, @Nonnull ServerPlayerEntity playerMP) {
        playerMP.connection.sendPacket(channel.toVanillaPacket(message, NetworkDirection.PLAY_TO_CLIENT));
    }

    /**
     * Send a message to all players on the server
     *
     * @param message message to send
     * @param <MSG>   message type
     */
    public <MSG extends IMessage> void sendToAll(MSG message) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().sendPacketToAllPlayers(
                channel.toVanillaPacket(message, NetworkDirection.PLAY_TO_CLIENT));
    }

    /**
     * Send a message to all players in specified dimension
     *
     * @param message   message to send
     * @param dimension dimension that players in
     * @param <MSG>     message type
     */
    public <MSG extends IMessage> void sendToDimension(MSG message, @Nonnull RegistryKey<World> dimension) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().func_232642_a_(
                channel.toVanillaPacket(message, NetworkDirection.PLAY_TO_CLIENT), dimension);
    }

    /**
     * Send a message to all players nearby a point with specified radius in specified dimension
     *
     * @param message   message to send
     * @param excluded  excluded player to send the packet
     * @param x         target point x
     * @param y         target point y
     * @param z         target point z
     * @param radius    radius to target point
     * @param dimension dimension that players in
     * @param <MSG>     message type
     */
    public <MSG extends IMessage> void sendToNearby(MSG message, @Nullable ServerPlayerEntity excluded,
                                                    double x, double y, double z, double radius,
                                                    @Nonnull RegistryKey<World> dimension) {
        ServerLifecycleHooks.getCurrentServer().getPlayerList().sendToAllNearExcept(
                excluded, x, y, z, radius, dimension,
                channel.toVanillaPacket(message, NetworkDirection.PLAY_TO_CLIENT));
    }

    /**
     * Send a message to all players tracking the specified entity. If a chunk that player loaded
     * on the client contains the chunk where the entity is located, and then the player is
     * tracking the entity.
     *
     * @param message message to send
     * @param entity  entity is tracking
     * @param <MSG>   message type
     */
    public <MSG extends IMessage> void sendToAllTracking(MSG message, @Nonnull Entity entity) {
        ((ServerWorld) entity.getEntityWorld()).getChunkProvider().sendToAllTracking(
                entity, channel.toVanillaPacket(message, NetworkDirection.PLAY_TO_CLIENT));
    }

    /**
     * Send a message to all players tracking the specified entity, and also send the message to
     * the entity if it is a player. If a chunk that player loaded on the client contains the
     * chunk where the entity is located, and then the player is tracking the entity.
     *
     * @param message message to send
     * @param entity  entity is tracking
     * @param <MSG>   message type
     */
    public <MSG extends IMessage> void sendToTrackingAndSelf(MSG message, @Nonnull Entity entity) {
        ((ServerWorld) entity.getEntityWorld()).getChunkProvider().sendToTrackingAndSelf(
                entity, channel.toVanillaPacket(message, NetworkDirection.PLAY_TO_CLIENT));
    }

    /**
     * Send a message to all players in specified chunk
     *
     * @param message message to send
     * @param chunk   chunk that players in
     * @param <MSG>   message type
     */
    public <MSG extends IMessage> void sendToChunk(MSG message, @Nonnull Chunk chunk) {
        final IPacket<?> packet = channel.toVanillaPacket(message, NetworkDirection.PLAY_TO_CLIENT);
        ((ServerWorld) chunk.getWorld()).getChunkProvider().chunkManager.getTrackingPlayers(
                chunk.getPos(), false).forEach(e -> e.connection.sendPacket(packet));
    }
}
