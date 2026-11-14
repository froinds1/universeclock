package com.universeclock.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Плоская кнопка в стиле интерфейса «Часов вселенной» с цветным акцентом. */
final class FlatButton extends Button {
    private final int accent;

    FlatButton(int x, int y, int width, int height, Component label, int accent, Button.OnPress onPress) {
        super(x, y, width, height, label, onPress, DEFAULT_NARRATION);
        this.accent = accent;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        boolean hot = this.isHoveredOrFocused();
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();

        int border = hot ? accent : Gfx.lerpColor(accent, 0xFF1B2144, 0.55f);
        int top = hot ? Gfx.lerpColor(0xFF2A3262, accent, 0.28f) : 0xFF222A55;
        int bottom = hot ? Gfx.lerpColor(0xFF161B3C, accent, 0.20f) : 0xFF141937;
        Gfx.roundedPanel(g, x, y, w, h, border, top, bottom);

        // Светлая «блик»-линия сверху
        g.fill(x + 2, y + 1, x + w - 2, y + 2, 0x22FFFFFF);

        String label = this.getMessage().getString();
        float maxWidth = w - 4f;
        int color = hot ? 0xFFFFFFFF : 0xFFD6DBFF;
        float scale = 1f;
        int textWidth = font.width(label);
        if (textWidth > maxWidth) {
            scale = maxWidth / textWidth;
        }
        Gfx.centeredText(g, font, label, x + w / 2f, y + (h - font.lineHeight * scale) / 2f + 0.5f, scale, color, true);
    }
}
