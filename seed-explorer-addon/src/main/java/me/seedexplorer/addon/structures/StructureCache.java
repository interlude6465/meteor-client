/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.structures;

import me.seedexplorer.addon.events.SeedChangeEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.orbit.EventHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches predicted structure locations in 64x64 chunk "regions" to keep
 * map movement fluid. When the viewport moves, missed regions are computed
 * on a background thread and cached for instant re-rendering.
 */
public class StructureCache {
    private static final StructureCache INSTANCE = new StructureCache();

    // Cache keyed by region coordinates (64x64 chunk regions)
    // A region key is (rx << 32) | (rz & 0xFFFFFFFF)
    private final Map<Long, List<GeneratedStructure>> cache = new ConcurrentHashMap<>();

    private int lastDimension = 0;

    private StructureCache() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public static StructureCache get() {
        return INSTANCE;
    }

    @EventHandler
    private void onSeedChanged(SeedChangeEvent event) {
        clear();
    }

    /**
     * Ensures that a specific 64x64 chunk region is cached.
     * If not cached, computes all structure positions for that region.
     *
     * @param regionKey packed (rx << 32 | rz) region coordinates
     */
    public void ensureRegion(long regionKey) {
        if (cache.containsKey(regionKey)) return;

        int rx = (int) (regionKey >> 32);
        int rz = (int) (regionKey & 0xFFFFFFFFL);

        // Convert cache region to structure region coordinates
        // Our cache regions are 64x64 chunks; structure regions vary by type
        // We need to cover all structure regions that overlap this cache region
        int chunkStartX = rx * 64;
        int chunkStartZ = rz * 64;
        int chunkEndX = chunkStartX + 63;
        int chunkEndZ = chunkStartZ + 63;

        List<GeneratedStructure> structures = computeStructures(chunkStartX, chunkStartZ, chunkEndX, chunkEndZ);
        cache.put(regionKey, structures);
    }

    /**
     * Computes all structure positions overlapping the given chunk range.
     */
    private List<GeneratedStructure> computeStructures(int csx, int csz, int cex, int cez) {
        List<GeneratedStructure> results = new ArrayList<>();

        for (StructureType type : StructureType.values()) {
            if (type.dimension != lastDimension) continue;

            // Calculate which structure regions overlap the given chunk range
            int rMinX = Math.floorDiv(csx, type.regionSize);
            int rMinZ = Math.floorDiv(csz, type.regionSize);
            int rMaxX = Math.floorDiv(cex, type.regionSize);
            int rMaxZ = Math.floorDiv(cez, type.regionSize);

            long seed = me.seedexplorer.addon.seed.SeedManager.get().getWorldSeed();
            if (seed == 0) continue;
            long s48 = seed & ((1L << 48) - 1);

            for (int rz = rMinZ; rz <= rMaxZ; rz++) {
                for (int rx = rMinX; rx <= rMaxX; rx++) {
                    GeneratedStructure gs = StructurePredictor.predictInRegionRaw(type, s48, rx, rz);
                    if (gs != null) {
                        // Check if the structure's block position is actually within our chunk range
                        int gchunkX = gs.x >> 4;
                        int gchunkZ = gs.z >> 4;
                        if (gchunkX >= csx && gchunkX <= cex && gchunkZ >= csz && gchunkZ <= cez) {
                            results.add(gs);
                        }
                    }
                }
            }
        }

        return results;
    }

    /**
     * Gets all cached structures within the given chunk bounds.
     * Ensures all overlapping regions are cached first.
     */
    public List<GeneratedStructure> getStructures(int chunkMinX, int chunkMinZ,
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

        // Collect all structures within the requested bounds
        List<GeneratedStructure> results = new ArrayList<>();
        for (int rz = rMinZ; rz <= rMaxZ; rz++) {
            for (int rx = rMinX; rx <= rMaxX; rx++) {
                long key = ((long) rx << 32) | (rz & 0xFFFFFFFFL);
                List<GeneratedStructure> regionStructs = cache.get(key);
                if (regionStructs == null) continue;

                for (GeneratedStructure gs : regionStructs) {
                    int gcsx = gs.x >> 4;
                    int gcsz = gs.z >> 4;
                    if (gcsx >= chunkMinX && gcsx <= chunkMaxX && gcsz >= chunkMinZ && gcsz <= chunkMaxZ) {
                        results.add(gs);
                    }
                }
            }
        }

        return results;
    }

    /**
     * Clears all cached structures, for example when the seed changes.
     */
    public void clear() {
        cache.clear();
    }
}