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

import icyllis.modernui.annotation.NonNull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.StandardProtocolFamily;
import java.net.StandardSocketOptions;
import java.net.UnixDomainSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.UnsupportedAddressTypeException;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class RpcServer {

    private final RpcTransportCtx mCtx;

    private final ReentrantLock mLock = new ReentrantLock();
    private final Condition mShutdownCv = mLock.newCondition();

    private ServerSocketChannel mServer;

    private Thread mJoinThread;
    private boolean mJoinThreadRunning;

    private final HashMap<String, RpcSession> mSessions = new HashMap<>();

    public RpcServer(RpcTransportCtx ctx) {
        mCtx = ctx;
    }

    public void setupExternalServer(@NonNull ServerSocketChannel server) throws IOException {
        server.configureBlocking(true);

        mLock.lock();
        try {
            if (mServer != null) {
                throw new IllegalStateException("Each RpcServer can only have one server");
            }
            mServer = server;
        } finally {
            mLock.unlock();
        }
    }

    public SocketAddress setupSocketServer(@NonNull SocketAddress localAddr) throws IOException {

        ServerSocketChannel socket;
        if (localAddr instanceof InetSocketAddress) {
            socket = ServerSocketChannel.open();
            socket.setOption(StandardSocketOptions.TCP_NODELAY, Boolean.TRUE);
        }
        else if (localAddr instanceof UnixDomainSocketAddress)
            socket = ServerSocketChannel.open(StandardProtocolFamily.UNIX);
        else
            throw new UnsupportedAddressTypeException();

        socket.bind(localAddr);

        SocketAddress actualAddr = socket.getLocalAddress();

        setupExternalServer(socket);

        return actualAddr;
    }

    public void start() {
        mLock.lock();
        try {
            if (mJoinThread != null) {
                throw new IllegalStateException("Already started!");
            }
            mJoinThread = new Thread(this::join, "RpcJoinThread");
            mJoinThread.start();
        } finally {
            mLock.unlock();
        }
    }

    private void join() {
        mLock.lock();
        try {
            mJoinThreadRunning = true;
        } finally {
            mLock.unlock();
        }

        try {
            for (;;) {
                acceptConnection();
            }
        } catch (IOException e) {

        }

        mLock.lock();
        try {
            mJoinThreadRunning = false;
            mShutdownCv.signalAll();
        } finally {
            mLock.unlock();
        }
    }

    private void acceptConnection() throws IOException {


    }

    private void establishConnection(SocketChannel clientSocket) {
        
    }
}
