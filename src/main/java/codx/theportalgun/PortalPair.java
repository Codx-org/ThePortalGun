package codx.theportalgun;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import codx.ThePortalGun;

import codx.planeshift.plane.Plane;
import codx.planeshift.plane.PlaneBoundary;
import codx.planeshift.plane.PlaneShape;
import codx.planeshift.registry.Planes;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * The two portals one player has out, and the doorway between them.
 *
 * <p>This is the whole of how the mod talks to Planeshift, and it is the third shape a
 * consumer can take. Actual Portals <em>finds</em> its planes by looking at blocks, chunk
 * by chunk. OneDimension <em>declares</em> its planes once, from where the worlds end. This
 * one simply <em>remembers</em> two of them, because they exist for no reason except that
 * somebody shot them there — there is nothing in the world to read them back from.
 *
 * <p>Which is why the provider here is global rather than per chunk: a chunk provider is
 * for planes you would have to go and look for. These are already known, so the answer is
 * a lookup rather than a search.
 *
 * <p>A pair is only a doorway when both ends exist. One portal on its own is remembered and
 * yields nothing, exactly as a nether portal with no far side does — a plane whose far end
 * is undecided is worse than no plane, because the view through it would have to guess.
 */
public final class PortalPair {
	/** How tall an upright portal is, in blocks. Two is a doorway you can walk through. */
	private static final int HEIGHT = 2;

	/**
	 * How far in front of the surface the portal is drawn, in blocks — a negative inset,
	 * because an inset measures inwards.
	 *
	 * <p>It has to be in front of the wall rather than flush with it. The far side is shown
	 * wherever the opening is nearer to the eye than whatever this world drew there, and
	 * flush with the wall those two are the same distance: the wall wins the tie and the
	 * doorway is a picture of bricks. A hundredth of a block is invisible and settles it.
	 */
	private static final double PROUD = -0.01;

	/**
	 * What each end is drawn to look like.
	 *
	 * <p>Only a name. Planeshift syncs it with the plane and hands it to whatever the
	 * client has registered under it — PortalRim, here — and knows nothing about what
	 * blue means. A client without this mod sees a plain opening and can still walk
	 * through it.
	 */
	public static final Identifier BLUE = ThePortalGun.id("blue");
	public static final Identifier ORANGE = ThePortalGun.id("orange");

	private static final Map<UUID, PortalPair> BY_PLAYER = new HashMap<>();

	private @Nullable Portal blue;
	private @Nullable Portal orange;

	/**
	 * One portal: the surface it sits on, and which way it opens.
	 *
	 * <p>The plane covers the <em>surface</em> blocks — the wall, not the air in front of
	 * it — and its own surface sits a hair in front of their face. Nothing is dug out to
	 * make room: Planeshift lets everything through the blocks a portal's opening covers,
	 * so the wall stays where the player built it and is whole again the moment the portal
	 * moves on.
	 *
	 * @param level  the dimension it was shot in
	 * @param anchor for an upright portal the lower of its two blocks; for one on a floor
	 *               or ceiling the single block whose face it lies on
	 * @param facing the way it opens — out of the surface, towards whoever shot it
	 */
	private record Portal(ResourceKey<Level> level, BlockPos anchor, Direction facing) {
		/** The blocks of the opening, as a plane shape. */
		PlaneShape shape() {
			// Upright portals are two blocks tall; ones on a floor or ceiling are a single
			// block, because a rectangle has to be flat along the axis it faces.
			return facing.getAxis().isVertical()
					? new PlaneShape.Rectangle(anchor, anchor, PROUD)
					: new PlaneShape.Rectangle(anchor, anchor.above(HEIGHT - 1), PROUD);
		}
	}

	private PortalPair() {
	}

	/** Registers the one provider this mod needs. Called once, from the mod's entry point. */
	public static void register() {
		// Global rather than per chunk: these planes are held in memory, not found in the
		// world, so there is nothing to search for and no chunk to index them by.
		Planes.registerGlobal((level, out) ->
				BY_PLAYER.values().forEach(pair -> pair.planesIn(level.dimension(), out)));
	}

	/** The pair belonging to this player, made if they have not shot anything yet. */
	public static PortalPair of(UUID player) {
		return BY_PLAYER.computeIfAbsent(player, key -> new PortalPair());
	}

	/** Forgets a player's portals, for when they leave. */
	public static void forget(MinecraftServer server, UUID player) {
		if (BY_PLAYER.remove(player) != null) {
			Planes.invalidateGlobal();
		}
	}

	/** Forgets everybody's portals, for shutting a server down. */
	public static void forgetAll(MinecraftServer server) {
		BY_PLAYER.clear();
		Planes.invalidateGlobal();
	}

	/** Puts one end of the pair here, replacing wherever it was. */
	public void shoot(boolean isBlue, ServerLevel level, BlockPos anchor, Direction facing) {
		Portal portal = new Portal(level.dimension(), anchor, facing);

		if (isBlue) {
			blue = portal;
		} else {
			orange = portal;
		}

		// Global planes are worked out once and remembered, so a provider answering from
		// memory has to say when the memory changed. Every level, because the pair's two
		// ends can be in two of them and either may have just lost its plane.
		Planes.invalidateGlobal();
	}

	/**
	 * Whether a portal facing this way could be joined to the pair's other end.
	 *
	 * <p>A plane's transform turns about the vertical axis and nothing else, so walking into
	 * one opening and out of another only works out when a yaw can take one to the other.
	 * Two upright portals always can. A floor and a ceiling can, because falling into the
	 * one and out of the other is the same direction of travel and needs no turn at all. A
	 * floor and a wall cannot: you would arrive travelling downwards out of an opening whose
	 * far side is sideways, which puts you inside whatever the wall is standing on.
	 *
	 * <p>So the gun refuses the shot rather than making a doorway that swallows people. This
	 * is the mod admitting a limit of the library it is built on, in the one place where a
	 * player would otherwise meet it as a bug.
	 */
	public boolean joinable(boolean isBlue, Direction facing) {
		Portal far = isBlue ? orange : blue;

		if (far == null) {
			return true;
		}

		if (facing.getAxis().isVertical() != far.facing().getAxis().isVertical()) {
			return false;
		}

		return !facing.getAxis().isVertical() || facing == far.facing().getOpposite();
	}

	/** Takes one end away entirely. */
	public void drop(boolean isBlue, MinecraftServer server) {
		if (isBlue) {
			blue = null;
		} else {
			orange = null;
		}

		Planes.invalidateGlobal();
	}

	/** Whether both ends exist, and so whether there is a doorway at all. */
	public boolean linked() {
		return blue != null && orange != null;
	}

	/**
	 * This pair's plane in one dimension, if it has one there.
	 *
	 * <p>Both ends are asked for separately, because the two may be in different dimensions —
	 * a portal shot in the Overworld and one in the Nether is a perfectly good pair, and the
	 * far side of each is simply somewhere further away than usual.
	 */
	private void planesIn(ResourceKey<Level> dimension, Consumer<Plane> out) {
		if (!linked()) {
			// One end on its own is still worth telling the client about: a portal you have
			// placed and not yet paired should be visible where you put it, closed, rather
			// than being nothing at all until the second shot lands. It goes as a plane with
			// no transform — a surface rather than a portal — which is the difference the
			// rim reads to know whether to draw the way through or the shut face of one.
			// Being no portal, it also opens no blocks and carries nobody: shut means shut.
			closed(blue, BLUE, dimension, out);
			closed(orange, ORANGE, dimension, out);
			return;
		}

		PlaneBoundary boundary = PlaneBoundary.linking(
				blue.level(), blue.shape(), blue.facing(),
				orange.level(), orange.shape(), orange.facing())
				.looking(BLUE, ORANGE);

		// linking() works out the turn for us: walking into one comes out of the other
		// facing away from it, which is exactly how a portal gun's portals behave.
		if (blue.level().equals(dimension)) {
			out.accept(boundary.a());
		}

		if (orange.level().equals(dimension)) {
			out.accept(boundary.b());
		}
	}

	/** The one end that exists, as a plain surface, if it is in this dimension. */
	private static void closed(@Nullable Portal portal, Identifier looks,
			ResourceKey<Level> dimension, Consumer<Plane> out) {
		if (portal != null && portal.level().equals(dimension)) {
			out.accept(new Plane(portal.shape(), portal.facing(), Optional.empty()).looking(looks));
		}
	}

	/** Where the far end of this pair is, for telling the player what they have done. */
	public @Nullable BlockPos other(boolean fromBlue) {
		Portal far = fromBlue ? orange : blue;
		return far == null ? null : far.anchor();
	}
}
