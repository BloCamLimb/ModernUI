/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.system.network;

import icyllis.modernui.system.NetworkManager;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public interface IMessage {

    /**
     * Encode message to byte buf
     *
     * @param buf buf to write
     */
    void encode(@Nonnull PacketBuffer buf);

    /**
     * Decode message from byte buf
     *
     * @param buf buf to read
     */
    void decode(@Nonnull PacketBuffer buf);

    /**
     * Handle this message, not necessarily on the main thread
     *
     * @param ctx context
     */
    void handle(@Nonnull SimpleContext ctx);

    /**
     * Simplified network context for user modding
     */
    class SimpleContext {

        private final NetworkEvent.Context ctx;

        public SimpleContext(NetworkEvent.Context ctx) {
            this.ctx = ctx;
        }

        /**
         * Get current player for bi-directional packet
         */
        public PlayerEntity getPlayer() {
            if (ctx.getDirection().getOriginationSide().isClient()) {
                return getServerPlayer();
            } else {
                return getClientPlayer();
            }
        }

        /**
         * Get client player instance on logic client side
         */
        @OnlyIn(Dist.CLIENT)
        public PlayerEntity getClientPlayer() {
            return Minecraft.getInstance().player;
        }

        /**
         * Get who sends this packet on logic server side
         */
        public ServerPlayerEntity getServerPlayer() {
            return ctx.getSender();
        }

        /**
         * Enqueue a async work to main thread or run immediately
         */
        public CompletableFuture<Void> enqueueWork(Runnable runnable) {
            return ctx.enqueueWork(runnable);
        }

        /**
         * Enqueue a async work to main thread or run immediately with appropriate player
         */
        public CompletableFuture<Void> enqueueWork(Consumer<PlayerEntity> consumer) {
            return ctx.enqueueWork(() -> consumer.accept(getPlayer()));
        }

        /**
         * Reply a message to sender or server
         *
         * @param msg   message
         * @param <MSG> message type
         */
        public <MSG extends IMessage> void reply(MSG msg) {
            NetworkManager.INSTANCE.reply(msg, ctx);
        }

        /**
         * Get original context
         */
        public NetworkEvent.Context getContext() {
            return ctx;
        }
    }
}
