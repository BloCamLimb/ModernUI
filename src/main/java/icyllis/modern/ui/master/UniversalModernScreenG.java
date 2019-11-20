package icyllis.modern.ui.master;

import icyllis.modern.api.internal.IGlobalManager;
import icyllis.modern.api.module.IModernScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraft.util.text.ITextComponent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public final class UniversalModernScreenG<G extends Container> extends ContainerScreen<G> {

    private IGlobalManager manager = GlobalModuleManager.INSTANCE;

    public UniversalModernScreenG(IModernScreen injector, G container) {
        super(container, Minecraft.getInstance().player.inventory, injector.getTitle());
        injector.createModules(manager);
    }

    @Override
    protected void init() {
        super.init();
        manager.build(width, height);
    }

    @Override
    public void resize(@Nonnull Minecraft minecraft, int width, int height) {
        //super.resize(minecraft, width, height);
        manager.resize(width, height);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float partialTicks, int mouseX, int mouseY) {

    }

    @Override
    public void render(int mouseX, int mouseY, float partialTicks) {
        manager.draw();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void removed() {
        manager.clear();
    }
}
