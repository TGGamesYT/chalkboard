package dev.tggamesyt.chalkboard;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;
import java.util.function.Consumer;

public class ChalkItem extends Item {

    public static final int DEFAULT_COLOR = 0xF9FFFE;

    public ChalkItem(Properties props) {
        super(props);
    }

    public static int getColor(ItemStack stack) {
        DyedItemColor c = stack.get(DataComponents.DYED_COLOR);
        return (c != null ? c.rgb() : DEFAULT_COLOR) | 0xFF000000;
    }

    public static void applyDye(ItemStack stack, DyeColor dye) {
        stack.set(DataComponents.DYED_COLOR,
            new DyedItemColor(dye.getTextureDiffuseColor()));
    }

    public static void applyRgb(ItemStack stack, int rgb) {
        stack.set(DataComponents.DYED_COLOR,
            new DyedItemColor(rgb | 0xFF000000));
    }

    // ── Tooltip ───────────────────────────────────────────────────────────────

    @Override
    public void appendHoverText(final ItemStack stack, final TooltipContext context, final TooltipDisplay display, final Consumer<Component> builder, final TooltipFlag tooltipFlag) {
        int argb = getColor(stack);
        String hex = String.format("#%06X", argb & 0xFFFFFF);
        builder.accept(Component.literal("Color: " + hex).withStyle(ChatFormatting.GRAY));
    }
}
