package codx.client;

import codx.theportalgun.PortalPair;
import codx.theportalgun.client.BlueTrigger;
import codx.theportalgun.client.PortalRim;

import codx.planeshift.client.appearance.PlaneAppearances;

import net.fabricmc.api.ClientModInitializer;

public class ThePortalGunClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		// Left click has to be noticed here; see BlueTrigger for why.
		BlueTrigger.register();

		// What the two ends look like. The server only sends the names; this is the client
		// saying what they mean, which is why a client without this mod still sees a working
		// doorway — just a plain one.
		PlaneAppearances.register(PortalPair.BLUE, new PortalRim(PortalRim.BLUE));
		PlaneAppearances.register(PortalPair.ORANGE, new PortalRim(PortalRim.ORANGE));

		// The gun's own rainbow colour needs nothing set up: it is a tint source added to the
		// game's list by ItemTintSourcesMixin, and the item's definition asks for it.
	}
}
