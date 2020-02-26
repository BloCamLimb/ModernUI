package icyllis.modernui.api;

import icyllis.modernui.api.handler.INetworkHandler;
import icyllis.modernui.api.handler.IGuiHandler;
import icyllis.modernui.system.ModernUI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.loading.FMLEnvironment;

public enum ModernUI_API {
    INSTANCE;

    private INetworkHandler network;

    private IGuiHandler gui;

    {
        try {
            Class<?> ac = Class.forName("icyllis.modernui.system.NetworkHandler");
            network = (INetworkHandler) ac.getField("INSTANCE").get(ac);
            if (FMLEnvironment.dist == Dist.CLIENT) {
                ac = Class.forName("icyllis.modernui.system.GuiHandler");
                gui = (IGuiHandler) ac.getField("INSTANCE").get(ac);
            }
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
