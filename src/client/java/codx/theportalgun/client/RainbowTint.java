package codx.theportalgun.client;

import com.mojang.serialization.MapCodec;

import net.minecraft.client.color.item.ItemTintSource;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * The gun's colour, cycled through the spectrum a few times a second.
 *
 * In 26.3 an item's colour is data driven: the item's definition in
 * {@code assets/<mod>/items/<item>.json} lists tint sources by name, and each name maps to
 * a codec in {@link net.minecraft.client.color.item.ItemTintSources}. That map is private
 * and vanilla-only, so {@code ItemTintSourcesMixin} adds this one to it.
 *
 * The texture it tints is grey, so multiplying by a colour gives the whole gun that colour.
 */
public record RainbowTint() implements ItemTintSource {
	public static final Identifier ID = Identifier.fromNamespaceAndPath("the-portal-gun", "rainbow");

	/** No settings, so the codec reads and writes an empty object. */
	public static final MapCodec<RainbowTint> MAP_CODEC = MapCodec.unit(RainbowTint::new);

	/** How long one trip around the colour wheel takes. */
	private static final float CYCLE_MILLIS = 3000.0F;

	@Override
	public int calculate(ItemStack stack, ClientLevel level, LivingEntity holder) {
		// Wall clock rather than game time: the gun keeps shifting in menus and while paused,
		// and nothing about its colour has to agree between players.
		float hue = (System.currentTimeMillis() % (long) CYCLE_MILLIS) / CYCLE_MILLIS;

		return ARGB.opaque(hsvToRgb(hue));
	}

	@Override
	public MapCodec<? extends ItemTintSource> type() {
		return MAP_CODEC;
	}

	/** Full-saturation, full-value hue to packed RGB. */
	private static int hsvToRgb(float hue) {
		float h = hue * 6.0F;
		int sector = (int) h;
		float f = h - sector;

		float rising = f;
		float falling = 1.0F - f;

		return switch (sector % 6) {
			case 0 -> pack(1.0F, rising, 0.0F);
			case 1 -> pack(falling, 1.0F, 0.0F);
			case 2 -> pack(0.0F, 1.0F, rising);
			case 3 -> pack(0.0F, falling, 1.0F);
			case 4 -> pack(rising, 0.0F, 1.0F);
			default -> pack(1.0F, 0.0F, falling);
		};
	}

	private static int pack(float red, float green, float blue) {
		return ((int) (red * 255.0F) << 16) | ((int) (green * 255.0F) << 8) | (int) (blue * 255.0F);
	}
}
