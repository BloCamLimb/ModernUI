package icyllis.modern.api;

import icyllis.modern.api.internal.INetworkHandler;
import icyllis.modern.api.internal.IScreenManager;

public enum ModernUIAPI {
    INSTANCE;

    private final INetworkHandler network;

    private final IScreenManager screen;

    {
        try {
            Class ac = Class.forName("icyllis.modern.network.NetworkHandler");
            network = (INetworkHandler) ac.getField("INSTANCE").get(ac);
            ac = Class.forName("icyllis.modern.core.ScreenManager");
            screen = (IScreenManager) ac.getField("INSTANCE").get(ac);
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException | ClassCastException e) {
            throw new RuntimeException(e);
        }
    }

    public INetworkHandler network() {
        return network;
    }

    public IScreenManager screen() {
        return screen;
    }

}
