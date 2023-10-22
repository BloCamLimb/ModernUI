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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

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

    private final CopyOnWriteArrayList<Track> mTracks = new CopyOnWriteArrayList<>();

    private boolean mInitialized;
    private boolean mIntegrated;
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
        t.setDaemon(true);
        return t;
    }

    @MainThread
    public void initialize() {
        initialize(false);
    }

    @MainThread
    public synchronized void initialize(boolean integrated) {
        if (mInitialized) {
            return;
        }
        mIntegrated = integrated;
        if (!integrated) {
            setDevice(null);
            List<String> devices = ALUtil.getStringList(NULL, ALC_ALL_DEVICES_SPECIFIER);
            if (devices != null) {
                mDeviceList.addAll(devices);
            }
        }
        mExecutorService.scheduleAtFixedRate(this::tick, 0, TICK_PERIOD, TimeUnit.MILLISECONDS);
        mInitialized = true;
    }

    public void setDevice(@Nullable String name) {
        long context = alcGetCurrentContext();
        if (context == NULL) {
            long device = alcOpenDevice(name);
            if (device == NULL && name != null) {
                device = nalcOpenDevice(NULL);
            }
            if (device != NULL) {
                context = nalcCreateContext(device, NULL);
                alcMakeContextCurrent(context); // if null, make no context
                if (context != NULL) {
                    ALCCapabilities alcCapabilities = ALC.createCapabilities(device);
                    ALCapabilities alCapabilities = AL.createCapabilities(alcCapabilities);
                    if (!alcCapabilities.OpenALC11 || !alCapabilities.OpenAL11) {
                        ModernUI.LOGGER.fatal(MARKER, "OpenAL 1.1 is not supported");
                    }
                    /*if (alCapabilities.AL_EXT_source_distance_model) {
                        alEnable(EXTSourceDistanceModel.AL_SOURCE_DISTANCE_MODEL);
                    }
                    if (ALC10.alcGetInteger(device, SOFTHRTF.ALC_NUM_HRTF_SPECIFIERS_SOFT) > 0) {
                        try (MemoryStack memoryStack = MemoryStack.stackPush()){
                            SOFTHRTF.alcResetDeviceSOFT(device,
                                    memoryStack.callocInt(5)
                                            .put(SOFTHRTF.ALC_HRTF_SOFT).put(1)
                                            .put(SOFTHRTF.ALC_HRTF_ID_SOFT).put(0)
                                            .put(0).flip());
                        }
                    }*/
                    String devName = alcGetString(device, ALC_DEVICE_SPECIFIER);
                    ModernUI.LOGGER.info(MARKER, "Open audio device {}", devName);
                } else {
                    ModernUI.LOGGER.error(MARKER, "Failed to create audio context");
                }
            } else {
                ModernUI.LOGGER.info(MARKER, "No suitable audio device was found");
            }
        }
    }

    // audio thread
    private void tick() {
        int timer = (mTimer + 1) & 0x7f;
        try {
            if (timer == 0 && !mIntegrated) {
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
            for (Track track : mTracks) {
                track.tick();
            }
        } catch (Throwable t) {
            ModernUI.LOGGER.error(MARKER, "Caught an exception on audio thread", t);
        }
        mTimer = timer;
    }

    public void destroy() {
        long context = alcGetCurrentContext();
        if (context != NULL) {
            long device = alcGetContextsDevice(context);
            alcMakeContextCurrent(NULL);
            alcDestroyContext(context);
            if (device != NULL) {
                alcCloseDevice(device);
            }
        }
    }

    public void addTrack(@Nonnull Track track) {
        mTracks.add(track);
    }

    public void removeTrack(@Nonnull Track track) {
        mTracks.remove(track);
    }

    @Override
    public void close() {
        mExecutorService.shutdown();
        ArrayList<Track> tracks = new ArrayList<>(mTracks);
        for (Track track : tracks) {
            try {
                track.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (!mIntegrated) {
            destroy();
        }
    }
}
