package codx;

import codx.theportalgun.FireBlue;
import codx.theportalgun.PortalGun;
import codx.theportalgun.PortalGunItem;
import codx.theportalgun.PortalPair;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThePortalGun implements ModInitializer {
	public static final String MOD_ID = "the-portal-gun";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** The gun itself, as the registry knows it. */
	public static final ResourceKey<Item> PORTAL_GUN_KEY =
			ResourceKey.create(Registries.ITEM, id("portal_gun"));

	public static final Item PORTAL_GUN = Registry.register(BuiltInRegistries.ITEM, PORTAL_GUN_KEY,
			new PortalGunItem(new Item.Properties().stacksTo(1).setId(PORTAL_GUN_KEY)));

	@Override
	public void onInitialize() {
		// The one line that makes this a Planeshift mod: from here on, the doorways a player
		// shoots are drawn and walked through by the library.
		PortalPair.register();

		// Left click arrives as a packet of this mod's own; see FireBlue for why it cannot
		// ride on vanilla's attack.
		FireBlue.register();

		// A player's portals are theirs; nobody else can walk through them once they are
		// gone, and leaving them behind would leave planes pointing at nothing.
		ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
				PortalPair.forget(server, handler.getPlayer().getUUID()));

		// And on shutdown, so a world is never saved with holes nothing will ever fill.
		ServerLifecycleEvents.SERVER_STOPPING.register(PortalPair::forgetAll);

		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(output -> output.accept(PORTAL_GUN));

		LOGGER.info("The portal gun is loaded; its portals are Planeshift planes");
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
