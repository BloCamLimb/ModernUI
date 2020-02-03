package icyllis.modern.api;

import icyllis.modern.api.handler.INetworkHandler;
import icyllis.modern.api.handler.IGuiHandler;

public enum ModernUIApi {
    INSTANCE;

    private final INetworkHandler network;

    private final IGuiHandler gui;

    {
        try {
            Class ac = Class.forName("icyllis.modern.system.NetworkHandler");
            network = (INetworkHandler) ac.getField("INSTANCE").get(ac);
            ac = Class.forName("icyllis.modern.system.GuiHandler");
            gui = (IGuiHandler) ac.getField("INSTANCE").get(ac);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public INetworkHandler getNetworkHandler() {
        return network;
    }

    public IGuiHandler getGuiHandler() {
        return gui;
    }

}
