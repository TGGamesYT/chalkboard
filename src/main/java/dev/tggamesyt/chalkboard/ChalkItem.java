package dev.tggamesyt.chalkboard;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.Consumable;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class ChalkItem extends Item {

    public static final int DEFAULT_COLOR = 0xF9FFFE;

    public ChalkItem(Properties props) {
        super(props);
    }

    public static int getColor(ItemStack stack) {
        DyedItemColor c = stack.get(DataComponents.DYED_COLOR);
        int color = (c != null ? c.rgb() : DEFAULT_COLOR);

        return color & 0xFFFFFF;
    }


    private static @Nullable DyeColor getMatchingDye(int rgb) {
        rgb &= 0xFFFFFF;

        DyeColor best = null;
        double bestDist = Double.MAX_VALUE;

        for (DyeColor dye : DyeColor.values()) {
            int d = dye.getTextureDiffuseColor() & 0xFFFFFF;

            double dist =
                    Math.pow(((rgb >> 16) & 0xFF) - ((d >> 16) & 0xFF), 2) +
                            Math.pow(((rgb >> 8) & 0xFF) - ((d >> 8) & 0xFF), 2) +
                            Math.pow((rgb & 0xFF) - (d & 0xFF), 2);

            if (dist < bestDist) {
                bestDist = dist;
                best = dye;
            }
        }

        // threshold so random RGB doesn't become a dye
        return bestDist < 2000 ? best : null;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                TooltipDisplay display,
                                Consumer<Component> builder,
                                TooltipFlag tooltipFlag) {

        // 🔥 Prevent duplicate tooltip calls
        if (tooltipFlag.isAdvanced()) return;

        int rgb = getColor(stack);
        DyeColor dye = getMatchingDye(rgb);

        if (dye != null) {
            builder.accept(Component.literal("Color: " + capitalize(dye.getName()))
                    .withStyle(ChatFormatting.GRAY));
        } else {
            String hex = String.format("#%06X", rgb);
            builder.accept(Component.literal("Color: " + hex)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static String capitalize(String s) {
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
