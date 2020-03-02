package icyllis.modernui.api;

import icyllis.modernui.api.handler.IModuleManager;
import icyllis.modernui.api.handler.INetworkManager;
import icyllis.modernui.api.handler.IGuiManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;
import net.minecraftforge.fml.loading.FMLEnvironment;

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
