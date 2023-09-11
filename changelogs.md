Changelogs
===
### Modern UI 3.8.2 (2023-08-30)
#### Core Framework 3.8.2
* Add compatibility with LWJGL 3.2
#### Arc 3D Graphics Engine 3.8.2

### Modern UI 3.8.1 (2023-08-30)
#### Core Framework 3.8.1
* Add LayerDrawable
* Allow typeface change for toasts
* Fix adapter views not getting attached
* Fix changing focus with TAB key
* Make ScrollView auto scroll to focus
* Add implementation of LineBreakConfig
* Stop text from being split into small MeasureText.Runs
#### Arc 3D Graphics Engine 3.8.1
* Add rect stroke bevel and round shaders
* Limit FontAtlas size
* Make use of GLSL version in ShaderCaps
* Add numDraws and renderPasses stats

### Modern UI 3.8.0 (2023-08-10)
#### Core Framework 3.8.0
* Update font itemization for color emoji
* Fit sub-windows in main window
* Add ColorEmoji support for font itemization
* Optimize GPU glyph memory usage
* Block NUL and DEL character from input
* Add EmojiFont features
* Replace Matrix4 with Matrix for View
* Add TooltipPopup support for View
* Add Font interface for layout engine, move old Font usage to StandardFont
* Add text pre-computation
* Replace all ascents with negative values
* Fix TextLine context range
* Add offset parameters to GlyphsConsumer
* Add TextShaper.shapeText for multi-styled text
* Fix Canvas.drawText(ShapedText) method
* Update TextShaper and Canvas methods
* Add TrailingMarginSpan, add LineBackgroundSpan, add TypefaceSpan
* Enhance rich text spans, add AlignmentSpan, BulletSpan, LeadingMarginSpan, QuoteSpan
* Optimize MeasuredText
* Supports text layout with optional per-glyph advances and pixel bounds
* Separate Arc Paint, Graphics Paint, TextPaint and FontPaint
* Enhance temp TextBuffer recycling
* Fix fast path for BoringLayout.draw()
* Remove Canvas.drawText(CharSequence)
* Replace old LayoutPiece usage with ShapedText.doLayout
* Optimize text rendering
* Fix FontFamily is not thread safe
* Add Matrix for 2D transform (xyw), Matrix3 for 3D transform (xyz)
* Migrate graphics package to Arc 3D
* Add explicit camera distance
* Other small updates...
#### Markdown 3.8.0
* Add Code and CodeBlock
* Initial basic Markdown support
#### Arc 3D Graphics Engine 3.8.0
* Add experimental instanced rendering
* Optimize text rendering
* Always use buffer orphaning for OpenGL
* Add backend render target wrapping
* Add submit method for BufferPool
* Add IndexMeshPool
* Move some context states out of GLCommandBuffer
* Add RingBuffer
* Add some Vulkan backend classes

### Modern UI 3.7.1 (2023-07-09)
#### Core Framework 3.7.1
* Add context range to LayoutCache
* Add draw ShapedText, add register method to FontFamily
* Add ShapedText and TextShaper for drawing text, make drawText deprecated
* Rework LayoutPiece, optimize cluster work, now it won't do texture work
* Add FontFamily aliases
* Moving low-level text layout to graphics.text package
* Fix MessageQueue parkNanos time unit
* Don't use perspective if View has no 3D transform
* Add WindowGroup for toasts and popups
* Add context to Toast
* Add MpmcArrayQueue
#### Arc 3D Graphics Engine 3.7.1
* Fix BufferAllocPool
* Fix OpsRenderPass buffer pointers
* Update VaryingHandler and UniformHandler layout qualifier
* Add compatibility for base instance support
* Add GLUniformBuffer and buffer upload
* Add FilterMode, MipmapMode and AnisotropicFiltering for Paint
* Add async pipeline state cache (thread safe)
* Add separate objects from graphics package
* Add CustomDrawable for handling unmanaged draw
* Fix Buffer unlock method
* Fix BufferAlloc pointers
* Add RoundRectGeoProc
* Add Buffer creation method to Engine
* Add DrawOp and MeshDrawOp
* Add OpListTask
* Replace GLPipeline with separate GLProgram and GLVertexArray
* Add GLOpsRenderPass
* Add GLUniformDataManager
* Support base offset for GLVertexArray in OpenGL 3.3
* Fix Surface hierarchy

### Modern UI 3.7.0 (2023-06-20)
#### Core Framework 3.7
* Optimize view matrix composition
* Rework threading system
* Add ProgressBar and SeekBar
* Add Locale alias for FontFamily
* Add ScaleDrawable
* Change ScrollView default scrollbar
* Fix bitmap getPixelARGB on BIG ENDIAN machine
* Rework ShapeDrawable, remove old Shape classes
* Set long press timeout to 1000ms by default
* Add LinkMovementMethod
* Update BinaryIO
* Update ListView
* Update Pool and Pools
* Update Paint methods
* Add ViewPager module
* Rework drawLine method with SDF
* Add drawPie drawMesh canvas method
* Add GIF image decoder
* Add more Bitmap formats and image decode methods
* Merge Arc 3D graphics engine
* Add framework Nullable/NonNull annotations
* Allow Bitmap's pixels to be shared
* Add ColorSpace implementation
* Update Matrix and MathUtil
* Optimize for styled text, make text style serializable
* Add complex value and display metrics
* Add linked structure and custom data class for DataSet
* Add property name for Property
#### Arc 3D Graphics Engine 3.7
* Use MSAA resolve instead of MS texture sampler
* Add compatibility methods for creating render target objects
* Add close() for LazyCallback proxies
* Add GL_RG for GrayAlpha surface usage
* Add OpenGL 3.3 compatible shaders
* Add mutable texture allocation methods
* Add swizzle on texture binding
* Use separate min/mag filter for SamplerState
* Optimize surface canvas methods
* Optimize font atlas generation
* Change default font atlas size to 4 chunks (1024x1024)
* Change number of font atlases from multiple to single
* Add efficient rectangle packing algorithms
#### View Pager 1.0
* Add linear pager indicator
* Initial update

### Modern UI 3.6.3.117 (2022-12-15)
#### Forge Extension 1.19.2-43.1.2
* Adjust GUI scale algorithm
* Adjust tooltip border width
#### Modern Text Engine 1.19.2
* Add distance field text to 3D world
* Fix total advance of layout is never pixel-aligned
* Fix TTF loading in vanilla resource packs
* Adjust texture sharpening factor
#### Core Framework 3.6
* Fix distance-to-edge anti-aliasing is gamma correct

### Modern UI 3.6.2.116 (2022-11-30)
#### Forge Extension 1.19.2-43.1.2
* Support capability for fragments and screens
* Fix incorrect color blending for tooltip
* Fix GUI scale is always auto after restart
* Fix black screen with blur effect
* Add glow wave effect
#### Modern Text Engine 1.19.2
* Fix line breaking for obfuscated chars
* Remove enchantment font hack
* Support vanilla bitmap font in replacement run
* Support vanilla bitmap font as font family
* Support vanilla font declaration
* Adjust text decoration thickness
* Reduce unnecessary native memory usage in vanilla
* Make font resource reloading work async
* Optimize ChatFormatting.getByCode to O(1)
* Optimize layout caching strategy
* Add font set to layout key
#### Core Framework 3.6
* Adjust analytic anti-aliasing to screen-space
* Improve font collection itemization
* Update shaders to a modern version

### Modern UI 3.6.1.115 (2022-10-07)
#### Forge Extension 1.19.2-43.1.2
* Change the background blur from 4-pass box blur to 2-pass gaussian blur with lod and noise
* Improve config reloading and action center UI
* Fix crash on dedicated server (though you shouldn't install on server)
* Add config to disable slider GUI scale
* Update GUI scale algorithm
#### Modern Text Engine 1.19.2
* Add sharpen font atlases
* Change blend mode for glowing text
#### Core Framework 3.6
* Use indexed rendering for glyph meshes
* Add sharpen textures for all shaders
* Always use premultiplied alpha for builtin renderer

### Modern UI 3.6.0.114 (2022-09-07)
#### Forge Extension 1.19.2-43.1.2
* Add I18n compat characters
* Add new API and remove server dependency
* Change registration and network to internal
* Support for OptiFine 1.19.2
* Migrate to MC & Forge 1.19.2
#### Modern Text Engine 1.19.2
* Add new glowing text effect
* Add deferred rendering to improve performance
* Migrate to MC 1.19.2
#### Core Framework 3.6
* Compact font atlas generation

### Modern UI 3.5.4.113 (2022-08-19)
#### Forge Extension 1.18.2-40.0.12
* Add support for creating window with the highest OpenGL it can
* Fix config reloading even if nothing changed
* Add debug stuff
#### Modern Text Engine 1.18.2
* No updates
#### Core Framework 3.5
* Workaround SPACE tunneling event is not consumed in EditText

### Modern UI 3.5.3.112 (2022-08-17)
#### Forge Extension 1.18.2-40.0.12
* Enhance GL caps error screen
* Add support for requesting OpenGL 4.6 or 4.5 core profile window avoiding GL caps errors
#### Modern Text Engine 1.18.2
* No updates
#### Core Framework 3.5
* Internal changes

### Modern UI 3.5.2.111 (2022-07-27)
#### Forge Extension 1.18.2-40.0.12
* Add debug stuff
* Add traditional Chinese support
#### Modern Text Engine 1.18.2
* Add support for sign glowing text
* Add shadow offset and outline offset config
* Fix auto disable OptiFine fast render not working
* Add support for COMBINING ENCLOSING KEYCAP
* Fix index crash when EditBox contains formatting codes
#### Core Framework 3.5
* Internal changes

### Modern UI 3.5.1.110 (2022-07-20)
#### Forge Extension 1.18.2-40.0.12
* Fix window mode sometimes didn't work correctly
* Fix tooltip BG or FG transparency sorting and write into depth buffer
#### Modern Text Engine 1.18.2
* Enhance and smooth EditBox rendering
* Adjust alpha threshold from 1 to 2
* Fix NPE when font renders fast chars nothing
* Fix line feed ignored when using fast path of line breaking
* Fix text empty when using fast path of text breaking backwards
#### Core Framework 3.5
* Internal changes

### Modern UI 3.5.0.109 (2022-07-12)
#### Forge Extension 1.18.2-40.0.12
* Make extensions backward compatible to OpenGL 3.3
* Add window mode config (like fullscreen borderless)
* Add config of ignoring GL errors
* Add GL error GUI and link directing
* Add right-to-left layout for modern tooltips
* Add in-game GUI for all new configs
* Add dump of memory info of all textures
* Fix tooltip shaking when rendering at bottom
* Dispatch generic pointer events
* Enable more configs to take effect in real-time without restarting
* Disable Minecraft render thread assertions
* Auto disable OptiFine fast render
#### Modern Text Engine 1.18.2
* Make text engine backward compatible to OpenGL 3.3
* Add Slack and Discord Emoji shortcode support
* Add BiDi text line breaking with color Emoji support
* Add Unicode color Emoji support (Twemoji 14.0)
* Add pre mipmap generation for bitmaps
* Add bitmap replacement support for text layout
* Add Unicode line breaking algorithm
* Add Unicode text breaking algorithm
* Add Unicode grapheme cluster break algorithm
* Add substring conservative algorithm for line breaking
* Add config of color emoji and grayscale emoji
* Add config of BiDi text direction heuristic algorithm
* Add config of font anti-aliasing and precise metrics
* Add config of font base size and baseline shift
* Add config of rehash threshold and recycle time
* Add config of snapping to pixels for text layout
* Add config of fixing invalid surrogate pairs
* Add config of fast digit replacement
* Add Minecraft vanilla TTF font
* Add fast character replacement and optimize its generator
* Add super-sampling config and disable it by default
* Make BiDi text layout always in visual order
* Fix discontinuous style in continuous text layout
* Reduce the overhead of GL state changing
* Optimize the recycling logic of text layout nodes
* Optimize text layout and iteration performance
* Optimize lookup key and searching
* Refactor text layout engine
#### Core Framework 3.5
* Internal changes

### Modern UI 3.4.7.108 (2022-06-18)
#### Forge Extension 1.18.2-40.0.12
* Enhance layout transition when closed
* Add support for custom OpenGL driver
#### Modern Text Engine 1.18.2
* Fix GUI crash when disabled
#### Core Framework 3.4
* Enhance GL capability check

### Modern UI 3.4.6.107 (2022-06-11)
#### Forge Extension 1.18.2-40.0.12
* Add server version
* Add tooltip anim duration config
* Add inventory pause
* Fix dedicated server startup
#### Modern Text Engine 1.18.2
* Add font atlas dump
#### Core Framework 3.4
* Internal changes

### Modern UI 3.4.5.106 (2022-04-24)
#### Forge Extension 1.18.2-40.0.12
#### Modern Text Engine 1.18.2
* Add baseline config
* Add more obfuscated chars
* Enhance breaking multilayer text at a point
* Add font size config
#### Core Framework 3.4
* Internal changes

### Modern UI 3.4.3.102 (2022-04-01)
#### Forge Extension 1.18.2-40.0.12
* Refactor the loader and improved compatibility
* Add buttons to disable text engine and extensions
* Add radial blur effect (beta)
* Port to 1.18.2 (1.18.1 works as well)
#### Modern Text Engine 1.18.2
* Preload the engine and parallel cleaner
* Fix font blur when GUI scale is 1 or 2
* Fix obfuscated chars layout
#### Core Framework 3.4
* Internal changes

### Modern UI 3.4.0.99 (2022-03-09)
#### Forge Extension 1.18.1-39.1.2
* Stop crashing if some mods failed to load
* Fix screen lifecycle and threading bugs
* Add DataSet utils
* Add more configs to Center UI
* Make all registries only in dev mode
* Quit UI thread safely
#### Modern Text Engine 1.18.1
* No updates
#### Core Framework 3.4
* Improve documentation
* Add ReactiveX
* Add BlendMode and color blending
* Add TextView context menu
* Add clickable text styles
* Add Slide and Explode transition
* Add transition SFX to fragments
* Add FULL support for Transition framework
* Add int keys for DataSet
* Add standalone application bootstrap
* Add multithreaded event synchronizer
* Add ContextMenu popup
* Add Menus and Radio Buttons
* Add DropDownList and relevant components
* Add ListView and relevant components
* Add FULL support for ScrollView
* Add FULL support for Nested Scrolling
* Add triangle drawing operation
* Add new scrollbar features
* Add foreground layer to View
* Fix PointerIcon resolving
* Fix some RTL layout bugs
* Fix padding not working sometimes
* Fix inverse matrix
* Fix orthographic matrix
* Auto lose EditText focus
* New touch event handling
* Multithreading AnimationHandler
* Remove generic of animated values
* Add new features:
  - Add RelativeRadioGroup
  - Add FragmentResultListener
  - Add ArrayAdapter
  - Add PopupMenu
  - Add Spinner
  - Add MenuItemView
  - Add ImageView
  - Add EdgeEffect
  - Add AnimatorSet
  - Add PopupWindow
  - Add CoordinatorLayout
  - Add Filter
  - Add HandlerThread
  - Add ActionProvider
  - Add ActionMode
  - Add ContextMenu
  - Add MenuItem
  - Add SubMenu
  - Add Menu
  - Add Menus
  - Add CharacterMap
  - Add SparseBooleanMap
  - Add EditText
  - Add SoundEffects
  - Add AdapterView
  - Add VelocityTracker
  - Add ValueAnimator
  - Add Shapes
  - Add StateListDrawable
  - Add DrawableContainer
  - Add ColorStateListDrawable
  - Add ImageDrawable
  - Add ColorDrawable
  - Add Drawables
  - Add StateListAnimator
  - Add StateListColor
  - Add StateSet

### Modern UI 3.3.0.98 (2022-01-23)
#### Forge Extension 1.18.1-39.0.5
* Stabilize Forge API and components
* Add Center UI (Ctrl+K)
* Fix several transition and lifecycle bugs
* Add blur effect to screen background
* Fix vanilla tooltip text not rendering
#### Modern Text Engine 1.18.1
* Add bootstrap config, OR 1 to disable
* Fix underline and strikethrough not rendering
* Close package
#### Core Framework 3.3
* Always linear sampling font textures
* Merge view alpha property and transitions
* Change paint properties
* Change gradient color ordering
* Add tree base for new render pipeline
* Update Libraries
  - Log4j 2.14.1 -> 2.17.0
  - caffeine 3.0.4 -> 3.0.5
  - icu4j 69.1 -> 70.1
  - LWJGL 3.2.2 -> 3.3.0
* Synchronize UI messages
* Update shaders and canvas pipeline
* Update high precision time source
* Add new event loop framework (Fast blocking) (Big Update)
  - Native Main Thread
  - Async Tasks
  - ...
* Optimize bitmap I/O, add .jfif .jif detect
* Add Fragment (inherited from Module) full support (Big Update)
  - Back Stack
  - State Manager
  - Special Effects
  - Callback
  - ...
* Add DataSet (Fast I/O and persistent storage)
* Add tree observer methods
* Add new movement method
* Add fast key-held linked list with removal support
* Add Lifecycle full support (Big Update)
  - Live Data
  - Observer
  - ...

### Modern UI 3.2.0.97 (2021-12-07)
#### Forge Extension 1.18-38.0.15
* Port to 1.18, remove deprecated methods
* Add destroy state to callback lifecycle
* Fix container not closed when backing
#### Modern Text 1.18 (Embedded)
#### Core Framework 3.2
* Require Java 17, update libraries
* Add toast, toast manager and presenter
* Fix anticipate overshoot interpolator
* Expose thread scheduling methods
* Fix invisible state not working
* Fix null layouts not working
* Notify hierarchy changes to ViewGroup
* Add selected and activated states to View
* Update RelativeLayout
* Fix alpha blending between render targets
* Rename view scale usages
* Add number input filters
* Fix primitive array increment
* Add text color attributes
* Add listener setters

### 1.17.1-3.1.0.95 (2021-11-21)
#### Forge Extension 1.17.1-37.0.97
* Integrate multi-threading pipeline with Blaze3D
* Public stable APIs
* Check code style
#### Modern Text 1.17 (Embedded)
#### Core Framework 3.1
* Fix single line text alignment and scrolling
* Add compound drawables to text view
* Fix background paddings and transitions
* Add cursor selection, movement/scrolling
* Update clipboard manager
* Add blinking cursor and selection rendering
* Add transformation method
* Add arrow key movement method
* Add cursor movement and text/word deletion
* Add text selection and movement
* Add key event dispatching
* Add pointer icon resolution
* Add focus system for view tree
* Fix transform with non-homogeneous vectors
* Update view root and view tree protocol
* Add layout transition for a view group
* Add alpha transition of a view layer
* Add child off-screen rendering targets
* Improve MSAA framebuffer and attachments
* Make animators cloneable
* Add replacement style

### 1.17.1-3.0.1.94 (2021-10-27)
#### Forge Extension 1.17.1-37.0.96
* Update new tooltip events
* Fix registry references
* Add network handler on netty thread
* Update network protocol
* Rename packages
#### Modern Text 1.17 (Embedded)
* Fix external fonts cannot be loaded on Linux
#### Core Framework 3.0 (Preview)
* Add view paddings and RTL properties
* Update layout containers
* Update UI thread scheduling
* Enhance quadratic bezier shader
* Add DynamicLayout
* Add text methods
* Fix various bugs on text engine
* Add SpannableStringBuilder
* Fix bugs on LineBreaker
* Avoid allocating large arrays for getSpans
* Add GrowingArrayUtils

### 1.17.1-3.0.0.93 (2021-10-02)
#### Forge Extension 1.17.1-37.0.70
* Enhance tooltip rendering and add gradient effect
* Fix hex colors cannot be parsed
* Port to 1.17.1
#### Modern Text 1.17 (Embedded)
* Port to 1.17.1, use OpenGL core profile
#### Core Framework 3.0 (Preview)
* Require Java 16

### 1.16.5-2.6.4.92 (2021-09-29)
#### Forge Extension 1.16.5-36.2.0
* Optimize packet dispatcher
* Add gradient color on screen background blurring
* Add new tooltip rendering based on the new engine
* Adjust the lifecycle of render system and text engine
* Fix smooth scrolling for vanilla/forge panels
* Modify network protocol algorithm
* Add efficient network channel
* Fix compatibility to several mods
* Add namespaced events to mod buses
#### Modern Text 1.16 (Embedded)
* Support rendering and sampling with bitmap-like fonts
* Fix rendering with bold and italic styles
* Fix rendering with enchantment characters
* Improve typeface setting and font run algorithm
* Improve BiDi and style algorithms on text layouts
* Add new cache key on deep processed char sequences
* Add new layout cache system and tracker
* Add automatic resolution level switching
* Support continuous text layout from deep processors
* Support taking over text layout and caching from the source
* Optimize text and effect rendering pipeline
* Optimize formatting code resolver algorithm
* Optimize layout caching on text components and sequences
* Optimize rendering on multilayer styled text
* Introduce character style carrier for state injection
* Expand the scope of application of text engine
* Fix bidirectional text rendering with multiple styles
* Fix rendering with texts computed from fast digit algorithm
* Fix dirty font texture data on sprite borders
* Change the behavior on built-in font loading
* Improve experience bar text rendering
* Fix rendering with Thai and Bengali
#### Core Framework 3.0 (Preview)
* Finish StaticLayout for text pages
* Add recyclable span set and draw text command
* Add deferred calculation grapheme advance and full layout
* Fix various bugs for layout cache
* Add texture manager and image creation
* Add scaling image drawing and dimension
* Remove context selector
* Cleanup GLTexture usages in subclasses
* Add quadratic Bezier curve drawing
* Optimize OpenGL rendering pipeline for canvas
* Cleanup deprecated classes and code
* Optimize spannable string implementation
* Fix sample array allocating
* Add streaming ogg vorbis decoder and wave decoder
* Add audio tracks for 2D sound playback
* Add more text styles and effects
* Optimize layout cache, measured text and rendering
* Add new switch button widget
* Add visual audio spectrum
* Optimize view refresh mechanism
* Add touch event for mouse operations, add click listener
* Remove multiple pointers on event delivery, capture mouse
* Add fast fourier transform for spectrum analysis
* Optimize input event dispatching
* Introduce AudioManager based on OpenAL
* Apply 4x MSAA to UI framebuffer
* Add the base part for text lines
* Add memory calculation on measured text
* Add efficient layout cache for layout pieces
* Optimize glyph layout and rendering for text runs
* Optimize text rendering pipeline
* Add layout piece for the layout of a text run
* Add PMX model parser
* Add directions information for bidirectional text
* Optimize GlyphManager for glyph layouts and rendering
* Add new dynamic generation algorithm for font atlases
* Add new scrolling algorithm to the view system
* Support gradient color for geometries rendering
* Add new scroller for controlling 2D scrolling
* Add decomposable transformation
* Apply clipping to the view system
* Add quick reject against local clipping region
* Introduce new clipping system and stencil test
* Add drawables and host callbacks
* Cleanup forge canvas API and lifecycles
* Add automatic resizing for framebuffers
* Add local state switching on drawing commands
* Coordinate UI thread animation and drawing
* Optimize rendering for view hierarchy
* Drop support for Java 10 or below
* Add round lines and more drawing methods
* Add image drawing and recycle bitmaps
* Switch to multithreaded rendering
* Add automatic vertex array object generation
* Add vertex attributes and vertex buffer binding points
* Introduce GLCanvas and update shaders
* Introduce a new method to create shader programs
* Update to OpenGL 4.5 and Direct State Access
* Update core package structure
* Add MSAA framebuffer and renderbuffer objects
* Use cleaner to release native resources
* Support tab stops and base paragraph-level style
* Merge new animation API with the old one
* Add evaluator for custom interpolation algorithm
* Add more time interpolator(s)
* Add custom keyframe types and keyframe set
* Introduce new animation framework and state machine
* Add supported image formats and open dialog
* Add bitmap for decoding images into memory and exporting
* Add texture objects on client side
* Suppress unsupported clipboard contents
* Fix resource reading using native memory
* Optimize rotation about arbitrary axis
* Add efficient quaternion math for rotations
* Add efficient matrix/vector math for rendering
* Add Rect, Point, and their float forms
* Add the concept of UI thread that differs from render thread
* Abstract the API on the rendering pipeline
* Add platform components to operating system
* Add fragment shader to draw circular arcs
* Add paint for rendering geometries
* Update shaders to GLSL 430 core
* Add font metric calculation
* Separate text paint at different levels
* Add support for emoji code points
* Add application-level typeface and text locale
* Add itemization algorithm for font runs
* Add Unicode-based line breaker for text pages
* Add base measured text for text shaping
* Add object pools for recycling
* Enhance style run transitions for paragraph layout
* Add new BiDi analyzer for measuring paragraphs
* Add various text direction heuristic algorithm
* Add metric affecting styles
* Add character-level appearance styles
* Add support for Unicode grapheme cluster break
* Add texts with markup objects
* Add text package for high-level layouts
* Change coordinates to match window framebuffer
* Add lifecycle handler for screens

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