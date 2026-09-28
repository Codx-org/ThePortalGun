package codx.theportalgun.client;

import codx.planeshift.client.appearance.PlaneAppearance;
import codx.planeshift.plane.Plane;
import codx.planeshift.plane.PlaneShape;

import net.minecraft.client.Minecraft;
import net.minecraft.core.Direction;
import net.minecraft.gizmos.GizmoStyle;
import net.minecraft.gizmos.Gizmos;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * The ring of light around a portal: blue at one end, orange at the other, turning.
 *
 * <p>It is what makes a pair readable. Two openings that look alike leave you working out
 * which one you are standing at from what is behind it, which is the one thing you cannot
 * see while you are standing in it. A colour on the rim answers that from across the room,
 * and matching it to the button that made the portal is the whole of this game's language
 * for which is which.
 *
 * <p>The ring is not one band but a chain of short ones, each lit by how far round the edge
 * it sits and what time it is. Two bright places travel round it and the rest glows softly,
 * which is what a still picture cannot tell you and the eye reads instantly: this is a way
 * through, and it is on.
 *
 * <p>Drawn just <em>outside</em> the opening, never on it. Planeshift composites the far
 * side over everything inside the opening after the world has been drawn, so anything
 * painted within the frame is painted over; a band a few hundredths of a block out sits on
 * the wall around the hole and reads as a rim rather than as a border drawn on a picture.
 */
public final class PortalRim implements PlaneAppearance {
	/** Portal blue and portal orange, as light rather than as paint. */
	public static final int BLUE = 0x44CCFF;
	public static final int ORANGE = 0xFF8C14;

	/** How wide the turning ring is, in blocks. */
	private static final double EDGE = 0.09;

	/** And the soft halo around it, which does not turn. */
	private static final double GLOW = 0.16;

	/** How long a piece of the ring is, in blocks. Shorter is smoother and costs more quads. */
	private static final double STEP = 0.2;

	/** How bright the ring is where nothing is passing, and how much a passing light adds. */
	private static final int DIM_ALPHA = 0x50;
	private static final int BRIGHT_ALPHA = 0xFF;

	private static final int GLOW_ALPHA = 0x40;

	/** How many bright places travel round at once, and how long one lap takes. */
	private static final int ARMS = 2;
	private static final float LAP = 2600.0F;

	/**
	 * How sharply a bright place falls off. Higher is a smaller, harder spark; this is wide
	 * enough to read as light moving through the ring rather than a bead sliding along it.
	 */
	private static final int FALLOFF = 4;

	/** How far off the surface the ring is drawn, so it does not fight the wall for depth. */
	private static final double OFF_SURFACE = 0.02;

	/** The wash across the opening while there is nothing behind it to show yet. */
	private static final int WAITING_ALPHA = 0x38;

	/**
	 * And the face of a portal that has been placed but has nowhere to lead yet.
	 *
	 * <p>Nearly solid on purpose. A portal with no partner is shut — nothing goes through it
	 * — and it should look shut, so that shooting the first one shows you exactly where it
	 * went while leaving no doubt that it is not a way anywhere yet.
	 */
	private static final int CLOSED_ALPHA = 0xD8;

	private final int color;

	public PortalRim(int color) {
		this.color = color;
	}

	@Override
	public void draw(Plane plane, Minecraft client, boolean shown) {
		if (!(plane.shape() instanceof PlaneShape.Rectangle rect)) {
			// The gun only ever makes rectangles; anything else is not ours to draw.
			return;
		}

		Direction.Axis normal = plane.facing().getAxis();
		double u0 = first(rect.min(), normal);
		double u1 = first(rect.max(), normal);
		double v0 = second(rect.min(), normal);
		double v1 = second(rect.max(), normal);
		float phase = (System.currentTimeMillis() % (long) LAP) / LAP;

		// Both faces of the surface. A portal is crossed from either side, and it sits on a
		// wall you can walk round, so the far face is one you meet as often.
		for (int side = -1; side <= 1; side += 2) {
			double surface = plane.surface() + OFF_SURFACE * side;

			halo(plane, surface, u0, v0, u1, v1);
			ring(plane, surface, u0, v0, u1, v1, phase);

			if (!plane.isPortal()) {
				// Placed, but with nothing to pair with: the shut face of a portal.
				quad(plane, surface, u0, v0, u1, v1, GizmoStyle.fill((CLOSED_ALPHA << 24) | color));
			} else if (!shown) {
				// Open, but nothing streamed behind it yet: a wash of the same colour says
				// "a way through, not ready" where an untouched wall would say "nothing
				// here". It is covered by the far side the moment there is one.
				quad(plane, surface, u0, v0, u1, v1, GizmoStyle.fill((WAITING_ALPHA << 24) | color));
			}
		}
	}

	/** The soft, still band outside the turning one. */
	private void halo(Plane plane, double surface, double u0, double v0, double u1, double v1) {
		GizmoStyle style = GizmoStyle.fill((GLOW_ALPHA << 24) | color);
		double out = EDGE + GLOW;

		quad(plane, surface, u0 - out, v1, u1 + out, v1 + out, style);
		quad(plane, surface, u0 - out, v0 - out, u1 + out, v0, style);
		quad(plane, surface, u0 - out, v0, u0, v1, style);
		quad(plane, surface, u1, v0, u1 + out, v1, style);
	}

	/**
	 * The turning ring, as a chain of short pieces.
	 *
	 * <p>Walked as one loop rather than four sides so that the light runs round the corners
	 * without stopping: each piece knows how far along the whole edge it is, and that is the
	 * only thing its brightness depends on.
	 */
	private void ring(Plane plane, double surface, double u0, double v0, double u1, double v1,
			float phase) {
		double lo = u0 - EDGE;
		double hi = u1 + EDGE;
		double width = hi - lo;
		double height = v1 - v0;
		double perimeter = 2.0 * (width + height);
		double travelled = 0.0;

		// Anticlockwise from the bottom left: along the bottom, up the far side, back along
		// the top, down the near side.
		travelled = side(plane, surface, phase, perimeter, travelled,
				lo, v0 - EDGE, hi, v0, true);
		travelled = side(plane, surface, phase, perimeter, travelled,
				u1, v0, hi, v1, false);
		travelled = side(plane, surface, phase, perimeter, travelled,
				hi, v1, lo, v1 + EDGE, true);
		side(plane, surface, phase, perimeter, travelled,
				u0, v1, lo, v0, false);
	}

	/**
	 * One side of the ring, cut into pieces and lit.
	 *
	 * <p>{@code alongFirst} says which way this side runs, and the corners from which it is
	 * given may be in either order — going round a loop means two of the four sides are
	 * walked backwards, and that is what keeps the light travelling one way.
	 *
	 * @return how far round the edge the walk has got by the end of this side
	 */
	private double side(Plane plane, double surface, float phase, double perimeter, double from,
			double a0, double b0, double a1, double b1, boolean alongFirst) {
		double length = Math.abs(alongFirst ? a1 - a0 : b1 - b0);
		int pieces = Math.max(1, (int) Math.round(length / STEP));

		for (int i = 0; i < pieces; i++) {
			double start = (double) i / pieces;
			double end = (double) (i + 1) / pieces;
			double middle = (from + length * (start + end) * 0.5) / perimeter;

			GizmoStyle style = GizmoStyle.fill((brightness(middle, phase) << 24) | color);

			if (alongFirst) {
				quad(plane, surface, lerp(a0, a1, start), b0, lerp(a0, a1, end), b1, style);
			} else {
				quad(plane, surface, a0, lerp(b0, b1, start), a1, lerp(b0, b1, end), style);
			}
		}

		return from + length;
	}

	/** How lit a piece is: dim everywhere, bright where one of the travelling places is. */
	private static int brightness(double along, float phase) {
		double offset = (along * ARMS - phase) % 1.0;
		float bump = Mth.cos((float) offset * Mth.TWO_PI);
		float lit = bump <= 0.0F ? 0.0F : (float) Math.pow(bump, FALLOFF);
		return (int) Mth.lerp(lit, DIM_ALPHA, BRIGHT_ALPHA);
	}

	private static double lerp(double from, double to, double at) {
		return from + (to - from) * at;
	}

	/** One quad, given in the plane's own two axes. */
	private static void quad(Plane plane, double surface, double u0, double v0, double u1, double v1,
			GizmoStyle style) {
		Direction.Axis normal = plane.facing().getAxis();
		Gizmos.rect(world(normal, surface, u0, v0), world(normal, surface, u1, v1),
				plane.facing(), style);
	}

	/**
	 * A point in the plane, from its two axes.
	 *
	 * <p>Which two axes those are depends on which way it faces: for an upright plane the
	 * horizontal it runs along and the vertical, for one lying flat the two horizontals. They
	 * are called first and second rather than across and up because only their pairing
	 * matters — every quad here is built and read through the same two helpers.
	 */
	private static Vec3 world(Direction.Axis normal, double surface, double first, double second) {
		return switch (normal) {
			case X -> new Vec3(surface, first, second);
			case Y -> new Vec3(first, surface, second);
			case Z -> new Vec3(first, second, surface);
		};
	}

	private static double first(Vec3 corner, Direction.Axis normal) {
		return normal == Direction.Axis.X ? corner.y : corner.x;
	}

	private static double second(Vec3 corner, Direction.Axis normal) {
		return normal == Direction.Axis.Z ? corner.y : corner.z;
	}
}
