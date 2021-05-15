/*
 * Modern UI.
 * Copyright (C) 2019-2021 BloCamLimb. All rights reserved.
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

package icyllis.modernui.core;

/**
 * Represents a plugin to Modern UI. All instances will be created from Modern UI system.
 * <p>
 * Creating a plugin by {@link DefinePlugin}, for example:
 * <pre>
 * &#64;DefinePlugin("example")
 * public class MPlugin extends Plugin {
 *      private static Plugin sInstance;
 *
 *      public MPlugin() {
 *          sInstance = this;
 *      }
 *
 *      public static Plugin get() {
 *          return sInstance;
 *      }
 * }
 * </pre>
 *
 * @see DefinePlugin
 */
public class Plugin extends ContextWrapper {

    public Plugin() {

    }
}
