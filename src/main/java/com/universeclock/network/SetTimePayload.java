package com.universeclock.network;

import com.universeclock.UniverseClockMod;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/** Клиент -> сервер: «установи время суток на этот тик (0..23999)». */
public record SetTimePayload(int ticks) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SetTimePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(UniverseClockMod.MOD_ID, "set_time"));

    public static final StreamCodec<ByteBuf, SetTimePayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            SetTimePayload::ticks,
            SetTimePayload::new);

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
