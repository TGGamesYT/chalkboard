package dev.tggamesyt.chalkboard.client;

import dev.tggamesyt.chalkboard.chalkboard.ChalkItem;
import dev.tggamesyt.chalkboard.chalkboard.ChalkboardBlockEntity;
import dev.tggamesyt.chalkboard.chalkboard.ChalkboardMod;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderers;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.level.ColorResolver;

import javax.swing.colorchooser.ColorChooserComponentFactory;
import javax.swing.plaf.ColorUIResource;

public class ChalkboardClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockEntityRenderers.register(
				ChalkboardBlockEntity.TYPE,
				ChalkboardBlockEntityRenderer::new
		);

	}
}