package icyllis.modern.ui.master;

import icyllis.modern.api.animation.IAlphaAnimation;
import icyllis.modern.ui.animation.AlphaAnimation;
import icyllis.modern.ui.animation.IAnimation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class GlobalAnimationManager {

    public static final GlobalAnimationManager INSTANCE = new GlobalAnimationManager();

    public final List<IAnimation> animations = new ArrayList<>();

    private int timer = 0;
    private float time = 0; // 0~20 every second
    private float sin = 0; // sine wave Ω=10

    public GlobalAnimationManager() {

    }

    public void clientTick() {
        timer++;
    }

    public void renderTick(float partialTick) {
        time = timer + partialTick;
        sin = (float) Math.sin(time * Math.PI);
        animations.forEach(a -> a.update(time));
        animations.removeIf(IAnimation::isFinish);
    }

    public void resetTimer() {
        time = 0;
        timer = 0;
    }

    public AlphaAnimation newAlpha(float st) {
        AlphaAnimation a = new AlphaAnimation(st);
        animations.add(a);
        return a;
    }

    public float time() {
        return time;
    }

    public float sin() {
        return sin;
    }
}
