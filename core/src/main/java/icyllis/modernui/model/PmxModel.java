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

package icyllis.modernui.model;

import icyllis.modernui.ModernUI;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * PMX (Polygon Model Extended) 2.1, PMX Model, PMX Parser
 */
public class PmxModel {

    public float mPmxVersion;
    public Charset mTextEncoding;
    public byte mAdditionalUV;

    public String mModelName;
    public String mModelNameEn;
    public String mModelComment;
    public String mModelCommentEn;

    public Vertex[] mVertices;

    public PmxModel() {
    }

    @Nonnull
    public static PmxModel decode(@Nonnull FileChannel channel) throws IOException {
        ByteBuffer buf = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        buf.order(ByteOrder.LITTLE_ENDIAN);
        buf.rewind();
        PmxModel model = new PmxModel();
        model.read(buf);
        return model;
    }

    private void read(@Nonnull ByteBuffer buf) {
        // Signature "PMX "
        if (buf.get() != 0x50 || buf.get() != 0x4D || buf.get() != 0x58 || buf.get() != 0x20) {
            throw new IllegalStateException("Not PMX format");
        }
        mPmxVersion = buf.getFloat();
        if (mPmxVersion != 2.0f && mPmxVersion != 2.1f) {
            throw new IllegalStateException("Not PMX v2.0 or v2.1 but " + mPmxVersion);
        }
        byte[] settings = new byte[buf.get()];
        if (settings.length < 8) {
            throw new IllegalStateException();
        }
        buf.get(settings);
        mTextEncoding = settings[0] == 0 ? StandardCharsets.UTF_16LE : StandardCharsets.UTF_8;
        mAdditionalUV = settings[1];
        mModelName = readText(buf);
        mModelNameEn = readText(buf);
        mModelComment = readText(buf);
        mModelCommentEn = readText(buf);
    }

    @Nonnull
    private String readText(@Nonnull ByteBuffer buf) {
        int len = buf.getInt();
        if (len == 0) {
            return "";
        }
        byte[] bytes = new byte[len];
        buf.get(bytes);
        return new String(bytes, mTextEncoding);
    }

    public void debug() {
        ModernUI.LOGGER.info(mModelName);
        ModernUI.LOGGER.info(mModelComment);
    }

    public static class Vertex {

    }
}
