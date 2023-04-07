/*
 * Modern UI.
 * Copyright (C) 2019-2023 BloCamLimb. All rights reserved.
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

package icyllis.modernui.akashi.image;

import icyllis.modernui.annotation.NonNull;
import icyllis.modernui.annotation.Nullable;

import java.io.EOFException;
import java.io.IOException;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * GIF decoder that created with compressed data and decode frame by frame,
 * GIFDecoder is not thread safe, but can be locked and use across threads.
 *
 * @author BloCamLimb
 */
public class GIFDecoder {

    public static volatile int sDefaultDelayMillis = 40;

    private final ByteBuffer mBuf;

    private final int mHeaderPos;

    private final int mScreenWidth;
    private final int mScreenHeight;
    @Nullable
    private final byte[] mGlobalPalette; // rgba0 rgba1 ...
    private final byte[] mImage; // rgba0 rgba1 ...

    @Nullable
    private byte[] mTmpPalette; // rgba0 rgba1 ...
    private final byte[] mTmpImage; // index0 index1 ...

    private final int[] mTmpInterlace;

    public GIFDecoder(ByteBuffer buf) throws IOException {
        mBuf = buf;
        int b;
        if (readByte() != 'G' || readByte() != 'I' || readByte() != 'F' ||
                readByte() != '8' || ((b = readByte()) != '7' && b != '9') || readByte() != 'a') {
            throw new IOException("Not GIF");
        }

        mScreenWidth = readShort();
        mScreenHeight = readShort();
        int packedField = readByte();
        skipBytes(2);

        if ((packedField & 0x80) != 0) {
            mGlobalPalette = readPalette(2 << (packedField & 7), -1, null);
        } else {
            mGlobalPalette = null;
        }
        mImage = new byte[mScreenWidth * mScreenHeight * 4];
        mTmpImage = new byte[mScreenWidth * mScreenHeight];

        mTmpInterlace = new int[mScreenHeight];

        mHeaderPos = mBuf.position();
    }

    public static boolean checkMagic(@NonNull byte[] buf) {
        return buf.length >= 6 &&
                buf[0] == 'G' && buf[1] == 'I' && buf[2] == 'F' &&
                buf[3] == '8' && (buf[4] == '7' || buf[4] == '9') && buf[5] == 'a';
    }

    public int getScreenWidth() {
        return mScreenWidth;
    }

    public int getScreenHeight() {
        return mScreenHeight;
    }

    /**
     * @return the frame delay in milliseconds
     */
    public int decodeNextFrame(ByteBuffer pixels) throws IOException {
        int imageControlCode = syncNextFrame();

        if (imageControlCode < 0) {
            throw new IOException();
        }

        int left = readShort(), top = readShort(), width = readShort(), height = readShort();

        // check if the image is in the virtual screen boundaries
        if (left + width > mScreenWidth || top + height > mScreenHeight) {
            throw new IOException();
        }

        int packedField = readByte();

        boolean isTransparent = ((imageControlCode >>> 24) & 1) != 0;
        int transparentIndex = isTransparent ? (imageControlCode >>> 16) & 0xFF : -1;
        boolean localPalette = (packedField & 0x80) != 0;
        boolean isInterlaced = (packedField & 0x40) != 0;

        int paletteSize = 2 << (packedField & 7);
        if (mTmpPalette == null || mTmpPalette.length < paletteSize * 4) {
            mTmpPalette = new byte[paletteSize * 4];
        }
        byte[] palette = localPalette
                ? readPalette(paletteSize, transparentIndex, mTmpPalette)
                : Objects.requireNonNull(mGlobalPalette);

        int delayTime = imageControlCode & 0xFFFF; // frame duration in centi-seconds

        int disposalCode = (imageControlCode >>> 26) & 7;
        decodeImage(mTmpImage, width, height,
                isInterlaced
                        ? computeInterlaceReIndex(height, mTmpInterlace)
                        : null);

        decodePalette(mTmpImage, palette, transparentIndex, left, top, width, height, disposalCode, pixels);

        return delayTime != 0 ? delayTime * 10 : sDefaultDelayMillis;
    }

    @NonNull
    private byte[] readPalette(int size, int transparentIndex, @Nullable byte[] palette) throws IOException {
        // max size is 256, flatten the array [r0 g0 b0 a0 r1 g1 b1 a1 ...]
        if (palette == null) {
            palette = new byte[size * 4];
        }
        for (int i = 0, j = 0; i < size; ++i) {
            for (int k = 0; k < 3; ++k) {
                palette[j++] = (byte) readByte();
            }
            palette[j++] = (i == transparentIndex) ? 0 : (byte) 0xFF;
        }
        return palette;
    }

    public void skipExtension() throws IOException {
        for (int blockSize = readByte();
             blockSize != 0; // Block Terminator
             blockSize = readByte()) {
            skipBytes(blockSize);
        }
    }

    // returns ((packedField & 0x1F) << 24) + (transparentIndexIndex << 16) + delayTime;
    private int readControlCode() throws IOException {
        int blockSize = readByte();
        int packedField = readByte();
        int delayTime = readShort();
        int transparentIndex = readByte();

        if (blockSize != 4 || readByte() != 0) { // Block Terminator
            throw new IOException();
        }
        return ((packedField & 0x1F) << 24) + (transparentIndex << 16) + delayTime;
    }

    private int syncNextFrame() throws IOException {
        int controlData = 0;
        boolean restarted = false;
        // @formatter:off
        for (;;) {
        // @formatter:on
            int ch = read();
            switch (ch) {
                case 0x2C -> { // Image Separator
                    return controlData;
                }
                case 0x21 -> { // Extension Introducer
                    if (readByte() == 0xF9) { // Graphic Control Extension
                        controlData = readControlCode();
                    } else {
                        skipExtension();
                    }
                }
                case -1, 0x3B -> {  // EOF or Trailer
                    if (restarted) {
                        // Dead loop or no data
                        return -1;
                    }
                    mBuf.position(mHeaderPos); // Return to beginning
                    controlData = 0;
                    restarted = true;
                }
                default -> throw new IOException(String.valueOf(ch));
            }
        }
    }

    // Decode the one frame of GIF form the input stream using internal LZWDecoder class
    private void decodeImage(byte[] image, int width, int height, @Nullable int[] interlace) throws IOException {
        final LZWDecoder dec = LZWDecoder.getInstance();
        byte[] data = dec.setData(mBuf, mBuf.get());
        int y = 0, iPos = 0, xr = width;
        // @formatter:off
        for (;;) {
        // @formatter:on
            int len = dec.readString();
            if (len == -1) { // end of stream
                skipExtension();
                return;
            }
            for (int pos = 0; pos < len; ) {
                int ax = Math.min(xr, (len - pos));
                System.arraycopy(data, pos, image, iPos, ax);
                iPos += ax;
                pos += ax;
                if ((xr -= ax) == 0) {
                    if (++y == height) { // image is full
                        skipExtension();
                        return;
                    }
                    int iY = interlace == null ? y : interlace[y];
                    iPos = iY * width;
                    xr = width;
                }
            }
        }
    }

    // computes row re-index for interlaced case
    @NonNull
    private int[] computeInterlaceReIndex(int height, int[] data) {
        int pos = 0;
        for (int i = 0; i < height; i += 8) data[pos++] = i;
        for (int i = 4; i < height; i += 8) data[pos++] = i;
        for (int i = 2; i < height; i += 4) data[pos++] = i;
        for (int i = 1; i < height; i += 2) data[pos++] = i;
        return data;
    }

    // GIF specification states that restore to background should fill the frame
    // with background color, but actually all modern programs fill with transparent color.
    private void restoreToBackground(byte[] image, int left, int top, int width, int height) {
        for (int y = 0; y < height; ++y) {
            int iPos = ((top + y) * mScreenWidth + left) * 4;
            for (int x = 0; x < width; iPos += 4, ++x) {
                image[iPos + 3] = 0;
            }
        }
    }

    private void decodePalette(byte[] srcImage, byte[] palette, int transparentIndex,
                               int left, int top, int width, int height, int disposalCode,
                               ByteBuffer pixels) {
        // Restore to previous
        if (disposalCode == 3) {
            pixels.put(mImage);
            for (int y = 0; y < height; ++y) {
                int iPos = ((top + y) * mScreenWidth + left) * 4;
                int i = y * width;
                if (transparentIndex < 0) {
                    for (int x = 0; x < width; ++x) {
                        int index = 0xFF & srcImage[i + x];
                        pixels.put(iPos, palette, index * 4, 4);
                        iPos += 4;
                    }
                } else {
                    for (int x = 0; x < width; ++x) {
                        int index = 0xFF & srcImage[i + x];
                        if (index != transparentIndex) {
                            pixels.put(iPos, palette, index * 4, 4);
                        }
                        iPos += 4;
                    }
                }
            }
            pixels.rewind();
        } else {
            final byte[] image = mImage;
            for (int y = 0; y < height; ++y) {
                int iPos = ((top + y) * mScreenWidth + left) * 4;
                int i = y * width;
                if (transparentIndex < 0) {
                    for (int x = 0; x < width; ++x) {
                        int index = 0xFF & srcImage[i + x];
                        System.arraycopy(palette, index * 4, image, iPos, 4);
                        iPos += 4;
                    }
                } else {
                    for (int x = 0; x < width; ++x) {
                        int index = 0xFF & srcImage[i + x];
                        if (index != transparentIndex) {
                            System.arraycopy(palette, index * 4, image, iPos, 4);
                        }
                        iPos += 4;
                    }
                }
            }

            pixels.put(image).rewind();
            // Restore to background color
            if (disposalCode == 2) {
                restoreToBackground(mImage, left, top, width, height);
            }
        }
    }

    private int read() {
        try {
            return mBuf.get() & 0xFF;
        } catch (BufferUnderflowException e) {
            return -1;
        }
    }

    public int readByte() throws IOException {
        try {
            return mBuf.get() & 0xFF;
        } catch (BufferUnderflowException e) {
            throw new EOFException();
        }
    }

    private int readShort() throws IOException {
        int lsb = readByte(), msb = readByte();
        return lsb + (msb << 8);
    }

    private void skipBytes(int n) throws IOException {
        try {
            mBuf.position(mBuf.position() + n);
        } catch (IllegalArgumentException e) {
            throw new EOFException();
        }
    }
}
