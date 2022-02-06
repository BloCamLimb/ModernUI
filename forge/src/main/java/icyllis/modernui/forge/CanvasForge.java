/*
 * Modern UI.
 * Copyright (C) 2019-2022 BloCamLimb. All rights reserved.
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

package icyllis.modernui.forge;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.datafixers.util.Pair;
import com.mojang.math.Matrix4f;
import icyllis.modernui.annotation.RenderThread;
import icyllis.modernui.graphics.Canvas;
import icyllis.modernui.graphics.GLSurfaceCanvas;
import icyllis.modernui.graphics.Paint;
import icyllis.modernui.math.Matrix4;
import icyllis.modernui.util.Pool;
import icyllis.modernui.util.Pools;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayDeque;
import java.util.Objects;
import java.util.Queue;

import static icyllis.modernui.graphics.GLWrapper.*;

/**
 * CanvasForge is an extension to {@link Canvas}, which provides more drawing
 * methods used in Minecraft on UI thread.
 *
 * @author BloCamLimb
 */
public final class CanvasForge {

    private static final CanvasForge sInstance = new CanvasForge();

    private static final Pool<DrawItem> sDrawItemPool = Pools.simple(60);

    private final BufferBuilder mBufferBuilder = new BufferBuilder(256);
    private final BufferSource mBufferSource = new BufferSource();

    private final Queue<DrawItem> mDrawItems = new ArrayDeque<>();
    private final FloatBuffer mMatBuf = BufferUtils.createFloatBuffer(16);

    private final Matrix4f mProjection = new Matrix4f();

    private final Object2IntMap<String> mSamplerUnits = new Object2IntArrayMap<>();

    private final Runnable mDrawItem = this::drawItem;

    private final ItemRenderer mRenderer = Minecraft.getInstance().getItemRenderer();
    private final TextureManager mTextureManager = Minecraft.getInstance().getTextureManager();

    private volatile GLSurfaceCanvas mCanvas;

    private CanvasForge() {
        for (int i = 0; i < 8; i++) {
            mSamplerUnits.put("Sampler" + i, i);
        }
        mSamplerUnits.defaultReturnValue(-1);
    }

    /**
     * Get a CanvasForge from the given Canvas, if available.
     *
     * @return the instance
     * @throws IllegalArgumentException no capability found with the canvas
     */
    @Nonnull
    public static CanvasForge get(@Nonnull Canvas canvas) {
        if (sInstance.mCanvas == null) {
            synchronized (CanvasForge.class) {
                if (sInstance.mCanvas == null) {
                    sInstance.mCanvas = UIManager.sInstance.mCanvas;
                }
            }
        }
        if (canvas == sInstance.mCanvas) {
            return sInstance;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Draw an item stack. The client player may affect the rendering results.
     * <p>
     * A paint may be used to tint the item, but multicolor is ignored.
     *
     * @param stack the item stack to draw
     * @param x     the center x pos
     * @param y     the center y pos
     * @param size  the size in pixels, it's generally 32 sip
     * @param paint the paint used to draw the item, can be null
     */
    public void drawItemStack(@Nonnull ItemStack stack, float x, float y, float size, @Nullable Paint paint) {
        final GLSurfaceCanvas canvas = mCanvas;

        canvas.save();

        Matrix4 mat = canvas.getMatrix();
        // items are 3D, do not clip them in Z direction
        mat.translate(x, y, 400);
        mat.scale(size, -size, 1);

        mat.put(mMatBuf.rewind());

        canvas.restore();

        final int color = paint == null ? ~0 : paint.getColor();

        DrawItem t = sDrawItemPool.acquire();
        if (t == null) {
            t = new DrawItem();
        }
        mDrawItems.add(t.set(stack, mMatBuf.flip(), color));

        canvas.drawCustom(mDrawItem);
    }

    private static class DrawItem {

        private ItemStack mStack;
        private final Matrix4f mModelView = new Matrix4f();
        private float mR, mG, mB, mA;

        private DrawItem() {
        }

        @Nonnull
        private DrawItem set(@Nonnull ItemStack stack, @Nonnull FloatBuffer mv, int color) {
            mStack = stack;
            mModelView.load(mv);
            mR = ((color >> 16) & 0xFF) / 255f;
            mG = ((color >> 8) & 0xFF) / 255f;
            mB = (color & 0xFF) / 255f;
            mA = (color >>> 24) / 255f;
            return this;
        }

        private void recycle() {
            mStack = null;
            sDrawItemPool.release(this);
        }
    }

    private void drawItem() {
        DrawItem t = mDrawItems.element();
        BakedModel model = mRenderer.getModel(t.mStack, null, Minecraft.getInstance().player, 0);
        AbstractTexture texture = mTextureManager.getTexture(InventoryMenu.BLOCK_ATLAS);
        RenderSystem.setShaderTexture(0, texture.getId());

        BufferSource bufferSource = mBufferSource;
        boolean light2D = !model.usesBlockLight();
        if (light2D) {
            Lighting.setupForFlatItems();
        }
        PoseStack localTransform = new PoseStack();
        mRenderer.render(t.mStack, ItemTransforms.TransformType.GUI, false, localTransform, bufferSource,
                LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, model);
        bufferSource.endBatch();
        if (light2D) {
            Lighting.setupFor3DItems();
        }

        mDrawItems.remove().recycle();

        RenderSystem.enableCull();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
    }

    @RenderThread
    private void end(@Nonnull ByteBuffer buffer, @Nonnull VertexFormat.Mode mode, @Nonnull VertexFormat format,
                     @Nonnull VertexFormat.IndexType indexType, int indexCount, boolean sequentialIndex) {
        final GLSurfaceCanvas canvas = mCanvas;

        if (canvas.bindVertexArray(format.getOrCreateVertexArrayObject())) {
            // minecraft is stupid so that it clears these bindings after a draw call
            glBindBuffer(GL_ARRAY_BUFFER, format.getOrCreateVertexBufferObject());
            format.setupBufferState();
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
        } else {
            glNamedBufferData(format.getOrCreateVertexBufferObject(), buffer, GL_DYNAMIC_DRAW);
        }

        final int indexBufferType;
        if (sequentialIndex) {
            RenderSystem.AutoStorageIndexBuffer indexBuffer = RenderSystem.getSequentialBuffer(mode, indexCount);
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBuffer.name());
            indexBufferType = indexBuffer.type().asGLType;
        } else {
            glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, format.getOrCreateIndexBufferObject());
            int pos = buffer.limit();
            buffer.position(pos);
            buffer.limit(pos + indexCount * indexType.bytes);
            glBufferData(GL_ELEMENT_ARRAY_BUFFER, buffer, GL_DYNAMIC_DRAW);
            indexBufferType = indexType.asGLType;
        }

        final ShaderInstance shader = RenderSystem.getShader();
        assert shader != null;

        final DrawItem t = mDrawItems.element();
        if (shader.MODEL_VIEW_MATRIX != null) {
            shader.MODEL_VIEW_MATRIX.set(t.mModelView);
        }

        if (shader.PROJECTION_MATRIX != null) {
            mProjection.load(canvas.getProjection());
            shader.PROJECTION_MATRIX.set(mProjection);
        }

        if (shader.COLOR_MODULATOR != null) {
            shader.COLOR_MODULATOR.set(t.mR, t.mG, t.mB, t.mA);
        }

        if (shader.FOG_START != null) {
            shader.FOG_START.set(RenderSystem.getShaderFogStart());
        }

        if (shader.FOG_END != null) {
            shader.FOG_END.set(RenderSystem.getShaderFogEnd());
        }

        if (shader.FOG_COLOR != null) {
            shader.FOG_COLOR.set(RenderSystem.getShaderFogColor());
        }

        if (shader.TEXTURE_MATRIX != null) {
            shader.TEXTURE_MATRIX.set(RenderSystem.getTextureMatrix());
        }

        if (shader.GAME_TIME != null) {
            shader.GAME_TIME.set(RenderSystem.getShaderGameTime());
        }

        if (shader.SCREEN_SIZE != null) {
            Window window = Minecraft.getInstance().getWindow();
            shader.SCREEN_SIZE.set(window.getWidth(), window.getHeight());
        }

        if (shader.LINE_WIDTH != null && (mode == VertexFormat.Mode.LINES || mode == VertexFormat.Mode.LINE_STRIP)) {
            shader.LINE_WIDTH.set(RenderSystem.getShaderLineWidth());
        }

        RenderSystem.setupShaderLights(shader);
        ((FastShader) shader).fastApply(canvas, mSamplerUnits);

        RenderSystem.disableDepthTest();
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);

        glDrawElements(mode.asGLMode, indexCount, indexBufferType, MemoryUtil.NULL);
    }

    private class BufferSource implements MultiBufferSource {

        @Nullable
        private RenderType mLastType;

        public BufferSource() {
        }

        @Nonnull
        @Override
        public VertexConsumer getBuffer(@Nonnull RenderType type) {
            BufferBuilder builder = mBufferBuilder;
            if (!Objects.equals(type, mLastType)) {
                endBatch();
                builder.begin(type.mode(), type.format());
                mLastType = type;
            }
            return builder;
        }

        public void endBatch() {
            BufferBuilder builder = mBufferBuilder;
            if (mLastType != null && builder.building()) {
                /*if (((AccessRenderType) mLastType).isSortOnUpload()) {
                    builder.setQuadSortOrigin(0, 0, 0);
                }*/

                builder.end();
                mLastType.setupRenderState();

                Pair<BufferBuilder.DrawState, ByteBuffer> pair = builder.popNextBuffer();
                BufferBuilder.DrawState state = pair.getFirst();
                ByteBuffer buffer = pair.getSecond();

                if (state.vertexCount() > 0) {
                    buffer.position(0)
                            .limit(state.vertexBufferSize());
                    end(buffer, state.mode(), state.format(), state.indexType(),
                            state.indexCount(), state.sequentialIndex());
                }
                buffer.clear();
            }
            mLastType = null;
        }
    }

    // fast rotate
    public interface FastShader {

        void fastApply(@Nonnull GLSurfaceCanvas canvas, @Nonnull Object2IntMap<String> units);
    }
}
