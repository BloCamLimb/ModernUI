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

package icyllis.akashigi.test;

import icyllis.akashigi.core.MathUtil;
import icyllis.akashigi.core.*;
import icyllis.akashigi.engine.*;
import icyllis.akashigi.engine.geom.RoundRectProcessor;
import icyllis.akashigi.opengl.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.*;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static icyllis.akashigi.engine.Engine.*;
import static org.lwjgl.system.MemoryUtil.memAddress;

public class TestManagedResource {

    public static void main(String[] args) {
        long time = System.nanoTime();
        PrintWriter pw = new PrintWriter(System.out, true, StandardCharsets.UTF_8);

        GLFW.glfwInit();
        // load first
        Objects.requireNonNull(GL.getFunctionProvider());
        GLFW.glfwDefaultWindowHints();
        GLFW.glfwWindowHint(GLFW.GLFW_VISIBLE, GLFW.GLFW_FALSE);
        long window = GLFW.glfwCreateWindow(1, 1, "Test Window", 0, 0);
        if (window == 0) {
            throw new RuntimeException();
        }
        GLFW.glfwMakeContextCurrent(window);

        DirectContext dContext = DirectContext.makeOpenGL();
        if (dContext == null) {
            throw new RuntimeException();
        }
        String glVersion = GLCore.glGetString(GLCore.GL_VERSION);
        pw.println("OpenGL version: " + glVersion);
        pw.println("OpenGL vendor: " + GLCore.glGetString(GLCore.GL_VENDOR));
        pw.println("OpenGL renderer: " + GLCore.glGetString(GLCore.GL_RENDERER));
        pw.println("Max vertex attribs: " + GLCore.glGetInteger(GLCore.GL_MAX_VERTEX_ATTRIBS));
        pw.println("Max vertex bindings: " + GLCore.glGetInteger(GLCore.GL_MAX_VERTEX_ATTRIB_BINDINGS));
        pw.println("Max vertex stride: " + GLCore.glGetInteger(GLCore.GL_MAX_VERTEX_ATTRIB_STRIDE));
        pw.println("Max label length: " + GLCore.glGetInteger(GLCore.GL_MAX_LABEL_LENGTH));

        pw.println("quickModPow: " + MathUtil.quickModPow(95959595, 87878787, 998244353));

        pw.println("BinaryFormats: " + Arrays.toString(((GLCaps)dContext.getCaps()).mProgramBinaryFormats));

        if (dContext.getCaps().isFormatTexturable(
                GLBackendFormat.make(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT))) {
            pw.println("Compressed format: OK");
        }

        Swizzle.make("rgb1");
        int sampler = SamplerState.make(SamplerState.FILTER_MODE_NEAREST, SamplerState.MIPMAP_MODE_NONE);

        pw.println("Linear 0.5 to SRGB: " + 0f * Float.POSITIVE_INFINITY);

        testTexture(pw, dContext);

        try {
            double v = -Double.MAX_VALUE;
            pw.println("Float v: " + v);
        } catch (NumberFormatException e) {
            pw.println(e.getMessage());
        }

        GLServer server = (GLServer) dContext.getServer();
        GLPipelineStateCache pipelineStateCache = server.getPipelineBuilder();
        TextureProxy proxy2 = dContext.getProxyProvider().createRenderTextureProxy(
                GLBackendFormat.make(GLCore.GL_RGBA8),
                800, 800, 4, SURFACE_FLAG_BUDGETED |
                        SURFACE_FLAG_RENDERABLE
        );
        GLPipelineState pipelineState = pipelineStateCache.findOrCreatePipelineState(
                new PipelineInfo(new SurfaceProxyView(proxy2), new RoundRectProcessor(true),
                        null, null, null, null,
                        PipelineInfo.FLAG_NONE));
        try (proxy2) {
            pw.println(proxy2);
        }

        pw.println(dContext.getServer().getPipelineBuilder().getStates());

        testLexicon(pw);

        if (Platform.get() == Platform.WINDOWS) {
            if (!Kernel32.CloseHandle(959595595959595959L)) {
                pw.println("Failed to close handle");
            }
        }

        //testCamera(pw);

        //testRightHandedRotation(pw);

        //testKeyBuilder(pw);

        //testSimilarity(pw);

        dContext.close();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();

        try {
            assert false;
        } catch (AssertionError e) {
            System.out.println("Assert: " + (System.nanoTime() - time) / 1000000 + "ms");
        }
    }

    public static void testTexture(PrintWriter pw, DirectContext dContext) {
        ByteBuffer pixels = null;
        try (FileChannel channel = FileChannel.open(Path.of("F:/", "6e6862e6e414225fa933d101d68efd0c.jpeg"),
                StandardOpenOption.READ)) {
            ByteBuffer byteBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            int[] x = {0};
            int[] y = {0};
            int[] channels = {0};
            pixels = STBImage.stbi_load_from_memory(byteBuffer, x, y, channels, 4);
            pw.println("Channels: " + channels[0]);
            pw.println("W H: " + x[0] + " " + y[0]);
            assert pixels != null;
            pw.println("Image Bytes: " + pixels.remaining());

            Texture texture = dContext.getServer().createTexture(
                    x[0], y[0],
                    GLBackendFormat.make(GLCore.GL_RGBA8),
                    1,
                    SURFACE_FLAG_MIPMAPPED |
                            SURFACE_FLAG_BUDGETED |
                            SURFACE_FLAG_RENDERABLE,
                    "MyTexture");
            if (texture != null) {
                pw.println(texture);
                pw.println(texture.getRenderTarget());
                texture.unref();
            }
            texture = dContext.getResourceProvider().createTexture(
                    x[0], y[0],
                    GLBackendFormat.make(GLCore.GL_RGBA8),
                    1, SURFACE_FLAG_MIPMAPPED |
                            SURFACE_FLAG_BUDGETED |
                            SURFACE_FLAG_RENDERABLE,
                    ImageInfo.COLOR_TYPE_RGBA_8888,
                    ImageInfo.COLOR_TYPE_RGBA_8888,
                    0,
                    MemoryUtil.memAddress(pixels),
                    null);
            if (texture != null) {
                pw.println(texture); // same texture
                texture.unref();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (pixels != null) {
                STBImage.stbi_image_free(pixels);
            }
        }
    }

    public static void testRightHandedRotation(PrintWriter pw) {
        Matrix4 mat = Matrix4.identity();
        mat.preRotateZ(MathUtil.PI_O_3);
        pw.println("preRotateX " + mat);

        Matrix4 mat2 = Matrix4.identity();
        mat2.setPerspective(Math.toRadians(75), 16 / 9., 0, 2000, false);
        pw.println("preRotateAxisAngle " + mat2);

        final double x = mat2.m11 * 2 + mat2.m21 * 2 + mat2.m31 * 2 + mat2.m41;
        final double y = mat2.m12 * 2 + mat2.m22 * 2 + mat2.m32 * 2 + mat2.m42;
        final double z = mat2.m13 * 2 + mat2.m23 * 2 + mat2.m33 * 2 + mat2.m43;
        pw.println("Point: " + x + ", " + y + ", " + z);
    }

    public static void testLexicon(PrintWriter pw) {
        pw.println("Matrix3 offset: " + Matrix3.OFFSET);
    }

    public static void testKeyBuilder(PrintWriter pw) {
        IntArrayList intArrayList = new IntArrayList();
        KeyBuilder keyBuilder = new KeyBuilder.StringKeyBuilder(intArrayList);
        keyBuilder.addBits(6, 0x2F, "A");
        keyBuilder.addInt32(0xF111_1111, "B");
        keyBuilder.flush();
        pw.println(keyBuilder);
    }

    public static void testSimilarity(PrintWriter pw) {
        Matrix4 transform = Matrix4.identity();
        transform.m34 = 1 / 4096f;
        transform.preRotateX(MathUtil.PI_O_3);
        Matrix3 matrix3 = transform.toM33NoZ();
        pw.println(matrix3);

        Matrix4 mat = Matrix4.identity();
        mat.preRotateZ(MathUtil.PI_O_2 * 29);
        Matrix3 m3 = mat.toM33NoZ();
        pw.println(m3);
        pw.println(m3.getType());
        pw.println("Similarity: " + m3.isSimilarity());
    }

    /*public static void testCamera(PrintWriter pw) {
        Matrix4 mat = Matrix4.identity();
        mat.m34 = 1 / 576f;
        //mat.preTranslateZ(-20f);
        mat.preRotateY(MathUtil.PI_O_3);
        float[] p1 = new float[]{-25, -15};
        float[] p2 = new float[]{25, -15};
        float[] p3 = new float[]{25, 15};
        float[] p4 = new float[]{-25, 15};
        pw.println(mat);
        mat.mapPoint(p1);
        mat.mapPoint(p2);
        mat.mapPoint(p3);
        mat.mapPoint(p4);
        pw.println(Arrays.toString(p1));
        pw.println(Arrays.toString(p2));
        pw.println(Arrays.toString(p3));
        pw.println(Arrays.toString(p4));


        Camera.Camera3D camera3D = new Camera.Camera3D();
        Matrix4 transformMat = Matrix4.identity();
        transformMat.preRotateY(MathUtil.PI_O_3);
        Matrix3 outMatrix = new Matrix3();
        camera3D.getMatrix(transformMat, outMatrix, pw);
        pw.println("Orien: " + camera3D.mOrientation);
        pw.println(outMatrix);
        p1 = new float[]{-25, -15};
        p2 = new float[]{25, -15};
        p3 = new float[]{25, 15};
        p4 = new float[]{-25, 15};
        outMatrix.mapPoint(p1);
        outMatrix.mapPoint(p2);
        outMatrix.mapPoint(p3);
        outMatrix.mapPoint(p4);
        pw.println(Arrays.toString(p1));
        pw.println(Arrays.toString(p2));
        pw.println(Arrays.toString(p3));
        pw.println(Arrays.toString(p4));
    }*/

    public static void decodeLargeGIFUsingSTBImage(PrintWriter pw, String path) {
        ByteBuffer buffer = null;
        long image = 0;
        try (FileChannel channel = FileChannel.open(Path.of(path),
                StandardOpenOption.READ)) {
            //ByteBuffer mapper = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            buffer = MemoryUtil.memAlloc((int) channel.size());
            channel.read(buffer);
            buffer.rewind();
            pw.println("Raw size in bytes " + channel.size());
            try (MemoryStack stack = MemoryStack.stackPush()) {
                PointerBuffer delays = stack.mallocPointer(1);
                IntBuffer x = stack.mallocInt(1);
                IntBuffer y = stack.mallocInt(1);
                IntBuffer z = stack.mallocInt(1);
                IntBuffer channels = stack.mallocInt(1);
                new Thread(() -> {
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    System.gc();
                }).start();
                image = STBImage.nstbi_load_gif_from_memory(memAddress(buffer), buffer.remaining(),
                        memAddress(delays), memAddress(x), memAddress(y), memAddress(z), memAddress(channels), 0);
                if (image == 0) {
                    throw new IOException(STBImage.stbi_failure_reason());
                }
                IntBuffer delay = delays.getIntBuffer(z.get(0));
                pw.printf("width:%d height:%s layers:%s channels:%s size_in_bytes:%s\n", x.get(0), y.get(0), z.get(0),
                        channels.get(0), (long) x.get(0) * y.get(0) * z.get(0) * channels.get(0));
                for (int i = 0; i < z.get(0); i++) {
                    pw.print(delay.get(i) + " ");
                }
                pw.println();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (image != 0) {
                STBImage.nstbi_image_free(image);
            }
            MemoryUtil.memFree(buffer);
        }
    }
}
