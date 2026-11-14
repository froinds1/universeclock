package com.universeclock.network;

import com.universeclock.TimeUtil;
import com.universeclock.UniverseClockMod;

import net.minecraft.network.protocol.game.ClientboundSetTimePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameRules;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class ServerPayloadHandler {

    private ServerPayloadHandler() {
    }

    public static void handleSetTime(final SetTimePayload payload, final IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }

            // Защита от «читерских» пакетов: часы должны быть в руке игрока.
            boolean holdsClock = player.getMainHandItem().is(UniverseClockMod.UNIVERSE_CLOCK.get())
                    || player.getOffhandItem().is(UniverseClockMod.UNIVERSE_CLOCK.get());
            if (!holdsClock) {
                return;
            }

            MinecraftServer server = player.getServer();
            if (server == null) {
                return;
            }

            int target = Math.floorMod(payload.ticks(), TimeUtil.TICKS_PER_DAY);

            // Сохраняем номер текущих суток (фаза луны и счётчик дней не сбиваются),
            // меняем только время внутри суток.
            ServerLevel overworld = server.overworld();
            long current = overworld.getDayTime();
            long dayStart = current - Math.floorMod(current, (long) TimeUtil.TICKS_PER_DAY);
            overworld.setDayTime(dayStart + target);

            // Сразу сообщаем клиентам, чтобы небо поменялось без задержки.
            for (ServerLevel level : server.getAllLevels()) {
                ClientboundSetTimePacket packet = new ClientboundSetTimePacket(
                        level.getGameTime(),
                        level.getDayTime(),
                        level.getGameRules().getBoolean(GameRules.RULE_DAYLIGHT));
                for (ServerPlayer other : level.players()) {
                    other.connection.send(packet);
                }
            }
        });
    }
}
