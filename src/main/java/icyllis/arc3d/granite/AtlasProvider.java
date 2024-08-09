/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.granite;

import icyllis.arc3d.engine.*;

/**
 * AtlasProvider groups various texture atlas management algorithms together.
 */
public class AtlasProvider implements AutoCloseable {

    private final RecordingContext mRC;
    private GlyphAtlasManager mGlyphAtlasManager;

    public AtlasProvider(RecordingContext rc) {
        mRC = rc;
        mGlyphAtlasManager = new GlyphAtlasManager(rc);
    }

    @Override
    public void close() {
        mGlyphAtlasManager.close();
        mGlyphAtlasManager = null;
    }

    /**
     * Returns the {@link GlyphAtlasManager} that provides access to persistent
     * {@link DrawAtlas} instances used in glyph rendering. The {@link GlyphAtlasManager}
     * is managed by this object, then the return value is a raw pointer.
     */
    public GlyphAtlasManager getGlyphAtlasManager() {
        return mGlyphAtlasManager;
    }

    public void recordUploads(SurfaceDrawContext sdc) {
        if (!mGlyphAtlasManager.recordUploads(sdc)) {
            mRC.getLogger().error("GlyphAtlasManager uploads have failed");
        }
    }

    public void compact() {
        mGlyphAtlasManager.compact();
    }

    public void invalidateAtlases() {
        mGlyphAtlasManager.evictAtlases();
    }
}
