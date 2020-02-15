package icyllis.modern.ui.master;

import icyllis.modern.api.animation.IAnimationBuilder;
import icyllis.modern.ui.animation.DisposableAnimation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@OnlyIn(Dist.CLIENT)
public class GlobalAnimationManager {

    public static final GlobalAnimationManager INSTANCE = new GlobalAnimationManager();

    private final List<DisposableAnimation> animations = new ArrayList<>();

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
        animations.removeIf(DisposableAnimation::isFinish);
    }

    public void resetTimer() {
        time = 0;
        timer = 0;
    }

    public DisposableAnimation create(Consumer<IAnimationBuilder> builder, Consumer<Float> receiver) {
        DisposableAnimation a = new DisposableAnimation(time, receiver);
        builder.accept(a);
        animations.add(a);
        return a;
    }

    void clearAll() {
        animations.clear();
    }

    public float time() {
        return time;
    }
}
