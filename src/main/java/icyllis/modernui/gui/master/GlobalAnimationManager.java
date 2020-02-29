package icyllis.modernui.gui.master;

import icyllis.modernui.api.global.IAnimationBuilder;
import icyllis.modernui.gui.animation.DisposableAnimation;
import icyllis.modernui.gui.animation.HighStatusAnimation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@OnlyIn(Dist.CLIENT)
public class GlobalAnimationManager {

    public static final GlobalAnimationManager INSTANCE = new GlobalAnimationManager();

    private final List<DisposableAnimation> dAnimations = new ArrayList<>();
    private final List<HighStatusAnimation> hsAnimations = new ArrayList<>();

    private int timer = 0;
    private float time = 0; // unit ticks

    private int width, height;

    private GlobalAnimationManager() {

    }

    public void clientTick() {
        timer++;
        dAnimations.removeIf(DisposableAnimation::isFinish);
    }

    public void renderTick(float partialTick) {
        time = timer + partialTick;
        dAnimations.forEach(a -> a.update(time));
        hsAnimations.forEach(a -> a.update(time));
    }

    public void resetTimer() {
        time = 0;
        timer = 0;
    }

    public void create(Consumer<IAnimationBuilder> builder, Consumer<Float> receiver) {
        create(builder, receiver, rs -> {});
    }

    public void create(Consumer<IAnimationBuilder> builder, Consumer<Float> receiver, Consumer<Function<Integer, Float>> relativeReceiver) {
        DisposableAnimation a = new DisposableAnimation(time, receiver, relativeReceiver);
        builder.accept(a);
        a.resize(width, height);
        dAnimations.add(a);
    }

    public Consumer<Boolean> createHS(Consumer<IAnimationBuilder> builder, Consumer<Float> receiver) {
        return createHS(builder, receiver, rs -> {});
    }

    public Consumer<Boolean> createHS(Consumer<IAnimationBuilder> builder, Consumer<Float> receiver, Consumer<Function<Integer, Float>> relativeReceiver) {
        HighStatusAnimation a = new HighStatusAnimation(receiver, relativeReceiver);
        builder.accept(a);
        a.resize(width, height);
        hsAnimations.add(a);
        return a;
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        dAnimations.forEach(e -> e.resize(width, height));
        hsAnimations.forEach(e -> e.resize(width, height));
    }

    void clear() {
        dAnimations.clear();
        hsAnimations.clear();
    }

    public float time() {
        return time;
    }
}
