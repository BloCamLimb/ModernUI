package icyllis.modern.api.global;

import icyllis.modern.api.module.IModernGui;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public interface IGuiManager {

    /**
     * Register a gui with container
     *
     * @param id registry name
     * @param factory factory to create container
     * @param screen gui to display
     */
    <M extends Container> void registerContainerGui(ResourceLocation id, IContainerFactory<M> factory, Supplier<IModernGui> screen);

    /**
     * Open a gui on client side.
     */
    void openGui(Supplier<IModernGui> guiSupplier);
}
