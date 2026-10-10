module icyllis.modernui.base {
    requires transitive java.xml;

    requires transitive icyllis.arc3d.core;
    requires transitive icyllis.arc3d.sketch;
    requires transitive icyllis.arc3d.engine;
    requires transitive icyllis.arc3d.granite;

    requires transitive org.slf4j;

    requires transitive com.ibm.icu;

    requires transitive org.lwjgl;
    requires transitive org.lwjgl.sdl;

    requires static icyllis.arc3d.opengl;
    requires static icyllis.arc3d.vulkan;

    //TODO delete glfw, stb, tinyfd
    requires static org.lwjgl.glfw;
    requires static org.lwjgl.stb;
    requires static org.lwjgl.tinyfd;

    requires static org.lwjgl.opengl;
    requires static org.lwjgl.opengles;
    requires static org.lwjgl.vulkan;

    //TODO delete fastutil
    requires static it.unimi.dsi.fastutil;

    // Compile only
    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    exports icyllis.modernui.animation;
    exports icyllis.modernui.annotation;
    exports icyllis.modernui.graphics;
    exports icyllis.modernui.graphics.drawable;
    exports icyllis.modernui.graphics.pipeline;
    exports icyllis.modernui.graphics.text;
    exports icyllis.modernui.renderer;
    exports icyllis.modernui.resources;
    exports icyllis.modernui.system;
    exports icyllis.modernui.system.windows;
    exports icyllis.modernui.text;
    exports icyllis.modernui.text.style;
    exports icyllis.modernui.util;
}