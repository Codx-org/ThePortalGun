package codx.theportalgun;

import io.netty.buffer.ByteBuf;

import codx.ThePortalGun;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * "I pressed the left mouse button", sent by the client that is holding the gun.
 *
 * <p>Left click has no path of its own. Vanilla sends the server an attack only once the
 * crosshair is on something within arm's reach, so a shot at a wall across the room —
 * which is most shots — reaches the server as nothing at all. So the client says the
 * button was pressed and the server decides what that means, exactly as it does for the
 * right button.
 *
 * <p>Carrying no data is deliberate: where the shot goes is traced from the player the
 * server already has. A client that sent coordinates could put a portal anywhere.
 */
public record FireBlue() implements CustomPacketPayload {
	public static final Type<FireBlue> TYPE =
			new Type<>(ThePortalGun.id("fire_blue"));

	public static final StreamCodec<ByteBuf, FireBlue> STREAM_CODEC =
			StreamCodec.unit(new FireBlue());

	@Override
	public Type<? extends CustomPacketPayload> type() {
		return TYPE;
	}

	/** Called from the common entry point: both sides need the type. */
	public static void register() {
		PayloadTypeRegistry.serverboundPlay().register(TYPE, STREAM_CODEC);

		ServerPlayNetworking.registerGlobalReceiver(TYPE, (payload, context) -> {
			// Holding the gun is checked here too. The client only sends this while it is
			// held, but a client is not something to take the word of.
			if (context.player().getMainHandItem().is(ThePortalGun.PORTAL_GUN)) {
				PortalGun.fire(context.player(), true);
			}
		});
	}
}
