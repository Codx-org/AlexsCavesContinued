package com.github.alexmodguy.alexscaves.server.misc;

/**
 * Packing of four channels into one {@code int}, done here rather than through vanilla's helper.
 *
 * <p>Vanilla spells this on the ARGB32 nested class of its fast-colour utility, and that name is a
 * problem for this tree: 1.21 renamed the whole {@code VertexConsumer} builder DSL, so a
 * {@code !mc21-vc-color} replacement rule rewrites every colour call in the source to
 * {@code setColor} — and a textual rule cannot tell a vertex consumer from a colour packer. Rather
 * than teach the rule an exception it cannot express, the nine packing call sites moved here.
 *
 * <p>The bodies are the shift arithmetic vanilla's own methods do, so nothing about the values
 * changes; keeping them local also survives 1.21.5 renaming {@code FastColor} to {@code ARGB}.
 */
public class ACColors {

    /** Alpha, red, green, blue — the layout every {@code GuiGraphics} and font call wants. */
    public static int argb(int alpha, int red, int green, int blue) {
        return (alpha & 0xFF) << 24 | (red & 0xFF) << 16 | (green & 0xFF) << 8 | (blue & 0xFF);
    }

    /**
     * Alpha, red, green, blue as 0..1 channels — the tint the renderers in this mod compute.
     *
     * <p>Same clamp-then-scale vanilla's {@code colorFromFloat} does, so the packed value matches
     * what the game would have produced from the same four floats.
     */
    public static int argbF(float alpha, float red, float green, float blue) {
        return argb(channel(alpha), channel(red), channel(green), channel(blue));
    }

    private static int channel(float value) {
        return (int) (net.minecraft.util.Mth.clamp(value, 0.0F, 1.0F) * 255.0F);
    }

    /** Alpha, blue, green, red — the byte order a {@code NativeImage} pixel is stored in. */
    public static int abgr(int alpha, int blue, int green, int red) {
        return (alpha & 0xFF) << 24 | (blue & 0xFF) << 16 | (green & 0xFF) << 8 | (red & 0xFF);
    }

    // ── unpacking, for the other direction ──────────────────────────────────────────────────
    //
    // 1.21 folded the four float tint arguments of Model#renderToBuffer into a single packed ARGB
    // int. This mod's models are written against the four-float form all the way down to
    // BasicModelPart, so the 1.21 bridge in BasicEntityModel splits the packed value back apart
    // with these rather than rewriting ~90 model classes.

    /** 0..1 alpha of a packed ARGB colour. */
    public static float alphaF(int argb) {
        return (argb >>> 24 & 0xFF) / 255.0F;
    }

    /** 0..1 red of a packed ARGB colour. */
    public static float redF(int argb) {
        return (argb >> 16 & 0xFF) / 255.0F;
    }

    /** 0..1 green of a packed ARGB colour. */
    public static float greenF(int argb) {
        return (argb >> 8 & 0xFF) / 255.0F;
    }

    /** 0..1 blue of a packed ARGB colour. */
    public static float blueF(int argb) {
        return (argb & 0xFF) / 255.0F;
    }

    // ── 0..255 unpacking ────────────────────────────────────────────────────────────────────
    //
    // What vanilla's FastColor.ARGB32 / FastColor.ABGR32 answered up to 1.21.1. 1.21.2 deleted
    // FastColor and moved the ARGB half onto net.minecraft.util.ARGB — with no ABGR accessors at
    // all, only whole-value toABGR/fromABGR conversions — so both halves live here instead.

    /** 0..255 alpha of a packed ARGB colour. */
    public static int alpha(int argb) {
        return argb >>> 24 & 0xFF;
    }

    /** 0..255 red of a packed ARGB colour. */
    public static int red(int argb) {
        return argb >> 16 & 0xFF;
    }

    /** 0..255 green of a packed ARGB colour. */
    public static int green(int argb) {
        return argb >> 8 & 0xFF;
    }

    /** 0..255 blue of a packed ARGB colour. */
    public static int blue(int argb) {
        return argb & 0xFF;
    }

    /** 0..255 red of a packed ABGR colour — the byte order a {@code NativeImage} pixel uses. */
    public static int abgrRed(int abgr) {
        return abgr & 0xFF;
    }

    /** 0..255 green of a packed ABGR colour. */
    public static int abgrGreen(int abgr) {
        return abgr >> 8 & 0xFF;
    }

    /** 0..255 blue of a packed ABGR colour. */
    public static int abgrBlue(int abgr) {
        return abgr >> 16 & 0xFF;
    }

    /** Per-channel interpolation between two packed ARGB colours, as vanilla's {@code lerp} does. */
    public static int lerp(float delta, int from, int to) {
        return argb(lerpChannel(delta, alpha(from), alpha(to)),
                lerpChannel(delta, red(from), red(to)),
                lerpChannel(delta, green(from), green(to)),
                lerpChannel(delta, blue(from), blue(to)));
    }

    private static int lerpChannel(float delta, int from, int to) {
        return net.minecraft.util.Mth.floor(net.minecraft.util.Mth.lerp(delta, from, to));
    }
}
