/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * 3.0 any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.api.network;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.network.NetworkEvent;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

@SuppressWarnings("unused")
public interface IMessage {

    /**
     * Encode message to byte buffer
     *
     * @param buf buf to write
     */
    void encode(@Nonnull PacketBuffer buf);

    /**
     * Decode message from byte buffer
     *
     * @param buf buf to read
     */
    void decode(@Nonnull PacketBuffer buf);

    /**
     * Handle message on sided effective thread
     *
     * @param ctx context
     */
    void handle(@Nonnull SimpleContext ctx);

    /**
     * Simplified network context wrapper
     */
    class SimpleContext {

        private final NetworkEvent.Context ctx;

        private final BiConsumer<IMessage, NetworkEvent.Context> replier;

        public SimpleContext(NetworkEvent.Context ctx, BiConsumer<IMessage, NetworkEvent.Context> replier) {
            this.ctx = ctx;
            this.replier = replier;
        }

        /**
         * Get current player when you don't know the packet should
         * be handled on server or client
         */
        @Nullable
        public PlayerEntity getPlayer() {
            if (ctx.getDirection().getOriginationSide().isClient()) {
                return getServerPlayer();
            } else {
                return getClientPlayer();
            }
        }

        /**
         * Get client player
         * Call this on client side
         */
        @OnlyIn(Dist.CLIENT)
        @Nullable
        public PlayerEntity getClientPlayer() {
            return Minecraft.getInstance().player;
        }

        /**
         * Get who sends this packet to server
         * Call this on server side
         */
        @Nullable
        public ServerPlayerEntity getServerPlayer() {
            return ctx.getSender();
        }

        /**
         * Enqueue an async work to main thread of appropriate side or run immediately
         */
        public CompletableFuture<Void> enqueueWork(Runnable runnable) {
            return ctx.enqueueWork(runnable);
        }

        /**
         * Reply a message to sender or server according to network context
         *
         * @param msg   message
         * @param <MSG> message type
         */
        public <MSG extends IMessage> void reply(MSG msg) {
            replier.accept(msg, ctx);
        }

        /**
         * Get original context if needed
         */
        public NetworkEvent.Context getOriginalContext() {
            return ctx;
        }
    }

}
