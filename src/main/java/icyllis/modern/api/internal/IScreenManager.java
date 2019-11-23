package icyllis.modern.api.internal;

import icyllis.modern.api.module.ModernUIType;
import icyllis.modern.api.module.IModernScreen;
import icyllis.modern.core.ScreenManager;
import net.minecraft.inventory.container.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Supplier;

public interface IScreenManager {

    /**
     * Register a screen with container
     *
     * @param type Screen reference
     * @param factory Factory to create container
     * @param screen Modern screen to display
     */
    @OnlyIn(Dist.CLIENT)
    <M extends Container, T extends TileEntity> void registerContainerScreen(ModernUIType type, ScreenManager.IContainerFactory<M> factory, Supplier<IModernScreen> screen);

    /**
     * Open a screen on client side.
     *
     * @param screenSupplier Screen
     */
    @OnlyIn(Dist.CLIENT)
    void openScreen(Supplier<IModernScreen> screenSupplier);
}
