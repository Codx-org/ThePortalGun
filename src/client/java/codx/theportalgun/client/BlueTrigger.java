package codx.theportalgun.client;

import codx.ThePortalGun;
import codx.theportalgun.FireBlue;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.event.client.player.ClientPreAttackCallback;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.component.SwingAnimation;

/**
 * Left click, while the gun is held: tell the server, and swallow the click.
 *
 * <p>This is the only client-side logic the mod has, and it exists because of where vanilla
 * draws the line. Attacking is a thing you do to a block or an entity in front of you, so
 * nothing is sent when you swing at a wall thirty blocks away — which for a gun is the
 * normal case. Cancelling it stops the swing, the mining and the miss all at once.
 */
public final class BlueTrigger {
	/** Ticks before the same button can fire again, so a held click is not a stream. */
	private static final int COOLDOWN = 4;

	private static int cooldown;

	private BlueTrigger() {
	}

	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			if (cooldown > 0) {
				cooldown--;
			}
		});

		ClientPreAttackCallback.EVENT.register((client, player, clickCount) -> {
			if (!player.getMainHandItem().is(ThePortalGun.PORTAL_GUN)) {
				return false;
			}

			if (cooldown == 0) {
				cooldown = COOLDOWN;
				ClientPlayNetworking.send(new FireBlue());
				swing(client);
			}

			// True cancels the attack, so the gun neither mines nor hits anything.
			return true;
		});
	}

	/** The arm movement vanilla would have played, since the attack is being cancelled. */
	private static void swing(Minecraft client) {
		if (client.player != null) {
			client.player.swing(InteractionHand.MAIN_HAND, SwingAnimation.DEFAULT, false);
		}
	}
}
