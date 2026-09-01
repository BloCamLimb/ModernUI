/*
 * ModernUI.
 * Copyright (C) 2026 BloCamLimb. All rights reserved.
 *
 * ModernUI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * ModernUI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with ModernUI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.util;

import icyllis.modernui.annotation.NonNull;
import org.jetbrains.annotations.ApiStatus;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * @hidden
 */
@ApiStatus.Internal
public final class Unchecked {

    @SuppressWarnings("unchecked")
    public static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }

    public static <T> T getUninterruptibly(@NonNull Future<T> future) {
        boolean interrupted = false;
        try {
            for (;;) {
                try {
                    return future.get();
                } catch (ExecutionException e) {
                    var cause = e.getCause();
                    throw sneakyThrow(cause != null ? cause : e);
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted)
                Thread.currentThread().interrupt();
        }
    }

    private Unchecked() {
    }
}
