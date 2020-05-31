/*
 * Modern UI.
 * Copyright (C) 2019 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.test;

@Deprecated
public class ConfirmWindow {

    /*private String title;

    private String[] info;

    private float frameSizeHOffset;

    private float alpha;

    private TextFrameButton cancelButton;

    private TextFrameButton.Countdown confirmButton;

    public ConfirmWindow(Consumer<IGuiEventListener> listenerAdder, String title, String info, Consumer<Boolean> callback) {
        this(listenerAdder, title, info, I18n.format("gui.yes"), callback, 0);
    }

    public ConfirmWindow(Consumer<IGuiEventListener> listenerAdder, String title, String info, String confirmText, Consumer<Boolean> callback) {
        this(listenerAdder, title, info, confirmText, callback, 0);
    }

    *//**
     * Constructor
     * @param countdown Countdown to confirm button available (unit-seconds)
     *//*
    public ConfirmWindow(Consumer<IGuiEventListener> listenerAdder, String title, String info, String confirmText, Consumer<Boolean> callback, int countdown) {
        super(width -> width / 2f - 90f, height -> height / 2f - 40f);
        this.title = title;
        this.info = FontTools.splitStringToWidth(info, 164);
        cancelButton = new TextFrameButton(w -> w / 2f + 80f, h -> h / 2f + 20f, I18n.format("gui.no"), () -> callback.accept(false), true);
        confirmButton = new TextFrameButton.Countdown(w -> w / 2f + 74f - cancelButton.sizeW, h -> h / 2f + 20f, confirmText, () -> callback.accept(true), true, countdown);
        cancelButton.setTextAlpha(0);
        confirmButton.setTextAlpha(0);
        moduleManager.addAnimation(new Animation(3, true)
                .applyTo(new Applier(0, 80, value -> frameSizeHOffset = value)));
        moduleManager.addAnimation(new Animation(3)
                .applyTo(new Applier(1, value -> {
                    alpha = value;
                    cancelButton.setTextAlpha(value);
                    confirmButton.setTextAlpha(value);
                }))
                .withDelay(3));
        listenerAdder.accept(cancelButton);
        listenerAdder.accept(confirmButton);
    }

    @Override
    public void draw(float currentTime) {
        DrawTools.fillRectWithFrame(x, y, x + 180, y + frameSizeHOffset, 0.51f, 0x101010, 0.7f, 0x404040, 1.f);
        DrawTools.fillRectWithColor(x, y, x + 180, y + Math.min(frameSizeHOffset, 16), 0x080808, 0.85f);
        fontRenderer.drawString(title, x + 90, y + 4, 1, alpha, TextAlign.CENTER);
        int i = 0;
        for (String t : info) {
            fontRenderer.drawString(t, x + 8, y + 24 + i++ * 12, 1, alpha);
        }
        cancelButton.draw(currentTime);
        confirmButton.draw(currentTime);
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        cancelButton.resize(width, height);
        confirmButton.resize(width, height);
    }

    @Override
    public void tick(int ticks) {
        confirmButton.tick(ticks);
    }*/
}
