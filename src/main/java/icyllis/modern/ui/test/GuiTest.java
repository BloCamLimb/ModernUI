package icyllis.modern.ui.test;

import icyllis.modern.api.module.IModernGui;
import icyllis.modern.api.internal.IModuleReceiver;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;

public class GuiTest implements IModernGui {

    private static final ITextComponent TEST_TITLE = new StringTextComponent("Test Screen");

    @Override
    public void createModules(IModuleReceiver receiver) {
        receiver.addModule(new ModuleTest());
    }

    @Override
    public void updateData(PacketBuffer extraData) {
        //ModernUI.logger.info(extraData.readBlockPos());
    }

    @Override
    public ITextComponent getTitle() {
        return TEST_TITLE;
    }
}
