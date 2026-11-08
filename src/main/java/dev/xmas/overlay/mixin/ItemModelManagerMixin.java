package dev.xmas.overlay.mixin;

import dev.xmas.overlay.Skin;
import net.minecraft.client.item.ItemModelManager;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Gives the totem the model of the skin picked in the menu, everywhere the item is drawn. */
@Mixin(ItemModelManager.class)
public class ItemModelManagerMixin {
	@ModifyVariable(method = "update", at = @At("HEAD"), argsOnly = true)
	private ItemStack xmas$swapUpdate(ItemStack stack) { return Skin.swap(stack); }

	@ModifyVariable(method = "clearAndUpdate", at = @At("HEAD"), argsOnly = true)
	private ItemStack xmas$swapClear(ItemStack stack) { return Skin.swap(stack); }
}
