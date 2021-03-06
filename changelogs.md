Changelogs
===
###
* Fix smooth scrolling
* Introduce new animation API

### 1.16.5-2.5.1.86 (2021-05-13)
* Fix rendering on some graphics cards
* Fix first scroll bar dragging

### 1.16.5-2.5.0.85 (2021-04-16)
* Broaden scrolling compatibility with other mods
* Tweak circular progress bar animation
* Expose additional HUD bars option
* Add platform window
* Add fast matrix calculation

### 1.16.5-2.4.9.84 (2021-03-24)
* Update shaders to GLSL 430 core
* Improve filling and stroking of round rect shader
* Add filling and stroking of arc and circle shader
* Add circular progress bar for world loading screen
* Smooth scrolling for vanilla and forge scroll panels

### 1.16.5-2.4.7.82 (2021-03-17)
* Several optimizations
* Fix lifecycle crash
* Warn old Java only once
* Fix enchantment characters

### 1.16.5-2.4.5.80 (2021-02-17)
* Add text layouts, line breaker
* Broaden compatibility

### 1.16.5-2.4.4.79 (2021-02-03)
* Support down to Java 8u51
* Improve digits alignment

### 1.16.5-2.4.3.78 (2021-01-31)
* Optimize memory usage for text layout cache
* Fix font mipmap texture not reset after reusing

### 1.16.5-2.4.2.77 (2021-01-27)
* Fix compatibility with ItemZoom
* Auto switch font resolution level
* Add grapheme cluster breaker
* Change to absolute coordinate system

### 1.16.5-2.4.1.76 (2021-01-22)
* Improve experience bar rendering
* Improve gui scale setting
* Fix compatibility layer with vanilla

### 1.16.4-2.4.0.75 (2021-01-17)
* Change project structure
* Add SFX when game loaded
* Configurable tooltip frame color
* Make font renderer reloadable at runtime

### 1.16.4-2.3.5.74 (2021-01-11)
* Fix backslash path
* Fix tooltip matrix transformation

### 1.16.4-2.3.4.73 (2021-01-09)
* Fix tooltip compatibilities
* Smooth tooltip rendering

### 1.16.4-2.3.3.72 (2021-01-08)
* Add new tooltip style
* Fix crash with some java versions
* Fix crash when running data generator
* Update the network protocol

### 1.16.4-2.3.2.71 (2021-01-04)
* Fix client crash due to parallel mod loading
* Add auto-shutdown for server

### 1.16.4-2.3.1.70 (2020-12-31)
* Fix crash when forge event bus not started
* Fix crash on dedicated server

### 1.16.4-2.3.0.69 (2020-12-26)
* Fix ingame GUI rendering with no texture in some case
* Fix font renderer not override some mods (like InventoryHud)
* Add input event handling
* Add signature

### 1.16.4-2.2.4.68 (2020-11-21)
* Fix sometimes crash when caching digit texture
* (2.2.3.67) (2020-11-17)
* Fix rendering when an invalid formatting code applied
* (2.2.2.66) (2020-11-16)
* Fix rendering with space character

### 1.16.4-2.2.1.65 (2020-11-14)
* Fix rendering with Thai
* Add support for external fonts
* Expose built-in blacklist

### 1.16.3-2.2.0.64 (2020-09-17)
* Port to 1.16.3

### 1.16.2-2.1.1.63 (2020-09-01)
* Fix IReorderingProcessor generator
* Fix empty layout node not being considered
* Fix blur effect with pumpkins on the head
* Fix animation time disruption between frames
* Fix animation timer on game paused

### 1.16.2-2.1.0.62 (2020-08-18)
* Optimize blur shader
* Add OpenGL capabilities check
* Make text caching work asynchronously
* Add support for IReorderingProcessor

### 1.16.1-2.0.4.61 (2020-08-07)
* Fix font renderer see through type
* Add ultra-high definition for font rendering

### 1.16.1-2.0.3.60 (2020-08-03)
* Fix text position texture out of limit bounds
* Fix empty text node can't be processed
* Adjust text render layer, avoid performance loss

### 1.16.1-2.0.2.59 (2020-08-02)
* Fix text color background rendering
* Fix invalid text formatting codes not being removed
* Require Forge 32.0.93+

### 1.16.1-2.0.1.58 (2020-07-24)
* Rewrite text processing, fix text formatting
* Remove vanilla bidi analysis for every text in every frame
* Fix fragmentary bidirectional text layout (mixed LTR RTL)
* Fix vanilla's Arabic letters shaping (start, middle, end)
* Fix Devanagari (Hindi etc) and other characters rendering
* Fix text effect rendering and render type
* Fix text width measuring, trimming
* Optimize text rendering on RAM and FPS
* Adjust the alignment accuracy of the digit rendering

### 1.16.1-2.0.0.57 (2020-07-18)
* Reduce the requirement of OpenGL 4.6 to OpenGL 4.3
* Improve the text layout accuracy of font renderer
* Improve the size and advance accuracy of glyph
* Improve the alignment accuracy of digit rendering
* Improve the rule of font priority to use
* Use grayscale pixels to store font textures to reduce memory usage
* Use render nodes for text rendering to improve performance
* Support higher level mipmap for font textures
* Support vanilla text styles and components
* Add font size style to text component
* Add more configs for font renderer
* Add more configs for blur effect and background opacity

### 1.15.2-1.5.9.56 (2020-05-26)
* Fix search bar crash
* Fix server crash on start

### 1.15.2-1.5.8.55 (2020-05-17)
* Fix keyboard listener auto lose focus, and integrated in module
* Fix widget relocate method
* Fix pause only main menu screen logic
* Fix scroll controller minimum precision
* Fix not disable keyboard listener repeat mode when gui closed
* Change layout editor shortcut key
* Update icon textures

### 1.15.2-1.5.7.54 (2020-05-15)
* Reimplement double-click event, make it work in resource packs GUI  
* Adjust status changing behaviour (API broken)  
* Fix multi-page scroll panel page bug  
* Fix animation chain crash  
* Fix scroll window total height bug  
* Add layout editing GUI  
* Add feathered rect shader  
* Add two-way expandable box area  
* Add config to set whether enable lib only mode  
* Add config to set whether enable blurring effect  
* Add echo char to text field 

### 1.15.2-1.5.6.53 (2020-05-10)
* Fix font renderer render type not switched properly  
* Fix special render face culling of font renderer  
* Fix characters not display / layout properly (extremely confused) while game is paused  
* Fix characters get dislocated (combined with other chars) when a new texture mapping started  
* Add new methods to animation, and fix button brightness bug  
* Add config to set whether allow drawing font with shadow  
* Add config to set whether pause game when any screen is open  
* Add multi-page scroll panel  
* Add clip to canvas  

### 1.15.2-1.5.5.52 (2020-05-08)
Add mipmap support for mui font renderer  
Fix render type of mui font renderer

### 1.15.2-1.5.4.51 (2020-05-06)
Fix crash with optifine connected textures

### 1.15.2-1.5.3.50 (2020-05-05)
Make font renderer work globally in game

### 1.15.2-1.5.2.49 (2020-05-03)
Fix compatibility with OptiFine 1.15.2 HD U G1 pre14+  
Add almost all optifine settings in iteratable form  
Add scroll panel, a light-weighted scroll window  
Require Forge 31.1.63+

### 1.15.2-1.5.1.48 (2020-05-02)
Fix text icon button, default module in  
Fix number input field, max long  
Fix slided toggle button, default on status  
Fix dynamic button press

### 1.15.2-1.5.0.47 (2020-05-02)
Rework animations  
Rework all widgets  
Rework all implementations  
Add button sounds  
Add developer mode  
Fix ingame menu opened before load complete  
Fix wrong font renderer calling  

### 1.15.2-1.4.7.41 (2020-04-23)
Fix background alpha reset incorrectly  
Keep MUI screens / modules instance when using vanilla's parent screen system, also fixed container screen

### 1.15.2-1.4.5.39 (2020-04-19)
Add gui background alpha gradient  
Fix compatibility with vanilla and container screen  
Make "reset keys" button in Controls into an icon button for cleaner look

### 1.15.2-1.4.4.38 (2020-04-18)
Fix client container won't be closed  
Fix switch child module can't be called by root module constructor  
Fix text icon button won't light up when was called by constructor  
Add russian localization (by vanja-san)

### 1.15.2-1.4.3.37 (2020-04-18)
Fix API compatibility

### 1.15.2-1.4.2.36 (2020-04-18)
Fix wrong displacement while following texturedGlyph in scroll window  
Improve and perfect the KeyBinding search function

### 1.15.2-1.4.1.35 (2020-04-17)
Make rounded frame render more smooth  
Add new text field  
Add search bar to Controls GUI, allows to search key or name  
Add show KeyBinding conflicts function  

### 1.15.2-1.4.0.34 (2020-04-16)
Rework drawing system, and use new canvas system now  
Add new widgets  
Add new shaders  
Bug fixes and UI tweaks  
Code reduction, clean-up and optimization  
Reduce resources size  

### 1.15.2-1.3.7.32 (2020-04-14)
Add rounded rectangle shaders  
Make transition animation more smooth  

### 1.15.2-1.3.6.31 (2020-04-12)
Add transition animation  
Add java version detection  

### 1.15.2-1.3.5.30 (2020-04-10)
Initial Release  