package icyllis.modern.api;

import icyllis.modern.api.handler.INetworkHandler;
import icyllis.modern.api.handler.IGuiHandler;
import icyllis.modern.system.ModernUI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.concurrent.atomic.AtomicReference;

public enum ModernUI_API {
    INSTANCE;

    private INetworkHandler network;

    private IGuiHandler gui;

    {
        try {
            Class ac = Class.forName("icyllis.modern.system.NetworkHandler");
            network = (INetworkHandler) ac.getField("INSTANCE").get(ac);
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ac = Class.forName("icyllis.modern.system.GuiHandler");
                gui = (IGuiHandler) ac.getField("INSTANCE").get(ac);
            }
            ModernUI.LOGGER.info(ModernUI.MARKER, "ModernUI API has been initialized");
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public INetworkHandler getNetworkHandler() {
        return network;
    }

    @OnlyIn(Dist.CLIENT)
    public IGuiHandler getGuiHandler() {
        return gui;
    }

}
