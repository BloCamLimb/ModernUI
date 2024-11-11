## Performance Comparison

Here is a *crude* performance comparison between Arc3D Granite and Skia Ganesh.
The tests were done on the same machine (Ryzen 7 4800H), both used OpenGL API and GTX 1660 Ti to perform rendering.
Neither of them reached the GPU bottleneck.

Common requirements:
* have a 3D perspective transform
* have screen-space antialiasing without MSAA
* all objects are translucent (must respect painter's order)
* stroke cap is round
* stroke join is round

### Test Scene 1
Requirements:
* 10,000 random round rectangles (changed per frame)
* overstroking (half stroke width > corner radius) is allowed

The following parameters are random:
* the solid color
* the width value ranges from 20 to 370 px
* the height value ranges from 20 to 270 px
* the corner radius ranges from 0 to 50 px, and no greater than min(width,height)/2
* the rectangle's x/y position
* the rectangle's rotation
* the style (fill/stroke)
* the stroke width ranges from 10 to 60 px

![test_10000_roundrects2_arc3d.png](https://s2.loli.net/2024/11/11/mSbZo4DO2EiVprT.png)
![test_10000_roundrects2_skia.png](https://s2.loli.net/2024/11/11/6t4yMWYLrI5dcEN.png)

Result:
- Arc3D: 203 FPS on average
- Skia: 0.79 FPS on average

Arc3D is 256 times faster than Skia

### Test Scene 2
Requirements:
* 10,000 random pairs of object (changed per frame)
* each pair has a round rectangle followed by an arc
* arc's span angle is 130 degrees
* always strokes, stroke width is 10 px
* overstroking (half stroke width > corner radius) is allowed

The following parameters are random:
* the solid color
* the width value ranges from 20 to 370 px
* the height value ranges from 20 to 270 px
* the corner radius ranges from 0 to 50 px, and no greater than min(width,height)/2
* the rectangle's x/y position
* the rectangle's rotation

![test_complex_scene_arc3d.png](https://s2.loli.net/2024/11/11/P6AtHudEhOv8FBw.png)
![test_complex_scene_skia.png](https://s2.loli.net/2024/11/11/UisfbVaF1nJLA9z.png)

Result:
- Arc3D: 75 FPS on average
- Skia: 0.22 FPS on average

Arc3D is 340 times faster than Skia

#### Postscript
Not yet compared with Vulkan API, as Arc3D's Vulkan backend is not yet completed;  
Not yet compared with Skia Graphite, but in Chrome, Graphite performs worse than Ganesh.