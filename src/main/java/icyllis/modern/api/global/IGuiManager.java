package icyllis.modern.api.global;

import icyllis.modern.api.module.IGuiScreen;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Function;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public interface IGuiManager {

    /**
     * Register a gui with container
     *
     * @param id registry name
     * @param factory factory to create container
     * @param gui gui to display
     */
    <M extends Container> void registerContainerGui(ResourceLocation id, IContainerFactory<M> factory, Function<PacketBuffer, IGuiScreen> gui);

    /**
     * Open a gui on client side.
     */
    void openGui(Supplier<IGuiScreen> guiSupplier);
}
