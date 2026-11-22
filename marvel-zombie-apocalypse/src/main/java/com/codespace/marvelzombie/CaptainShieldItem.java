package com.codespace.marvelzombie;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

public class CaptainShieldItem extends ShieldItem {
    public CaptainShieldItem() {
        super(new Properties().stacksTo(1).durability(672));
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.marvel_zombie.captain_shield.tooltip.block"));
        tooltip.add(Component.translatable("item.marvel_zombie.captain_shield.tooltip.throw"));
    }
}
