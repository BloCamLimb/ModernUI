package icyllis.modern.api.element;

import icyllis.modern.api.animation.IAlphaAnimation;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface ITextureBuilder {

    ITextureBuilder tex(ResourceLocation texture);

    ITextureBuilder pos(float x, float y);

    ITextureBuilder uv(float u, float v);

    ITextureBuilder size(float w, float h);

    ITextureBuilder color(Supplier<Integer> color);

    ITextureAnimator toAnimated();
}
