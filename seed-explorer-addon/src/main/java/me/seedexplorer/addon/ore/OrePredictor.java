/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.ore;

import me.seedexplorer.addon.seed.SeedManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicts ore patch locations using seed-based deterministic RNG.
 * For each ore type, checks every chunk using a coordinated-seeded
 * random number generator to determine if a patch generates and at
 * what position within the chunk.
 */
public class OrePredictor {
    private static final long K = 0x5deece66dL;       // Java RNG multiplier
    private static final long M = (1L << 48) - 1;    // 48-bit mask
    private static final long B = 0xbL;               // Java RNG increment
    private static final long CHUNK_X_MULT = 341873128712L;
    private static final long CHUNK_Z_MULT = 132897987541L;

    /**
     * Predicts all ore patches within a single chunk.
     *
     * @param chunkX Chunk X coordinate
     * @param chunkZ Chunk Z coordinate
     * @param seed   The world seed (lower 48 bits)
     * @param dimension The dimension to predict for (0=Overworld, -1=Nether, 1=End)
     * @return List of predicted OrePatch objects for this chunk
     */
    public static List<OrePatch> predictInChunk(int chunkX, int chunkZ, long seed, int dimension) {
        List<OrePatch> results = new ArrayList<>();

        for (OreType type : OreType.values()) {
            if (type.dimension != dimension) continue;

            OrePatch patch = predictSingle(type, chunkX, chunkZ, seed);
            if (patch != null) {
                results.add(patch);
            }
        }

        return results;
    }

    /**
     * Predicts a single ore patch for the given type in the given chunk.
     * Uses coordinated-seeded RNG to determine if a patch exists and its position.
     */
    private static OrePatch predictSingle(OreType type, int chunkX, int chunkZ, long seed) {
        // Seed the RNG with world seed + chunk coordinates + ore salt
        long rnd = seed + (long) chunkX * CHUNK_X_MULT + (long) chunkZ * CHUNK_Z_MULT + type.salt;
        rnd = (rnd ^ K);
        rnd = (rnd * K + B) & M;

        // Check if a patch generates in this chunk
        int roll = (int) (rnd >> 17) % 100;
        if (roll >= type.chance) return null;

        // Determine position within the chunk (0-15 block offset)
        rnd = (rnd * K + B) & M;
        int blockOffsetX = (int) (rnd >> 17) % 16;

        rnd = (rnd * K + B) & M;
        int blockOffsetZ = (int) (rnd >> 17) % 16;

        // Convert to world block coordinates (center of the block)
        int blockX = chunkX * 16 + blockOffsetX;
        int blockZ = chunkZ * 16 + blockOffsetZ;

        return new OrePatch(blockX, blockZ, type);
    }

    /**
     * Predicts all ore patches for a range of chunks.
     *
     * @param chunkMinX Minimum chunk X (inclusive)
     * @param chunkMinZ Minimum chunk Z (inclusive)
     * @param chunkMaxX Maximum chunk X (inclusive)
     * @param chunkMaxZ Maximum chunk Z (inclusive)
     * @param dimension The dimension to predict for
     * @return List of predicted OrePatch objects
     */
    public static List<OrePatch> predictInChunkRange(int chunkMinX, int chunkMinZ,
                                                      int chunkMaxX, int chunkMaxZ,
                                                      int dimension) {
        List<OrePatch> results = new ArrayList<>();

        long seed = SeedManager.get().getWorldSeed();
        if (seed == 0) return results;

        long s48 = seed & M;

        for (int cz = chunkMinZ; cz <= chunkMaxZ; cz++) {
            for (int cx = chunkMinX; cx <= chunkMaxX; cx++) {
                results.addAll(predictInChunk(cx, cz, s48, dimension));
            }
        }

        return results;
    }
}