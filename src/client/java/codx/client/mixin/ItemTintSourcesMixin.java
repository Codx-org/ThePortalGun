package codx.client.mixin;

import codx.theportalgun.client.RainbowTint;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.color.item.ItemTintSources;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;

import com.mojang.serialization.MapCodec;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds {@code the-portal-gun:rainbow} to the item tint sources the game knows about.
 *
 * There is no registry event for this: the id-to-codec map is a private field filled in by
 * {@code bootstrap}, so the entry goes in right after vanilla's, before any item definition
 * is parsed.
 */
@Mixin(ItemTintSources.class)
public class ItemTintSourcesMixin {
	@Shadow @Final
	private static ExtraCodecs.LateBoundIdMapper<Identifier, MapCodec<? extends ItemTintSource>> ID_MAPPER;

	@Inject(method = "bootstrap", at = @At("TAIL"))
	private static void theportalgun$addRainbow(CallbackInfo info) {
		ID_MAPPER.put(RainbowTint.ID, RainbowTint.MAP_CODEC);
	}
}
