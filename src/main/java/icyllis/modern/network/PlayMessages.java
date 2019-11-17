package icyllis.modern.network;

import icyllis.modern.core.ScreenManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

class PlayMessages {

    static class OpenContainer {

        final int id;
        final int windowId;
        final boolean hasPos;
        final BlockPos pos;

        OpenContainer(int id, int windowId, boolean hasPos, BlockPos pos) {
            this.id = id;
            this.windowId = windowId;
            this.hasPos = hasPos;
            this.pos = pos;
        }

        static void encode(OpenContainer msg, PacketBuffer buf) {
            buf.writeVarInt(msg.id);
            buf.writeVarInt(msg.windowId);
            buf.writeBoolean(msg.hasPos);
            if(msg.hasPos) {
                buf.writeBlockPos(msg.pos);
            }
        }

        static OpenContainer decode(PacketBuffer buf) {
            int id = buf.readVarInt(), windowId = buf.readVarInt();
            boolean hasPos = buf.readBoolean();
            return new OpenContainer(id, windowId, hasPos, hasPos ? buf.readBlockPos() : null);
        }

        static boolean handle(OpenContainer msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> ScreenManager.INSTANCE.openContainerScreen(msg.id, msg.windowId, msg.pos));
            return true;
        }
    }
}
