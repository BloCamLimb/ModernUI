package icyllis.modern.network;

import icyllis.modern.core.ScreenManager;
import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class STCMessages {

    public static class OpenContainerMessage {

        final int id;
        final int windowId;
        final ITextComponent name;
        final PacketBuffer additionalData;

        public OpenContainerMessage(int id, int windowId, ITextComponent name, PacketBuffer additionalData) {
            this.id = id;
            this.windowId = windowId;
            this.name = name;
            this.additionalData = additionalData;
        }

        public static void encode(OpenContainerMessage msg, PacketBuffer buf) {
            buf.writeVarInt(msg.id);
            buf.writeVarInt(msg.windowId);
            buf.writeTextComponent(msg.name);
            buf.writeByteArray(msg.additionalData.readByteArray());
        }

        public static OpenContainerMessage decode(PacketBuffer buf) {
            return new OpenContainerMessage(buf.readVarInt(), buf.readVarInt(), buf.readTextComponent(), new PacketBuffer(Unpooled.wrappedBuffer(buf.readByteArray(32600))));
        }

        public static void handle(OpenContainerMessage msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> ScreenManager.INSTANCE.openContainerScreen(msg.id, msg.windowId, msg.name, msg.additionalData.readBoolean() ? msg.additionalData.readBlockPos() : BlockPos.ZERO));
            ctx.get().setPacketHandled(true);
        }
    }
}
