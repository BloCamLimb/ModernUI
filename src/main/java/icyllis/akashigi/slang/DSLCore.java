/*
 * Akashi GI.
 * Copyright (C) 2022-2022 BloCamLimb. All rights reserved.
 *
 * Akashi GI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Akashi GI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Akashi GI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.akashigi.slang;

public final class DSLCore {

    /**
     * Starts the DSL on the current thread for compiling shader modules.
     */
    public static void start(Compiler compiler, ModuleKind kind,
                             ModuleSettings settings, Module baseModule) {
        if (ThreadContext.isActive()) {
            throw new IllegalStateException("DSL is already started");
        }
        new ThreadContext(compiler, kind, settings, baseModule, false);
    }

    /**
     * Starts the DSL on the current thread for compiling base modules (include files).
     */
    public static void startModule(Compiler compiler, ModuleKind kind,
                                   ModuleSettings settings, Module baseModule) {
        if (ThreadContext.isActive()) {
            throw new IllegalStateException("DSL is already started");
        }
        new ThreadContext(compiler, kind, settings, baseModule, true);
    }

    /**
     * Ends the DSL on the current thread.
     */
    public static void end() {
        ThreadContext.getInstance().kill();
    }
}
