package icyllis.modern.ui.master;

import icyllis.modern.api.internal.IGlobalManager;
import icyllis.modern.api.module.IModernGui;
import icyllis.modern.system.ModernUI;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.inventory.ContainerScreen;
import net.minecraft.inventory.container.Container;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.glfw.GLFW;

import javax.annotation.Nonnull;

@OnlyIn(Dist.CLIENT)
public final class UniversalModernScreenG<G extends Container> extends ContainerScreen<G> {

    private IGlobalManager manager = GlobalModuleManager.INSTANCE;

    public UniversalModernScreenG(IModernGui injector, G container) {
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

    @Override
    public boolean changeFocus(boolean p_changeFocus_1_) {
        ModernUI.LOGGER.info("chang foc");
        return false;
    }

    @Override
    public void mouseMoved(double p_212927_1_, double p_212927_3_) {
        //ModernUI.LOGGER.info("{} {}", p_212927_1_, p_212927_3_);
    }

    @Override
    public boolean keyPressed(int p_keyPressed_1_, int p_keyPressed_2_, int p_keyPressed_3_) {
        ModernUI.LOGGER.info("{} {} {}", p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);

        return super.keyPressed(p_keyPressed_1_, p_keyPressed_2_, p_keyPressed_3_);
    }
}
