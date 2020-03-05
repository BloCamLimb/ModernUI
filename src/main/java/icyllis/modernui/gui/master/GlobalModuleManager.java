package icyllis.modernui.gui.master;

import icyllis.modernui.api.animation.IAnimation;
import icyllis.modernui.api.element.IElement;
import icyllis.modernui.api.global.IModuleFactory;
import icyllis.modernui.api.manager.IModuleManager;
import net.minecraft.client.gui.IGuiEventListener;
import net.minecraft.network.PacketBuffer;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntPredicate;

public class GlobalModuleManager implements IModuleFactory, IModuleManager {

    public static final GlobalModuleManager INSTANCE = new GlobalModuleManager();

    @Nullable
    public IMasterScreen master;

    private PacketBuffer extraData;

    private List<ElementPool> pools = new ArrayList<>();

    private List<IntConsumer> moduleEvents = new ArrayList<>();

    private List<IAnimation> animations = new ArrayList<>();

    private int ticks = 0;

    private float floatingPointTicks = 0;

    private int width, height;

    public void build(IMasterScreen master, int width, int height) {
        this.master = master;
        this.width = width;
        this.height = height;
        this.switchTo(0);
    }
    @Override
    public void switchTo(int id) {
        pools.stream().filter(m -> m.test(id)).forEach(ElementPool::build);
        pools.stream().filter(m -> !m.test(id)).forEach(ElementPool::clear);
        pools.forEach(e -> e.resize(width, height));
        moduleEvents.forEach(e -> e.accept(id));
    }

    @Override
    public void addModuleSwitchEvent(IntConsumer event) {
        moduleEvents.add(event);
    }

    @Override
    public void addEventListener(IGuiEventListener listener) {
        if (master != null) {
            master.addEventListener(listener);
        }
    }

    @Override
    public void addAnimation(IAnimation animation) {
        animation.resize(width, height);
        animations.add(animation);
    }

    public void draw() {
        animations.forEach(e -> e.update(floatingPointTicks));
        pools.forEach(elementPool -> elementPool.draw(floatingPointTicks));
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        pools.forEach(e -> e.resize(width, height));
        animations.forEach(e -> e.resize(width, height));
    }

    public void clear() {
        pools.clear();
        animations.clear();
        master = null;
    }

    @Override
    public IModuleFactory add(IntPredicate availability, Consumer<Consumer<IElement>> pool) {
        pools.add(new ElementPool(availability, pool));
        return this;
    }

    public void clientTick() {
        ticks++;
        animations.removeIf(IAnimation::shouldRemove);
        pools.forEach(e -> e.tick(ticks));
    }

    public void renderTick(float partialTick) {
        floatingPointTicks = ticks + partialTick;
    }

    public void resetTicks() {
        ticks = 0;
        floatingPointTicks = 0;
    }

    public float getAnimationTime() {
        return floatingPointTicks;
    }

    public void setExtraData(PacketBuffer extraData) {
        this.extraData = extraData;
    }

    @Override
    public PacketBuffer getExtraData() {
        return extraData;
    }
}
