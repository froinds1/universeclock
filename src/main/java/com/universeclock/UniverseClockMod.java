package com.universeclock;

import com.universeclock.item.UniverseClockItem;
import com.universeclock.network.ServerPayloadHandler;
import com.universeclock.network.SetTimePayload;

import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Часы вселенной — предмет, позволяющий выставить время суток с точностью до секунды.
 */
@Mod(UniverseClockMod.MOD_ID)
public class UniverseClockMod {
    public static final String MOD_ID = "universeclock";

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);

    /** Предмет «Часы вселенной» (universeclock:universe_clock). */
    public static final DeferredItem<Item> UNIVERSE_CLOCK = ITEMS.register("universe_clock",
            () -> new UniverseClockItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public UniverseClockMod(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
        modEventBus.addListener(this::addToCreativeTab);
        modEventBus.addListener(this::registerPayloads);
    }

    private void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(UNIVERSE_CLOCK.get());
        }
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        registrar.playToServer(SetTimePayload.TYPE, SetTimePayload.STREAM_CODEC, ServerPayloadHandler::handleSetTime);
    }
}
