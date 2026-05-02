package dev.tggamesyt.chalkboard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ChalkboardMod implements ModInitializer {
    public static final String ID = "chalkboard";

    public static final Identifier USE_PACKET =
            Identifier.fromNamespaceAndPath(ID, "chalkboard_use");

    public static final Block CHALKBOARD = registerBlock("chalkboard", ChalkboardBlock::new, BlockBehaviour.Properties.of().strength(4.0f));

    public static final Item CHALKBOARD_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(ID, "chalkboard"),
            new BlockItem(CHALKBOARD, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "chalkboard"))))
    );

    public static final Item CHALK_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(ID, "chalk"),
            new ChalkItem(new Item.Properties().durability(256).stacksTo(1).setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "chalk"))))
    );

    public static final ResourceKey<CreativeModeTab> ITEM_GROUP_KEY = ResourceKey.create(Registries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath(ID, "itemgroup"));
    public static final CreativeModeTab ITEM_GROUP = FabricCreativeModeTab.builder()
            .icon(() -> new ItemStack(CHALK_ITEM))
            .title(Component.translatable("itemGroup.chalkboard"))
            .build();

    private static Block registerBlock(String path, Function<BlockBehaviour.Properties, Block> factory, BlockBehaviour.Properties settings) {
        final Identifier identifier = Identifier.fromNamespaceAndPath(ID, path);
        final ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, identifier);
        return Blocks.register(key, factory, settings.setId(key));
    }

    private static class LastPoint {
        Vec3 worldHit;
        BlockPos pos;
        long tick;
    }

    private static final Map<Player, LastPoint> LAST_POINTS = new HashMap<>();

    @Override
    public void onInitialize() {
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ITEM_GROUP_KEY, ITEM_GROUP);

        CreativeModeTabEvents.modifyOutputEvent(ITEM_GROUP_KEY).register(entries -> {
            entries.accept(CHALKBOARD_ITEM);
            entries.accept(CHALK_ITEM);
        });

        PayloadTypeRegistry.serverboundPlay().register(
                ChalkboardUsePayload.TYPE,
                ChalkboardUsePayload.CODEC
        );

        ServerPlayNetworking.registerGlobalReceiver(
                ChalkboardUsePayload.TYPE,
                (payload, context) -> {

                    var player = context.player();

                    context.server().execute(() -> {
                        var level = player.level();
                        BlockPos pos = payload.pos();

                        if (!(level.getBlockState(pos).getBlock() instanceof ChalkboardBlock)) return;
                        if (!(level.getBlockEntity(pos) instanceof ChalkboardBlockEntity cbe)) return;

                        var stack = player.getMainHandItem();
                        var item = stack.getItem();

                        if (!(item instanceof ChalkItem ||
                                item == Items.SPONGE ||
                                item == Items.WET_SPONGE)) return;

                        var state = level.getBlockState(pos);
                        var hit = payload.hit();

                        long currentTick = level.getGameTime();

                        LastPoint last = LAST_POINTS.get(player);

                        boolean changedAny = false;

                        if (last != null && currentTick - last.tick <= 1) {

                            // Only interpolate if both hits are roughly on same plane
                            if (last.pos.closerThan(pos, 2.0)) {

                                Vec3 delta = hit.subtract(last.worldHit);
                                int steps = (int)(delta.length() * 16);

                                for (int i = 0; i <= steps; i++) {
                                    Vec3 stepPos = last.worldHit.add(delta.scale(i / (double) steps));
                                    changedAny |= applyAtWorld(level, player, stack, stepPos, i == 0);
                                }

                            } else {
                                changedAny |= applyAtWorld(level, player, stack, hit, true);
                            }

                        } else {
                            changedAny |= applyAtWorld(level, player, stack, hit, true);
                        }

                        LastPoint now = new LastPoint();
                        now.worldHit = hit;
                        now.pos = pos;
                        now.tick = currentTick;
                        LAST_POINTS.put(player, now);

                        if (changedAny) {
                            level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
                        }
                    });
                }
        );
    }

    private static boolean applyAtWorld(Level level, Player player, ItemStack stack, Vec3 hit, boolean allowRadius) {
        BlockPos pos = BlockPos.containing(hit);

        if (!(level.getBlockState(pos).getBlock() instanceof ChalkboardBlock)) return false;
        if (!(level.getBlockEntity(pos) instanceof ChalkboardBlockEntity cbe)) return false;

        var state = level.getBlockState(pos);
        var facing = state.getValue(ChalkboardBlock.FACING);

        var relative = hit.subtract(pos.getX(), pos.getY(), pos.getZ());

        double u = switch (facing) {
            case SOUTH -> 1.0 - relative.x;
            case WEST  -> 1.0 - relative.z;
            case EAST  -> relative.z;
            default    -> relative.x;
        };

        double v = relative.y;

        int px = Math.max(0, Math.min(15, (int)(u * 16)));
        int py = 15 - Math.max(0, Math.min(15, (int)(v * 16)));

        var item = stack.getItem();

        if (item == Items.WET_SPONGE && !allowRadius) return false;

        int before = cbe.getPixels()[py * 16 + px];

        ChalkboardBlock.handleInteraction(
                level,
                pos,
                state,
                player,
                net.minecraft.world.InteractionHand.MAIN_HAND,
                stack,
                cbe,
                px,
                py
        );

        int after = cbe.getPixels()[py * 16 + px];

        return before != after;
    }
}
