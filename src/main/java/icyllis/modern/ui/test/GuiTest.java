package icyllis.modern.ui.test;

import icyllis.modern.api.module.IGuiScreen;
import icyllis.modern.api.global.IModuleList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class GuiTest implements IGuiScreen {

    private static final ITextComponent TEST_TITLE = new StringTextComponent("Test Screen");

    public GuiTest(PacketBuffer buffer) {

    }

    @Override
    public void addModules(IModuleList list) {
        list.add(new ModuleTest());
    }

    @Override
    public ITextComponent getTitle() {
        return TEST_TITLE;
    }
}
