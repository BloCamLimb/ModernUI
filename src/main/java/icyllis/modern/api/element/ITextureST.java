package icyllis.modern.api.element;

import net.minecraft.util.ResourceLocation;

import java.util.function.Supplier;

public interface ITextureST {

    ITextureST tex(ResourceLocation texture);

    ITextureST pos(float x, float y);

    ITextureST uv(float u, float v);

    ITextureST size(float w, float h);

    ITextureST color(Supplier<Integer> color);
}
