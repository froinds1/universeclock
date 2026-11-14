package com.universeclock.client;

import java.util.Locale;

import org.lwjgl.glfw.GLFW;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Одно поле цифрового табло (часы / минуты / секунды).
 *
 * Управление: клик по стрелкам, колесо мыши, стрелки ↑/↓ на клавиатуре,
 * либо клик по цифрам и ввод числа с клавиатуры (после двух цифр фокус
 * сам перепрыгивает на следующее поле). Shift — крупный шаг.
 */
final class TimeField extends AbstractWidget {
    static final int ARROW_H = 14;
    static final int DIGIT_H = 36;
    static final int LABEL_H = 8;
    static final int TOTAL_H = ARROW_H * 2 + DIGIT_H + LABEL_H;

    private final UniverseClockScreen screen;
    private final int unit;   // сколько секунд суток в одной единице поля: 3600 / 60 / 1
    private final int range;  // 24 / 60 / 60
    private TimeField next;
    private int typed;        // 1 — уже введена первая цифра, ждём вторую

    TimeField(UniverseClockScreen screen, int x, int y, int width, int unit, int range, String labelKey) {
        super(x, y, width, TOTAL_H, Component.translatable(labelKey));
        this.screen = screen;
        this.unit = unit;
        this.range = range;
    }

    void setNext(TimeField next) {
        this.next = next;
    }

    private int value() {
        return (screen.getClockSeconds() / unit) % range;
    }

    private void setValue(int newValue) {
        screen.setClockSeconds(screen.getClockSeconds() + (newValue - value()) * unit);
    }

    private void bump(int direction) {
        int step = Screen.hasShiftDown() ? (unit == 3600 ? 6 : 10) : 1;
        screen.addSeconds(direction * step * unit);
        typed = 0;
    }

    // ---------- рисование ----------

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        Font font = Minecraft.getInstance().font;
        int x = this.getX();
        int y = this.getY();
        int w = this.getWidth();
        boolean hovered = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TOTAL_H;
        boolean focused = this.isFocused();

        boolean overUp = hovered && mouseY < y + ARROW_H;
        boolean overDown = hovered && mouseY >= y + ARROW_H + DIGIT_H && mouseY < y + ARROW_H * 2 + DIGIT_H;
        boolean overDigits = hovered && !overUp && !overDown;

        drawArrow(g, x + w / 2, y + ARROW_H / 2, true, overUp);

        int boxY = y + ARROW_H;
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 250.0);
        int border;
        if (focused) {
            border = Gfx.lerpColor(0xFFFFB627, 0xFFFFE9A6, pulse);
        } else if (overDigits) {
            border = 0xFF8FA0F0;
        } else {
            border = 0xFF2E3566;
        }
        Gfx.roundedPanel(g, x, boxY, w, DIGIT_H, border, 0xFF0A0D20, 0xFF171C3C);
        // внутреннее свечение
        g.fill(x + 2, boxY + 1, x + w - 2, boxY + 2, 0x18FFFFFF);

        String digits = String.format(Locale.ROOT, "%02d", value());
        float scale = 3f;
        float textY = boxY + (DIGIT_H - font.lineHeight * scale) / 2f + 1f;
        Gfx.centeredText(g, font, digits, x + w / 2f, textY, scale, focused ? 0xFFFFFFFF : 0xFFFFE8A0, true);

        drawArrow(g, x + w / 2, boxY + DIGIT_H + ARROW_H / 2, false, overDown);

        Gfx.centeredText(g, font, this.getMessage().getString(), x + w / 2f, y + ARROW_H * 2 + DIGIT_H + 1, 0.75f, 0xFF8A93C8, false);
    }

    private static void drawArrow(GuiGraphics g, int cx, int cy, boolean up, boolean hot) {
        int color = hot ? 0xFFFFD866 : 0xFF6F79B8;
        for (int i = 0; i < 4; i++) {
            int half = up ? i : 3 - i;
            int yy = cy - 2 + i;
            g.fill(cx - half, yy, cx + half + 1, yy + 1, color);
        }
    }

    // ---------- ввод ----------

    @Override
    public void onClick(double mouseX, double mouseY) {
        double localY = mouseY - this.getY();
        if (localY < ARROW_H) {
            bump(1);
        } else if (localY >= ARROW_H + DIGIT_H && localY < ARROW_H * 2 + DIGIT_H) {
            bump(-1);
        }
        typed = 0;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) {
            return false;
        }
        bump(scrollY > 0 ? 1 : -1);
        return true;
    }

    @Override
    public boolean charTyped(char codePoint, int modifiers) {
        if (!this.isFocused() || codePoint < '0' || codePoint > '9') {
            return false;
        }
        int digit = codePoint - '0';
        boolean second = typed == 1;
        int newValue = second ? value() * 10 + digit : digit;
        if (newValue >= range) {
            // число вышло за границу — начинаем ввод заново с этой цифры
            newValue = digit;
            second = false;
        }
        setValue(newValue);
        if (!second && newValue * 10 < range) {
            typed = 1; // возможна вторая цифра
        } else {
            typed = 0; // поле заполнено — переходим к следующему
            if (next != null) {
                screen.setFocused(next);
            }
        }
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!this.isFocused()) {
            return false;
        }
        switch (keyCode) {
            case GLFW.GLFW_KEY_UP:
                bump(1);
                return true;
            case GLFW.GLFW_KEY_DOWN:
                bump(-1);
                return true;
            case GLFW.GLFW_KEY_BACKSPACE:
                setValue(value() / 10);
                typed = 0;
                return true;
            case GLFW.GLFW_KEY_ENTER:
            case GLFW.GLFW_KEY_KP_ENTER:
                screen.applyTime();
                return true;
            default:
                return false;
        }
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            typed = 0;
        }
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        this.defaultButtonNarrationText(output);
    }
}
