package com.universeclock.client;

import java.util.Locale;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

import com.universeclock.TimeUtil;

/**
 * «Небесная шкала»: 24 часа суток в виде градиента неба (ночь → рассвет → день → закат).
 * Ползунок-солнце/луна показывает выбранное время, тонкая белая линия — текущее время мира.
 * Можно кликать и перетаскивать мышью, чтобы быстро выбрать время.
 */
final class SkySlider extends AbstractWidget {
    private final UniverseClockScreen screen;

    SkySlider(UniverseClockScreen screen, int x, int y, int width, int height) {
        super(x, y, width, height, Component.translatable("gui.universeclock.sky"));
        this.screen = screen;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        int h = this.getHeight();
        long ms = System.currentTimeMillis();

        // рамка
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, this.isHoveredOrFocused() ? 0xFF8FA0F0 : 0xFF3A4275);

        // небо
        for (int i = 0; i < w; i++) {
            g.fill(x + i, y, x + i + 1, y + h, Gfx.skyColor(i * 24f / w));
        }

        // звёзды в тёмных участках
        for (int k = 0; k < 46; k++) {
            int sx = (k * 61 + 17) % w;
            int sy = (k * 29 + 5) % (h - 4) + 1;
            float lum = Gfx.luminance(Gfx.skyColor(sx * 24f / w));
            if (lum > 0.22f) {
                continue;
            }
            float twinkle = 0.5f + 0.5f * (float) Math.sin(ms / 350.0 + k * 1.7);
            int alpha = (int) (80 + 150 * twinkle * (1f - lum * 4f));
            alpha = Math.max(0, Math.min(255, alpha));
            g.fill(x + sx, y + sy, x + sx + 1, y + sy + 1, (alpha << 24) | 0xFFFFFF);
        }

        // объём
        g.fillGradient(x, y, x + w, y + h, 0x30FFFFFF, 0x55000000);

        // деления
        for (int hr = 0; hr <= 24; hr += 3) {
            int tx = x + Math.min(w - 1, hr * w / 24);
            int len = (hr % 6 == 0) ? 4 : 2;
            g.fill(tx, y + h - len, tx + 1, y + h, 0x88FFFFFF);
            if (hr % 6 == 0) {
                String label = String.format(Locale.ROOT, "%02d", hr);
                Gfx.centeredText(g, font, label, tx, y + h + 3, 0.75f, 0xFF8A93C8, false);
            }
        }

        // текущее время мира — тонкая линия
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            int worldSeconds = TimeUtil.ticksToClockSeconds(mc.level.getDayTime());
            int wx = x + Math.min(w - 1, Math.round(worldSeconds / 86400f * (w - 1)));
            g.fill(wx, y, wx + 1, y + h, 0xAAFFFFFF);
        }

        // ползунок: солнце или луна
        int seconds = screen.getClockSeconds();
        float hour = seconds / 3600f;
        int tx = x + Math.round(seconds / 86400f * (w - 1));
        int cy = y + h / 2;
        g.fill(tx - 1, y - 3, tx + 2, y + h + 3, 0xCCFFFFFF);
        boolean day = hour >= 6f && hour < 18f;
        if (day) {
            Gfx.disc(g, tx, cy, 7, 0xFFFFA726);
            Gfx.disc(g, tx, cy, 5, 0xFFFFF59D);
        } else {
            Gfx.disc(g, tx, cy, 7, 0xFF7C88C9);
            Gfx.disc(g, tx, cy, 5, 0xFFE8ECFF);
            g.fill(tx - 2, cy - 2, tx - 1, cy - 1, 0xFFB5BCE6);
            g.fill(tx + 1, cy + 1, tx + 3, cy + 2, 0xFFB5BCE6);
        }
    }

    private void setFromMouse(double mouseX) {
        double frac = (mouseX - this.getX()) / (double) (this.getWidth() - 1);
        frac = Math.max(0.0, Math.min(1.0, frac));
        int minutes = (int) Math.round(frac * 1440.0);
        screen.setClockSeconds(Math.min(minutes, 1439) * 60);
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        setFromMouse(mouseX);
    }

    @Override
    public void onDrag(double mouseX, double mouseY, double dragX, double dragY) {
        setFromMouse(mouseX);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return false;
        }
        screen.addSeconds(scrollY > 0 ? 600 : -600);
        return true;
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
