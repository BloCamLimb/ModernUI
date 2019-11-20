package icyllis.modern.ui.test;

import icyllis.modern.api.module.IModernModule;
import icyllis.modern.api.internal.IElementBuilder;
import net.minecraft.client.Minecraft;

import java.text.DecimalFormat;

public class TestMainModule implements IModernModule {

    @Override
    public void createElements(IElementBuilder builder) {
        DecimalFormat df = new DecimalFormat("0.00");
        builder.defaultBackground();
        builder.textLine()
                .text(() -> "Snownee likes to eat lemons")
                .pos(0, -60, true);
        builder.textLine()
                .text(() -> "World partial ticks: " + df.format(Minecraft.getInstance().getRenderPartialTicks()))
                .pos(0, -20, true);
    }

}
