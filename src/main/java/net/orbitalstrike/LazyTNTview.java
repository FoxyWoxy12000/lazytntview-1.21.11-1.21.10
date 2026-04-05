package net.orbitalstrike;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.orbitalstrike.network.TntUpdatePayload;
import net.orbitalstrike.server.ServerTntTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LazyTNTview implements ModInitializer {

	public static final String MOD_ID = "lazytntview";
	public static final Logger LOGGER  = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		PayloadTypeRegistry.playS2C().register(
				TntUpdatePayload.ID,
				TntUpdatePayload.CODEC
		);
		ServerTntTracker.register();
	}
}