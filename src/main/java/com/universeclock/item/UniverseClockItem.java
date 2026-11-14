package com.universeclock.item;

import java.util.List;

import com.universeclock.client.ClockClientHooks;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class UniverseClockItem extends Item {

    public UniverseClockItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            // Класс клиентский — вызывается только на клиенте, поэтому на сервере не загрузится.
            ClockClientHooks.openClockScreen();
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true; // эффект зачарования
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.universeclock.universe_clock.tooltip").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.universeclock.universe_clock.tooltip2").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
