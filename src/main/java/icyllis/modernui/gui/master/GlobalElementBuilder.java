package icyllis.modernui.gui.master;

import icyllis.modernui.api.builder.IRectangleBuilder;
import icyllis.modernui.api.builder.ITextLineBuilder;
import icyllis.modernui.api.builder.ITextureBuilder;
import icyllis.modernui.api.template.*;
import icyllis.modernui.api.global.IElementBuilder;
import icyllis.modernui.gui.element.Rectangle;
import icyllis.modernui.gui.element.Texture2D;
import icyllis.modernui.gui.template.ButtonT1;
import icyllis.modernui.gui.template.Background;
import icyllis.modernui.gui.element.TextLine;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.ResourceLocation;

import java.util.function.Consumer;
import java.util.function.Function;

public class GlobalElementBuilder implements IElementBuilder {

    public static final GlobalElementBuilder INSTANCE = new GlobalElementBuilder();

    private PacketBuffer extraData;

    private MasterModule receiver;
    private IMasterScreen master;

    public void setReceiver(MasterModule receiver, IMasterScreen master) {
        this.receiver = receiver;
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
    public IBackground defaultBackground() {
        Background u = new Background();
        receiver.addElement(u);
        return u;
    }

    @Override
    public ITextLineBuilder textLine() {
        TextLine u = new TextLine();
        receiver.addElement(u);
        return u;
    }

    @Override
    public IButtonT1 buttonT1() {
        ButtonT1 b = new ButtonT1();
        receiver.addElement(b);
        master.addChild(b.listener);
        return b;
    }

    @Override
    public ITextureBuilder texture() {
        return TextureBuilder.INSTANCE.reset();
    }

    @Override
    public IRectangleBuilder rectangle() {
        return RectangleBuilder.INSTANCE.reset();
    }

    private static class BaseBuilder {

        protected Function<Integer, Float> fakeX,
                fakeY,
                fakeW,
                fakeH;

        protected float r,
                g,
                b,
                a,
                s;

    }

    public static class RectangleBuilder extends BaseBuilder implements IRectangleBuilder {

        public static final RectangleBuilder INSTANCE = new RectangleBuilder();

        private RectangleBuilder reset() {
            fakeX = fakeY = fakeW = fakeH = Float::valueOf;
            s = r = g = b = a = 1.0f;
            return this;
        }

        @Override
        public IRectangleBuilder setPos(float x, float y) {
            fakeX = w -> w / 2f + x;
            fakeY = h -> h / 2f + y;
            return this;
        }

        @Override
        public IRectangleBuilder setPos(Function<Integer, Float> x, Function<Integer, Float> y) {
            fakeX = x;
            fakeY = y;
            return this;
        }

        @Override
        public IRectangleBuilder setAlpha(float a) {
            this.a = a;
            return this;
        }

        @Override
        public IRectangleBuilder setColor(int rgb) {
            r = (rgb >> 16 & 255) / 255.0f;
            g = (rgb >> 8 & 255) / 255.0f;
            b = (rgb & 255) / 255.0f;
            return this;
        }

        @Override
        public IRectangleBuilder setColor(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
            return this;
        }

        @Override
        public IRectangleBuilder setSize(float w, float h) {
            fakeW = c -> w;
            fakeH = c -> h;
            return this;
        }

        @Override
        public IRectangleBuilder setSize(Function<Integer, Float> w, Function<Integer, Float> h) {
            fakeW = w;
            fakeH = h;
            return this;
        }

        @Override
        public void buildToPool() {
            GlobalElementBuilder.INSTANCE.receiver.addElement(new Rectangle(fakeX, fakeY, fakeW, fakeH, r, g, b, a));
        }

        @Override
        public void buildToPool(Consumer<Rectangle> consumer) {
            Rectangle q = new Rectangle(fakeX, fakeY, fakeW, fakeH, r, g, b, a);
            GlobalElementBuilder.INSTANCE.receiver.addElement(q);
            consumer.accept(q);
        }
    }

    public static class TextureBuilder extends BaseBuilder implements ITextureBuilder {

        public static final TextureBuilder INSTANCE = new TextureBuilder();

        private ResourceLocation res;

        private float u, v;

        private TextureBuilder reset() {
            fakeX = fakeY = fakeW = fakeH = Float::valueOf;
            u = v = 0;
            s = r = g = b = a = 1.0f;
            res = null;
            return this;
        }

        @Override
        public ITextureBuilder setPos(float x, float y) {
            fakeX = w -> w / 2f + x;
            fakeY = h -> h / 2f + y;
            return this;
        }

        @Override
        public ITextureBuilder setPos(Function<Integer, Float> x, Function<Integer, Float> y) {
            fakeX = x;
            fakeY = y;
            return this;
        }

        @Override
        public ITextureBuilder setAlpha(float a) {
            this.a = a;
            return this;
        }

        @Override
        public ITextureBuilder setSize(float w, float h) {
            fakeW = c -> w;
            fakeH = c -> h;
            return this;
        }

        @Override
        public ITextureBuilder setSize(Function<Integer, Float> w, Function<Integer, Float> h) {
            fakeW = w;
            fakeH = h;
            return this;
        }

        @Override
        public ITextureBuilder setTexture(ResourceLocation texture) {
            res = texture;
            return this;
        }

        @Override
        public ITextureBuilder setUV(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public ITextureBuilder setTint(int rgb) {
            r = (rgb >> 16 & 255) / 255.0f;
            g = (rgb >> 8 & 255) / 255.0f;
            b = (rgb & 255) / 255.0f;
            return this;
        }

        @Override
        public ITextureBuilder setTint(float r, float g, float b) {
            this.r = r;
            this.g = g;
            this.b = b;
            return this;
        }

        @Override
        public ITextureBuilder setScale(float scale) {
            s = scale;
            return this;
        }

        @Override
        public void buildToPool() {
            GlobalElementBuilder.INSTANCE.receiver.addElement(new Texture2D(fakeX, fakeY, fakeW, fakeH, res, u, v, r, g, b, a, s));
        }

        @Override
        public void buildToPool(Consumer<Texture2D> consumer) {
            Texture2D q = new Texture2D(fakeX, fakeY, fakeW, fakeH, res, u, v, r, g, b, a, s);
            GlobalElementBuilder.INSTANCE.receiver.addElement(q);
            consumer.accept(q);
        }

        @Override
        public Texture2D buildForMe() {
            return new Texture2D(fakeX, fakeY, fakeW, fakeH, res, u, v, r, g, b, a, s);
        }
    }

}
