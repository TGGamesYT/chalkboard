package dev.tggamesyt.chalkboard.client.mixin;


import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.Identifier;

import net.minecraft.world.level.Level;
import org.lwjgl.system.SharedLibrary;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import dev.tggamesyt.chalkboard.client.ThirdpersonModelRegisterer;

@Mixin(ItemModelResolver.class)
public abstract class ItemRendererMixin {

    @ModifyVariable(
            method = "appendItemLayers",
            at = @At(
                    value = "STORE",
                    ordinal = 0
            )
    )
    private Identifier chalkboard$swapModel(
            Identifier original,
            ItemStackRenderState output,
            ItemStack item,
            ItemDisplayContext displayContext,
            Level level,
            ItemOwner owner,
            int seed
    ) {
        Identifier custom = ThirdpersonModelRegisterer.get(item.getItem());
        if (custom == null) return original;

        boolean allowed =
                displayContext == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ||
                        displayContext == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ||
                        displayContext == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ||
                        displayContext == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ||
                        displayContext == ItemDisplayContext.HEAD;

        if (!allowed) return original;

        return custom;
    }
}