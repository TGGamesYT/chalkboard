package dev.tggamesyt.chalkboard.chalkboard;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.TooltipDisplay;

import java.util.List;
import java.util.function.Consumer;

/**
 * A chalk item coloured exactly like leather armour:
 *   - One texture (white), tinted in-game via DyedItemColor.
 *   - Default colour: white (0xF9FFFE — the same white as white concrete).
 *   - Craft with any dye to recolour.  The stored int is full 24-bit RGB,
 *     so /give commands or other mods can set any arbitrary colour.
 */
public class ChalkItem extends Item {

    /** Default chalk colour — plain white. */
    public static final int DEFAULT_COLOR = 0xF9FFFE;

    public ChalkItem(Properties props) {
        super(props);
    }

    // ── Color helpers ─────────────────────────────────────────────────────────

    /** Packed ARGB color of this chalk stack (alpha always 0xFF). */
    public static int getColor(ItemStack stack) {
        DyedItemColor c = stack.get(DataComponents.DYED_COLOR);
        return (c != null ? c.rgb() : DEFAULT_COLOR) | 0xFF000000;
    }

    /** Apply a vanilla DyeColor's exact RGB to a chalk stack in-place. */
    public static void applyDye(ItemStack stack, DyeColor dye) {
        // getTextureDiffuseColor() returns the packed 0xAARRGGBB colour
        // used by leather armour — reuse it directly so colours look identical.
        stack.set(DataComponents.DYED_COLOR,
            new DyedItemColor(dye.getTextureDiffuseColor()));
    }

    /** Apply any arbitrary 24-bit RGB (for commands / creative palette tools). */
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
