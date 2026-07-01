package com.botato.blockhighlight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class MarkerListPanel {
    private static final int PAD = 8;
    private static final int HEADER_H = 16;
    private static final int ROW_H = 38;
    private static final int ADD_H = 22;
    private static final int NAME_MAX = 28;

    private int x0, y0, x1, y1, viewBottom;
    private int scroll = 0;
    private boolean draggingScrollbar = false;
    private int scrollbarGrab;

    private int colorEditIndex = -1;
    private ColorPicker picker;

    private int coordEditIndex = -1;
    private int coordEditField = -1;
    private String coordBuffer = "";

    private int nameEditIndex = -1;
    private String nameBuffer = "";

    private boolean addMode = false;
    private final String[] addBuf = {"", "", ""};
    private int addField = 0;

    private List<BlockMarker> markers() {
        Minecraft mc = Minecraft.getInstance();
        String dim = mc.level != null ? mc.level.dimension().identifier().toString() : "minecraft:overworld";
        return MarkerDataManager.getMarkersForDimension(dim);
    }

    public void layout(int px0, int py0, int px1, int py1) {
        x0 = px0;
        y0 = py0;
        x1 = px1;
        y1 = py1;
        viewBottom = y1;
    }

    public boolean isPointInside(double mx, double my) {
        return mx >= x0 && mx <= x1 && my >= y0 && my <= y1;
    }

    private int contentHeight() {
        int h = PAD + HEADER_H;
        List<BlockMarker> ms = markers();
        for (int i = 0; i < ms.size(); i++) {
            h += ROW_H;
            if (i == colorEditIndex && picker != null) h += picker.height();
        }
        h += ADD_H + (addMode ? ADD_H : 0);
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

        ctx.drawString(tr, "Highlighted Blocks", x0 + PAD, y + 4, 0xFFAAAAAA, false);
        ctx.fill(x0 + PAD + tr.width("Highlighted Blocks") + 4, y + 8, x1 - PAD, y + 9, 0xFF2A2A2A);
        y += HEADER_H;

        List<BlockMarker> ms = markers();
        for (int i = 0; i < ms.size(); i++) {
            BlockMarker m = ms.get(i);
            if (y + ROW_H >= y0 && y <= viewBottom) renderRow(ctx, tr, m, i, y, mouseX, mouseY);
            y += ROW_H;
            if (i == colorEditIndex && picker != null) {
                if (y + picker.height() >= y0 && y <= viewBottom) picker.render(ctx, tr, x0 + PAD, x1 - PAD, y, mouseX, mouseY);
                y += picker.height();
            }
        }

        boolean addHover = mouseX >= x0 && mouseX <= x1 && mouseY >= y && mouseY < y + ADD_H && mouseY < viewBottom;
        if (addHover) ctx.fill(x0 + 2, y, x1 - 2, y + ADD_H, 0x20FFFFFF);
        ctx.drawString(tr, addMode ? "Cancel" : "+ Add block", x0 + PAD, y + 7, 0xFF8FE08F, false);
        y += ADD_H;
        if (addMode) {
            renderAddFields(ctx, tr, y);
            y += ADD_H;
        }
        ctx.disableScissor();

        int barH = scrollbarThumbH();
        if (barH > 0) {
            int barY = scrollbarThumbY();
            ctx.fill(x1 - 5, y0, x1 - 1, viewBottom, 0x18FFFFFF);
            ctx.fill(x1 - 5, barY, x1 - 1, barY + barH, draggingScrollbar ? 0xFFAAAAAA : 0xFF666666);
        }
    }

    private void renderRow(GuiGraphics ctx, Font tr, BlockMarker m, int i, int y, int mouseX, int mouseY) {
        boolean hovered = mouseX >= x0 && mouseX <= x1 && mouseY >= y && mouseY < y + ROW_H && mouseY < viewBottom;
        if (hovered || i == colorEditIndex) ctx.fill(x0 + 2, y, x1 - 2, y + ROW_H, i == colorEditIndex ? 0x33FFFFFF : 0x20FFFFFF);

        int swX = x0 + PAD, swY = y + 4;
        int rgb = 0xFF000000 | (m.hexColor & 0xFFFFFF);
        ctx.fill(swX - 1, swY - 1, swX + 15, swY + 15, 0xFF666666);
        ctx.fill(swX, swY, swX + 14, swY + 14, m.hidden ? 0xFF333333 : rgb);

        int textX = swX + 20;
        int ctrlRight = x1 - PAD - 8;

        boolean editingName = (i == nameEditIndex);
        String name = editingName ? nameBuffer : m.displayName();
        boolean nameEmpty = name.isEmpty();
        String shownName = (nameEmpty && !editingName) ? "Unnamed" : name;
        int nameCol = editingName ? 0xFFFFFFFF : (nameEmpty ? 0xFF707070 : 0xFFF0F0F0);
        ctx.drawString(tr, shownName, textX, y + 3, nameCol, false);
        if (editingName && (System.currentTimeMillis() / 500) % 2 == 0) {
            int cc = textX + tr.width(name) + 1;
            ctx.fill(cc, y + 2, cc + 1, y + 11, 0xFFFFFFFF);
        }

        int cx = textX;
        for (int f = 0; f < 3; f++) {
            String prefix = (f == 0 ? "X " : f == 1 ? " Y " : " Z ");
            int coord = (f == 0 ? m.x : f == 1 ? m.y : m.z);
            String val = (i == coordEditIndex && f == coordEditField) ? coordBuffer : String.valueOf(coord);
            ctx.drawString(tr, prefix, cx, y + 14, 0xFF777777, false);
            cx += tr.width(prefix);
            boolean editingThis = (i == coordEditIndex && f == coordEditField);
            ctx.drawString(tr, val, cx, y + 14, editingThis ? 0xFFFFFFFF : 0xFFE0E0E0, false);
            if (editingThis && (System.currentTimeMillis() / 500) % 2 == 0) {
                int cc = cx + tr.width(val) + 1;
                ctx.fill(cc, y + 13, cc + 1, y + 22, 0xFFFFFFFF);
            }
            cx += tr.width(val);
        }

        String dim = m.dimension.contains(":") ? m.dimension.substring(m.dimension.lastIndexOf(':') + 1) : m.dimension;
        String hex = HexColor.format(0xFF000000 | (m.hexColor & 0xFFFFFF));
        ctx.drawString(tr, hex + "  (" + dim + ")", textX, y + 25, 0xFF888888, false);

        int[] tg = toggleRects(tr, y);
        int tgTop = tg[6], tgH = tg[7];
        drawChip(ctx, tr, tg[0], tgTop, tg[1], tgH, "Fill", m.renderFilled);
        drawChip(ctx, tr, tg[2], tgTop, tg[3], tgH, "Border", m.renderOutline);
        drawChip(ctx, tr, tg[4], tgTop, tg[5], tgH, "X-Ray", m.renderSeeThrough);

        int ctrlY = y + 24;
        ctx.drawString(tr, "✕", ctrlRight, ctrlY, hovered ? 0xFFFF6666 : 0xFFAA5555, false);
        int eyeX = ctrlRight - 16;
        ctx.drawString(tr, m.hidden ? "+" : "-", eyeX, ctrlY, m.hidden ? 0xFF66AA66 : 0xFFCCCCCC, false);
    }

    private int[] toggleRects(Font tr, int y) {
        int zoneRight = x1 - PAD;
        int gap = 4;
        int fillW = tr.width("Fill") + 6;
        int borderW = tr.width("Border") + 6;
        int xrayW = tr.width("X-Ray") + 6;
        int xrayX = zoneRight - xrayW;
        int borderX = xrayX - gap - borderW;
        int fillX = borderX - gap - fillW;
        return new int[]{ fillX, fillW, borderX, borderW, xrayX, xrayW, y + 3, 10 };
    }

    private void drawChip(GuiGraphics ctx, Font tr, int x, int y, int w, int h, String label, boolean on) {
        ctx.fill(x - 1, y - 1, x + w + 1, y + h + 1, on ? 0xFF3FA046 : 0xFF555555);
        ctx.fill(x, y, x + w, y + h, on ? 0xFF2E7D32 : 0xFF3A3A3A);
        ctx.drawString(tr, label, x + (w - tr.width(label)) / 2, y + 1, on ? 0xFFFFFFFF : 0xFF9A9A9A, false);
    }

    private void renderAddFields(GuiGraphics ctx, Font tr, int y) {
        int fieldW = ((x1 - x0) - PAD * 2 - 40) / 3;
        int fx = x0 + PAD;
        String[] labels = {"X", "Y", "Z"};
        for (int f = 0; f < 3; f++) {
            ctx.fill(fx, y + 2, fx + fieldW, y + 16, 0xFF000000);
            ctx.fill(fx, y + 2, fx + fieldW, y + 3, addField == f ? 0xFFAAAAAA : 0xFF555555);
            String s = addBuf[f].isEmpty() ? labels[f] : addBuf[f];
            ctx.drawString(tr, s, fx + 3, y + 5, addBuf[f].isEmpty() ? 0xFF555555 : 0xFFE0E0E0, false);
            fx += fieldW + 4;
        }
        boolean ready = isInt(addBuf[0]) && isInt(addBuf[1]) && isInt(addBuf[2]);
        ctx.drawString(tr, "Add", x1 - PAD - tr.width("Add"), y + 5, ready ? 0xFF8FE08F : 0xFF555555, false);
    }

    private int headerBottomY() { return y0 + PAD - scroll + HEADER_H; }

    public boolean mouseClicked(double mx, double my) {
        if (!isPointInside(mx, my)) return false;
        int barH = scrollbarThumbH();
        if (barH > 0 && mx >= x1 - 7 && my >= y0 && my <= viewBottom) {
            int thumbY = scrollbarThumbY();
            if (my >= thumbY && my < thumbY + barH) scrollbarGrab = (int) (my - thumbY);
            else { scrollbarGrab = barH / 2; scrollToThumbTop((int) (my - barH / 2)); }
            draggingScrollbar = true;
            return true;
        }

        int y = headerBottomY();
        List<BlockMarker> ms = markers();
        Font tr = Minecraft.getInstance().font;
        for (int i = 0; i < ms.size(); i++) {
            BlockMarker m = ms.get(i);
            if (my >= y && my < y + ROW_H) {
                int[] tg = toggleRects(tr, y);
                if (my >= tg[6] - 1 && my <= tg[6] + tg[7]) {
                    if (mx >= tg[0] && mx <= tg[0] + tg[1]) { MarkerDataManager.setRenderFilled(m, !m.renderFilled); return true; }
                    if (mx >= tg[2] && mx <= tg[2] + tg[3]) { MarkerDataManager.setRenderOutline(m, !m.renderOutline); return true; }
                    if (mx >= tg[4] && mx <= tg[4] + tg[5]) { MarkerDataManager.setRenderSeeThrough(m, !m.renderSeeThrough); return true; }
                }
                int ctrlY = y + 24;
                int delX = x1 - PAD - 8;
                if (mx >= delX - 2 && mx <= delX + 10 && my >= ctrlY - 2 && my <= ctrlY + 11) { closeEdits(); MarkerDataManager.remove(m); return true; }
                int eyeX = delX - 16;
                if (mx >= eyeX - 2 && mx <= eyeX + 10 && my >= ctrlY - 2 && my <= ctrlY + 11) { MarkerDataManager.setHidden(m, !m.hidden); return true; }
                int handled = coordHitTest(tr, m, i, mx, my, y);
                if (handled >= 0) { beginCoordEdit(i, handled, m); return true; }
                if (my >= y + 1 && my < y + 13 && mx >= x0 + PAD + 20 && mx <= tg[0] - 4) { beginNameEdit(i, m); return true; }
                openColorEdit(i, m);
                return true;
            }
            y += ROW_H;
            if (i == colorEditIndex && picker != null) {
                if (my >= y && my < y + picker.height()) {
                    picker.mouseClicked(mx, my, x0 + PAD, x1 - PAD, y);
                    return true;
                }
                y += picker.height();
            }
        }

        if (my >= y && my < y + ADD_H) {
            if (addMode) { addMode = false; }
            else { closeEdits(); addMode = true; addField = 0; addBuf[0] = addBuf[1] = addBuf[2] = ""; }
            return true;
        }
        y += ADD_H;
        if (addMode && my >= y && my < y + ADD_H) {
            Font ftr = Minecraft.getInstance().font;
            int fieldW = ((x1 - x0) - PAD * 2 - 40) / 3;
            int fx = x0 + PAD;
            for (int f = 0; f < 3; f++) {
                if (mx >= fx && mx < fx + fieldW) { addField = f; return true; }
                fx += fieldW + 4;
            }
            if (mx >= x1 - PAD - ftr.width("Add") - 2) { commitAdd(); return true; }
        }
        return true;
    }

    private int coordHitTest(Font tr, BlockMarker m, int i, double mx, double my, int rowY) {
        if (my < rowY + 13 || my >= rowY + 25) return -1;
        int cx = x0 + PAD + 20;
        for (int f = 0; f < 3; f++) {
            String prefix = (f == 0 ? "X " : f == 1 ? " Y " : " Z ");
            int coord = (f == 0 ? m.x : f == 1 ? m.y : m.z);
            String val = String.valueOf(coord);
            cx += tr.width(prefix);
            if (mx >= cx - 1 && mx <= cx + tr.width(val) + 1) return f;
            cx += tr.width(val);
        }
        return -1;
    }

    private void openColorEdit(int i, BlockMarker m) {
        closeEdits();
        colorEditIndex = i;
        picker = new ColorPicker(
                () -> HexColor.format(0xFF000000 | (m.hexColor & 0xFFFFFF)),
                hex -> { Integer v = HexColor.parse(hex); if (v != null) MarkerDataManager.setColor(m, v & 0xFFFFFF); });
        picker.open();
    }

    private void beginCoordEdit(int i, int field, BlockMarker m) {
        closeEdits();
        coordEditIndex = i; coordEditField = field;
        coordBuffer = String.valueOf(field == 0 ? m.x : field == 1 ? m.y : m.z);
    }

    private void beginNameEdit(int i, BlockMarker m) {
        closeEdits();
        nameEditIndex = i;
        nameBuffer = m.displayName();
    }

    private void commitNameEdit() {
        if (nameEditIndex < 0) return;
        List<BlockMarker> ms = markers();
        if (nameEditIndex < ms.size()) {
            MarkerDataManager.setName(ms.get(nameEditIndex), nameBuffer.trim());
        }
        nameEditIndex = -1; nameBuffer = "";
    }

    private void commitCoordEdit() {
        if (coordEditIndex < 0) return;
        List<BlockMarker> ms = markers();
        if (coordEditIndex < ms.size() && isInt(coordBuffer)) {
            BlockMarker m = ms.get(coordEditIndex);
            int v = Integer.parseInt(coordBuffer);
            int nx = coordEditField == 0 ? v : m.x;
            int ny = coordEditField == 1 ? v : m.y;
            int nz = coordEditField == 2 ? v : m.z;
            MarkerDataManager.setCoords(m, nx, ny, nz);
        }
        coordEditIndex = -1; coordEditField = -1; coordBuffer = "";
    }

    private void commitAdd() {
        if (!(isInt(addBuf[0]) && isInt(addBuf[1]) && isInt(addBuf[2]))) return;
        Minecraft mc = Minecraft.getInstance();
        String dim = mc.level != null ? mc.level.dimension().identifier().toString() : "minecraft:overworld";
        BlockHighlightConfig cfg = BlockHighlightConfig.get();
        BlockMarker bm = new BlockMarker(
                Integer.parseInt(addBuf[0]), Integer.parseInt(addBuf[1]), Integer.parseInt(addBuf[2]),
                dim, cfg.highlightColorArgb() & 0xFFFFFF);
        bm.renderFilled = cfg.editorFilled;
        bm.renderOutline = cfg.editorOutline;
        bm.renderSeeThrough = cfg.editorSeeThrough;
        MarkerDataManager.add(bm);
        addBuf[0] = addBuf[1] = addBuf[2] = "";
        addMode = false;
    }

    private void closeEdits() {
        commitCoordEdit();
        commitNameEdit();
        colorEditIndex = -1; picker = null;
        addMode = false;
    }

    public boolean mouseDragged(double mx, double my) {
        if (draggingScrollbar) { scrollToThumbTop((int) (my - scrollbarGrab)); return true; }
        if (colorEditIndex >= 0 && picker != null && picker.isDragging()) { picker.mouseDragged(mx, my, x0 + PAD, 0); return true; }
        return false;
    }

    public void mouseReleased() {
        draggingScrollbar = false;
        if (picker != null) picker.mouseReleased();
    }

    public boolean mouseScrolled(double mx, double my, double amount) {
        if (!isPointInside(mx, my)) return false;
        int max = scrollMax();
        scroll = Math.max(0, Math.min(max, scroll - (int) (amount * 12)));
        return true;
    }

    public boolean charTyped(char c) {
        if (nameEditIndex >= 0) {
            if (c >= ' ' && c != 127 && nameBuffer.length() < NAME_MAX) nameBuffer += c;
            return true;
        }
        if (coordEditIndex >= 0) {
            if ((c >= '0' && c <= '9') || (c == '-' && coordBuffer.isEmpty())) {
                if (coordBuffer.replace("-", "").length() < 9) coordBuffer += c;
            }
            return true;
        }
        if (addMode) {
            if ((c >= '0' && c <= '9') || (c == '-' && addBuf[addField].isEmpty())) {
                if (addBuf[addField].replace("-", "").length() < 9) addBuf[addField] += c;
            }
            return true;
        }
        if (colorEditIndex >= 0 && picker != null) return picker.charTyped(c);
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (nameEditIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!nameBuffer.isEmpty()) nameBuffer = nameBuffer.substring(0, nameBuffer.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { commitNameEdit(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { nameEditIndex = -1; nameBuffer = ""; return true; }
            return false;
        }
        if (coordEditIndex >= 0) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!coordBuffer.isEmpty()) coordBuffer = coordBuffer.substring(0, coordBuffer.length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { commitCoordEdit(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { coordEditIndex = -1; coordEditField = -1; coordBuffer = ""; return true; }
            return false;
        }
        if (addMode) {
            if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                if (!addBuf[addField].isEmpty()) addBuf[addField] = addBuf[addField].substring(0, addBuf[addField].length() - 1);
                return true;
            }
            if (keyCode == GLFW.GLFW_KEY_TAB) { addField = (addField + 1) % 3; return true; }
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) { commitAdd(); return true; }
            if (keyCode == GLFW.GLFW_KEY_ESCAPE) { addMode = false; return true; }
            return false;
        }
        if (colorEditIndex >= 0 && picker != null) return picker.keyPressed(keyCode);
        return false;
    }

    private static boolean isInt(String s) {
        if (s == null || s.isEmpty() || s.equals("-")) return false;
        try { Integer.parseInt(s); return true; } catch (NumberFormatException e) { return false; }
    }
}
