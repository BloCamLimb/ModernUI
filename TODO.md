## TODO List
Plan:
* For 2D pipeline, use two uniform block binding, one for geometry steps, including vec4 projection;
  another for fragment substages

- [ ] Small Image Atlas - stitch small images (e.g. width<=64, height<=64) into an atlas, to reduce
  sampled image descriptor binding change
- [x] Uniform Block Cache - cache the whole uniform block, hash block data using ByteBuffer.hashCode()
  and mismatch, to reduce buffer uploads and descriptor binding change
- [ ] Raster Path Atlas - use two or three atlas texture for path image rendered by software, atlases have
  different caching strategies
- [x] Univariate Gradient Shader - specializing for 2, 4 (if branches), 8 (unrolled binary search) stops,
  use texture and binary search for more, 256 stops at most
- [x] New Clip Stack - Use depth test for sequential clip (z is increasing, z range is 0..65535 fixed-point),
  instead of using stencil test for hierarchical clip (push/pop); this makes use of depth buffer and
  reduce state change (stencil ref); 24-bit depth format is fixed-point, 32-bit depth format is
  floating-point, but ensure 24 bit precision in the range 0..1, we use 16 bits just in case of z-fighting?
- [ ] Ring Buffer - for 2D pipeline, streaming buffer allocation, vertex/instance and uniform
- [ ] Static Index Buffer - cache index buffer using pattern, such as rect or nine slice
- [ ] Various SDF Geometry Renderer
- [ ] GPU GIF decoding - upload compressed GIF data to SSBO, use compute shader for GIF decoding, insert
  barriers before sampling, no mipmapping
- [ ] OpenGL Texture Views - use texture views for GLImage to reduce change on texture's swizzle state
  and mipmap level range
- [ ] New 2D Pipeline Cache
- [ ] Use compute shader for path tessellation, based on [Vello](https://github.com/linebender/vello)
- [x] If the paint has a solid color, try to put color into instance data instead of uniform data
- [ ] Vulkan push constants support
- [ ] Compute exact GPU memory size for Vulkan memoryless images

Shader Compiler:
- [ ] Disallow struct types that used in SSBO/UBO also used for local variables, because their memory
  layouts are different; no pointer types in GLSL
- [ ] Complete the GLSL and SPIR-V generator

Note:
* For OpenGL ES, if base vertex is unavailable, gl_VertexID always begins at 0
* For OpenGL, regardless of the baseInstance value, gl_InstanceID always begins at 0