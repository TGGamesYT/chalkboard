package dev.tggamesyt.chalkboard.client;

import dev.tggamesyt.chalkboard.ChalkItem;
import dev.tggamesyt.chalkboard.ChalkboardBlockEntity;
import dev.tggamesyt.chalkboard.ChalkboardMod;
import dev.tggamesyt.chalkboard.ChalkboardUsePayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.phys.BlockHitResult;

import javax.swing.colorchooser.ColorChooserComponentFactory;
import javax.swing.plaf.ColorUIResource;

public class ChalkboardClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRenderers.register(
				ChalkboardBlockEntity.TYPE,
				ChalkboardBlockEntityRenderer::new
		);
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (client.player == null || client.level == null) return;

			if (!client.options.keyUse.isDown()) return;

			if (!(client.hitResult instanceof BlockHitResult hit)) return;

			ItemStack stack = client.player.getMainHandItem();
			var item = stack.getItem();

			if (!(item instanceof ChalkItem ||
					item == Items.SPONGE ||
					item == Items.WET_SPONGE)) return;

			// only send if looking at chalkboard
			if (!(client.level.getBlockState(hit.getBlockPos()).getBlock()
					instanceof dev.tggamesyt.chalkboard.ChalkboardBlock)) return;

			ClientPlayNetworking.send(
					new ChalkboardUsePayload(hit.getBlockPos(), hit.getLocation())
			);
		});
		ThirdpersonModelRegisterer.register(
				Identifier.fromNamespaceAndPath(ChalkboardMod.ID, "chalk"),
				Identifier.fromNamespaceAndPath(ChalkboardMod.ID, "chalk_3d")
		);
	}
}