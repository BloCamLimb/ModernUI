package icyllis.modern.ui.test;

import com.google.gson.Gson;
import icyllis.modern.api.module.IGuiModule;
import icyllis.modern.api.global.IElementBuilder;
import icyllis.modern.system.ModernUI;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public class ModuleTest implements IGuiModule {

    public static final ResourceLocation BACKGROUND = new ResourceLocation(ModernUI.MODID, "gui/gui_default_background.png");
    public static final ResourceLocation FRAME = new ResourceLocation(ModernUI.MODID, "gui/gui_default_frame.png");
    public static final ResourceLocation BUTTON = new ResourceLocation(ModernUI.MODID, "gui/gui_button.png");

    @Override
    public void createElements(IElementBuilder builder) {
        builder.defaultBackground();
        builder.texture()
                .tex(BACKGROUND)
                .pos(-128, -128)
                .uv(0, 0)
                .size(256, 256)
                .animated()
                .alpha(a -> a
                        .translate(-1f)
                        .fixedTiming(4f)
                );
        builder.texture()
                .tex(FRAME)
                .pos(-128, -128)
                .uv(0, 0)
                .size(256, 256)
                .color(() -> 0xeedc82)
                .animated()
                .alpha(a -> a
                        .translate(-1f)
                        .fixedTiming(4f)
                );
        builder.constText()
                .text(TextFormatting.AQUA + "Please select a network")
                .pos(0, -73)
                .align(0.25f)
                .animated()
                .alpha(a -> a
                        .translate(-1)
                        .fixedTiming(16.0f)
                );
        for (int i = 0; i < 7; i++) {
            int f = i;
            builder.navigation()
                    .tex(e -> e
                            .tex(BUTTON)
                            .uv(16 * f, 0)
                            .pos(18 * f - 76, -99)
                            .size(16, 16)
                            .animated()
                            .alpha(a -> a
                                    .delay(f * 0.7f)
                                    .translate(-1f)
                                    .fixedTiming(2f)
                            )
                    )
                    .pos(18 * f - 76, -99)
                    .size(16, 16)
                    .text(e -> e
                            .text(() -> "\u6709\u8bf4")
                            .align(0.25f)
                            .pos(18 * f - 68, -109)
                    )
                    .to(i);
        }
        builder.navigation()
                .tex(e -> e
                        .tex(BUTTON)
                        .uv(112, 0)
                        .pos(60, -99)
                        .size(16, 16)
                        .animated()
                        .alpha(a -> a
                                .delay(5f)
                                .translate(-1f)
                                .fixedTiming(2f)
                        )
                )
                .pos(60, -99)
                .size(16, 16)
                .text(e -> e
                        .text(() -> "Create New Network")
                        .align(0.25f)
                        .pos(68, -109)
                )
                .to(7);
        builder.constText()
                .text("INFORMATION")
                .pos(-68, -58)
                .scale(() -> 0.8f)
                .style()
                .animated()
                .alpha(a -> a
                        .delay(2)
                        .translate(-1)
                        .fixedTiming(4)
                );
        builder.constText()
                .text("CONNECTION")
                .pos(-68, 0)
                .scale(() -> 0.8f)
                .style()
                .animated()
                .alpha(a -> a
                        .delay(4f)
                        .translate(-1)
                        .fixedTiming(4)
                );

    }

}
