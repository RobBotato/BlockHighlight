package com.botato.blockhighlight;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class MarkerManagerScreen extends Screen {

    private final SettingsPanel settings = new SettingsPanel();
    private final MarkerListPanel list = new MarkerListPanel();

    private Integer savedBlurriness;

    public MarkerManagerScreen() {
        super(Component.literal("Block Highlight"));
    }

    @Override
    protected void init() {
        int top = 24, bottom = height - 24;
        int edgeMargin = Math.max(16, Math.round(width * 0.06f));
        int midGap = Math.max(12, Math.round(width * 0.03f));
        int panelW = Math.min(320, (width - 2 * edgeMargin - midGap) / 2);
        int total = 2 * panelW + midGap;
        int startX = (width - total) / 2;
        int leftX0 = startX, leftX1 = startX + panelW;
        int rightX0 = leftX1 + midGap, rightX1 = rightX0 + panelW;

        list.layout(leftX0, top, leftX1, bottom);
        settings.layout(rightX0, top, rightX1, bottom);

        if (savedBlurriness == null) {
            savedBlurriness = Minecraft.getInstance().options.menuBackgroundBlurriness().get();
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        BlockHighlightConfig cfg = BlockHighlightConfig.get();
        int blur = cfg.backgroundBlur >= 0 ? cfg.backgroundBlur
                : (savedBlurriness != null ? savedBlurriness
                        : Minecraft.getInstance().options.menuBackgroundBlurriness().get());
        Minecraft.getInstance().options.menuBackgroundBlurriness().set(blur);

        super.render(g, mouseX, mouseY, delta);
        list.render(g, mouseX, mouseY);
        settings.render(g, mouseX, mouseY);
    }

    @Override
    public void removed() {
        if (savedBlurriness != null) {
            Minecraft.getInstance().options.menuBackgroundBlurriness().set(savedBlurriness);
        }
        super.removed();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean bl) {
        double mx = event.x(), my = event.y();
        if (settings.mouseClicked(mx, my)) return true;
        if (list.mouseClicked(mx, my)) return true;
        return super.mouseClicked(event, bl);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        double mx = event.x(), my = event.y();
        if (settings.mouseDragged(mx, my)) return true;
        if (list.mouseDragged(mx, my)) return true;
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        settings.mouseReleased();
        list.mouseReleased();
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double scrollX, double scrollY) {
        if (settings.mouseScrolled(mx, my, scrollY)) return true;
        if (list.mouseScrolled(mx, my, scrollY)) return true;
        return false;
    }

    @Override
    public boolean charTyped(CharacterEvent event) {
        char c = (char) event.codepoint();
        if (settings.charTyped(c)) return true;
        if (list.charTyped(c)) return true;
        return super.charTyped(event);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int keyCode = event.key();
        if (settings.keyPressed(keyCode)) return true;
        if (list.keyPressed(keyCode)) return true;
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
