package icyllis.modern.api;

import icyllis.modern.api.network.INetworkHandler;

public class ModernAPI {

    private static ModernAPI api;

    private final INetworkHandler networkHandler;

    public ModernAPI(INetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
        api = this;
    }

    public INetworkHandler network() {
        return networkHandler;
    }

    public synchronized static ModernAPI instance() {
        return api;
    }

}
