/*
 * This file is part of Arc 3D.
 *
 * Copyright (C) 2022-2023 BloCamLimb <pocamelards@gmail.com>
 *
 * Arc 3D is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Arc 3D is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Arc 3D. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.arc3d.test;

import icyllis.arc3d.core.MathUtil;
import icyllis.arc3d.core.*;
import icyllis.arc3d.engine.Surface;
import icyllis.arc3d.engine.*;
import icyllis.arc3d.engine.geom.SDFRoundRectGeoProc;
import icyllis.arc3d.opengl.*;
import icyllis.arc3d.shaderc.*;
import icyllis.arc3d.shaderc.parser.Lexer;
import icyllis.arc3d.shaderc.parser.Token;
import icyllis.arc3d.shaderc.tree.Type;
import org.lwjgl.PointerBuffer;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.EXTTextureCompressionS3TC;
import org.lwjgl.opengl.GL;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.*;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.regex.Pattern;

import static org.lwjgl.system.MemoryUtil.*;

public class TestManagedResource {

    public static void main(String[] args) {
        PrintWriter pw = new PrintWriter(System.out, true, StandardCharsets.UTF_8);
        long time = System.nanoTime();

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

        if (glVersion != null) {
            var pattern = Pattern.compile("(\\d+)\\.(\\d+)");
            var matcher = pattern.matcher(glVersion);
            if (matcher.find()) {
                pw.println("Version major " + matcher.group(1) + " minor " + matcher.group(2));
            }
        }

        {
            ModuleLoader moduleLoader = ModuleLoader.getInstance();
            DSL.start(ModuleKind.GENERIC, new ModuleOptions(), moduleLoader.getRootModule());
            Type[] types = new Type[3];
            boolean success = Operator.MUL.determineBinaryType(moduleLoader.getBuiltinTypes().mHalf3x4,
                    moduleLoader.getBuiltinTypes().mFloat3, types);
            pw.println("Operator types: " + success + ", " + Arrays.toString(types));
            DSL.end();
        }

        {
            long bytes = 0;
            bytes += 16 + MathUtil.align8(Lexer.MAPPINGS.length);
            bytes += 16 + MathUtil.align8(Lexer.ACCEPTS.length);
            bytes += 16 + MathUtil.align8(Lexer.INDICES.length * 2);
            bytes += 16 + MathUtil.align8(Lexer.FULL.length * 4);
            for (short[] elem : Lexer.FULL) {
                bytes += 16 + MathUtil.align8(elem.length * 2);
            }
            bytes += 16 + MathUtil.align8(Lexer.PACKED.length * 4);
            for (Lexer.PackedEntry elem : Lexer.PACKED) {
                bytes += 16 + 4 + 4 + 16 + MathUtil.align8(elem.data().length);
            }
            pw.println("Lexer bytes: " + bytes);

            pw.println("Decode int: " + Long.decode("4294967295"));
        }

        var str = """
                static func <T> copy(dest: List<in T>, src: List<out T>)
                    where T : Object & Comparable<in T>;
                        """;

        testShaderBuilder(pw, dContext);

        pw.println("BinaryFormats: " + Arrays.toString(((GLCaps) dContext.getCaps()).mProgramBinaryFormats));

        if (dContext.getCaps().isFormatTexturable(
                GLBackendFormat.make(EXTTextureCompressionS3TC.GL_COMPRESSED_RGBA_S3TC_DXT1_EXT))) {
            pw.println("Compressed format: OK");
        }

        Swizzle.make("rgb1");
        //noinspection unused
        int sampler = SamplerState.make(SamplerState.FILTER_NEAREST, SamplerState.MIPMAP_MODE_NONE);

        testTexture(pw, dContext);

        //tokenize(pw);

        if (Platform.get() == Platform.WINDOWS) {
            if (!Kernel32.CloseHandle(959595595959595959L)) {
                pw.println("Failed to close handle");
            }
        }

        //testCamera(pw);

        //testRightHandedRotation(pw);

        //testKeyBuilder(pw);

        //testSimilarity(pw);

        dContext.unref();
        GLFW.glfwDestroyWindow(window);
        GLFW.glfwTerminate();

        try {
            assert false;
        } catch (AssertionError e) {
            pw.println("Assert: " + (System.nanoTime() - time) / 1000000 + "ms");
        }
    }

    public static void tokenize(PrintWriter pw) {
        String[] tokens = {"END_OF_FILE",
                "INTLITERAL",
                "FLOATLITERAL",
                "TRUE",
                "FALSE",
                "BREAK",
                "CONTINUE",
                "DO",
                "FOR",
                "WHILE",
                "IF",
                "ELSE",
                "SWITCH",
                "CASE",
                "DEFAULT",
                "DISCARD",
                "RETURN",
                "IN",
                "OUT",
                "INOUT",
                "CONST",
                "UNIFORM",
                "BUFFER",
                "WORKGROUP",
                "FLAT",
                "NOPERSPECTIVE",
                "COHERENT",
                "VOLATILE",
                "RESTRICT",
                "READONLY",
                "WRITEONLY",
                "LAYOUT",
                "STRUCT",
                "INLINE",
                "NOINLINE",
                "PURE",
                "EXPORT",
                "RESERVED",
                "IDENTIFIER",
                "LPAREN",
                "RPAREN",
                "LBRACE",
                "RBRACE",
                "LBRACKET",
                "RBRACKET",
                "DOT",
                "COMMA",
                "EQ",
                "LT",
                "GT",
                "BANG",
                "TILDE",
                "QUES",
                "COLON",
                "EQEQ",
                "LTEQ",
                "GTEQ",
                "BANGEQ",
                "PLUSPLUS",
                "MINUSMINUS",
                "PLUS",
                "MINUS",
                "STAR",
                "SLASH",
                "PERCENT",
                "LTLT",
                "GTGT",
                "AMPAMP",
                "PIPEPIPE",
                "CARETCARET",
                "AMP",
                "PIPE",
                "CARET",
                "PLUSEQ",
                "MINUSEQ",
                "STAREQ",
                "SLASHEQ",
                "PERCENTEQ",
                "LTLTEQ",
                "GTGTEQ",
                "AMPEQ",
                "PIPEEQ",
                "CARETEQ",
                "SEMICOLON",
                "WHITESPACE",
                "LINE_COMMENT",
                "BLOCK_COMMENT",
                "INVALID"};
        Lexer lexer = new Lexer("""
                layout(std140, binding = 0) uniform UniformBlock {
                    mat4 u_Projection;
                    mat4 u_ModelView;
                    vec4 u_Color;
                };
                layout(location = 0) smooth in vec2 f_Position;
                layout(location = 1) smooth in vec4 f_Color;
                layout(location = 0, index = 0) out vec4 FragColor0;
                float rand(vec2 n) {
                    return fract(sin(dot(n, vec2(12.9898,12.1414))) * 83758.5453);
                }
                void main() {
                    vec2 pos = f_Position;
                    float dist = abs(pos.y-sin(pos.x*10.0-u_Color.x*5.0)*0.1-cos(pos.x*5.0)*0.05);
                    dist = pow(0.1/dist,0.8);
                    vec4 col = vec4(mix(vec3(0.2,0.85,0.95),vec3(0.85,0.5,0.75),pos.x*0.5+0.5),1.0);
                    col *= (dist+rand(pos.yx)*0.05);
                    col = 1.0 - exp(-col*0.5);
                    FragColor0 = col;
                }
                """);
        long token;
        int kind;
        while ((kind = Token.kind(token = lexer.next())) != Lexer.TK_END_OF_FILE) {
            if (kind == Lexer.TK_WHITESPACE) continue;
            int offset = Token.offset(token);
            pw.println("(" + offset + ", " + (offset + Token.length(token)) + ") " + tokens[kind]);
        }
    }

    public static void printIpConfig(PrintWriter pw) {
        try {
            Process process = new ProcessBuilder("ipconfig", "/all").start();
            process.onExit().thenAccept(p -> {
                BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
                try {
                    for (; ; ) {
                        String line = reader.readLine();
                        if (line == null)
                            break;
                        pw.println(line);
                    }
                } catch (Exception ignored) {
                }
            });
        } catch (Exception ignored) {
        }
    }

    public static void testShaderBuilder(PrintWriter pw, DirectContext dContext) {
        @SharedPtr
        TextureProxy target = dContext.getProxyProvider().createRenderTextureProxy(
                GLBackendFormat.make(GLCore.GL_RGBA8),
                800, 800, 4,
                Surface.FLAG_BUDGETED | Surface.FLAG_RENDERABLE
        );
        Objects.requireNonNull(target);
        GLPipelineState pso = (GLPipelineState) dContext.findOrCreatePipelineState(
                new PipelineInfo(new SurfaceProxyView(target), new SDFRoundRectGeoProc(true),
                        null, null, null, null,
                        PipelineInfo.kNone_Flag));
        {
            pw.println(target);
            target.unref();
        }

        pw.println(dContext.getPipelineStateCache().getStats());
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
            pw.println("WxH: " + x[0] + "x" + y[0]);
            assert pixels != null;
            pw.println("Image Bytes: " + pixels.remaining());

            Texture texture = dContext.getServer().createTexture(
                    x[0], y[0],
                    GLBackendFormat.make(GLCore.GL_RGBA8),
                    1, Surface.FLAG_MIPMAPPED |
                            Surface.FLAG_BUDGETED |
                            Surface.FLAG_RENDERABLE,
                    "MyTexture");
            if (texture != null) {
                pw.println(texture);
                pw.println(texture.asRenderTarget());
                texture.unref();
            }
            texture = dContext.getResourceProvider().createTexture(
                    x[0], y[0],
                    GLBackendFormat.make(GLCore.GL_RGBA8),
                    1, Surface.FLAG_MIPMAPPED |
                            Surface.FLAG_BUDGETED |
                            Surface.FLAG_RENDERABLE,
                    ImageInfo.CT_RGBA_8888,
                    ImageInfo.CT_RGBA_8888,
                    0,
                    memAddress(pixels),
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

    public static void testKeyBuilder(PrintWriter pw) {
        Key.Builder keyBuilder = new Key.StringBuilder();
        keyBuilder.addBits(6, 0x2F, "A");
        keyBuilder.addInt32(0xF111_1111, "B");
        keyBuilder.flush();
        pw.println(keyBuilder);
    }

    public static void testSimilarity(PrintWriter pw) {
        Matrix4 transform = Matrix4.identity();
        transform.m34 = 1 / 4096f;
        transform.preRotateX(MathUtil.PI_O_3);
        Matrix matrix3 = transform.toMatrix();
        pw.println(matrix3);

        Matrix4 mat = Matrix4.identity();
        mat.preRotateZ(MathUtil.PI_O_2 * 29);
        Matrix m3 = mat.toMatrix();
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


        icyllis.arc3d.test.Camera.Camera3D camera3D = new icyllis.arc3d.test.Camera.Camera3D();
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
            memFree(buffer);
        }
    }
}
