/*
 * Modern UI.
 * Copyright (C) 2019-2020 BloCamLimb. All rights reserved.
 *
 * Modern UI is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Modern UI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Modern UI. If not, see <https://www.gnu.org/licenses/>.
 */

package icyllis.modernui.ui.master;

import com.mojang.blaze3d.systems.RenderSystem;
import icyllis.modernui.graphics.math.Point;
import icyllis.modernui.graphics.renderer.Canvas;
import icyllis.modernui.system.Config;
import icyllis.modernui.system.ModernUI;
import icyllis.modernui.ui.animation.Animation;
import icyllis.modernui.ui.layout.Gravity;
import icyllis.modernui.ui.layout.MeasureSpec;
import icyllis.modernui.ui.test.IModule;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.IHasContainer;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.inventory.container.Container;
import net.minecraft.inventory.container.ContainerType;
import net.minecraft.inventory.container.INamedContainerProvider;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.lwjgl.opengl.GL11;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * UI system service, almost everything is here.
 * Manage HUD and the current UI window, and all the windows that accompany the UI.
 */
@SuppressWarnings("unused")
@OnlyIn(Dist.CLIENT)
public enum UIManager implements IViewParent {
    /**
     * Render thread instance
     */
    INSTANCE;

    // logger marker
    public static final Marker MARKER = MarkerManager.getMarker("UI");

    // cached minecraft instance
    private final Minecraft minecraft = Minecraft.getInstance();

    // cached gui to open
    @Nullable
    private Screen guiToOpen;

    // current screen instance, must be ModernScreen or ModernContainerScreen or null
    @Nullable
    private Screen modernScreen;

    // main fragment of a UI
    private Fragment fragment;

    // main UI view that created from main fragment
    private View view;

    private final List<View> upperViews = new ArrayList<>();

    @Deprecated
    @Nullable
    private IModule popup;

    // scaled game window width / height
    private int width, height;

    // scaled mouseX, mouseY on screen
    private double mouseX, mouseY;

    // a list of animations in render loop
    private final List<Animation> animations = new ArrayList<>();

    // a list of UI tasks
    private final List<DelayedTask> tasks = new CopyOnWriteArrayList<>();

    // elapsed ticks from a gui open, update every tick, 20 = 1 second
    private int ticks = 0;

    // elapsed time from a gui open in milliseconds
    private int time = 0;

    // the canvas to draw things shared in all views and drawables
    // lazy loading because this class is loaded before GL initialization
    // will be init when Minecraft finished loading, and open MainMenuScreen
    // also init font renderer when loaded
    private Canvas canvas;

    // the most child hovered view, render at the top of other hovered ancestor views
    @Nullable
    private View mHovered;

    // focused view
    @Nullable
    private View mDragging;
    @Nullable
    private View mKeyboard;

    // for double click check, default 10 tick = 0.5s
    private int lastDClickTick = Integer.MIN_VALUE;

    // to schedule layout on next frame
    private boolean layoutRequested = false;

    // to fix layout freq at 40Hz at most
    private int lastLayoutTime = 0;

    // drag center, also marks whether a drag and drop operation is ongoing
    @Nullable
    private Point dragCenter;

    UIManager() {

    }

    /**
     * Returns the instance on render thread
     *
     * @return instance
     */
    public static UIManager getInstance() {
        return INSTANCE;
    }

    /**
     * Open GUI screen and UI window
     *
     * @param mainFragment main fragment of the UI
     * @see #init(Screen, int, int)
     */
    public void openGui(@Nonnull Fragment mainFragment) {
        this.fragment = mainFragment;
        minecraft.displayGuiScreen(new ModernScreen());
    }

    /**
     * Close GUI screen, put UI window and all windows above it into recycler
     *
     * @see #destroy()
     */
    public void closeGui() {
        minecraft.displayGuiScreen(null);
    }

    /**
     * Register a container screen to open the gui screen.
     *
     * @param type    container type
     * @param factory main fragment factory
     * @param <T>     container class type
     * @see net.minecraftforge.fml.network.NetworkHooks#openGui(ServerPlayerEntity, INamedContainerProvider, Consumer)
     */
    public <T extends Container, U extends Screen & IHasContainer<T>> void registerContainerScreen(
            @Nonnull ContainerType<? extends T> type, @Nonnull Function<T, Fragment> factory) {
        // prevent compiler error
        ScreenManager.registerFactory(type, cast(factory));
    }

    @Nonnull
    @SuppressWarnings({"unchecked", "ConstantConditions"})
    private <T extends Container, U extends Screen & IHasContainer<T>> ScreenManager.IScreenFactory<T, U> cast(
            @Nonnull Function<T, Fragment> factory) {
        return (container, playerInventory, title) -> {
            // The client container can be null sometimes, but a container screen doesn't allow the container to be null
            // so return null, there's no gui will be open, and the server container will be closed automatically
            if (container == null) {
                return null;
            }
            this.fragment = factory.apply(container);
            return (U) new ModernContainerScreen<>(container, playerInventory, title);
        };
    }

    /**
     * Open a popup module, a special module
     *
     * @param popup   popup module
     * @param refresh true will post mouseMoved(-1, -1) to root module
     *                confirm window should reset mouse
     *                context menu should not reset mouse
     */
    //TODO new popup system
    public void openPopup(IModule popup, boolean refresh) {
        /*if (root == null) {
            ModernUI.LOGGER.fatal(MARKER, "#openPopup() shouldn't be called when there's NO gui open");
            return;
        }*/
        if (this.popup != null) {
            ModernUI.LOGGER.warn(MARKER, "#openPopup() shouldn't be called when there's already a popup, the previous one has been overwritten");
        }
        if (refresh) {
            this.sMouseMoved(-1, -1);
        }
        this.popup = popup;
        this.popup.resize(width, height);
        refreshMouse();
    }

    /**
     * Close current popup
     */
    public void closePopup() {
        if (popup != null) {
            popup = null;
            refreshMouse();
        }
    }

    /**
     * Called when open a gui screen, or back to the gui screen
     *
     * @param mui    modern screen or modern container screen
     * @param width  screen width (= game main window width)
     * @param height screen height (= game main window height)
     */
    void init(@Nonnull Screen mui, int width, int height) {
        modernScreen = mui;

        // init view of this UI
        if (view == null) {
            if (fragment == null) {
                ModernUI.LOGGER.fatal(MARKER, "Fragment can't be null when opening a gui screen");
                closeGui();
                return;
            }
            view = fragment.createView();
            if (view == null) {
                ModernUI.LOGGER.fatal(MARKER, "The main view created from the fragment shouldn't be null");
                view = new View();
            }
            fragment.root = view;

            ViewGroup.LayoutParams params = view.getLayoutParams();
            // convert layout params
            if (!(params instanceof LayoutParams)) {
                params = new LayoutParams(LayoutParams.UI_WINDOW);
                view.setLayoutParams(params);
            }
            ((LayoutParams) params).type = LayoutParams.UI_WINDOW;

            view.assignParent(this);
        }

        resize(width, height);
    }

    /**
     * Inner method, do not call
     */
    @SubscribeEvent
    void gGuiOpen(@Nonnull GuiOpenEvent event) {
        guiToOpen = event.getGui();

        // create canvas, also font renderer
        if (canvas == null) {
            canvas = new Canvas(minecraft);
        }

        if (guiToOpen == null) {
            destroy();
            return;
        }

        if (modernScreen != guiToOpen && ((guiToOpen instanceof ModernScreen) || (guiToOpen instanceof ModernContainerScreen<?>))) {
            if (view != null) {
                // prevent repeated opening sometimes
                event.setCanceled(true);
                return;
            }
            ticks = 0;
            time = 0;
        }
        // hotfix 1.5.2, but there's no way to work with screens that will pause game
        if (modernScreen != guiToOpen && modernScreen != null) {
            sMouseMoved(-1, -1);
        }
        // for non-modern-ui screens
        if (modernScreen == null) {
            ticks = 0;
            time = 0;
        }
    }

    /**
     * Get current open screen differently from Minecraft's,
     * which will only return Modern UI's screen or null
     * {@link Minecraft#currentScreen}
     *
     * @return open modern screen
     */
    @Nullable
    public Screen getModernScreen() {
        return modernScreen;
    }

    /**
     * Add an active animation, which will be removed from list if finished
     *
     * @param animation animation to add
     */
    public void addAnimation(@Nonnull Animation animation) {
        if (!animations.contains(animation)) {
            animations.add(animation);
        }
    }

    /**
     * Post a task that will run on next pre-tick
     *
     * @param runnable     runnable
     * @param delayedTicks delayed ticks to run the task
     */
    public void postTask(@Nonnull Runnable runnable, int delayedTicks) {
        if (delayedTicks <= 0) {
            runnable.run();
            return;
        }
        tasks.add(new DelayedTask(runnable, delayedTicks));
    }

    void sMouseMoved(double mouseX, double mouseY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        mouseMoved();
        /*if (popup != null) {
            popup.mouseMoved(mouseX, mouseY);
            return;
        }*/
    }

    boolean sMouseClicked(double mouseX, double mouseY, int mouseButton) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        /*if (popup != null) {
            return popup.mouseClicked(mouseX, mouseY, mouseButton);
        }*/
        if (mHovered != null) {
            IViewParent parent;
            View view;
            double viewMX = getViewMouseX(mHovered);
            double viewMY = getViewMouseY(mHovered);
            if (mouseButton == 0) {
                int delta = ticks - lastDClickTick;
                if (delta < 10) {
                    lastDClickTick = Integer.MIN_VALUE;
                    if (mHovered.onMouseDoubleClicked(viewMX, viewMY)) {
                        return true;
                    }
                    parent = mHovered.getParent();
                    double viewMX2 = viewMX;
                    double viewMY2 = viewMY;
                    while (parent instanceof View) {
                        view = (View) parent;
                        viewMX2 -= parent.getScrollX();
                        viewMY2 -= parent.getScrollY();
                        if (view.onMouseDoubleClicked(viewMX2, viewMY2)) {
                            return true;
                        }
                        parent = parent.getParent();
                    }
                } else {
                    lastDClickTick = ticks;
                }
            }
            if (mHovered.mouseClicked(viewMX, viewMY, mouseButton)) {
                return true;
            }
            parent = mHovered.getParent();
            while (parent instanceof View) {
                view = (View) parent;
                viewMX -= parent.getScrollX();
                viewMY -= parent.getScrollY();
                if (view.mouseClicked(viewMX, viewMY, mouseButton)) {
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    boolean sMouseReleased(double mouseX, double mouseY, int mouseButton) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        /*if (popup != null) {
            return popup.mouseReleased(mouseX, mouseY, mouseButton);
        }*/
        if (mDragging != null) {
            setDragging(null);
            return true;
        }
        if (mHovered != null) {
            double viewMX = getViewMouseX(mHovered);
            double viewMY = getViewMouseY(mHovered);
            if (mHovered.onMouseReleased(viewMX, viewMY, mouseButton)) {
                return true;
            }
            IViewParent parent = mHovered.getParent();
            View view;
            while (parent instanceof View) {
                view = (View) parent;
                viewMX -= parent.getScrollX();
                viewMY -= parent.getScrollY();
                if (view.onMouseReleased(viewMX, viewMY, mouseButton)) {
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;//root.mouseReleased(mouseX, mouseY, mouseButton);
    }

    boolean sMouseDragged(double mouseX, double mouseY, double deltaX, double deltaY) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        /*if (popup != null) {
            return popup.mouseDragged(mouseX, mouseY, deltaX, deltaY);
        }*/
        if (mDragging != null) {
            return mDragging.onMouseDragged(getViewMouseX(mDragging), getViewMouseY(mDragging), deltaX, deltaY);
        }
        return false;
    }

    boolean sMouseScrolled(double mouseX, double mouseY, double amount) {
        this.mouseX = mouseX;
        this.mouseY = mouseY;
        /*if (popup != null) {
            return popup.mouseScrolled(mouseX, mouseY, amount);
        }*/
        if (mHovered != null) {
            double viewMX = getViewMouseX(mHovered);
            double viewMY = getViewMouseY(mHovered);
            if (mHovered.onMouseScrolled(viewMX, getViewMouseY(mHovered), amount)) {
                return true;
            }
            IViewParent parent = mHovered.getParent();
            View view;
            while (parent != null) {
                view = (View) parent;
                viewMX -= parent.getScrollX();
                viewMY -= parent.getScrollY();
                if (view.onMouseScrolled(mouseX, mouseY, amount)) {
                    return true;
                }
                parent = parent.getParent();
            }
        }
        return false;
    }

    boolean sKeyPressed(int keyCode, int scanCode, int modifiers) {
        /*if (popup != null) {
            return popup.keyPressed(keyCode, scanCode, modifiers);
        }*/
        if (mKeyboard != null) {
            return mKeyboard.onKeyPressed(keyCode, scanCode, modifiers);
        }
        return false;
    }

    boolean sKeyReleased(int keyCode, int scanCode, int modifiers) {
        /*if (popup != null) {
            return popup.keyReleased(keyCode, scanCode, modifiers);
        }*/
        if (mKeyboard != null) {
            return mKeyboard.onKeyReleased(keyCode, scanCode, modifiers);
        }
        return false;//root.keyReleased(keyCode, scanCode, modifiers);
    }

    boolean sCharTyped(char codePoint, int modifiers) {
        /*if (popup != null) {
            return popup.charTyped(codePoint, modifiers);
        }*/
        if (mKeyboard != null) {
            return mKeyboard.onCharTyped(codePoint, modifiers);
        }
        return false;//root.charTyped(codePoint, modifiers);
    }

    boolean sChangeKeyboard(boolean searchNext) {
        //TODO change focus implementation
        return false;
    }

    boolean sBack() {
        if (popup != null) {
            closePopup();
            return true;
        }
        return false;//root.onBack();
    }

    /**
     * Refocus mouse cursor and update mouse hovering state
     */
    public void refreshMouse() {
        mouseMoved();
    }

    /**
     * Find mouse hovering focus for entire UI
     */
    private void mouseMoved() {
        if (view != null && !view.updateMouseHover(mouseX, mouseY)) {
            setHovered(null);
        }
    }

    /**
     * Raw draw method, draw entire UI
     */
    void draw() {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableAlphaTest();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);

        /*canvas.moveToZero();
        canvas.setColor(0, 0, 0, 51);
        canvas.drawRect(0, 0, width, height);
        RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.DST_COLOR, GlStateManager.DestFactor.SRC_ALPHA, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ZERO);
        canvas.setColor(255, 255, 255, (int) (255 * (255.0f / (255 - 51) - 1)));
        canvas.drawRect(60, 60, width - 60, height - 60);
        RenderSystem.defaultBlendFunc();*/

        canvas.setDrawingTime(time);
        canvas.moveTo(view);
        view.draw(canvas);
        /*if (popup != null) {
            popup.draw(drawTime);
        }*/
        //UIEditor.INSTANCE.draw(canvas);
        GL11.glDisable(GL11.GL_LINE_SMOOTH);
        GL11.glLineWidth(1.0f);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    @SubscribeEvent
    void gRenderGameOverlay(@Nonnull RenderGameOverlayEvent.Pre event) {
        if (modernScreen != null && event.getType() == RenderGameOverlayEvent.ElementType.CROSSHAIRS) {
            event.setCanceled(true);
        }
        if (event.getType() == RenderGameOverlayEvent.ElementType.HOTBAR) {
            // hotfix 1.16 for BlurHandler shader, I don't why...
            RenderSystem.enableTexture();
        }
    }

    /**
     * Called when game window size changed, used to re-layout the UI
     *
     * @param width  scaled game window width
     * @param height scaled game window height
     */
    void resize(int width, int height) {
        this.width = width;
        this.height = height;
        double scale = minecraft.getMainWindow().getGuiScaleFactor();
        mouseX = (minecraft.mouseHelper.getMouseX() / scale);
        mouseY = (minecraft.mouseHelper.getMouseY() / scale);
        layoutUI();
    }

    /**
     * Layout entire UI views, a bit performance hungry
     * {@link #requestLayout()}
     */
    private void layoutUI() {
        long startTime = System.nanoTime();

        int widthSpec = MeasureSpec.makeMeasureSpec(width, MeasureSpec.Mode.EXACTLY);
        int heightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.Mode.EXACTLY);

        LayoutParams lp = (LayoutParams) view.getLayoutParams();

        int childWidthMeasureSpec = ViewGroup.getChildMeasureSpec(widthSpec,
                0, lp.width);
        int childHeightMeasureSpec = ViewGroup.getChildMeasureSpec(heightSpec,
                0, lp.height);

        view.measure(childWidthMeasureSpec, childHeightMeasureSpec);

        int measuredWidth = view.getMeasuredWidth();
        int measuredHeight = view.getMeasuredHeight();

        int childLeft;
        int childTop;

        switch (lp.gravity & Gravity.HORIZONTAL_GRAVITY_MASK) {
            case Gravity.HORIZONTAL_CENTER:
                childLeft = (width - measuredWidth) / 2;
                break;
            case Gravity.RIGHT:
                childLeft = width - measuredWidth;
                break;
            default:
                childLeft = 0;
        }

        switch (lp.gravity & Gravity.VERTICAL_GRAVITY_MASK) {
            case Gravity.VERTICAL_CENTER:
                childTop = (height - measuredHeight) / 2;
                break;
            case Gravity.BOTTOM:
                childTop = height - measuredHeight;
                break;
            default:
                childTop = 0;
        }

        view.layout(childLeft, childTop, childLeft + measuredWidth, childTop + measuredHeight);
        /*if (popup != null) {
            popup.resize(width, height);
        }*/
        layoutRequested = false;

        if (Config.isDeveloperMode()) {
            ModernUI.LOGGER.debug(MARKER, "Layout performed in {} \u03bcs", (System.nanoTime() - startTime) / 1000.0f);
        }
        mouseMoved();
    }

    void destroy() {
        // Hotfix 1.4.7
        if (guiToOpen == null) {
            animations.clear();
            tasks.clear();
            view = null;
            popup = null;
            fragment = null;
            modernScreen = null;
            upperViews.clear();
            lastDClickTick = Integer.MIN_VALUE;
            lastLayoutTime = 0;
            layoutRequested = false;
            UIEditor.INSTANCE.setHoveredWidget(null);
            UITools.useDefaultCursor();
            // Hotfix 1.5.8
            minecraft.keyboardListener.enableRepeatEvents(false);
        }
    }

    /**
     * Inner method, do not call
     */
    @SubscribeEvent
    void gClientTick(@Nonnull TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.START) {
            return;
        }
        ++ticks;
        /*if (popup != null) {
            popup.tick(ticks);
        }*/
        if (view != null) {
            view.tick(ticks);
        }
        // view ticking is always performed before tasks
        if (!tasks.isEmpty()) {
            Iterator<DelayedTask> iterator = tasks.iterator();
            DelayedTask task;
            while (iterator.hasNext()) {
                task = iterator.next();
                task.tick(ticks);
                if (task.shouldRemove()) {
                    iterator.remove();
                }
            }
        }
    }

    /**
     * Inner method
     */
    @SuppressWarnings("ForLoopReplaceableByForEach")
    @SubscribeEvent
    void gRenderTick(@Nonnull TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            // remove animations from loop in next frame
            if (!animations.isEmpty()) {
                animations.removeIf(Animation::shouldRemove);
            }

        } else {
            // convert ticks to milliseconds
            time = (int) ((ticks + event.renderTickTime) * 50.0f);

            // list size is dynamically changeable, because updating animation may add new animation to the list
            for (int i = 0; i < animations.size(); i++) {
                animations.get(i).update(time);
            }

            // layout after updating animations and before drawing
            if (layoutRequested) {
                // fixed at 40Hz
                if (time - lastLayoutTime >= 25) {
                    lastLayoutTime = time;
                    layoutUI();
                }
            }
        }
    }

    /**
     * Get elapsed time from a gui open, update every frame
     *
     * @return drawing time in milliseconds
     */
    public int getDrawingTime() {
        return time;
    }

    /**
     * Get elapsed ticks from a gui open, update every tick, 20 = 1 second
     *
     * @return elapsed ticks
     */
    public int getElapsedTicks() {
        return ticks;
    }

    /**
     * Get scaled UI screen width which is equal to game main window width
     *
     * @return window width
     */
    public int getGameWidth() {
        return width;
    }

    /**
     * Get scaled UI screen height which is equal to game main window height
     *
     * @return window height
     */
    public int getGameHeight() {
        return height;
    }

    /**
     * Get scaled mouse X position on screen
     *
     * @return mouse x
     */
    public double getMouseX() {
        return mouseX;
    }

    /**
     * Get scaled mouse Y position on screen
     *
     * @return mouse y
     */
    public double getMouseY() {
        return mouseY;
    }

    /**
     * Get logical mouse x for a view, generally used by system
     *
     * @param view view
     * @return relative mouse x
     */
    public double getViewMouseX(@Nonnull View view) {
        IViewParent parent = view.getParent();
        double mouseX = this.mouseX;

        while (parent != null) {
            mouseX += parent.getScrollX();
            parent = parent.getParent();
        }

        return mouseX;
    }

    /**
     * Get logical mouse y for a view, generally used by system
     *
     * @param view view
     * @return relative mouse y
     */
    public double getViewMouseY(@Nonnull View view) {
        IViewParent parent = view.getParent();
        double mouseY = this.mouseY;

        while (parent != null) {
            mouseY += parent.getScrollY();
            parent = parent.getParent();
        }

        return mouseY;
    }

    public float getGuiScale() {
        return (float) minecraft.getMainWindow().getGuiScaleFactor();
    }

    /**
     * Get main view of current UI
     *
     * @return main view
     */
    public View getMainView() {
        return view;
    }

    /**
     * Request layout all views with force layout mark in next frame
     * See {@link View#requestLayout()}
     * See {@link View#forceLayout()}
     */
    @Override
    public void requestLayout() {
        layoutRequested = true;
    }

    /**
     * Internal method, to tell the manager the most child view hovered
     *
     * @param view the most child view hovered
     */
    void setHovered(@Nullable View view) {
        mHovered = view;
    }

    @Nullable
    public View getHovered() {
        return mHovered;
    }

    /**
     * Set current active dragging view
     *
     * @param view dragging view
     */
    public void setDragging(@Nullable View view) {
        if (mDragging != view) {
            if (mDragging != null) {
                mDragging.onStopDragging();
            }
            mDragging = view;
            if (mDragging != null) {
                mDragging.onStartDragging();
            }
        }
    }

    @Nullable
    public View getDragging() {
        return mDragging;
    }

    /**
     * Set active keyboard listener to listen key events
     *
     * @param view keyboard view
     */
    public void setKeyboard(@Nullable View view) {
        if (mKeyboard != view) {
            minecraft.keyboardListener.enableRepeatEvents(view != null);
            if (mKeyboard != null) {
                mKeyboard.onStopKeyboard();
            }
            mKeyboard = view;
            if (mKeyboard != null) {
                mKeyboard.onStartKeyboard();
            }
        }
    }

    @Nullable
    public View getKeyboard() {
        return mKeyboard;
    }

    /**
     * Inner method, do not call
     */
    @Nullable
    @Override
    public IViewParent getParent() {
        return null;
    }

    /**
     * Inner method, do not call
     */
    @Deprecated
    @Override
    public float getScrollX() {
        return 0;
    }

    /**
     * Inner method, do not call
     */
    @Deprecated
    @Override
    public float getScrollY() {
        return 0;
    }

    /**
     * Window layout params
     */
    public static class LayoutParams extends ViewGroup.LayoutParams {

        public static final int UI_WINDOW = 500;

        public static final int UI_OVERLAY = 2500;

        /**
         * The general type of window.
         */
        public int type;

        /*
         * X position for this window.  With the default gravity it is ignored.
         * When using {@link Gravity#LEFT} or {@link Gravity#RIGHT} it provides
         * an offset from the given edge.
         */
        //public int x;

        /*
         * Y position for this window.  With the default gravity it is ignored.
         * When using {@link Gravity#TOP} or {@link Gravity#BOTTOM} it provides
         * an offset from the given edge.
         */
        //public int y;

        /**
         * Placement of window within the screen as per {@link Gravity}.
         *
         * @see Gravity
         */
        public int gravity = Gravity.TOP_LEFT;

        public LayoutParams() {
            super(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
        }

        public LayoutParams(int type) {
            this();
            this.type = type;
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(int width, int height, int type) {
            super(width, height);
            this.type = type;
        }

        public LayoutParams(int width, int height, int type, int gravity) {
            super(width, height);
            this.type = type;
            this.gravity = gravity;
        }
    }
}
