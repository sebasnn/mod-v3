package dev.xmas.overlay.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemDisplayContext;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(HeldItemRenderer.class)
public interface HeldItemRendererInvoker {
	@Invoker("renderArmHoldingItem")
	void xmas$renderArmHoldingItem(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, float equip, float swing, Arm arm);

	@Invoker("applyEquipOffset")
	void xmas$applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress);

	@Invoker("applySwingOffset")
	void xmas$applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);

	@Invoker("renderItem")
	void xmas$renderItem(LivingEntity entity, ItemStack stack, ItemDisplayContext ctx, MatrixStack matrices, OrderedRenderCommandQueue queue, int light);
}
