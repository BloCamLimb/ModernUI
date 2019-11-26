package icyllis.modern.api;

import icyllis.modern.api.global.INetworkHandler;
import icyllis.modern.api.global.IGuiManager;

public enum ModernUIApi {
    INSTANCE;

    private final INetworkHandler network;

    private final IGuiManager gui;

    {
        try {
            Class ac = Class.forName("icyllis.modern.system.NetworkHandler");
            network = (INetworkHandler) ac.getField("INSTANCE").get(ac);
            ac = Class.forName("icyllis.modern.system.GuiManager");
            gui = (IGuiManager) ac.getField("INSTANCE").get(ac);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public INetworkHandler network() {
        return network;
    }

    public IGuiManager gui() {
        return gui;
    }

}
