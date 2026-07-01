package com.botato.blockhighlight;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ColorPicker {
    private static final int SV_W = 120, SV_H = 80;
    private static final int STRIP_W = 12, STRIP_GAP = 6;
    private static final int PRESET_COLS = 8;
    private static final int PRESET_CELL = 16;
    private static final int PRESET_TOP_OFFSET = 4 + SV_H + 4 + 9 + 3;

    private final Supplier<String> getter;
    private final Consumer<String> setter;

    private float pickH, pickS, pickV, pickA = 1f;
    private boolean editingHex = false;
    private String editBuffer = "";
    private enum PickDrag { NONE, SV, HUE, ALPHA }
    private PickDrag pickDrag = PickDrag.NONE;
    private int pkSvX, pkSvY, pkHueX, pkAlphaX, pkHexX, pkHexY, pkPreviewX, pkPresetTop;
    private int rightBound;

    public ColorPicker(Supplier<String> getter, Consumer<String> setter) {
        this.getter = getter;
        this.setter = setter;
    }

    private static List<String> presets() {
        List<String> p = BlockHighlightConfig.get().presetColors;
        return p == null ? Collections.emptyList() : p;
    }

    private static int presetRows() {
        int items = presets().size() + 1;
        return Math.max(1, (items + PRESET_COLS - 1) / PRESET_COLS);
    }

    public int height() {
        return PRESET_TOP_OFFSET + presetRows() * PRESET_CELL + 4;
    }

    public void open() {
        editingHex = false;
        pickDrag = PickDrag.NONE;
        Integer argb = HexColor.parse(getter.get());
        setHsvaFromArgb(argb != null ? argb : 0xFFFFFFFF);
        editBuffer = currentHex();
    }

    public void mouseReleased() { pickDrag = PickDrag.NONE; }

    public boolean isDragging() { return pickDrag != PickDrag.NONE; }

    private void computeGeom(int contentLeft, int rightBound, int top) {
        pkSvX = contentLeft;
        pkSvY = top + 4;
        pkHueX = pkSvX + SV_W + STRIP_GAP;
        pkAlphaX = pkHueX + STRIP_W + STRIP_GAP;
        pkPreviewX = pkAlphaX + STRIP_W + 8;
        pkHexX = pkSvX;
        pkHexY = pkSvY + SV_H + 4;
        pkPresetTop = top + PRESET_TOP_OFFSET;
        this.rightBound = rightBound;
    }

    public void render(GuiGraphics ctx, Font tr, int contentLeft, int rightBound, int top, int mouseX, int mouseY) {
        computeGeom(contentLeft, rightBound, top);

        for (int dx = 0; dx < SV_W; dx++) {
            float s = dx / (float) SV_W;
            ctx.fillGradient(pkSvX + dx, pkSvY, pkSvX + dx + 1, pkSvY + SV_H,
                    ColorUtil.hsvToRgb(pickH, s, 1f), 0xFF000000);
        }
        int svTx = pkSvX + Math.round(pickS * SV_W);
        int svTy = pkSvY + Math.round((1 - pickV) * SV_H);
        drawRing(ctx, svTx, svTy);

        for (int dy = 0; dy < SV_H; dy++) {
            ctx.fill(pkHueX, pkSvY + dy, pkHueX + STRIP_W, pkSvY + dy + 1, ColorUtil.hsvToRgb(dy / (float) SV_H, 1f, 1f));
        }
        int hueY = pkSvY + Math.round(pickH * SV_H);
        ctx.fill(pkHueX - 1, hueY - 1, pkHueX + STRIP_W + 1, hueY + 1, 0xFFFFFFFF);

        drawChecker(ctx, pkAlphaX, pkSvY, STRIP_W, SV_H);
        int opaque = ColorUtil.hsvToRgb(pickH, pickS, pickV);
        ctx.fillGradient(pkAlphaX, pkSvY, pkAlphaX + STRIP_W, pkSvY + SV_H, opaque, opaque & 0x00FFFFFF);
        int aY = pkSvY + Math.round((1 - pickA) * SV_H);
        ctx.fill(pkAlphaX - 1, aY - 1, pkAlphaX + STRIP_W + 1, aY + 1, 0xFFFFFFFF);

        if (rightBound - pkPreviewX >= 16) {
            drawChecker(ctx, pkPreviewX, pkSvY, rightBound - pkPreviewX, SV_H);
            int argb = ColorUtil.hsvaToArgb(pickH, pickS, pickV, pickA);
            ctx.fill(pkPreviewX - 1, pkSvY - 1, rightBound + 1, pkSvY + SV_H + 1, 0xFF666666);
            ctx.fill(pkPreviewX, pkSvY, rightBound, pkSvY + SV_H, argb);
        }

        String hex = editingHex ? editBuffer : currentHex();
        Integer parsed = HexColor.parse(hex.startsWith("#") ? hex : "#" + hex);
        int hexCol = editingHex ? (parsed != null ? 0xFFFFFFFF : 0xFFFF5555) : 0xFFC0C0C0;
        ctx.drawString(tr, hex, pkHexX, pkHexY, hexCol, false);
        if (editingHex && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cx = pkHexX + tr.width(hex) + 1;
            ctx.fill(cx, pkHexY - 1, cx + 1, pkHexY + 8, 0xFFFFFFFF);
        }

        List<String> ps = presets();
        int cellW = (rightBound - contentLeft) / PRESET_COLS;
        int total = ps.size() + 1;
        for (int idx = 0; idx < total; idx++) {
            int col = idx % PRESET_COLS, row = idx / PRESET_COLS;
            int cx = contentLeft + col * cellW, cy = pkPresetTop + row * PRESET_CELL;
            int w = cellW - 3, h = PRESET_CELL - 3;
            if (idx == 0) {
                ctx.fill(cx, cy, cx + w, cy + h, 0xFF444444);
                ctx.fill(cx, cy, cx + w, cy + 1, 0xFF777777);
                String plus = "+";
                ctx.drawString(tr, plus, cx + (w - tr.width(plus)) / 2, cy + (h - 8) / 2, 0xFFE0E0E0, false);
            } else {
                Integer pc = HexColor.parse(ps.get(idx - 1));
                ctx.fill(cx, cy, cx + w, cy + h, 0xFF000000);
                ctx.fill(cx + 1, cy + 1, cx + w - 1, cy + h - 1, pc == null ? 0xFFFF00FF : pc);
                if (inRect(mouseX, mouseY, cx, cy, w, h)) {
                    int bx0 = cx + w - 8;
                    ctx.fill(bx0, cy, cx + w, cy + 8, 0xCC000000);
                    ctx.drawString(tr, "✕", bx0 + 2, cy, 0xFFFF6666, false);
                }
            }
        }
    }

    public boolean mouseClicked(double mx, double my, int contentLeft, int rightBound, int top) {
        computeGeom(contentLeft, rightBound, top);
        editingHex = false;
        if (inRect(mx, my, pkSvX, pkSvY, SV_W, SV_H)) { pickDrag = PickDrag.SV; updateSV(mx, my); return true; }
        if (inRect(mx, my, pkHueX, pkSvY, STRIP_W, SV_H)) { pickDrag = PickDrag.HUE; updateHue(my); return true; }
        if (inRect(mx, my, pkAlphaX, pkSvY, STRIP_W, SV_H)) { pickDrag = PickDrag.ALPHA; updateAlpha(my); return true; }

        Font tr = net.minecraft.client.Minecraft.getInstance().font;
        if (my >= pkHexY - 1 && my < pkHexY + 9 && mx >= pkHexX && mx <= pkHexX + tr.width(currentHex()) + 4) {
            editingHex = true;
            editBuffer = currentHex();
            return true;
        }

        List<String> ps = presets();
        int cellW = (rightBound - contentLeft) / PRESET_COLS;
        int total = ps.size() + 1;
        for (int idx = 0; idx < total; idx++) {
            int col = idx % PRESET_COLS, row = idx / PRESET_COLS;
            int cx = contentLeft + col * cellW, cy = pkPresetTop + row * PRESET_CELL;
            int w = cellW - 3;
            if (inRect(mx, my, cx, cy, w, PRESET_CELL - 3)) {
                if (idx == 0) {
                    BlockHighlightConfig.addPreset(currentHex());
                } else if (mx >= cx + w - 8 && my < cy + 8) {
                    BlockHighlightConfig.removePreset(ps.get(idx - 1));
                } else {
                    Integer pc = HexColor.parse(ps.get(idx - 1));
                    if (pc != null) { setHsvaFromArgb(pc); applyPickColor(); }
                }
                return true;
            }
        }
        return false;
    }

    public void mouseDragged(double mx, double my, int contentLeft, int top) {
        switch (pickDrag) {
            case SV -> updateSV(mx, my);
            case HUE -> updateHue(my);
            case ALPHA -> updateAlpha(my);
            default -> {}
        }
    }

    public boolean charTyped(char c) {
        if (!editingHex) return false;
        if (c == '#') {
            if (editBuffer.isEmpty()) editBuffer = "#";
        } else if (isHexDigit(c) && editBuffer.replace("#", "").length() < 8) {
            editBuffer += Character.toUpperCase(c);
        }
        applyHexLive();
        return true;
    }

    public boolean keyPressed(int keyCode) {
        if (!editingHex) return false;
        switch (keyCode) {
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (!editBuffer.isEmpty()) editBuffer = editBuffer.substring(0, editBuffer.length() - 1);
                applyHexLive();
                return true;
            }
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER, GLFW.GLFW_KEY_ESCAPE -> {
                editingHex = false;
                return true;
            }
            default -> { return false; }
        }
    }

    private void applyHexLive() {
        String norm = (editBuffer.startsWith("#") ? editBuffer : "#" + editBuffer).toUpperCase();
        Integer argb = HexColor.parse(norm);
        if (argb != null) {
            setHsvaFromArgb(argb);
            setter.accept(norm);
        }
    }

    private String currentHex() {
        return HexColor.format(ColorUtil.hsvaToArgb(pickH, pickS, pickV, pickA));
    }

    private void setHsvaFromArgb(int argb) {
        float[] hsva = ColorUtil.argbToHsva(argb);
        pickH = hsva[0]; pickS = hsva[1]; pickV = hsva[2]; pickA = hsva[3];
    }

    private void applyPickColor() {
        String hex = currentHex();
        editBuffer = hex;
        setter.accept(hex);
    }

    private void updateSV(double mx, double my) {
        pickS = clamp01((float) ((mx - pkSvX) / SV_W));
        pickV = clamp01(1f - (float) ((my - pkSvY) / SV_H));
        applyPickColor();
    }
    private void updateHue(double my) { pickH = clamp01((float) ((my - pkSvY) / SV_H)); applyPickColor(); }
    private void updateAlpha(double my) { pickA = clamp01(1f - (float) ((my - pkSvY) / SV_H)); applyPickColor(); }

    private void drawRing(GuiGraphics ctx, int cx, int cy) {
        ctx.fill(cx - 3, cy - 3, cx + 4, cy - 2, 0xFFFFFFFF);
        ctx.fill(cx - 3, cy + 3, cx + 4, cy + 4, 0xFFFFFFFF);
        ctx.fill(cx - 3, cy - 2, cx - 2, cy + 3, 0xFFFFFFFF);
        ctx.fill(cx + 3, cy - 2, cx + 4, cy + 3, 0xFFFFFFFF);
    }

    private void drawChecker(GuiGraphics ctx, int x, int y, int w, int h) {
        int cs = 4;
        for (int dy = 0; dy < h; dy += cs)
            for (int dx = 0; dx < w; dx += cs) {
                boolean dark = ((dx / cs) + (dy / cs)) % 2 == 0;
                ctx.fill(x + dx, y + dy, Math.min(x + dx + cs, x + w), Math.min(y + dy + cs, y + h),
                        dark ? 0xFF999999 : 0xFF666666);
            }
    }

    private static boolean inRect(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
    private static float clamp01(float x) { return x < 0f ? 0f : (x > 1f ? 1f : x); }
    private static boolean isHexDigit(char c) {
        return (c >= '0' && c <= '9') || (c >= 'a' && c <= 'f') || (c >= 'A' && c <= 'F');
    }
}
