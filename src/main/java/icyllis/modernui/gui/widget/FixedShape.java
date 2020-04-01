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

package icyllis.modernui.gui.widget;

public class FixedShape {

    public boolean isMouseInShape(float posX, float posY, double mouseX, double mouseY) {
        return false;
    }

    public static class Rect extends FixedShape {

        private float width, height;

        public Rect(float width, float height) {
            this.width = width;
            this.height = height;
        }

        @Override
        public boolean isMouseInShape(float posX, float posY, double mouseX, double mouseY) {
            return mouseX >= posX && mouseX <= posX + width && mouseY >= posY && mouseY <= posY + height;
        }
    }

    public static class Circle extends FixedShape {

        private float radius;

        public Circle(float radius) {
            this.radius = radius;
        }

        @Override
        public boolean isMouseInShape(float posX, float posY, double mouseX, double mouseY) {
            return Math.sqrt((mouseX - posX) * (mouseX - posX) + (mouseY - posY) * (mouseY - posY)) <= radius;
        }
    }

    public static class Sector extends FixedShape {

        private float radius;

        private double clockwise, flare;

        public Sector(float radius, double clockwise, double flare) {
            this.radius = radius;
            this.clockwise = Math.tan(clockwise);
            this.flare = Math.tan(flare);
        }

        @Override
        public boolean isMouseInShape(float posX, float posY, double mouseX, double mouseY) {
            boolean inRadius = Math.sqrt((mouseX - posX) * (mouseX - posX) + (mouseY - posY) * (mouseY - posY)) <= radius;
            if (inRadius) {
                double angle = (mouseX - posX) / (posY - mouseY);
                return angle >= clockwise && angle <= clockwise + flare;
            } else {
                return false;
            }
        }
    }
}
