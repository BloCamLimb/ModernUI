module icyllis.modernui.core {
    requires transitive java.datatransfer;

    requires transitive icyllis.modernui.base;

    //TODO delete fastutil
    requires static it.unimi.dsi.fastutil;

    //TODO delete glfw, openal, opengl, stb, tinyfd
    requires static org.lwjgl.glfw;
    requires static org.lwjgl.openal;
    requires static org.lwjgl.opengl;
    requires static org.lwjgl.stb;
    requires static org.lwjgl.tinyfd;

    // Compile only
    requires static org.jetbrains.annotations;
    requires static org.jspecify;

    exports icyllis.modernui;
    exports icyllis.modernui.app;
    exports icyllis.modernui.app.windows;
    exports icyllis.modernui.audio;
    exports icyllis.modernui.core;
    exports icyllis.modernui.fragment;
    exports icyllis.modernui.lifecycle;
    exports icyllis.modernui.material;
    exports icyllis.modernui.material.drawable;
    exports icyllis.modernui.text.method;
    exports icyllis.modernui.transition;
    exports icyllis.modernui.view;
    exports icyllis.modernui.view.menu;
    exports icyllis.modernui.widget;
}