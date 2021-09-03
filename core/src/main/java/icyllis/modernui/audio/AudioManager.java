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

package icyllis.modernui.audio;

import icyllis.modernui.ModernUI;
import icyllis.modernui.annotation.MainThread;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.openal.*;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.lwjgl.openal.AL11.alEnable;
import static org.lwjgl.openal.ALC11.*;
import static org.lwjgl.system.MemoryUtil.NULL;

//TODO WIP
public class AudioManager implements AutoCloseable {

    public static final Marker MARKER = MarkerManager.getMarker("Audio");

    // 1, 2, 4, 5, 8, 10, 20, 25, 40, 50, 100, 125, 200, 250 milliseconds
    public static final int TICK_PERIOD = 20;

    private static final AudioManager sInstance = new AudioManager();

    private final ScheduledExecutorService mExecutorService =
            Executors.newSingleThreadScheduledExecutor(this::createThread);

    private final List<String> mDeviceList = new ArrayList<>();

    private final Set<Track> mTracks = new HashSet<>();

    private boolean mInitialized;
    private int mTimer;

    private AudioManager() {
    }

    /**
     * Returns the global <code>AudioManager</code> instance.
     *
     * @return the global instance
     */
    @Nonnull
    public static AudioManager getInstance() {
        return sInstance;
    }

    @Nonnull
    private Thread createThread(Runnable target) {
        Thread t = new Thread(target, "Audio-Thread");
        if (t.isDaemon())
            t.setDaemon(false);
        return t;
    }

    @MainThread
    public synchronized void initialize() {
        if (mInitialized) {
            return;
        }
        setDevice(null);
        List<String> devices = ALUtil.getStringList(NULL, ALC_ALL_DEVICES_SPECIFIER);
        if (devices != null) {
            mDeviceList.addAll(devices);
        }
        mExecutorService.scheduleAtFixedRate(this::tick, 0, TICK_PERIOD, TimeUnit.MILLISECONDS);
        mInitialized = true;
    }

    public void setDevice(@Nullable String name) {
        long context = alcGetCurrentContext();
        if (context == NULL) {
            long device = alcOpenDevice(name);
            if (device != NULL) {
                context = nalcCreateContext(device, NULL);
                alcMakeContextCurrent(context); // if null, make no context
                if (context != NULL) {
                    ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
                    ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
                    if (!alcCapabilities.OpenALC11) {
                        ModernUI.LOGGER.fatal(MARKER, "OpenAL 1.1 is not supported");
                    }
                    if (!alCapabilities.AL_EXT_FLOAT32) {
                        ModernUI.LOGGER.fatal(MARKER, "EXTFloat32 is not supported");
                    }
                    if (alCapabilities.AL_EXT_source_distance_model) {
                        alEnable(EXTSourceDistanceModel.AL_SOURCE_DISTANCE_MODEL);
                    }
                }
            } else {
                ModernUI.LOGGER.info(MARKER, "No suitable audio device was found");
            }
        }
    }

    private void tick() {
        int timer = (mTimer + 1) & 0x3f;
        if (timer == 0) {
            List<String> devices = ALUtil.getStringList(NULL, ALC_ALL_DEVICES_SPECIFIER);
            if (!mDeviceList.equals(devices)) {
                mDeviceList.clear();
                if (devices != null) {
                    mDeviceList.addAll(devices);
                }
                destroy();
                setDevice(null);
                ModernUI.LOGGER.info(MARKER, "Device list changed");
            }
        }
        mTracks.forEach(Track::tick);
        mTimer = timer;
    }

    public void destroy() {
        long context = alcGetCurrentContext();
        if (context != NULL) {
            long device = alcGetContextsDevice(context);
            alcMakeContextCurrent(NULL);
            alcDestroyContext(context);
            if (device != 0) {
                alcCloseDevice(device);
            }
        }
    }

    public void addTrack(@Nonnull Track track) {
        mTracks.add(track);
    }

    @Override
    public void close() {
        mExecutorService.shutdown();
        for (Track track : mTracks) {
            try {
                track.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        destroy();
    }
}
