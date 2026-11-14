package com.universeclock.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** Небольшие помощники для рисования красивого интерфейса. */
final class Gfx {

    private Gfx() {
    }

    // ---------- цвета ----------

    static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int al = (int) (((a >>> 24) & 0xFF) * (1f - t) + ((b >>> 24) & 0xFF) * t);
        int r = (int) (((a >> 16) & 0xFF) * (1f - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1f - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1f - t) + (b & 0xFF) * t);
        return (al << 24) | (r << 16) | (g << 8) | bl;
    }

    static int withAlpha(int color, int alpha) {
        return ((alpha & 0xFF) << 24) | (color & 0xFFFFFF);
    }

    static float luminance(int color) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        return (r * 0.299f + g * 0.587f + b * 0.114f) / 255f;
    }

    // Ключевые точки цвета неба по часам суток (0..24).
    private static final float[] SKY_HOURS = {0f, 4.5f, 5.5f, 6.5f, 8f, 12f, 16.5f, 18f, 19f, 20f, 22f, 24f};
    private static final int[] SKY_COLORS = {
            0xFF0A0F2E, // полночь
            0xFF101A48, // глубокая ночь
            0xFF5A3A7A, // предрассветная фиолетовая
            0xFFF08A4B, // рассвет
            0xFF6FB7F5, // утро
            0xFF4FA0F0, // полдень
            0xFF6FB7F5, // день
            0xFFF2A04A, // закат
            0xFFB4476B, // сумерки
            0xFF2A2055, // вечер
            0xFF0F1538, // ночь
            0xFF0A0F2E  // полночь
    };

    static int skyColor(float hour) {
        for (int i = 1; i < SKY_HOURS.length; i++) {
            if (hour <= SKY_HOURS[i]) {
                float t = (hour - SKY_HOURS[i - 1]) / (SKY_HOURS[i] - SKY_HOURS[i - 1]);
                return lerpColor(SKY_COLORS[i - 1], SKY_COLORS[i], t);
            }
        }
        return SKY_COLORS[SKY_COLORS.length - 1];
    }

    // ---------- фигуры ----------

    /** Панель со скруглёнными (срезанными) углами, рамкой и вертикальным градиентом. */
    static void roundedPanel(GuiGraphics g, int x, int y, int w, int h, int border, int topColor, int bottomColor) {
        g.fill(x + 1, y, x + w - 1, y + h, border);
        g.fill(x, y + 1, x + w, y + h - 1, border);
        g.fillGradient(x + 1, y + 1, x + w - 1, y + h - 1, topColor, bottomColor);
    }

    /** Закрашенный круг (пиксельный). */
    static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy <= r; dy++) {
            int dx = (int) Math.round(Math.sqrt(r * r + r * 0.6 - dy * dy));
            g.fill(cx - dx, cy + dy, cx + dx + 1, cy + dy + 1, color);
        }
    }

    // ---------- текст ----------

    static void text(GuiGraphics g, Font font, String s, float x, float y, float scale, int color, boolean shadow) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0f);
        pose.scale(scale, scale, 1f);
        g.drawString(font, s, 0, 0, color, shadow);
        pose.popPose();
    }

    static void centeredText(GuiGraphics g, Font font, String s, float cx, float y, float scale, int color, boolean shadow) {
        float w = font.width(s) * scale;
        text(g, font, s, cx - w / 2f, y, scale, color, shadow);
    }

    /** Центрированный текст, который сжимается, если не помещается в maxWidth. */
    static void fitCenteredText(GuiGraphics g, Font font, String s, float cx, float y, float maxWidth, float scale, int color, boolean shadow) {
        float w = font.width(s) * scale;
        if (w > maxWidth) {
            scale *= maxWidth / w;
        }
        centeredText(g, font, s, cx, y, scale, color, shadow);
    }
}
