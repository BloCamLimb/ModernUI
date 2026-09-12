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

package icyllis.modernui.system;

import java.io.IOException;
import java.nio.channels.SocketChannel;

/**
 * Represents the context that generates the socket connection.
 * All APIs are thread-safe. See {@link RpcTransportCtxRaw} and {@link RpcTransportCtxTls} for details.
 */
public abstract sealed class RpcTransportCtx permits RpcTransportCtxRaw, RpcTransportCtxTls {

    public abstract RpcTransport newTransport(SocketChannel socket) throws IOException;
}
