package com.universeclock.client;

import net.minecraft.client.Minecraft;

/** Единственная точка входа в клиентский код из общего кода предмета. */
public final class ClockClientHooks {

    private ClockClientHooks() {
    }

    public static void openClockScreen() {
        Minecraft.getInstance().setScreen(new UniverseClockScreen());
    }
}
