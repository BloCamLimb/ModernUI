package icyllis.modern.ui.master;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GlobalAnimationManager {

    public static final GlobalAnimationManager INSTANCE = new GlobalAnimationManager();

    private int cycle = 0;
    private float time = 0; // 0~20 every second
    private float sin = 0; // sine wave Ω=10

    public GlobalAnimationManager() {

    }

    public void tick() {
        cycle++;
        cycle %= 20;
    }

    public void tick(float partialTick) {
        time = cycle + partialTick;
        sin = (float) Math.sin(time * Math.PI);
    }

    public float time() {
        return time;
    }

    public float sin() {
        return sin;
    }
}
