package icyllis.modernui.gui.master;

import icyllis.modernui.api.global.IElementBuilder;
import icyllis.modernui.api.global.IModuleFactory;
import icyllis.modernui.api.handler.IModuleManager;
import icyllis.modernui.gui.element.IPool;
import icyllis.modernui.system.ModernUI;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;

public class GlobalModuleManager implements IModuleFactory, IModuleManager {

    public static final GlobalModuleManager INSTANCE = new GlobalModuleManager();

    @Nullable
    public IModernScreen master;

    private List<MasterModule> modules = new ArrayList<>();

    private List<IPool> pools = new ArrayList<>();

    private Set<IPool> activePools = new HashSet<>();

    private List<IntConsumer> moduleEvents = new ArrayList<>();

    private int width, height;

    public void build(IModernScreen master, int width, int height) {
        this.master = master;
        this.width = width;
        this.height = height;
        GlobalElementBuilder.INSTANCE.setMaster(master);
        this.switchModule(0);
        GlobalAnimationManager.INSTANCE.resize(width, height);
    }
    @Override
    public void switchModule(int id) {
        modules.stream().filter(m -> m.id == id).findFirst().ifPresent(MasterModule::build);
        moduleEvents.forEach(e -> e.accept(id));
        activePools.removeIf(c -> !c.test(id));
        activePools.addAll(pools.stream().filter(c -> c.test(id)).collect(Collectors.toCollection(ArrayList::new)));
        activePools.forEach(e -> e.resize(width, height));
    }

    @Override
    public void addModuleSwitchEvent(IntConsumer event) {
        moduleEvents.add(event);
    }

    public void draw() {
        activePools.forEach(IPool::draw);
    }

    public void resize(int width, int height) {
        this.width = width;
        this.height = height;
        activePools.forEach(e -> e.resize(width, height));
        GlobalAnimationManager.INSTANCE.resize(width, height);
    }

    public void clear() {
        modules.clear();
        pools.clear();
        activePools.clear();
        moduleEvents.clear();
        master = null;
        GlobalAnimationManager.INSTANCE.clear();
    }

    public void add(IPool component) {
        pools.add(component);
    }

    @Override
    public IModuleFactory add(Consumer<IElementBuilder> module, int id) {
        modules.add(new MasterModule(module, id));
        return this;
    }
}
