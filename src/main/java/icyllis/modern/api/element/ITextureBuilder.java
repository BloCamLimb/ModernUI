package icyllis.modern.api.element;

import net.minecraft.util.ResourceLocation;

import java.util.function.Supplier;

public interface ITextureBuilder {

    ITextureBuilder tex(ResourceLocation texture);

    ITextureBuilder pos(float x, float y);

    ITextureBuilder uv(float u, float v);

    ITextureBuilder size(float w, float h);

    ITextureBuilder color(Supplier<Integer> color);
}
