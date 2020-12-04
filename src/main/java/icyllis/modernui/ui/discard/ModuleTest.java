/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.discard;

import icyllis.modernui.view.UITools;
import icyllis.modernui.widget.SSExpandableBox;
import icyllis.modernui.widget.MultiPageScrollPanel;
import icyllis.modernui.widget.NumberInputField;
import icyllis.modernui.widget.SlidingToggleButton;
import icyllis.modernui.widget.StaticFrameButton;
import icyllis.modernui.widget.TextField;

@Deprecated
public class ModuleTest extends Module {

    /*public static final ResourceLocation BACKGROUND = new ResourceLocation(ModernUI.MODID, "textures/gui/gui_default_background.png");
    public static final ResourceLocation FRAME = new ResourceLocation(ModernUI.MODID, "textures/gui/gui_default_frame.png");
    public static final ResourceLocation BUTTON = new ResourceLocation(ModernUI.MODID, "textures/gui/gui_button.png");*/

    public ModuleTest() {
        addDrawable(new TestDraw());

        addWidget(new SlidingToggleButton.Builder(0x8020a0e0, 0x40808080, 4)
                .setLocator(new Locator(-10, -60))
                .build(this)
                .buildCallback(true, null));

        NumberInputField h;
        addWidget(h = new NumberInputField(this, new NumberInputField.Builder()
                .setWidth(120)
                .setHeight(12)
                .setLocator(new Locator(-60, -44))));
        h.setLimit(-54996, Long.MAX_VALUE);
        h.setDecoration(f -> new TextField.Frame(f, "Limit:", -1));
        h.setNumberListener(e -> {}, true);
        TextField password;
        addWidget(password = new TextField(this, new TextField.Builder()
                .setHeight(12)
                .setWidth(120)
                .setLocator(new Locator(-60, 4))));
        password.setDecoration(f -> new TextField.Frame(f, "Password:", -1));
        password.setEchoChar(UITools.BLACK_CIRCLE);

        SSExpandableBox sse = new SSExpandableBox(60, 80, SSExpandableBox.RIGHT_TOP_BOTTOM);
        addDrawable(sse);

        addWidget(new StaticFrameButton.Builder("Repeatable Btn")
                .setWidth(64)
                .setLocator(new Locator(-100, -20))
                .build(this)
                .buildCallback(sse::iterateOpen));

        StaticFrameButton a = new StaticFrameButton.Builder("Onetime Btn")
                .setWidth(64)
                .setLocator(new Locator(-20, -20))
                .build(this);
        a.buildCallback(() -> a.setStatus(WidgetStatus.INACTIVE, false));
        addWidget(a);

        addWidget(new MultiPageScrollPanel<>(this,
                new MultiPageScrollPanel.Builder(7)
                        .setWidth(120)
                        .setHeight(70)
                        .setAlign(Align9D.TOP_CENTER)
                        .setLocator(new Locator(0, 20)),
                TestScrollGr::new));
    }

    /*public void create() {

        builder.texture()
                .setTexture(BACKGROUND)
                .setPos(-128, -128)
                .setUV(0, 0)
                .setSize(256, 256)
                .buildToPool();
        builder.texture()
                .setTexture(FRAME)
                .setPos(-128, -128)
                .setUV(0, 0)
                .setSize(256, 256)
                .setTint(0xeedc82)
                .buildToPool();
        builder.textLine()
                .text(() -> TextFormatting.AQUA + "Please select a network")
                .setPos(0, -73)
                .align(0.25f);*/
        /*for (int i = 0; i < 7; i++) {
            int f = i;
            builder.navigation()
                    .setTexture(e -> e
                            .tex(BUTTON)
                            .uv(16 * f, 0)
                            .setRelPos(18 * f - 76, -99)
                            .setSize(16, 16)
                    )
                    .setRelPos(18 * f - 76, -99)
                    .setSize(16, 16)
                    .text(e -> e
                            .text(() -> "Network Connections")
                            .align(0.25f)
                            .setRelPos(18 * f - 68, -109)
                    )
                    .setTarget(i);
        }
        builder.navigation()
                .setTexture(e -> e
                        .tex(BUTTON)
                        .uv(112, 0)
                        .setRelPos(60, -99)
                        .setSize(16, 16)
                )
                .setRelPos(60, -99)
                .setSize(16, 16)
                .onMouseHoverOn(g -> g.getTexture().uv(112, 16))
                .text(e -> e
                        .text(() -> "Create New Network")
                        .align(0.25f)
                        .setRelPos(68, -109)
                )
                .setTarget(7);

    }*/

}
