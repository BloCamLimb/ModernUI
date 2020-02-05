package icyllis.modern.ui.test;

import icyllis.modern.api.global.IElementBuilder;
import icyllis.modern.system.ModernUI;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;

public class ModuleTest {

    public static final ResourceLocation BACKGROUND = new ResourceLocation(ModernUI.MODID, "gui/gui_default_background.png");
    public static final ResourceLocation FRAME = new ResourceLocation(ModernUI.MODID, "gui/gui_default_frame.png");
    public static final ResourceLocation BUTTON = new ResourceLocation(ModernUI.MODID, "gui/gui_button.png");

    public void create(IElementBuilder builder) {
        builder.defaultBackground();
        builder.texture()
                .tex(BACKGROUND)
                .setRelPos(-128, -128)
                .uv(0, 0)
                .setSize(256, 256)
                .animated()
                .alpha(a -> a
                        .translate(-1f)
                        .fixedTiming(4f)
                );
        builder.texture()
                .tex(FRAME)
                .setRelPos(-128, -128)
                .uv(0, 0)
                .setSize(256, 256)
                .color(() -> 0xeedc82)
                .animated()
                .alpha(a -> a
                        .translate(-1f)
                        .fixedTiming(4f)
                );
        builder.text()
                .text(() -> TextFormatting.AQUA + "Please select a network")
                .setRelPos(0, -73)
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
                            .setRelPos(18 * f - 76, -99)
                            .setSize(16, 16)
                            .animated()
                            .alpha(a -> a
                                    .delay(f * 0.7f)
                                    .translate(-1f)
                                    .fixedTiming(2f)
                            )
                    )
                    .setRelPos(18 * f - 76, -99)
                    .setSize(16, 16)
                    .text(e -> e
                            .text(() -> "Network Connections")
                            .align(0.25f)
                            .setRelPos(18 * f - 68, -109)
                    )
                    .to(i);
        }
        builder.navigation()
                .tex(e -> e
                        .tex(BUTTON)
                        .uv(112, 0)
                        .setRelPos(60, -99)
                        .setSize(16, 16)
                        .animated()
                        .alpha(a -> a
                                .delay(5f)
                                .translate(-1f)
                                .fixedTiming(2f)
                        )
                )
                .setRelPos(60, -99)
                .setSize(16, 16)
                .text(e -> e
                        .text(() -> "Create New Network")
                        .align(0.25f)
                        .setRelPos(68, -109)
                )
                .to(7);
        builder.text()
                .text(() -> "INFORMATION")
                .setRelPos(-68, -58)
                .scale(() -> 0.8f)
                .style()
                .animated()
                .alpha(a -> a
                        .delay(2)
                        .translate(-1)
                        .fixedTiming(4)
                );
        builder.text()
                .text(() -> "CONNECTION")
                .setRelPos(-68, 0)
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
