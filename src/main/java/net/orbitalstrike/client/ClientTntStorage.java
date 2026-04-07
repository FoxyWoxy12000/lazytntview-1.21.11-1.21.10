package net.orbitalstrike.client;

import net.orbitalstrike.network.TntUpdatePayload;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public final class ClientTntStorage {

    private ClientTntStorage() {}

    private static final Map<UUID, TntState> STATE = new ConcurrentHashMap<>();

    public static void updateAll(List<TntUpdatePayload.TntEntry> entries, long worldTime) {
        Set<UUID> incoming = new HashSet<>(entries.size());
        for (TntUpdatePayload.TntEntry e : entries) {
            incoming.add(e.uuid());
            STATE.put(e.uuid(), new TntState(
                    e.x(), e.y(), e.z(),
                    e.yaw(), e.pitch(),
                    e.fuse(), e.lazy(), worldTime
            ));
        }
        STATE.keySet().retainAll(incoming);
    }

    public static TntState get(UUID uuid) {
        return STATE.get(uuid);
    }

    public static Collection<Map.Entry<UUID, TntState>> getAll() {
        return STATE.entrySet();
    }

    public static void clear() {
        STATE.clear();
    }

    public static final class TntState {
        public final double x, y, z;
        public final float  yaw, pitch;
        public final int    fuse;
        public final boolean lazy;
        public final long   lastUpdateTime;

        TntState(double x, double y, double z,
                 float yaw, float pitch, int fuse, boolean lazy, long lastUpdateTime) {
            this.x = x; this.y = y; this.z = z;
            this.yaw = yaw; this.pitch = pitch;
            this.fuse = fuse; this.lazy = lazy;
            this.lastUpdateTime = lastUpdateTime;
        }
    }
}