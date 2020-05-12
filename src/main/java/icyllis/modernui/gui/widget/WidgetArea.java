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

/**
 * This is an template for complex shape such as circle, sector
 */
public interface WidgetArea {

    /**
     * Check if mouse is in this area
     * @param x widget x pos
     * @param y widget y pos
     * @param mouseX mouse x pos
     * @param mouseY mouse y pos
     * @return is in area
     */
    boolean isMouseInArea(float x, float y, double mouseX, double mouseY);

    final class Circle implements WidgetArea {

        private float radius;

        public Circle(float radius) {
            this.radius = radius;
        }

        @Override
        public boolean isMouseInArea(float x, float y, double mouseX, double mouseY) {
            return Math.sqrt((mouseX - x) * (mouseX - x) + (mouseY - y) * (mouseY - y)) <= radius;
        }

        public float getRadius() {
            return radius;
        }
    }

    final class Sector implements WidgetArea {

        private float radius;

        private double clockwise, flare;

        /**
         * Use radian, and clockwise direction
         * @param radius radius
         * @param clockwise start angle
         * @param flare flare angle
         */
        public Sector(float radius, double clockwise, double flare) {
            this.radius = radius;
            this.clockwise = Math.tan(clockwise);
            this.flare = Math.tan(flare);
        }

        @Override
        public boolean isMouseInArea(float x, float y, double mouseX, double mouseY) {
            boolean inRadius = Math.sqrt((mouseX - x) * (mouseX - x) + (mouseY - y) * (mouseY - y)) <= radius;
            if (inRadius) {
                double angle = (mouseX - x) / (y - mouseY);
                return angle >= clockwise && angle <= clockwise + flare;
            } else {
                return false;
            }
        }

        public float getRadius() {
            return radius;
        }
    }
}
