package com.botato.blockhighlight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

public class SettingsPanel {
    private static final int PAD = 8;
    private static final int HEADER_H = 16;
    private static final int ROW_H = 14;
    private static final int SLIDER_H = 14;
    private static final int SLIDER_VALUE_W = 26;
    private static final int RESET_BAND = 26;

    private int x0, y0, x1, y1, viewBottom;
    private int scroll = 0;
    private boolean draggingScrollbar = false;
    private int scrollbarGrab;

    private String draggingSlider = null;
    private int dsTrackX, dsTrackW;

    private String editingColorId = null;
    private ColorPicker picker;

    public void layout(int px0, int py0, int px1, int py1) {
        x0 = px0;
        y0 = py0;
        x1 = px1;
        y1 = py1;
        viewBottom = y1 - RESET_BAND;
    }

    private int resetTop() { return viewBottom + 4; }
    private int resetBottom() { return viewBottom + 22; }

    public boolean isPointInside(double mx, double my) {
        return mx >= x0 && mx <= x1 && my >= y0 && my <= y1;
    }

    private enum Kind { HEADER, SLIDER, COLOR, TOGGLE }
    private record Entry(String id, Kind kind, String label) {}

    private List<Entry> entries() {
        List<Entry> e = new ArrayList<>();
        e.add(new Entry("backgroundBlur", Kind.SLIDER, "Background Blur"));
        e.add(new Entry(null, Kind.HEADER, "Editor"));
        e.add(new Entry("highlightColor", Kind.COLOR, "Block Color"));
        e.add(new Entry("editorFilled", Kind.TOGGLE, "Fill"));
        e.add(new Entry("editorOutline", Kind.TOGGLE, "Border"));
        e.add(new Entry("editorSeeThrough", Kind.TOGGLE, "X-Ray"));
        return e;
    }

    private static int rowHeight(Kind k) {
        return switch (k) {
            case HEADER -> HEADER_H;
            case SLIDER -> SLIDER_H;
            default -> ROW_H;
        };
    }

    private boolean isEditingColor(Entry e) {
        return e.kind() == Kind.COLOR && e.id().equals(editingColorId);
    }

    private int contentHeight() {
        int h = PAD;
        for (Entry e : entries()) {
            h += rowHeight(e.kind());
            if (isEditingColor(e) && picker != null) h += picker.height();
        }
        return h + PAD;
    }

    private int scrollMax() { return Math.max(0, contentHeight() - (viewBottom - y0)); }

    private int scrollbarThumbH() {
        int view = viewBottom - y0, ch = contentHeight();
        if (ch <= view) return 0;
        return Math.max(10, view * view / ch);
    }

    private int scrollbarThumbY() {
        int view = viewBottom - y0, barH = scrollbarThumbH(), max = scrollMax();
        return y0 + (max <= 0 ? 0 : Math.round((view - barH) * (scroll / (float) max)));
    }

    private void scrollToThumbTop(int thumbTop) {
        int range = (viewBottom - y0) - scrollbarThumbH(), max = scrollMax();
        if (range <= 0 || max <= 0) { scroll = 0; return; }
        float frac = Math.max(0f, Math.min(1f, (thumbTop - y0) / (float) range));
        scroll = Math.round(frac * max);
    }

    public void render(GuiGraphics ctx, int mouseX, int mouseY) {
        Font tr = Minecraft.getInstance().font;
        ctx.fill(x0 - 1, y0 - 1, x1 + 1, y1 + 1, 0xAA2A2A2A);
        ctx.fill(x0, y0, x1, y1, 0x26101010);

        ctx.enableScissor(x0, y0, x1, viewBottom);
        int y = y0 + PAD - scroll;
        for (Entry e : entries()) {
            int h = rowHeight(e.kind());
            if (y + h >= y0 && y <= viewBottom) renderRow(ctx, tr, e, y, mouseX, mouseY);
            y += h;
            if (isEditingColor(e) && picker != null) {
                if (y + picker.height() >= y0 && y <= viewBottom) picker.render(ctx, tr, x0 + PAD, x1 - PAD, y, mouseX, mouseY);
                y += picker.height();
            }
        }
        ctx.disableScissor();

        int barH = scrollbarThumbH();
        if (barH > 0) {
            int barY = scrollbarThumbY();
            ctx.fill(x1 - 5, y0, x1 - 1, viewBottom, 0x18FFFFFF);
            ctx.fill(x1 - 5, barY, x1 - 1, barY + barH, draggingScrollbar ? 0xFFAAAAAA : 0xFF666666);
        }

        int rbTop = resetTop(), rbBot = resetBottom(), rbX0 = x0 + PAD, rbX1 = x1 - PAD;
        boolean rbHov = mouseX >= rbX0 && mouseX <= rbX1 && mouseY >= rbTop && mouseY <= rbBot;
        ctx.fill(rbX0 - 1, rbTop - 1, rbX1 + 1, rbBot + 1, 0xFF2A2A2A);
        ctx.fill(rbX0, rbTop, rbX1, rbBot, rbHov ? 0xFF5A2424 : 0xFF3A2020);
        String label = "Reset to Defaults";
        ctx.drawString(tr, label, (rbX0 + rbX1) / 2 - tr.width(label) / 2, rbTop + 5,
                rbHov ? 0xFFFFC0C0 : 0xFFE0A0A0, false);
    }

    private void renderRow(GuiGraphics ctx, Font tr, Entry e, int y, int mouseX, int mouseY) {
        int labelX = x0 + PAD;
        boolean rowHovered = mouseX >= x0 && mouseX <= x1 && mouseY >= y && mouseY < y + rowHeight(e.kind()) && mouseY < viewBottom;

        switch (e.kind()) {
            case HEADER -> {
                ctx.drawString(tr, e.label(), labelX, y + 4, 0xFFAAAAAA, false);
                ctx.fill(labelX + tr.width(e.label()) + 4, y + 8, x1 - PAD, y + 9, 0xFF2A2A2A);
            }
            case SLIDER -> {
                if (rowHovered) ctx.fill(x0 + 2, y, x1 - 2, y + ROW_H, 0x20FFFFFF);
                ctx.drawString(tr, e.label(), labelX, y + 3, 0xFFE0E0E0, false);
                String val = formatSliderValue(e.id());
                ctx.drawString(tr, val, x1 - PAD - tr.width(val), y + 3, 0xFFAAAAAA, false);

                int[] tb = sliderTrack(e.label());
                int trackX = tb[0], trackW = tb[1], trackY = y + 6;
                float[] r = sliderRange(e.id());
                float frac = (getSlider(e.id()) - r[0]) / (r[1] - r[0]);
                frac = Math.max(0f, Math.min(1f, frac));
                ctx.fill(trackX, trackY, trackX + trackW, trackY + 3, 0xFF000000);
                ctx.fill(trackX, trackY, trackX + (int) (trackW * frac), trackY + 3, 0xFF707070);
                int thumbX = trackX + (int) ((trackW - 4) * frac);
                ctx.fill(thumbX, trackY - 2, thumbX + 4, trackY + 5, 0xFFBEBEBE);
                ctx.fill(thumbX, trackY - 2, thumbX + 4, trackY - 1, 0xFFFFFFFF);
            }
            case COLOR -> {
                boolean editing = e.id().equals(editingColorId);
                if (rowHovered || editing) ctx.fill(x0 + 2, y, x1 - 2, y + ROW_H, editing ? 0x33FFFFFF : 0x20FFFFFF);
                ctx.drawString(tr, e.label(), labelX, y + 3, 0xFFE0E0E0, false);
                String hex = getColor(e.id());
                Integer argb = HexColor.parse(hex);
                int swX = x1 - PAD - 10;
                ctx.drawString(tr, hex, swX - 4 - tr.width(hex), y + 3, 0xFF888888, false);
                ctx.fill(swX - 1, y + 2, swX + 9, y + 12, 0xFF666666);
                ctx.fill(swX, y + 3, swX + 8, y + 11, argb == null ? 0xFFFF00FF : argb);
            }
            case TOGGLE -> {
                if (rowHovered) ctx.fill(x0 + 2, y, x1 - 2, y + ROW_H, 0x20FFFFFF);
                ctx.drawString(tr, e.label(), labelX, y + 3, 0xFFE0E0E0, false);
                boolean on = getToggle(e.id());
                int pillX = x1 - PAD - 20, pillY = y + 3;
                ctx.fill(pillX, pillY, pillX + 20, pillY + 8, on ? 0xFF2E7D32 : 0xFF4A4A4A);
                int dotX = on ? pillX + 13 : pillX + 1;
                ctx.fill(dotX, pillY + 1, dotX + 6, pillY + 7, 0xFFFFFFFF);
            }
        }
    }

    private int[] sliderTrack(String label) {
        Font tr = Minecraft.getInstance().font;
        int trackX = x0 + PAD + tr.width(label) + 6;
        int trackRight = x1 - PAD - SLIDER_VALUE_W;
        return new int[]{ trackX, Math.max(10, trackRight - trackX) };
    }

    public boolean mouseClicked(double mx, double my) {
        if (!isPointInside(mx, my)) return false;
        if (mx >= x0 + PAD && mx <= x1 - PAD && my >= resetTop() && my <= resetBottom()) {
            BlockHighlightConfig.resetToDefaults();
            editingColorId = null;
            picker = null;
            return true;
        }
        int barH = scrollbarThumbH();
        if (barH > 0 && mx >= x1 - 7 && my >= y0 && my <= viewBottom) {
            int thumbY = scrollbarThumbY();
            if (my >= thumbY && my < thumbY + barH) scrollbarGrab = (int) (my - thumbY);
            else { scrollbarGrab = barH / 2; scrollToThumbTop((int) (my - barH / 2)); }
            draggingScrollbar = true;
            return true;
        }
        if (my >= viewBottom) return true;

        int y = y0 + PAD - scroll;
        for (Entry e : entries()) {
            int h = rowHeight(e.kind());
            if (my >= y && my < y + h) { rowClicked(e, y, mx, my); return true; }
            y += h;
            if (isEditingColor(e) && picker != null) {
                if (my >= y && my < y + picker.height()) {
                    picker.mouseClicked(mx, my, x0 + PAD, x1 - PAD, y);
                    return true;
                }
                y += picker.height();
            }
        }
        return true;
    }

    private void rowClicked(Entry e, int rowY, double mx, double my) {
        switch (e.kind()) {
            case SLIDER -> {
                int[] tb = sliderTrack(e.label());
                int trackX = tb[0], trackW = tb[1];
                if (mx >= trackX - 2 && mx <= trackX + trackW + 2 && my >= rowY && my < rowY + SLIDER_H) {
                    draggingSlider = e.id();
                    dsTrackX = trackX;
                    dsTrackW = trackW;
                    dragSliderTo(e.id(), mx);
                }
            }
            case COLOR -> {
                if (e.id().equals(editingColorId)) closeColorEdit();
                else openColorEdit(e.id());
            }
            case TOGGLE -> setToggle(e.id(), !getToggle(e.id()));
            default -> {}
        }
    }

    private void openColorEdit(String id) {
        editingColorId = id;
        picker = new ColorPicker(() -> getColor(id), hex -> setColor(id, hex));
        picker.open();
    }

    private void closeColorEdit() {
        editingColorId = null;
        picker = null;
    }

    private void dragSliderTo(String id, double mx) {
        float frac = (float) ((mx - dsTrackX) / dsTrackW);
        frac = Math.max(0f, Math.min(1f, frac));
        float[] r = sliderRange(id);
        setSlider(id, r[0] + frac * (r[1] - r[0]));
    }

    public boolean mouseDragged(double mx, double my) {
        if (draggingScrollbar) { scrollToThumbTop((int) (my - scrollbarGrab)); return true; }
        if (draggingSlider != null) { dragSliderTo(draggingSlider, mx); return true; }
        if (editingColorId != null && picker != null && picker.isDragging()) {
            picker.mouseDragged(mx, my, x0 + PAD, 0);
            return true;
        }
        return false;
    }

    public void mouseReleased() {
        draggingScrollbar = false;
        draggingSlider = null;
        if (picker != null) picker.mouseReleased();
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!isPointInside(mx, my)) return false;
        int max = scrollMax();
        scroll = Math.max(0, Math.min(max, scroll - (int) (amount * 12)));
        return true;
    }

    public boolean charTyped(char c) {
        return editingColorId != null && picker != null && picker.charTyped(c);
    }

    public boolean keyPressed(int keyCode) {
        return editingColorId != null && picker != null && picker.keyPressed(keyCode);
    }

    private static int currentMenuBlur() {
        return Minecraft.getInstance().options.menuBackgroundBlurriness().get();
    }

    private float[] sliderRange(String id) {
        return "backgroundBlur".equals(id) ? new float[]{0f, 10f} : new float[]{0f, 1f};
    }

    private float getSlider(String id) {
        if ("backgroundBlur".equals(id)) {
            int b = BlockHighlightConfig.get().backgroundBlur;
            return b >= 0 ? b : currentMenuBlur();
        }
        return 0f;
    }

    private void setSlider(String id, float v) {
        float[] r = sliderRange(id);
        v = Math.max(r[0], Math.min(r[1], v));
        if ("backgroundBlur".equals(id)) BlockHighlightConfig.get().backgroundBlur = Math.round(v);
        BlockHighlightConfig.save();
    }

    private String formatSliderValue(String id) {
        return "backgroundBlur".equals(id) ? String.valueOf((int) getSlider(id)) : "";
    }

    private boolean getToggle(String id) {
        BlockHighlightConfig c = BlockHighlightConfig.get();
        if ("editorFilled".equals(id)) return c.editorFilled;
        if ("editorOutline".equals(id)) return c.editorOutline;
        if ("editorSeeThrough".equals(id)) return c.editorSeeThrough;
        return false;
    }

    private void setToggle(String id, boolean v) {
        BlockHighlightConfig c = BlockHighlightConfig.get();
        if ("editorFilled".equals(id)) c.editorFilled = v;
        if ("editorOutline".equals(id)) c.editorOutline = v;
        if ("editorSeeThrough".equals(id)) c.editorSeeThrough = v;
        BlockHighlightConfig.save();
    }

    private String getColor(String id) {
        return "highlightColor".equals(id) ? BlockHighlightConfig.get().highlightColor : "#FFFFFF";
    }

    private void setColor(String id, String hex) {
        if (HexColor.parse(hex) == null) return;
        if ("highlightColor".equals(id)) BlockHighlightConfig.get().highlightColor = hex;
        BlockHighlightConfig.save();
    }
}
