package icyllis.modern.ui.template;

/**
 * A button to switch module
 */
@Deprecated
public class NavigationButton { /*extends UIButton<INavigationBuilder, INavigationModifier> implements INavigationBuilder, INavigationModifier {

    private UITexture texture;

    public NavigationButton() {

    }

    @Override
    public void draw() {
        RenderSystem.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager.texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        if (mouseHovered) {
            textLine.draw();
        }
        texture.draw();
        RenderSystem.popMatrix();
    }

    @Override
    public INavigationBuilder setTarget(int id) {

        return this;
    }

    @Override
    public INavigationBuilder setTexture(Consumer<ITextureBuilder> consumer) {
        UITexture u = new UITexture();
        consumer.accept(u);
        texture = u;
        return this;
    }

    @Override
    public INavigationBuilder onMouseHoverOn(Consumer<INavigationModifier> consumer) {
        events.add(new InternalEvent<>(InternalEvent.MOUSE_HOVER_ON, consumer));
        return this;
    }

    @Override
    public INavigationBuilder onMouseHoverOff(Consumer<INavigationModifier> consumer) {
        events.add(new InternalEvent<>(InternalEvent.MOUSE_HOVER_OFF, consumer));
        return this;
    }

    @Override
    protected void onMouseHoverChanged() {
        super.onMouseHoverChanged();
        if (mouseHovered) {
            events.stream().filter(e -> e.getId() == InternalEvent.MOUSE_HOVER_ON).forEach(a -> a.run(this));
        } else {
            events.stream().filter(e -> e.getId() == InternalEvent.MOUSE_HOVER_OFF).forEach(a -> a.run(this));
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
        texture.resize(width, height);
    }

    @Override
    public ITextureBuilder getTexture() {
        return texture;
    }*/
}
