package icyllis.modernui.api.element;

import net.minecraft.util.ResourceLocation;

import java.util.function.Function;

public interface ITextureBuilder {

    /**
     * Set element relative position to window center. (0,0) will be at crosshair
     * @param x x position
     * @param y y position
     * @return builder
     */
    ITextureBuilder setPos(float x, float y);

    /**
     * Set element relative position to given window size.
     * @param x given game window width, return x position
     * @param y given game window height, return y position
     * @return builder
     */
    ITextureBuilder setPos(Function<Integer, Float> x, Function<Integer, Float> y);

    /**
     * Set initial constant alpha value, default is 1.0f.
     * You don't need this method if you create animation for alpha.
     * @param a alpha
     * @return builder
     */
    ITextureBuilder setAlpha(float a);

    /**
     * Set element size
     * @param w element width
     * @param h element height
     * @return builder
     */
    ITextureBuilder setSize(float w, float h);

    /**
     * Set element size
     * @param w given game window width, return element width
     * @param h given game window height, return element height
     * @return builder
     */
    ITextureBuilder setSize(Function<Integer, Float> w, Function<Integer, Float> h);

    ITextureBuilder setTexture(ResourceLocation texture);

    ITextureBuilder setUV(float u, float v);

    ITextureBuilder setTint(int rgb);

    ITextureBuilder setTint(float r, float g, float b);

    ITextureBuilder setScale(float scale);
}
