package icyllis.modernui.client.handler;

import icyllis.modernui.api.ModernAPI;
import icyllis.modernui.api.animation.IAnimationHandler;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public enum AnimationHandler implements IAnimationHandler {
    INSTANCE;

    public void setup() {
        ModernAPI.INSTANCE.setAnimationHandler(this);
    }
}
