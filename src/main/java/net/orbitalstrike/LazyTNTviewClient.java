package net.orbitalstrike;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.orbitalstrike.client.ClientTntStorage;
import net.orbitalstrike.client.TntOverlayRenderer;
import net.orbitalstrike.network.TntUpdatePayload;

public class LazyTNTviewClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(
                TntUpdatePayload.ID,
                (payload, context) -> context.client().execute(() -> {
                    if (context.client().world == null) return;
                    ClientTntStorage.updateAll(
                            payload.entries(),
                            context.client().world.getTime()
                    );
                })
        );

        ClientPlayConnectionEvents.DISCONNECT.register(
                (handler, client) -> ClientTntStorage.clear()
        );

        //TntOverlayRenderer.register();
    }
}