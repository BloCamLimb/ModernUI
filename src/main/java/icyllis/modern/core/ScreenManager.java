package icyllis.modern.core;

import icyllis.modern.api.ModernUIInject;
import icyllis.modern.api.module.ModernUIType;
import icyllis.modern.api.internal.IScreenManager;
import icyllis.modern.api.module.IModernScreen;
import icyllis.modern.ui.master.UniversalModernScreen;
import icyllis.modern.ui.master.UniversalModernScreenG;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.container.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.play.client.CCloseWindowPacket;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.ModFileScanData;
import org.objectweb.asm.Type;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public enum ScreenManager implements IScreenManager {
    INSTANCE;

    private final Map<Integer, IContainerFactory> CONTAINERS = new HashMap<>();
    private final Map<Integer, Supplier<IModernScreen>> SCREENS = new HashMap<>();
    private int registryId = 0;

    @OnlyIn(Dist.CLIENT)
    public void openContainerScreen(int id, int windowId, PacketBuffer extraData) {
        if (SCREENS.containsKey(id) && CONTAINERS.containsKey(id)) {
            IModernScreen screen = SCREENS.get(id).get();
            IContainerFactory factory = CONTAINERS.get(id);
            PacketBuffer copied = new PacketBuffer(extraData.copy());
            Container container = factory.create(windowId, Minecraft.getInstance().player.inventory, extraData);
            screen.updateData(copied);
            Minecraft.getInstance().player.openContainer = container;
            Minecraft.getInstance().displayGuiScreen(new UniversalModernScreenG<>(screen, container));
        } else {
            Minecraft.getInstance().player.connection.sendPacket(new CCloseWindowPacket(windowId));
        }
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public <M extends Container, T extends TileEntity> void registerContainerScreen(ModernUIType type, IContainerFactory<M> factory, Supplier<IModernScreen> screen) {
        SCREENS.put(type.getId(), screen);
        CONTAINERS.put(type.getId(), factory);
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public void openScreen(Supplier<IModernScreen> screenSupplier) {
        Minecraft.getInstance().displayGuiScreen(new UniversalModernScreen(screenSupplier.get()));
    }

    public void generateUITypes() {
        Type INJECT = Type.getType(ModernUIInject.class);
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
                    throw new RuntimeException("UI type field must be public", e);
                }
            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @FunctionalInterface
    public interface IContainerFactory<T extends Container> {

        T create(int windowId, PlayerInventory playerInventory, PacketBuffer extraData);
    }

    public static class ScreenType implements ModernUIType {

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
