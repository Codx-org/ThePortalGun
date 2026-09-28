package codx.theportalgun;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/**
 * The gun.
 *
 * <p>Right click fires the orange portal; left click fires the blue one. Left click is not
 * something an item can answer on its own — attacking is the block's business, not the
 * item's — so it arrives through an event instead, wired up in the mod's entry point.
 *
 * <p>Both buttons are answered on the server. The client asking for a portal and the server
 * deciding where it goes would be two answers to one question, and the far side is drawn
 * from whatever the server decided.
 */
public class PortalGunItem extends Item {
	public PortalGunItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (player instanceof ServerPlayer shooter) {
			PortalGun.fire(shooter, false);
		}

		// Consumed either way, so the swing is played and nothing else takes the click.
		return InteractionResult.SUCCESS;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		// Right-clicking a block comes here rather than to use(), so it is sent on to the
		// same place — otherwise the gun would fire in the air and do nothing up close.
		if (context.getPlayer() instanceof ServerPlayer shooter) {
			PortalGun.fire(shooter, false);
		}

		return InteractionResult.SUCCESS;
	}
}
