# Arc3D
Arc3D is a modern graphics engine, which is written purely in Java 17 and based on LWJGL 3.3.3

### Components
#### Compiler
A compiler that compiles shaders written in Arc3D shading language into SPIR-V 1.0+, standard GLSL 3.30+,
or standard ESSL 3.00+, it is 70x times faster than glslang/shaderc/glslc
#### Engine
It is the abstraction layer of various 3D graphics APIs (OpenGL 4, Vulkan), providing
common rendering code for both 2D and 3D, resource/memory management, API-abstract command list, etc.
#### OpenGL
OpenGL implementation to RHI/Engine
#### Vulkan
Vulkan implementation to RHI/Engine
#### Granite
A renderer inspired by Skia Graphite, for 2D geometry/text rendering. It has better optimizations on
discrete GPUs, usually faster than Chromium (not well tested, I'm waiting for Vector API and Project Valhalla).

### Dependencies
Arc3D depends on LWJGL (OpenGL binding, Vulkan binding, Vulkan Memory Allocator), fastutil, and SLF4J API,
no other dependencies.

### Progress/Plan
Currently, Arc3D is only used for [ModernUI](https://github.com/BloCamLimb/ModernUI).
I'm planning to use Arc3D to write a mod similar https://github.com/xCollateral/VulkanMod for Minecraft.
I am currently not interested in writing a documentation for Arc3D, you have to read the source code to find more.