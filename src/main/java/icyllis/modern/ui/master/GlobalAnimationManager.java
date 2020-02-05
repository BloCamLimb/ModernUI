package icyllis.modern.ui.master;

import icyllis.modern.api.animation.IAnimationBuilder;
import icyllis.modern.ui.animation.AlphaAnimation;
import icyllis.modern.ui.animation.UniversalAnimation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

@OnlyIn(Dist.CLIENT)
public class GlobalAnimationManager {

    public static final GlobalAnimationManager INSTANCE = new GlobalAnimationManager();

    private final List<UniversalAnimation> animations = new ArrayList<>();
    private List<Runnable> animationBuilders = new ArrayList<>();

    private int timer = 0;
    private float time = 0; // unit ticks

    private GlobalAnimationManager() {

    }

    public void clientTick() {
        timer++;
    }

    public void renderTick(float partialTick) {
        time = timer + partialTick;
        animations.forEach(a -> a.update(time));
        animations.removeIf(UniversalAnimation::isFinish);
    }

    public void resetTimer() {
        time = 0;
        timer = 0;
    }

    public UniversalAnimation create(Consumer<IAnimationBuilder> consumer, float init) {
        UniversalAnimation a = new UniversalAnimation(time, init);
        consumer.accept(a);
        animations.add(a);
        return a;
    }

    public void scheduleAnimationBuild(Runnable r) {
        animationBuilders.add(r);
    }

    void buildAnimations() {
        animationBuilders.forEach(Runnable::run);
        animationBuilders.clear();
    }

    void clearAll() {
        animations.clear();
        animationBuilders.clear();
    }

    public float time() {
        return time;
    }
}
