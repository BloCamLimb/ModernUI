package icyllis.modernui.api;

import icyllis.modernui.api.manager.IModuleManager;
import icyllis.modernui.api.manager.INetworkManager;
import icyllis.modernui.api.manager.IGuiManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

public enum ModernUI_API {
    INSTANCE;

    private INetworkManager network;

    private IGuiManager gui;

    private IModuleManager module;

    public INetworkManager getNetworkManager() {
        return network;
    }

    @OnlyIn(Dist.CLIENT)
    public IGuiManager getGuiManager() {
        return gui;
    }

    @OnlyIn(Dist.CLIENT)
    public IModuleManager getModuleManager() {
        return module;
    }

}
