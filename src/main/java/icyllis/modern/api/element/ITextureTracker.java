package icyllis.modern.api.element;

import net.minecraft.util.ResourceLocation;

import java.util.function.Supplier;

public interface ITextureTracker {

    ITextureTracker tex(ResourceLocation texture);

    ITextureTracker pos(float x, float y);

    ITextureTracker uv(float x, float y);

    ITextureTracker size(float x, float y);

    ITextureTracker color(Supplier<Integer> color);
}
