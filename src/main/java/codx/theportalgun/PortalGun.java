package codx.theportalgun;

import codx.ThePortalGun;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import org.jspecify.annotations.Nullable;

/**
 * Firing the gun: where the shot lands, and whether a portal can go there.
 *
 * <p>An upright portal needs two blocks of wall with room to stand in front of them; one on a
 * floor or ceiling needs a single block with room in front. Refusing loudly when it will not
 * fit is most of what makes the gun feel fair — a shot that silently does nothing reads as
 * the mod being broken, where "that surface is too small" reads as the player having missed.
 */
public final class PortalGun {
	/** How far the gun shoots, in blocks. Far enough to cross a room and not a valley. */
	private static final double RANGE = 64.0;

	private PortalGun() {
	}

	/** Fires one end of the pair. Called from both mouse buttons, with a different colour. */
	public static void fire(ServerPlayer player, boolean isBlue) {
		ServerLevel level = player.level();
		PortalPair pair = PortalPair.of(player.getUUID());

		BlockHitResult hit = trace(player);
		Direction facing = hit.getDirection();
		BlockPos anchor = hit.getType() == HitResult.Type.BLOCK
				? anchorFor(level, hit.getBlockPos(), facing)
				: null;

		if (anchor == null) {
			refuse(player, hit.getType() == HitResult.Type.BLOCK
					? "No room for a portal there"
					: "Nothing in range to put a portal on");
			return;
		}

		// A pair the library cannot express is not refused — that would strand a player whose
		// two portals are on a floor and a ceiling, since neither end could then be moved to
		// a wall. The shot lands and the other end closes, which is what the player is about
		// to want anyway.
		boolean closedTheOther = !pair.joinable(isBlue, facing);

		if (closedTheOther) {
			pair.drop(!isBlue, level.getServer());
		}

		pair.shoot(isBlue, level, anchor, facing);

		level.playSound(null, anchor, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F,
				isBlue ? 1.4F : 0.8F);
		player.sendSystemMessage(Component.literal(
				(isBlue ? "Blue" : "Orange") + " portal placed"
						+ (pair.linked() ? " — the pair is open" : ", shoot the other one"))
				.withStyle(isBlue ? ChatFormatting.AQUA : ChatFormatting.GOLD));

		if (closedTheOther) {
			refuse(player, "The other portal closed: one on a floor only pairs with one on "
					+ "a ceiling, and an upright one only with another upright one");
		}
	}

	/** Where the player is pointing, out to {@link #RANGE}. */
	private static BlockHitResult trace(Player player) {
		Vec3 from = player.getEyePosition();
		Vec3 to = from.add(player.getViewVector(1.0F).scale(RANGE));
		return player.level().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
				ClipContext.Fluid.NONE, player));
	}

	/**
	 * The block whose face the portal will lie on, or null if it does not fit.
	 *
	 * <p>Shot at the middle of a wall, a portal should hang where it was aimed; shot near the
	 * floor it should sit on the floor rather than half sink into it. So the block that was
	 * hit is tried as the bottom of the pair first and then as the top — which is what lets
	 * you put one at your feet and one at head height on the same wall.
	 */
	private static @Nullable BlockPos anchorFor(Level level, BlockPos hit, Direction facing) {
		if (facing.getAxis().isVertical()) {
			// A floor or a ceiling: one block, and it is whichever one was shot.
			return standingRoom(level, hit, facing) ? hit : null;
		}

		if (upright(level, hit, facing)) {
			return hit;
		}

		BlockPos lower = hit.below();
		return upright(level, lower, facing) ? lower : null;
	}

	/** Whether two blocks of wall stand here, with room in front of both. */
	private static boolean upright(Level level, BlockPos bottom, Direction facing) {
		for (int up = 0; up < 2; up++) {
			if (!standingRoom(level, bottom.above(up), facing)) {
				return false;
			}
		}

		return true;
	}

	/** Whether this block is surface to hang a portal on, with space to step into in front. */
	private static boolean standingRoom(Level level, BlockPos surface, Direction facing) {
		// Something to hang it on. A portal in mid-air is not a portal, it is a hoop.
		if (level.getBlockState(surface).canBeReplaced()) {
			return false;
		}

		// And somewhere to arrive, since walking out of the far end puts you right here.
		return level.getBlockState(surface.relative(facing)).canBeReplaced();
	}

	private static void refuse(ServerPlayer player, String why) {
		player.sendSystemMessage(Component.literal(why).withStyle(ChatFormatting.GRAY));
		ThePortalGun.LOGGER.debug("{} could not place a portal: {}",
				player.getName().getString(), why);
	}
}
