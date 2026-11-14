package com.universeclock.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.universeclock.TimeUtil;
import com.universeclock.UniverseClockMod;
import com.universeclock.network.SetTimePayload;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Меню «Часов вселенной»: цифровое табло ЧЧ:ММ:СС, небесная шкала, пресеты и кнопка применения.
 */
public class UniverseClockScreen extends Screen {
    static final int PANEL_W = 320;
    static final int PANEL_H = 222;
    private static final int PAD = 12;

    // тики Minecraft для быстрых пресетов
    private static final int[] PRESET_TICKS = {0, 1000, 6000, 12000, 13000, 18000};
    private static final String[] PRESET_KEYS = {"dawn", "morning", "noon", "sunset", "night", "midnight"};

    private final ItemStack icon;

    private int left;
    private int top;

    /** Выбранное в меню время: секунды суток по 24-часовому циферблату, 0..86399. */
    private int clockSeconds;

    private int fieldsX;
    private int fieldsY;
    private int fieldWidth;
    private int fieldGap;

    private Component status = Component.empty();
    private long statusUntil;

    public UniverseClockScreen() {
        super(Component.translatable("gui.universeclock.title"));
        this.icon = new ItemStack(UniverseClockMod.UNIVERSE_CLOCK.get());
        this.clockSeconds = worldClockSeconds();
    }

    // ---------- модель ----------

    int getClockSeconds() {
        return clockSeconds;
    }

    void setClockSeconds(int seconds) {
        this.clockSeconds = Math.floorMod(seconds, TimeUtil.SECONDS_PER_DAY);
    }

    void addSeconds(int delta) {
        setClockSeconds(this.clockSeconds + delta);
    }

    private static long worldDayTime() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level != null ? mc.level.getDayTime() : 0L;
    }

    private static int worldClockSeconds() {
        return TimeUtil.ticksToClockSeconds(worldDayTime());
    }

    /** Отправляет выбранное время на сервер. */
    void applyTime() {
        int ticks = TimeUtil.clockSecondsToTicks(clockSeconds);
        PacketDistributor.sendToServer(new SetTimePayload(ticks));
        String applied = TimeUtil.format(TimeUtil.ticksToClockSeconds(ticks));
        this.status = Component.translatable("gui.universeclock.applied", applied, ticks);
        this.statusUntil = System.currentTimeMillis() + 4000L;
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.6F));
    }

    // ---------- создание виджетов ----------

    @Override
    protected void init() {
        this.left = (this.width - PANEL_W) / 2;
        this.top = Math.max(2, (this.height - PANEL_H) / 2);

        // небесная шкала
        this.addRenderableWidget(new SkySlider(this, left + PAD, top + 41, PANEL_W - PAD * 2, 22));

        // цифровое табло
        this.fieldWidth = 58;
        this.fieldGap = 18;
        int total = fieldWidth * 3 + fieldGap * 2;
        this.fieldsX = left + (PANEL_W - total) / 2;
        this.fieldsY = top + 77;

        TimeField hours = this.addRenderableWidget(new TimeField(this, fieldsX, fieldsY, fieldWidth, 3600, 24, "gui.universeclock.hours"));
        TimeField minutes = this.addRenderableWidget(new TimeField(this, fieldsX + (fieldWidth + fieldGap), fieldsY, fieldWidth, 60, 60, "gui.universeclock.minutes"));
        TimeField seconds = this.addRenderableWidget(new TimeField(this, fieldsX + (fieldWidth + fieldGap) * 2, fieldsY, fieldWidth, 1, 60, "gui.universeclock.seconds"));
        hours.setNext(minutes);
        minutes.setNext(seconds);

        // пресеты
        int presetW = 46;
        int presetGap = 4;
        for (int i = 0; i < PRESET_TICKS.length; i++) {
            final int ticks = PRESET_TICKS[i];
            this.addRenderableWidget(new FlatButton(
                    left + PAD + i * (presetW + presetGap), top + 163, presetW, 16,
                    Component.translatable("gui.universeclock.preset." + PRESET_KEYS[i]),
                    0xFF6C7BDB,
                    button -> setClockSeconds(TimeUtil.ticksToClockSeconds(ticks))));
        }

        // нижний ряд кнопок
        int buttonsY = top + 185;
        this.addRenderableWidget(new FlatButton(left + PAD, buttonsY, 90, 18,
                Component.translatable("gui.universeclock.sync"), 0xFF4FA3E0,
                button -> setClockSeconds(worldClockSeconds())));
        this.addRenderableWidget(new FlatButton(left + PAD + 93, buttonsY, 110, 18,
                Component.translatable("gui.universeclock.apply"), 0xFFFFC83D,
                button -> applyTime()));
        this.addRenderableWidget(new FlatButton(left + PAD + 93 + 113, buttonsY, 90, 18,
                Component.translatable("gui.universeclock.close"), 0xFFD0606B,
                button -> this.onClose()));
    }

    @Override
    public boolean isPauseScreen() {
        return false; // мир идёт дальше — видно, как меняется небо
    }

    // ---------- рисование ----------

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        // Фон рисуем сами в render(), стандартное затемнение не нужно.
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long ms = System.currentTimeMillis();

        // лёгкое затемнение мира
        g.fillGradient(0, 0, this.width, this.height, 0x50050712, 0x90050712);

        drawPanel(g, ms);
        drawHeader(g, ms);
        drawLiveRow(g);

        super.render(g, mouseX, mouseY, partialTick);

        drawColons(g, ms);
        drawInfoLine(g);
        drawFooter(g, ms);
    }

    private void drawPanel(GuiGraphics g, long ms) {
        float pulse = 0.5f + 0.5f * (float) Math.sin(ms / 700.0);
        int glowColor = Gfx.lerpColor(0xFF6C5CE7, 0xFFFFC83D, pulse);

        // мягкое свечение вокруг панели
        for (int i = 4; i >= 1; i--) {
            g.fill(left - i, top - i, left + PANEL_W + i, top + PANEL_H + i, Gfx.withAlpha(glowColor, 12 - i * 2));
        }
        Gfx.roundedPanel(g, left, top, PANEL_W, PANEL_H, Gfx.lerpColor(0xFF3A3F86, glowColor, 0.45f), 0xF0141A3A, 0xF00A0D22);
    }

    private void drawHeader(GuiGraphics g, long ms) {
        Font font = this.font;
        float pulse = 0.5f + 0.5f * (float) Math.sin(ms / 700.0);

        g.fillGradient(left + 1, top + 1, left + PANEL_W - 1, top + 25, 0xFF2D2670, 0xFF1A1D4A);
        g.fill(left + 1, top + 25, left + PANEL_W - 1, top + 26, Gfx.lerpColor(0xFFB8860B, 0xFFFFD866, pulse));

        float bob = (float) Math.sin(ms / 450.0);
        drawIcon(g, left + 8, top + 3 + bob);
        drawIcon(g, left + PANEL_W - 8 - 18, top + 3 - bob);

        Gfx.centeredText(g, font, this.title.getString(), left + PANEL_W / 2f, top + 8, 1.25f, 0xFFFFD866, true);
    }

    private void drawIcon(GuiGraphics g, float x, float y) {
        PoseStack pose = g.pose();
        pose.pushPose();
        pose.translate(x, y, 0f);
        pose.scale(1.1f, 1.1f, 1f);
        g.renderItem(icon, 0, 0);
        pose.popPose();
    }

    private void drawLiveRow(GuiGraphics g) {
        long dayTime = worldDayTime();
        String now = TimeUtil.format(TimeUtil.ticksToClockSeconds(dayTime));
        String left1 = Component.translatable("gui.universeclock.now", now).getString();
        String right1 = Component.translatable("gui.universeclock.day_tick",
                dayTime / TimeUtil.TICKS_PER_DAY, Math.floorMod(dayTime, (long) TimeUtil.TICKS_PER_DAY)).getString();

        g.drawString(this.font, left1, left + PAD, top + 30, 0xFFA9B4E8, false);
        g.drawString(this.font, right1, left + PANEL_W - PAD - this.font.width(right1), top + 30, 0xFF7F89C0, false);
    }

    private void drawColons(GuiGraphics g, long ms) {
        boolean bright = (ms / 500L) % 2L == 0L;
        int color = bright ? 0xFFFFE8A0 : 0xFF6A6FA0;
        float scale = 3f;
        float y = fieldsY + TimeField.ARROW_H + (TimeField.DIGIT_H - this.font.lineHeight * scale) / 2f + 1f;
        for (int i = 0; i < 2; i++) {
            float cx = fieldsX + fieldWidth * (i + 1) + fieldGap * i + fieldGap / 2f;
            Gfx.centeredText(g, this.font, ":", cx, y, scale, color, true);
        }
    }

    private void drawInfoLine(GuiGraphics g) {
        int ticks = TimeUtil.clockSecondsToTicks(clockSeconds);
        String text = Component.translatable("gui.universeclock.tick_info",
                ticks, TimeUtil.TICKS_PER_DAY, TimeUtil.format(TimeUtil.ticksToClockSeconds(ticks))).getString();
        Gfx.fitCenteredText(g, this.font, text, left + PANEL_W / 2f, top + 151, PANEL_W - PAD * 2, 0.85f, 0xFF7F89C0, false);
    }

    private void drawFooter(GuiGraphics g, long ms) {
        float y = top + 209;
        float max = PANEL_W - PAD * 2;
        if (ms < statusUntil) {
            Gfx.fitCenteredText(g, this.font, status.getString(), left + PANEL_W / 2f, y, max, 0.9f, 0xFF7CF29A, true);
        } else {
            Gfx.fitCenteredText(g, this.font, Component.translatable("gui.universeclock.hint").getString(),
                    left + PANEL_W / 2f, y, max, 0.8f, 0xFF6B74A8, false);
        }
    }
}
