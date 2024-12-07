/*
 * This file is part of Arc3D.
 *
 * Copyright (C) 2024-2024 BloCamLimb <pocamelards@gmail.com>
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

package icyllis.arc3d.opengl;

import icyllis.arc3d.core.Color;
import icyllis.arc3d.core.ColorInfo;
import icyllis.arc3d.engine.*;
import org.jetbrains.annotations.ApiStatus;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GLCapabilities;
import org.lwjgl.system.*;
import org.slf4j.Logger;

import java.lang.ref.Reference;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.regex.Pattern;

import static org.lwjgl.opengl.AMDDebugOutput.*;
import static org.lwjgl.opengl.ARBDebugOutput.*;
import static org.lwjgl.opengl.EXTTextureCompressionS3TC.*;
import static org.lwjgl.opengl.GL43C.*;
import static org.lwjgl.opengl.GL46C.GL_SHADER_BINARY_FORMAT_SPIR_V;

/**
 * Provides OpenGL utilities.
 */
public final class GLUtil {

    /**
     * Creates a DirectContext for a backend context, using default context options.
     *
     * @return context or null if failed to create
     * @see #makeOpenGL(ContextOptions)
     */
    @ApiStatus.Internal
    @Nullable
    public static ImmediateContext makeOpenGL() {
        return makeOpenGL(new ContextOptions());
    }

    @ApiStatus.Internal
    @Nullable
    public static ImmediateContext makeOpenGL(@NonNull ContextOptions options) {
        GLCapabilities capabilities;
        try {
            capabilities = Objects.requireNonNullElseGet(
                    GL.getCapabilities(),
                    GL::createCapabilities
            );
        } catch (Exception x) {
            try {
                capabilities = GL.createCapabilities();
            } catch (Exception e) {
                return null;
            }
        }
        return makeOpenGL(capabilities, options);
    }

    /**
     * Creates a DirectContext for a backend context, using specified context options.
     * <p>
     * Example with GLFW:
     * <pre>{@code
     *  public static void main(String[] args) {
     *      System.setProperty("java.awt.headless", Boolean.TRUE.toString());
     *      if (!glfwInit()) {
     *          throw new IllegalStateException();
     *      }
     *      // default hints use highest OpenGL version and native API
     *      glfwDefaultWindowHints();
     *      long window = glfwCreateWindow(1280, 720, "Example Window", NULL, NULL);
     *      if (window == NULL) {
     *          throw new IllegalStateException();
     *      }
     *      // you can make a thread a render thread
     *      glfwMakeContextCurrent(window);
     *      DirectContext direct = DirectContext.makeOpenGL(
     *          GL.createCapabilities()
     *      );
     *      if (direct == null) {
     *          throw new IllegalStateException();
     *      }
     *      ...
     *      // destroy and close
     *      direct.discard();
     *      direct.unref();
     *      GL.setCapabilities(null);
     *      glfwDestroyWindow(window);
     *      glfwTerminate();
     *  }
     * }</pre>
     *
     * @return context or null if failed to create
     */
    @Nullable
    public static ImmediateContext makeOpenGL(@NonNull Object capabilities, @NonNull ContextOptions options) {
        var device = GLDevice.make(options, capabilities);
        if (device == null) {
            return null;
        }
        var queueManager = new GLQueueManager(device);
        ImmediateContext context = new ImmediateContext(device, queueManager);
        if (context.init()) {
            return context;
        }
        context.unref();
        return null;
    }

    /**
     * Known vendors. Internal use only.
     */
    public enum GLVendor {
        OTHER,
        NVIDIA,
        ATI,
        INTEL,
        QUALCOMM
    }

    /**
     * Known drivers. Internal use only.
     */
    public enum GLDriver {
        OTHER,
        NVIDIA,
        AMD,
        INTEL,
        FREEDRENO,
        MESA
    }

    public static GLVendor findVendor(String vendorString) {
        Objects.requireNonNull(vendorString);
        if (vendorString.equals("NVIDIA Corporation")) {
            return GLVendor.NVIDIA;
        }
        if (vendorString.equals("ATI Technologies Inc.")) {
            return GLVendor.ATI;
        }
        if (vendorString.startsWith("Intel ") || vendorString.equals("Intel")) {
            return GLVendor.INTEL;
        }
        if (vendorString.equals("Qualcomm") || vendorString.equals("freedreno")) {
            return GLVendor.QUALCOMM;
        }
        return GLVendor.OTHER;
    }

    public static GLDriver findDriver(GLVendor vendor,
                                      String vendorString,
                                      String versionString) {
        Objects.requireNonNull(vendorString);
        if (vendorString.equals("freedreno")) {
            return GLDriver.FREEDRENO;
        }
        if (vendor == GLVendor.NVIDIA) {
            return GLDriver.NVIDIA;
        }
        {
            var matcher = Pattern.compile("\\d+\\.\\d+( \\(Core Profile\\))? Mesa \\d+\\.\\d+")
                    .matcher(versionString);
            if (matcher.find()) {
                return GLDriver.MESA;
            }
        }
        if (vendor == GLVendor.ATI) {
            return GLDriver.AMD;
        }
        if (vendor == GLVendor.INTEL) {
            return GLDriver.INTEL;
        }
        return GLDriver.OTHER;
    }

    /**
     * @see #glFormatToIndex(int)
     */
    //@formatter:off
    public static final int
            LAST_COLOR_FORMAT_INDEX = 16,
            LAST_FORMAT_INDEX       = 23;

    /**
     * Lists all supported OpenGL texture formats and converts to table index.
     * 0 is reserved for unsupported formats.
     *
     * @see #glIndexToFormat(int)
     */
    public static int glFormatToIndex(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_RGBA8                         -> 1;
            case GL_R8                            -> 2;
            case GL_RGB565                        -> 3;
            case GL_RGBA16F                       -> 4;
            case GL_R16F                          -> 5;
            case GL_RGB8                          -> 6;
            case GL_RG8                           -> 7;
            case GL_RGB10_A2                      -> 8;
            case GL_SRGB8_ALPHA8                  -> 9;
            case GL_COMPRESSED_RGB8_ETC2          -> 10;
            case GL_COMPRESSED_RGB_S3TC_DXT1_EXT  -> 11;
            case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> 12;
            case GL_R16                           -> 13;
            case GL_RG16                          -> 14;
            case GL_RGBA16                        -> 15;
            case GL_RG16F                         -> 16; // LAST_COLOR_FORMAT_INDEX
            case GL_STENCIL_INDEX8                -> 17;
            case GL_STENCIL_INDEX16               -> 18;
            case GL_DEPTH_COMPONENT16             -> 19;
            case GL_DEPTH_COMPONENT24             -> 20;
            case GL_DEPTH_COMPONENT32F            -> 21;
            case GL_DEPTH24_STENCIL8              -> 22;
            case GL_DEPTH32F_STENCIL8             -> 23; // LAST_FORMAT_INDEX
            default -> 0;
        };
    }
    //@formatter:on

    /**
     * Reverse of {@link #glFormatToIndex(int)}.
     */
    @NativeType("GLenum")
    public static int glIndexToFormat(int index) {
        return switch (index) {
            case 0 -> GL_NONE;
            case 1 -> GL_RGBA8;
            case 2 -> GL_R8;
            case 3 -> GL_RGB565;
            case 4 -> GL_RGBA16F;
            case 5 -> GL_R16F;
            case 6 -> GL_RGB8;
            case 7 -> GL_RG8;
            case 8 -> GL_RGB10_A2;
            case 9 -> GL_SRGB8_ALPHA8;
            case 10 -> GL_COMPRESSED_RGB8_ETC2;
            case 11 -> GL_COMPRESSED_RGB_S3TC_DXT1_EXT;
            case 12 -> GL_COMPRESSED_RGBA_S3TC_DXT1_EXT;
            case 13 -> GL_R16;
            case 14 -> GL_RG16;
            case 15 -> GL_RGBA16;
            case 16 -> GL_RG16F;
            case 17 -> GL_STENCIL_INDEX8;
            case 18 -> GL_STENCIL_INDEX16;
            case 19 -> GL_DEPTH_COMPONENT16;
            case 20 -> GL_DEPTH_COMPONENT24;
            case 21 -> GL_DEPTH_COMPONENT32F;
            case 22 -> GL_DEPTH24_STENCIL8;
            case 23 -> GL_DEPTH32F_STENCIL8;
            default -> {
                assert false : index;
                yield GL_NONE;
            }
        };
    }

    /**
     * @see Color#COLOR_CHANNEL_FLAGS_RGBA
     */
    public static int glFormatChannels(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_RGBA8,
                    GL_RGBA16,
                    GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
                    GL_SRGB8_ALPHA8,
                    GL_RGB10_A2,
                    GL_RGBA16F -> Color.COLOR_CHANNEL_FLAGS_RGBA;
            case GL_R8,
                    GL_R16,
                    GL_R16F -> Color.COLOR_CHANNEL_FLAG_RED;
            case GL_RGB565,
                    GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
                    GL_COMPRESSED_RGB8_ETC2,
                    GL_RGB8 -> Color.COLOR_CHANNEL_FLAGS_RGB;
            case GL_RG8,
                    GL_RG16F,
                    GL_RG16 -> Color.COLOR_CHANNEL_FLAGS_RG;
            default -> 0;
        };
    }

    /**
     * Consistent with {@link #glFormatToIndex(int)}
     */
    public static boolean glFormatIsSupported(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_RGBA8,
                    GL_R8,
                    GL_RGB565,
                    GL_RGBA16F,
                    GL_R16F,
                    GL_RGB8,
                    GL_RG8,
                    GL_RGB10_A2,
                    GL_SRGB8_ALPHA8,
                    GL_COMPRESSED_RGB8_ETC2,
                    GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
                    GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
                    GL_R16,
                    GL_RG16,
                    GL_RGBA16,
                    GL_RG16F,
                    GL_STENCIL_INDEX8,
                    GL_STENCIL_INDEX16,
                    GL_DEPTH_COMPONENT16,
                    GL_DEPTH_COMPONENT24,
                    GL_DEPTH_COMPONENT32F,
                    GL_DEPTH24_STENCIL8,
                    GL_DEPTH32F_STENCIL8 -> true;
            default -> false;
        };
    }

    /**
     * @see ColorInfo#COMPRESSION_NONE
     */
    @ColorInfo.CompressionType
    public static int glFormatCompressionType(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_COMPRESSED_RGB8_ETC2 -> ColorInfo.COMPRESSION_ETC2_RGB8_UNORM;
            case GL_COMPRESSED_RGB_S3TC_DXT1_EXT -> ColorInfo.COMPRESSION_BC1_RGB8_UNORM;
            case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> ColorInfo.COMPRESSION_BC1_RGBA8_UNORM;
            default -> ColorInfo.COMPRESSION_NONE;
        };
    }

    public static int glFormatBytesPerBlock(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_RGBA8,
                    GL_DEPTH24_STENCIL8,
                    GL_DEPTH_COMPONENT32F,
                    GL_RG16F,
                    GL_RG16,
                    GL_SRGB8_ALPHA8,
                    GL_RGB10_A2,
                    // We assume the GPU stores this format 4 byte aligned
                    GL_RGB8,
                    GL_DEPTH_COMPONENT24 -> 4;
            case GL_R8,
                    GL_STENCIL_INDEX8 -> 1;
            case GL_DEPTH_COMPONENT16,
                    GL_STENCIL_INDEX16,
                    GL_R16,
                    GL_RG8,
                    GL_R16F,
                    GL_RGB565 -> 2;
            case GL_RGBA16F,
                    GL_RGBA16,
                    // We assume the GPU stores this format 8 byte aligned
                    GL_DEPTH32F_STENCIL8,
                    GL_COMPRESSED_RGBA_S3TC_DXT1_EXT,
                    GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
                    GL_COMPRESSED_RGB8_ETC2 -> 8;
            default -> 0;
        };
    }

    public static int glFormatDepthBits(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_DEPTH_COMPONENT16 -> 16;
            case GL_DEPTH_COMPONENT24,
                    GL_DEPTH24_STENCIL8 -> 24;
            case GL_DEPTH_COMPONENT32F,
                    GL_DEPTH32F_STENCIL8 -> 32;
            default -> 0;
        };
    }

    public static int glFormatStencilBits(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_STENCIL_INDEX8,
                    GL_DEPTH24_STENCIL8,
                    GL_DEPTH32F_STENCIL8 -> 8;
            case GL_STENCIL_INDEX16 -> 16;
            default -> 0;
        };
    }

    public static boolean glFormatIsPackedDepthStencil(@NativeType("GLenum") int format) {
        return format == GL_DEPTH24_STENCIL8 || format == GL_DEPTH32F_STENCIL8;
    }

    public static boolean glFormatIsSRGB(@NativeType("GLenum") int format) {
        return format == GL_SRGB8_ALPHA8;
    }

    public static boolean glFormatIsCompressed(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_COMPRESSED_RGB8_ETC2,
                    GL_COMPRESSED_RGB_S3TC_DXT1_EXT,
                    GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> true;
            default -> false;
        };
    }

    public static String glFormatName(@NativeType("GLenum") int format) {
        return switch (format) {
            case GL_RGBA8 -> "RGBA8";
            case GL_R8 -> "R8";
            case GL_RGB565 -> "RGB565";
            case GL_RGBA16F -> "RGBA16F";
            case GL_R16F -> "R16F";
            case GL_RGB8 -> "RGB8";
            case GL_RG8 -> "RG8";
            case GL_RGB10_A2 -> "RGB10_A2";
            case GL_RGBA32F -> "RGBA32F";
            case GL_SRGB8_ALPHA8 -> "SRGB8_ALPHA8";
            case GL_COMPRESSED_RGB8_ETC2 -> "ETC2";
            case GL_COMPRESSED_RGB_S3TC_DXT1_EXT -> "RGB8_BC1";
            case GL_COMPRESSED_RGBA_S3TC_DXT1_EXT -> "RGBA8_BC1";
            case GL_R16 -> "R16";
            case GL_RG16 -> "RG16";
            case GL_RGBA16 -> "RGBA16";
            case GL_RG16F -> "RG16F";
            case GL_STENCIL_INDEX8 -> "STENCIL_INDEX8";
            case GL_STENCIL_INDEX16 -> "STENCIL_INDEX16";
            case GL_DEPTH24_STENCIL8 -> "DEPTH24_STENCIL8";
            default -> APIUtil.apiUnknownToken("GLenum", format);
        };
    }

    @NonNull
    public static String getDebugSource(int source) {
        return switch (source) {
            case GL_DEBUG_SOURCE_API -> "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM -> "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER -> "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY -> "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION -> "Application";
            case GL_DEBUG_SOURCE_OTHER -> "Other";
            default -> APIUtil.apiUnknownToken(source);
        };
    }

    @NonNull
    public static String getDebugType(int type) {
        return switch (type) {
            case GL_DEBUG_TYPE_ERROR -> "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR -> "Deprecated Behavior";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR -> "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY -> "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE -> "Performance";
            case GL_DEBUG_TYPE_OTHER -> "Other";
            case GL_DEBUG_TYPE_MARKER -> "Marker";
            default -> APIUtil.apiUnknownToken(type);
        };
    }

    @NonNull
    public static String getDebugSeverity(int severity) {
        return switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH -> "High";
            case GL_DEBUG_SEVERITY_MEDIUM -> "Medium";
            case GL_DEBUG_SEVERITY_LOW -> "Low";
            case GL_DEBUG_SEVERITY_NOTIFICATION -> "Notification";
            default -> APIUtil.apiUnknownToken(severity);
        };
    }

    @NonNull
    public static String getSourceARB(int source) {
        return switch (source) {
            case GL_DEBUG_SOURCE_API_ARB -> "API";
            case GL_DEBUG_SOURCE_WINDOW_SYSTEM_ARB -> "Window System";
            case GL_DEBUG_SOURCE_SHADER_COMPILER_ARB -> "Shader Compiler";
            case GL_DEBUG_SOURCE_THIRD_PARTY_ARB -> "Third Party";
            case GL_DEBUG_SOURCE_APPLICATION_ARB -> "Application";
            case GL_DEBUG_SOURCE_OTHER_ARB -> "Other";
            default -> APIUtil.apiUnknownToken(source);
        };
    }

    @NonNull
    public static String getTypeARB(int type) {
        return switch (type) {
            case GL_DEBUG_TYPE_ERROR_ARB -> "Error";
            case GL_DEBUG_TYPE_DEPRECATED_BEHAVIOR_ARB -> "Deprecated Behavior";
            case GL_DEBUG_TYPE_UNDEFINED_BEHAVIOR_ARB -> "Undefined Behavior";
            case GL_DEBUG_TYPE_PORTABILITY_ARB -> "Portability";
            case GL_DEBUG_TYPE_PERFORMANCE_ARB -> "Performance";
            case GL_DEBUG_TYPE_OTHER_ARB -> "Other";
            default -> APIUtil.apiUnknownToken(type);
        };
    }

    @NonNull
    public static String getSeverityARB(int severity) {
        return switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH_ARB -> "High";
            case GL_DEBUG_SEVERITY_MEDIUM_ARB -> "Medium";
            case GL_DEBUG_SEVERITY_LOW_ARB -> "Low";
            default -> APIUtil.apiUnknownToken(severity);
        };
    }

    @NonNull
    public static String getCategoryAMD(int category) {
        return switch (category) {
            case GL_DEBUG_CATEGORY_API_ERROR_AMD -> "API Error";
            case GL_DEBUG_CATEGORY_WINDOW_SYSTEM_AMD -> "Window System";
            case GL_DEBUG_CATEGORY_DEPRECATION_AMD -> "Deprecation";
            case GL_DEBUG_CATEGORY_UNDEFINED_BEHAVIOR_AMD -> "Undefined Behavior";
            case GL_DEBUG_CATEGORY_PERFORMANCE_AMD -> "Performance";
            case GL_DEBUG_CATEGORY_SHADER_COMPILER_AMD -> "Shader Compiler";
            case GL_DEBUG_CATEGORY_APPLICATION_AMD -> "Application";
            case GL_DEBUG_CATEGORY_OTHER_AMD -> "Other";
            default -> APIUtil.apiUnknownToken(category);
        };
    }

    @NonNull
    public static String getSeverityAMD(int severity) {
        return switch (severity) {
            case GL_DEBUG_SEVERITY_HIGH_AMD -> "High";
            case GL_DEBUG_SEVERITY_MEDIUM_AMD -> "Medium";
            case GL_DEBUG_SEVERITY_LOW_AMD -> "Low";
            default -> APIUtil.apiUnknownToken(severity);
        };
    }

    @NativeType("GLuint")
    public static int glCompileShader(GLDevice device,
                                      @NativeType("GLenum") int shaderType,
                                      @NativeType("GLchar const *") ByteBuffer glsl,
                                      SharedResourceCache.Stats stats) {
        var gl = device.getGL();
        int shader = gl.glCreateShader(shaderType);
        if (shader == 0) {
            return 0;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long string = stack.npointer(glsl);
            long length = stack.nint(glsl.remaining());
            gl.glShaderSource(shader, 1, string, length);
        } finally {
            Reference.reachabilityFence(glsl);
        }

        gl.glCompileShader(shader);
        stats.incShaderCompilations();

        if (gl.glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = gl.glGetShaderInfoLog(shader);
            gl.glDeleteShader(shader);
            handleCompileError(device.getLogger(), MemoryUtil.memUTF8(glsl), log);
            return 0;
        }

        return shader;
    }

    @NativeType("GLuint")
    public static int glSpecializeShader(GLDevice device,
                                         @NativeType("GLenum") int shaderType,
                                         @NativeType("uint32_t *") ByteBuffer spirv,
                                         String entryPoint,
                                         SharedResourceCache.Stats stats) {
        var gl = device.getGL();
        int shader = gl.glCreateShader(shaderType);
        if (shader == 0) {
            return 0;
        }
        try (MemoryStack stack = MemoryStack.stackPush()) {
            var shaders = stack.ints(shader);
            gl.glShaderBinary(shaders, GL_SHADER_BINARY_FORMAT_SPIR_V, spirv);
            // no specialization constants, but must pass a valid pointer
            shaders.limit(0);
            gl.glSpecializeShader(shader, entryPoint, shaders, shaders);
        } finally {
            Reference.reachabilityFence(spirv);
        }

        stats.incShaderCompilations();

        if (gl.glGetShaderi(shader, GL_COMPILE_STATUS) == GL_FALSE) {
            String log = gl.glGetShaderInfoLog(shader);
            gl.glDeleteShader(shader);
            device.getLogger().error("Shader specialization error: {}", log);
            return 0;
        }

        return shader;
    }

    public static void handleCompileError(Logger logger,
                                          String source,
                                          String errors) {
        if (!logger.isErrorEnabled()) return;
        var f = new Formatter();
        f.format("Shader compilation error%n");
        f.format("------------------------%n");
        String[] lines = source.split("\n");
        for (int i = 0; i < lines.length; ++i) {
            f.format(Locale.ROOT, "%4d\t%s%n", i + 1, lines[i]);
        }
        f.format(errors);
        logger.error(f.toString());
    }

    public static void handleLinkError(Logger logger,
                                       String[] headers,
                                       String[] sources,
                                       String errors) {
        if (!logger.isErrorEnabled()) return;
        var f = new Formatter();
        f.format("Program linking error%n");
        f.format("---------------------%n");
        for (int i = 0; i < headers.length; i++) {
            f.format("%s%n", headers[i]);
            String[] lines = sources[i].split("\n");
            for (int j = 0; j < lines.length; ++j) {
                f.format(Locale.ROOT, "%4d\t%s%n", j + 1, lines[j]);
            }
        }
        f.format(errors);
        logger.error(f.toString());
    }

    //@formatter:off
    public static int toGLMagFilter(int filter) {
        return switch (filter) {
            case SamplerDesc.FILTER_NEAREST -> GL_NEAREST;
            case SamplerDesc.FILTER_LINEAR  -> GL_LINEAR;
            default -> throw new AssertionError(filter);
        };
    }

    public static int toGLMinFilter(int filter, int mipmapMode) {
        return switch (mipmapMode) {
            case SamplerDesc.MIPMAP_MODE_NONE    -> toGLMagFilter(filter);
            case SamplerDesc.MIPMAP_MODE_NEAREST -> switch (filter) {
                case SamplerDesc.FILTER_NEAREST  -> GL_NEAREST_MIPMAP_NEAREST;
                case SamplerDesc.FILTER_LINEAR   -> GL_LINEAR_MIPMAP_NEAREST;
                default -> throw new AssertionError(filter);
            };
            case SamplerDesc.MIPMAP_MODE_LINEAR  -> switch (filter) {
                case SamplerDesc.FILTER_NEAREST  -> GL_NEAREST_MIPMAP_LINEAR;
                case SamplerDesc.FILTER_LINEAR   -> GL_LINEAR_MIPMAP_LINEAR;
                default -> throw new AssertionError(filter);
            };
            default -> throw new AssertionError(mipmapMode);
        };
    }

    public static int toGLWrapMode(int addressMode) {
        return switch (addressMode) {
            case SamplerDesc.ADDRESS_MODE_REPEAT          -> GL_REPEAT;
            case SamplerDesc.ADDRESS_MODE_MIRRORED_REPEAT -> GL_MIRRORED_REPEAT;
            case SamplerDesc.ADDRESS_MODE_CLAMP_TO_EDGE   -> GL_CLAMP_TO_EDGE;
            case SamplerDesc.ADDRESS_MODE_CLAMP_TO_BORDER -> GL_CLAMP_TO_BORDER;
            default -> throw new AssertionError(addressMode);
        };
    }
    //@formatter:on

    //@formatter:off
    public static int toGLBlendEquation(byte equation) {
        return switch (equation) {
            case BlendInfo.EQUATION_SUBTRACT            -> GL_FUNC_SUBTRACT;
            case BlendInfo.EQUATION_REVERSE_SUBTRACT    -> GL_FUNC_REVERSE_SUBTRACT;
            default -> GL_FUNC_ADD;
        };
    }
    //@formatter:on

    //@formatter:off
    public static int toGLBlendFactor(byte factor) {
        return switch (factor) {
            case BlendInfo.FACTOR_ONE                       -> GL_ONE;
            case BlendInfo.FACTOR_SRC_COLOR                 -> GL_SRC_COLOR;
            case BlendInfo.FACTOR_ONE_MINUS_SRC_COLOR       -> GL_ONE_MINUS_SRC_COLOR;
            case BlendInfo.FACTOR_DST_COLOR                 -> GL_DST_COLOR;
            case BlendInfo.FACTOR_ONE_MINUS_DST_COLOR       -> GL_ONE_MINUS_DST_COLOR;
            case BlendInfo.FACTOR_SRC_ALPHA                 -> GL_SRC_ALPHA;
            case BlendInfo.FACTOR_ONE_MINUS_SRC_ALPHA       -> GL_ONE_MINUS_SRC_ALPHA;
            case BlendInfo.FACTOR_DST_ALPHA                 -> GL_DST_ALPHA;
            case BlendInfo.FACTOR_ONE_MINUS_DST_ALPHA       -> GL_ONE_MINUS_DST_ALPHA;
            case BlendInfo.FACTOR_CONSTANT_COLOR            -> GL_CONSTANT_COLOR;
            case BlendInfo.FACTOR_ONE_MINUS_CONSTANT_COLOR  -> GL_ONE_MINUS_CONSTANT_COLOR;
            case BlendInfo.FACTOR_CONSTANT_ALPHA            -> GL_CONSTANT_ALPHA;
            case BlendInfo.FACTOR_ONE_MINUS_CONSTANT_ALPHA  -> GL_ONE_MINUS_CONSTANT_ALPHA;
            case BlendInfo.FACTOR_SRC_ALPHA_SATURATE        -> GL_SRC_ALPHA_SATURATE;
            case BlendInfo.FACTOR_SRC1_COLOR                -> GL_SRC1_COLOR;
            case BlendInfo.FACTOR_ONE_MINUS_SRC1_COLOR      -> GL_ONE_MINUS_SRC1_COLOR;
            case BlendInfo.FACTOR_SRC1_ALPHA                -> GL_SRC1_ALPHA;
            case BlendInfo.FACTOR_ONE_MINUS_SRC1_ALPHA      -> GL_ONE_MINUS_SRC1_ALPHA;
            default -> GL_ZERO;
        };
    }
    //@formatter:on

    //@formatter:off
    static {
        //noinspection ConstantValue
        assert (0x200 | DepthStencilSettings.COMPARE_OP_NEVER   ) == GL_NEVER;
        //noinspection ConstantValue
        assert (0x200 | DepthStencilSettings.COMPARE_OP_LESS    ) == GL_LESS;
        //noinspection ConstantValue
        assert (0x200 | DepthStencilSettings.COMPARE_OP_EQUAL   ) == GL_EQUAL;
        //noinspection ConstantValue
        assert (0x200 | DepthStencilSettings.COMPARE_OP_LEQUAL  ) == GL_LEQUAL;
        //noinspection ConstantValue
        assert (0x200 | DepthStencilSettings.COMPARE_OP_GREATER ) == GL_GREATER;
        //noinspection ConstantValue
        assert (0x200 | DepthStencilSettings.COMPARE_OP_NOTEQUAL) == GL_NOTEQUAL;
        //noinspection ConstantValue
        assert (0x200 | DepthStencilSettings.COMPARE_OP_GEQUAL  ) == GL_GEQUAL;
        //noinspection ConstantValue
        assert (0x200 | DepthStencilSettings.COMPARE_OP_ALWAYS  ) == GL_ALWAYS;
    }
    //@formatter:on

    public static int toGLCompareFunc(byte compareOp) {
        // the above assertions ensure the conversion
        return 0x200 | compareOp;
    }

    //@formatter:off
    public static int toGLStencilOp(byte stencilOp) {
        return switch (stencilOp) {
            case DepthStencilSettings.STENCIL_OP_KEEP       -> GL_KEEP;
            case DepthStencilSettings.STENCIL_OP_ZERO       -> GL_ZERO;
            case DepthStencilSettings.STENCIL_OP_REPLACE    -> GL_REPLACE;
            case DepthStencilSettings.STENCIL_OP_INC_CLAMP  -> GL_INCR;
            case DepthStencilSettings.STENCIL_OP_DEC_CLAMP  -> GL_DECR;
            case DepthStencilSettings.STENCIL_OP_INVERT     -> GL_INVERT;
            case DepthStencilSettings.STENCIL_OP_INC_WRAP   -> GL_INCR_WRAP;
            case DepthStencilSettings.STENCIL_OP_DEC_WRAP   -> GL_DECR_WRAP;
            default -> throw new AssertionError();
        };
    }
    //@formatter:on

    public static int toGLPrimitiveType(byte primitiveType) {
        return switch (primitiveType) {
            case Engine.PrimitiveType.kPointList -> GL_POINTS;
            case Engine.PrimitiveType.kLineList -> GL_LINES;
            case Engine.PrimitiveType.kLineStrip -> GL_LINE_STRIP;
            case Engine.PrimitiveType.kTriangleList -> GL_TRIANGLES;
            case Engine.PrimitiveType.kTriangleStrip -> GL_TRIANGLE_STRIP;
            default -> throw new AssertionError();
        };
    }
}
