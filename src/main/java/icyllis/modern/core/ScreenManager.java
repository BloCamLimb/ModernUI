package icyllis.modern.core;

import icyllis.modern.api.ModernUITypes;
import icyllis.modern.api.internal.IScreenManager;
import icyllis.modern.api.module.IModernScreen;
import icyllis.modern.ui.master.UniversalModernScreen;
import icyllis.modern.ui.master.UniversalModernScreenG;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.objectweb.asm.Type;

import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public enum ScreenManager implements IScreenManager {
    INSTANCE;

    private final Marker MARKER = MarkerManager.getMarker("SCREEN");

    private final Map<Integer, IContainerFactory> CONTAINERS = new HashMap<>();
    private final Map<Integer, Supplier<IModernScreen>> SCREENS = new HashMap<>();
    private int registryId = 0;

    @SuppressWarnings("unchecked")
    @OnlyIn(Dist.CLIENT)
    public void openContainerScreen(int id, int windowId, BlockPos pos) {
        if (SCREENS.containsKey(id) && CONTAINERS.containsKey(id)) {
            IModernScreen screen = SCREENS.get(id).get();
            IContainerFactory factory = CONTAINERS.get(id);
            try {
                Container container = factory.create(windowId, Minecraft.getInstance().player.inventory, pos == null ? null : Minecraft.getInstance().world.getTileEntity(pos));
                Minecraft.getInstance().player.openContainer = container;
                Minecraft.getInstance().displayGuiScreen(new UniversalModernScreenG<>(screen, container));
            } catch (final ClassCastException e) {
                Minecraft.getInstance().player.connection.sendPacket(new CCloseWindowPacket(windowId));
                ModernUI.logger.warn(MARKER, "Failed to open container screen. Tile entity at {} can't be cast to target container constructor", pos, e);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void registerScreen(ModernUITypes.Type type, Supplier<IModernScreen> screen) {
        SCREENS.put(type.getId(), screen);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public <M extends Container, T extends TileEntity> void registerContainerScreen(ModernUITypes.Type type, IContainerFactory<M, T> factory, Supplier<IModernScreen> screen) {
        registerScreen(type, screen);
        CONTAINERS.put(type.getId(), factory);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void openScreen(ModernUITypes.Type type) {
        if (SCREENS.containsKey(type.getId())) {
            Minecraft.getInstance().displayGuiScreen(new UniversalModernScreen(SCREENS.get(type.getId()).get()));
        }
    }

    public void generateUITypes() {
        Type INJECT = Type.getType(ModernUITypes.class);
        ModList.get().getAllScanData().stream()
                .map(ModFileScanData::getAnnotations)
                .flatMap(Collection::stream)
                .filter(a -> INJECT.equals(a.getAnnotationType()))
                .collect(Collectors.toList())
                .forEach(a -> distribute(a.getMemberName()));
    }

    private void distribute(String name) {
        try {
            Class clazz = Class.forName(name);
            for (Field f : clazz.getDeclaredFields()) {
                try {
                    Field modifiers = f.getClass().getDeclaredField("modifiers");
                    modifiers.setAccessible(true);
                    modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                    f.set(null, new ScreenType(registryId++));
                    modifiers.setInt(f, f.getModifiers() & ~Modifier.FINAL);
                } catch (IllegalAccessException | NoSuchFieldException e) {
                    throw new IllegalAccessException("UI type field must be public");
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @FunctionalInterface
    public interface IContainerFactory<T extends Container, G extends TileEntity> {

        T create(int windowId, PlayerInventory playerInventory, @Nullable G tileEntity);
    }

    public static class ScreenType implements ModernUITypes.Type {

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
