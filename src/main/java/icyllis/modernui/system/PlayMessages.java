package icyllis.modernui.system;

import io.netty.buffer.Unpooled;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.network.NetworkEvent;

import java.util.function.Supplier;

public class PlayMessages {

    public static class OpenContainer {

        private final ResourceLocation id;
        private final int windowId;
        private final PacketBuffer additionalData;

        public OpenContainer(ResourceLocation id, int windowId, PacketBuffer additionalData) {
            this.id = id;
            this.windowId = windowId;
            this.additionalData = additionalData;
        }

        public static void encode(OpenContainer msg, PacketBuffer buf) {
            buf.writeResourceLocation(msg.id);
            buf.writeVarInt(msg.windowId);
            buf.writeByteArray(msg.additionalData.readByteArray());
        }

        public static OpenContainer decode(PacketBuffer buf) {
            return new OpenContainer(buf.readResourceLocation(), buf.readVarInt(), new PacketBuffer(Unpooled.wrappedBuffer(buf.readByteArray(32600))));
        }

        public static boolean handle(OpenContainer msg, Supplier<NetworkEvent.Context> ctx) {
            ctx.get().enqueueWork(() -> GuiManager.INSTANCE.openContainerScreen(msg.id, msg.windowId, msg.additionalData));
            return true;
        }
    }
}
