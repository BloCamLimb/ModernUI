package icyllis.modernui.api;

import icyllis.modernui.api.animation.IAnimationHandler;
import icyllis.modernui.api.network.INetworkHandler;

public enum ModernAPI {
    INSTANCE;

    private IAnimationHandler animationHandler;

    private INetworkHandler networkHandler;

    public IAnimationHandler animationHandler() {
        return animationHandler;
    }

    public INetworkHandler networkHandler() {
        return networkHandler;
    }

    public void setAnimationHandler(IAnimationHandler animationHandler) {
        this.animationHandler = animationHandler;
    }

    public void setNetworkHandler(INetworkHandler networkHandler) {
        this.networkHandler = networkHandler;
    }

}
