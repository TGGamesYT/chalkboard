package dev.tggamesyt.chalkboard.chalkboard;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;

import java.util.function.Function;

public class ChalkboardMod implements ModInitializer {
    public static final String ID = "chalkboard";

    // 1. Register Block
    public static final Block CHALKBOARD = registerBlock("chalkboard", ChalkboardBlock::new, BlockBehaviour.Properties.of().strength(4.0f));

    // 2. Register Items with .setId()
    public static final Item CHALKBOARD_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(ID, "chalkboard"),
            new BlockItem(CHALKBOARD, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "chalkboard"))))
    );

    public static final Item CHALK_ITEM = Registry.register(
            BuiltInRegistries.ITEM,
            Identifier.fromNamespaceAndPath(ID, "chalk"),
            new ChalkItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(ID, "chalk"))))
    );

    // 3. Creative Tab
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
        // Registry the group
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, ITEM_GROUP_KEY, ITEM_GROUP);

        // Add to group (essential for the items to be "visible" in the UI)
        CreativeModeTabEvents.modifyOutputEvent(ITEM_GROUP_KEY).register(entries -> {
            entries.accept(CHALKBOARD_ITEM);
            entries.accept(CHALK_ITEM);
        });
    }
}