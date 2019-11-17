package icyllis.modern.core;

import icyllis.modern.api.ModernUITypes;
import icyllis.modern.api.internal.IScreenManager;
import icyllis.modern.api.internal.IScreenType;
import icyllis.modern.api.module.IModernScreen;
import icyllis.modern.ui.master.UniversalModernScreen;
import icyllis.modern.ui.master.UniversalModernScreenG;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public enum ScreenManager implements IScreenManager {
    INSTANCE;

    private static final Type INJECT = Type.getType(ModernUITypes.class);
    private final Map<Integer, IContainerFactory> CONTAINERS = new HashMap<>();
    private final Map<Integer, Supplier<IModernScreen>> SCREENS = new HashMap<>();
    private int registryId = 0;

    @OnlyIn(Dist.CLIENT)
    public void openContainerScreen(int containerId, int windowId, ITextComponent name, BlockPos pos) {
        IModernScreen screen = SCREENS.getOrDefault(containerId, () -> null).get();
        IContainerFactory factory = CONTAINERS.get(containerId);
        ModernUI.logger.info(containerId);
        if(screen != null && factory != null) {
            Container container;
            if(factory instanceof IContainerTileFactory) {
                container = ((IContainerTileFactory) factory).create(windowId, Minecraft.getInstance().player.inventory, Minecraft.getInstance().world.getTileEntity(pos));
            } else {
                container = factory.create(windowId, Minecraft.getInstance().player.inventory);
            }
            Minecraft.getInstance().player.openContainer = container;
            Minecraft.getInstance().displayGuiScreen(new UniversalModernScreenG<>(screen, container, name));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void registerScreen(IScreenType type, Supplier<IModernScreen> screen) {
        SCREENS.put(type.getId(), screen);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public <M extends Container> void registerContainerScreen(IScreenType type, IContainerFactory<M> factory, Supplier<IModernScreen> screen) {
        SCREENS.put(type.getId(), screen);
        CONTAINERS.put(type.getId(), factory);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void openScreen(IScreenType type) {
        if(SCREENS.containsKey(type.getId())) {
            Minecraft.getInstance().displayGuiScreen(new UniversalModernScreen(SCREENS.get(type.getId()).get()));
        }
    }

    public void injectModernScreen() {
        ModList.get().getAllScanData().stream()
                .map(ModFileScanData::getAnnotations)
                .flatMap(Collection::stream)
                .filter(a -> INJECT.equals(a.getAnnotationType()))
                .collect(Collectors.toList())
                .forEach(a -> inject(a.getMemberName()));
    }

    private void inject(String name) {
        try {
            Class clazz = Class.forName(name);
            for(Field f : clazz.getDeclaredFields()) {
                try {
                    f.set(null, new ScreenType(registryId++));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    @OnlyIn(Dist.CLIENT)
    public interface IContainerFactory<T extends Container> {

        T create(int windowId, PlayerInventory playerInventory);
    }

    @OnlyIn(Dist.CLIENT)
    public interface IContainerTileFactory<T extends Container> extends IContainerFactory<T> {

        T create(int windowId, PlayerInventory playerInventory, TileEntity tileEntity);
    }

    public static class ScreenType implements IScreenType {

        private final int id;

        ScreenType(int id) {
            this.id = id;
        }

        @Override
        public int getId() {
            return id;
        }
    }
}
