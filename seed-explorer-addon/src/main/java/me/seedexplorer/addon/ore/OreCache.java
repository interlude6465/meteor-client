/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.ore;

import me.seedexplorer.addon.events.SeedChangeEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.orbit.EventHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches predicted ore patch locations in 64x64 chunk "regions" to keep
 * map movement fluid. Mirrors the StructureCache pattern for performant
 * seed-based ore prediction.
 */
public class OreCache {
    private static final OreCache INSTANCE = new OreCache();

    // Cache keyed by region coordinates (64x64 chunk regions)
    // A region key is (rx << 32) | (rz & 0xFFFFFFFF)
    private final Map<Long, List<OrePatch>> cache = new ConcurrentHashMap<>();

    private int lastDimension = 0;

    private OreCache() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public static OreCache get() {
        return INSTANCE;
    }

    @EventHandler
    private void onSeedChanged(SeedChangeEvent event) {
        clear();
    }

    /**
     * Ensures that a specific 64x64 chunk region is cached.
     * If not cached, computes all ore patches for that region.
     *
     * @param regionKey packed (rx << 32 | rz) region coordinates
     */
    public void ensureRegion(long regionKey) {
        if (cache.containsKey(regionKey)) return;

        int rx = (int) (regionKey >> 32);
        int rz = (int) (regionKey & 0xFFFFFFFFL);

        int chunkStartX = rx * 64;
        int chunkStartZ = rz * 64;
        int chunkEndX = chunkStartX + 63;
        int chunkEndZ = chunkStartZ + 63;

        List<OrePatch> patches = OrePredictor.predictInChunkRange(
            chunkStartX, chunkStartZ, chunkEndX, chunkEndZ, lastDimension
        );
        cache.put(regionKey, patches);
    }

    /**
     * Gets all cached ore patches within the given chunk bounds.
     * Ensures all overlapping regions are cached first.
     */
    public List<OrePatch> getOres(int chunkMinX, int chunkMinZ,
                                  int chunkMaxX, int chunkMaxZ,
                                  int dimension) {
        this.lastDimension = dimension;

        // Determine which 64x64 cache regions overlap the viewport
        int rMinX = Math.floorDiv(chunkMinX, 64);
        int rMinZ = Math.floorDiv(chunkMinZ, 64);
        int rMaxX = Math.floorDiv(chunkMaxX, 64);
        int rMaxZ = Math.floorDiv(chunkMaxZ, 64);

        // Ensure all needed regions are cached
        for (int rz = rMinZ; rz <= rMaxZ; rz++) {
            for (int rx = rMinX; rx <= rMaxX; rx++) {
                long key = ((long) rx << 32) | (rz & 0xFFFFFFFFL);
                ensureRegion(key);
            }
        }

        // Collect all ore patches within the requested bounds
        List<OrePatch> results = new ArrayList<>();
        for (int rz = rMinZ; rz <= rMaxZ; rz++) {
            for (int rx = rMinX; rx <= rMaxX; rx++) {
                long key = ((long) rx << 32) | (rz & 0xFFFFFFFFL);
                List<OrePatch> regionPatches = cache.get(key);
                if (regionPatches == null) continue;

                for (OrePatch patch : regionPatches) {
                    int pcsx = patch.x >> 4;
                    int pcsz = patch.z >> 4;
                    if (pcsx >= chunkMinX && pcsx <= chunkMaxX && pcsz >= chunkMinZ && pcsz <= chunkMaxZ) {
                        results.add(patch);
                    }
                }
            }
        }

        return results;
    }

    /**
     * Clears all cached ore patches, for example when the seed changes.
     */
    public void clear() {
        cache.clear();
    }
}