package icyllis.modern.ui.master;

import icyllis.modern.core.ModernUI;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.model.animation.Animation;

@OnlyIn(Dist.CLIENT)
public class GlobalAnimationManager {

    public static final GlobalAnimationManager INSTANCE = new GlobalAnimationManager();

    private int tickCycle = 0;
    private float timer = 0; // 0~20 every second
    private double sin = 0; // sine wave Ω=10

    public GlobalAnimationManager() {

    }

    public void tick() {
        tickCycle++;
        tickCycle %= 20;
    }

    public void tick(float partialTick) {
        timer = tickCycle + partialTick;
        sin = Math.sin(timer * Math.PI);
    }

    public float getTimer() {
        return timer;
    }

    public double getSin() {
        return sin;
    }
}
