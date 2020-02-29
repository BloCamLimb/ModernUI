package icyllis.modernui.gui.master;

import icyllis.modernui.api.builder.IRectangleBuilder;
import icyllis.modernui.api.builder.ITextLineBuilder;
import icyllis.modernui.api.builder.ITextureBuilder;
import icyllis.modernui.api.template.*;
import icyllis.modernui.api.global.IElementBuilder;
import icyllis.modernui.gui.element.*;
import icyllis.modernui.gui.template.ButtonT1;
import icyllis.modernui.gui.template.Background;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.IntPredicate;

public class GlobalElementBuilder implements IElementBuilder {

    public static final GlobalElementBuilder INSTANCE = new GlobalElementBuilder();

    private GlobalModuleManager manager = GlobalModuleManager.INSTANCE;

    private PacketBuffer extraData;

    private IModernScreen master;

    public void setMaster(IModernScreen master) {
        this.master = master;
    }

    public void setExtraData(PacketBuffer extraData) {
        this.extraData = extraData;
    }

    @Override
    public PacketBuffer getExtraData() {
        return extraData;
    }

    @Override
    public void pool(IntPredicate availability, Consumer<Consumer<IBase>> poolModifier) {
        Pool p = new Pool(availability);
        poolModifier.accept(p);
        GlobalModuleManager.INSTANCE.add(p);
    }

    @Override
    public IBackground defaultBackground() {
        return new Background();
    }

    @Override
    public ITextLineBuilder textLine() {
        return new TextLine();
    }

    @Override
    public IButtonT1 buttonT1() {
        ButtonT1 b = new ButtonT1();
        master.addChild(b.listener);
        return b;
    }

    @Override
    public ITextureBuilder texture() {
        return TextureBuilder.INSTANCE;
    }

    @Override
    public IRectangleBuilder rectangle() {
        return RectangleBuilder.INSTANCE;
    }

    private static class BaseBuilder {

        protected Function<Integer, Float> fakeX,
                fakeY,
                fakeW,
                fakeH;

        protected float r,
                g,
                b,
                a;

    }

    public static class RectangleBuilder extends BaseBuilder implements IRectangleBuilder {

        public static final RectangleBuilder INSTANCE = new RectangleBuilder();

        @Override
        public IRectangleBuilder init(Function<Integer, Float> x, Function<Integer, Float> y, Function<Integer, Float> w, Function<Integer, Float> h, int RGBA) {
            fakeX = x;
            fakeY = y;
            fakeW = w;
            fakeH = h;
            a = (RGBA >> 24 & 255) / 255.0f;
            r = (RGBA >> 16 & 255) / 255.0f;
            g = (RGBA >> 8 & 255) / 255.0f;
            b = (RGBA & 255) / 255.0f;
            return this;
        }

        @Override
        public void buildToPool(Consumer<IBase> pool) {
            pool.accept(new Rectangle(fakeX, fakeY, fakeW, fakeH, r, g, b, a));
        }

        @Override
        public void buildToPool(Consumer<IBase> pool, Consumer<Rectangle> consumer) {
            Rectangle q = new Rectangle(fakeX, fakeY, fakeW, fakeH, r, g, b, a);
            pool.accept(q);
            consumer.accept(q);
        }
    }

    public static class TextureBuilder extends BaseBuilder implements ITextureBuilder {

        public static final TextureBuilder INSTANCE = new TextureBuilder();

        private ResourceLocation res;

        private float w, h, u, v, s;

        @Override
        public ITextureBuilder init(Function<Integer, Float> x, Function<Integer, Float> y, float w, float h, ResourceLocation texture, float u, float v, int tintRGBA, float scale) {
            fakeX = x;
            fakeY = y;
            this.w = w;
            this.h = h;
            res = texture;
            this.u = u;
            this.v = v;
            a = (tintRGBA >> 24 & 255) / 255.0f;
            r = (tintRGBA >> 16 & 255) / 255.0f;
            g = (tintRGBA >> 8 & 255) / 255.0f;
            b = (tintRGBA & 255) / 255.0f;
            s = scale;
            return this;
        }

        @Override
        public void buildToPool(Consumer<IBase> pool) {
            pool.accept(new Texture2D(fakeX, fakeY, w, h, res, u, v, r, g, b, a, s));
        }

        @Override
        public void buildToPool(Consumer<IBase> pool, Consumer<Texture2D> consumer) {
            Texture2D q = new Texture2D(fakeX, fakeY, w, h, res, u, v, r, g, b, a, s);
            pool.accept(q);
            consumer.accept(q);
        }

        @Override
        public Texture2D buildForMe() {
            return new Texture2D(fakeX, fakeY, w, h, res, u, v, r, g, b, a, s);
        }
    }

}
