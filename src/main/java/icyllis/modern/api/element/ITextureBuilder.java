package icyllis.modern.api.element;

import icyllis.modern.api.animation.IAlphaAnimation;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ITextureBuilder extends IBaseBuilder<ITextureBuilder> {

    ITextureBuilder tex(ResourceLocation texture);

    ITextureBuilder uv(float u, float v);

    ITextureBuilder setTint(int rgb);

    /**
     * Faster than the upper one
     */
    ITextureBuilder setTint(float r, float g, float b);
}
