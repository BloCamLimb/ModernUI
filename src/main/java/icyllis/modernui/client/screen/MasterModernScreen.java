package icyllis.modernui.client.screen;

import icyllis.modernui.api.module.IModernScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MasterModernScreen<T extends Screen & IModernScreen> extends Screen {

    private T screen;

    public MasterModernScreen(T screen) {
        super(screen.getTitle());
        this.screen = screen;
    }
}
