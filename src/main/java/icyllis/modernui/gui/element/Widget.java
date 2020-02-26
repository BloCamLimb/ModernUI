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

package icyllis.modernui.gui.element;

import icyllis.modernui.api.element.*;
import icyllis.modernui.system.ModernUI;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;

import java.util.function.Consumer;

/**
 * Universal
 */
public class Widget extends Base implements IWidgetBuilder, IWidgetModifier {

    private static Marker MARKER = MarkerManager.getMarker("WIDGET");

    public EventListener listener = new EventListener(this);;

    private TextLine constText = TextLine.DEFAULT, hoverText = TextLine.DEFAULT;

    private Texture2D texture;

    @Override
    public void draw() {
        texture.draw();
        constText.draw();
        hoverText.draw();
    }

    @Override
    public void resize(int width, int height) {
        listener.resize(width, height);
        texture.resize(width, height);
        constText.resize(width, height);
        hoverText.resize(width, height);
    }

    @Override
    public IWidgetBuilder createConstTextLine(Consumer<ITextLineBuilder> builderConsumer) {
        TextLine t = new TextLine();
        builderConsumer.accept(t);
        constText = t;
        return this;
    }

    @Override
    public IWidgetBuilder createHoverTextLine(Consumer<ITextLineBuilder> builderConsumer) {
        TextLine t = new TextLine();
        builderConsumer.accept(t);
        hoverText = t;
        return this;
    }

    @Override
    public IWidgetBuilder createTexture(Consumer<ITextureBuilder> builderConsumer) {
        Texture2D t = new Texture2D();
        builderConsumer.accept(t);
        texture = t;
        return this;
    }

    @Override
    public IWidgetBuilder initEventListener(Consumer<IEventListenerBuilder> builderConsumer) {
        builderConsumer.accept(listener);
        return this;
    }

    @Override
    public IWidgetBuilder onHoverOn(Consumer<IWidgetModifier> consumer) {
        if (listener == null) {
            ModernUI.LOGGER.error(MARKER, "EventListener was not created");
        } else {
            listener.addHoverOn(consumer);
        }
        return this;
    }

    @Override
    public IWidgetBuilder onHoverOff(Consumer<IWidgetModifier> consumer) {
        if (listener == null) {
            ModernUI.LOGGER.error(MARKER, "EventListener was not created");
        } else {
            listener.addHoverOff(consumer);
        }
        return this;
    }

    @Override
    public IWidgetBuilder onLeftClick(Consumer<IWidgetModifier> consumer) {
        if (listener == null) {
            ModernUI.LOGGER.error(MARKER, "EventListener was not created");
        } else {
            listener.addLeftClick(consumer);
        }
        return this;
    }

    @Override
    public ITextureBuilder getTexture() {
        return texture;
    }
}
