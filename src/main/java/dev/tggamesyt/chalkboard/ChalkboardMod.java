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
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

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

                        if (!(level.getBlockState(pos).getBlock() instanceof ChalkboardBlock block)) return;
                        if (!(level.getBlockEntity(pos) instanceof ChalkboardBlockEntity cbe)) return;

                        var stack = player.getMainHandItem();
                        var item = stack.getItem();

                        if (!(item instanceof ChalkItem ||
                                item == Items.SPONGE ||
                                item == Items.WET_SPONGE)) return;

                        var state = level.getBlockState(pos);
                        var facing = state.getValue(ChalkboardBlock.FACING);

                        var relativeHit = payload.hit().subtract(pos.getX(), pos.getY(), pos.getZ());

                        double u = switch (facing) {
                            case SOUTH -> 1.0 - relativeHit.x;
                            case WEST  -> 1.0 - relativeHit.z;
                            case EAST  -> relativeHit.z;
                            default    -> relativeHit.x;
                        };

                        double v = relativeHit.y;

                        int px = Math.max(0, Math.min(15, (int)(u * 16)));
                        int py = 15 - Math.max(0, Math.min(15, (int)(v * 16)));

                        ChalkboardBlock.handleInteraction(
                                level, pos, state, player,
                                net.minecraft.world.InteractionHand.MAIN_HAND,
                                stack, cbe, px, py
                        );
                    });
                }
        );
    }
}