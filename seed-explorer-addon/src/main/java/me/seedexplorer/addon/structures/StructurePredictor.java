/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.structures;

import me.seedexplorer.addon.seed.SeedManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Predicts structure positions using region-based placement math derived
 * from the Cubiomes library (finders.c / finders.h).
 *
 * Minecraft divides the world into a grid of regions (usually 32x32 chunks)
 * and performs one generation attempt somewhere in each region.
 * The position of this attempt is governed by the structure type, the region
 * coordinates, and the lower 48 bits of the world seed.
 */
public class StructurePredictor {
    private static final long K = 0x5deece66dL;       // Java RNG multiplier
    private static final long M = (1L << 48) - 1;    // 48-bit mask
    private static final long B = 0xbL;               // Java RNG increment
    private static final long REGION_X_MULT = 341873128712L;
    private static final long REGION_Z_MULT = 132897987541L;

    /**
     * Predicts all structure positions for the given region range and dimension.
     *
     * @param regionMinX Minimum region X coordinate (inclusive)
     * @param regionMinZ Minimum region Z coordinate (inclusive)
     * @param regionMaxX Maximum region X coordinate (inclusive)
     * @param regionMaxZ Maximum region Z coordinate (inclusive)
     * @param dimension  0=Overworld, -1=Nether, 1=End
     * @return List of predicted GeneratedStructures
     */
    public static List<GeneratedStructure> predict(int regionMinX, int regionMinZ,
                                                    int regionMaxX, int regionMaxZ,
                                                    int dimension) {
        List<GeneratedStructure> results = new ArrayList<>();
        long seed = SeedManager.get().getWorldSeed();
        if (seed == 0) return results;

        // Only use lower 48 bits as Cubiomes does
        long s48 = seed & M;

        for (StructureType type : StructureType.values()) {
            if (type.dimension != dimension) continue;
            predictForType(results, type, s48, regionMinX, regionMinZ, regionMaxX, regionMaxZ);
        }

        return results;
    }

    /**
     * Predicts structures of a specific type within a region range.
     */
    private static void predictForType(List<GeneratedStructure> results, StructureType type,
                                        long seed, int rMinX, int rMinZ, int rMaxX, int rMaxZ) {
        for (int rz = rMinZ; rz <= rMaxZ; rz++) {
            for (int rx = rMinX; rx <= rMaxX; rx++) {
                GeneratedStructure gs = predictInRegionRaw(type, seed, rx, rz);
                if (gs != null) {
                    results.add(gs);
                }
            }
        }
    }

    /**
     * Predicts a structure position for a specific type in a single region.
     * Uses the Cubiomes getFeaturePos / getLargeStructurePos logic.
     * Package-private for use by StructureCache.
     */
    static GeneratedStructure predictInRegionRaw(StructureType type, long seed, int regX, int regZ) {
        int chunkX, chunkZ;

        if (type.isLargeStructure()) {
            // Triangular distribution for large structures (Monument, Mansion, End City)
            chunkX = getLargeStructureChunkInRegion(seed, type.salt, regX, regZ, type.chunkRange);
            chunkZ = (chunkX & 0xFFFF); // extract Z from packed result
            chunkX = (chunkX >> 16) & 0xFFFF;

            // Re-compute properly
            long rnd = seed + (long) regX * REGION_X_MULT + (long) regZ * REGION_Z_MULT + type.salt;
            rnd = (rnd ^ K);
            rnd = (rnd * K + B) & M;

            int x1 = (int) (rnd >> 17) % type.chunkRange;
            rnd = (rnd * K + B) & M;
            int x2 = (int) (rnd >> 17) % type.chunkRange;

            rnd = (rnd * K + B) & M;
            int z1 = (int) (rnd >> 17) % type.chunkRange;
            rnd = (rnd * K + B) & M;
            int z2 = (int) (rnd >> 17) % type.chunkRange;

            chunkX = (x1 + x2) >> 1;
            chunkZ = (z1 + z2) >> 1;
        } else {
            // Uniform distribution for small structures
            long rnd = seed + (long) regX * REGION_X_MULT + (long) regZ * REGION_Z_MULT + type.salt;
            rnd = (rnd ^ K);
            rnd = (rnd * K + B) & M;

            chunkX = (int) (rnd >> 17) % type.chunkRange;
            rnd = (rnd * K + B) & M;
            chunkZ = (int) (rnd >> 17) % type.chunkRange;
        }

        // Convert chunk-in-region to world block coordinates
        int blockX = (regX * type.regionSize + chunkX) * 16 + 8;
        int blockZ = (regZ * type.regionSize + chunkZ) * 16 + 8;

        // Handle special validation for certain structure types
        if (type == StructureType.OUTPOST) {
            // Outpost has a 1/5 chance per village check
            long outpostSeed = seed + (long) regX * REGION_X_MULT + (long) regZ * REGION_Z_MULT + type.salt;
            outpostSeed = (outpostSeed ^ K);
            outpostSeed = (outpostSeed * K + B) & M;

            // setAttemptSeed logic - skip some RNG calls
            long attemptSeed = outpostSeed;
            attemptSeed ^= (blockX >> 4) ^ ((blockZ >> 4) << 4);
            // simplified: Java RNG would be involved but for prediction we just check
            attemptSeed = (attemptSeed ^ K);
            attemptSeed = (attemptSeed * K + B) & M;
            // Skip 31 RNG calls (simplified - we'd need proper Java RNG)
            for (int i = 0; i < 31; i++) {
                attemptSeed = (attemptSeed * K + B) & M;
            }
            int roll = (int) (attemptSeed >> 17) % 5;
            if (roll != 0) return null; // 80% of positions are invalid
        }

        if (type == StructureType.END_CITY) {
            // End Cities only generate at distance >= 1008 blocks from origin
            if ((long) blockX * blockX + (long) blockZ * blockZ < 1008L * 1008L) return null;
        }

        GeneratedStructure gs = new GeneratedStructure(blockX, blockZ, type);
        return gs;
    }

    /**
     * Gets the large structure chunk position within a region, returning x and z packed.
     */
    private static int getLargeStructureChunkInRegion(long seed, int salt, int regX, int regZ, int chunkRange) {
        long rnd = seed + (long) regX * REGION_X_MULT + (long) regZ * REGION_Z_MULT + salt;
        rnd = (rnd ^ K);
        rnd = (rnd * K + B) & M;

        int x1 = (int) (rnd >> 17) % chunkRange;
        rnd = (rnd * K + B) & M;
        int x2 = (int) (rnd >> 17) % chunkRange;

        rnd = (rnd * K + B) & M;
        int z1 = (int) (rnd >> 17) % chunkRange;
        rnd = (rnd * K + B) & M;
        int z2 = (int) (rnd >> 17) % chunkRange;

        return ((x1 + x2) >> 1) << 16 | ((z1 + z2) >> 1) & 0xFFFF;
    }

    /**
     * Gets the region coordinate for a given block position and structure type.
     */
    public static int getRegionForBlock(int blockX, StructureType type) {
        return Math.floorDiv(blockX, type.regionSize * 16);
    }

    /**
     * Returns the region key for a given chunk position (used by StructureCache).
     * Regions are 64x64 chunks for caching purposes.
     */
    public static long getRegionKey(int chunkX, int chunkZ) {
        int rx = Math.floorDiv(chunkX, 64);
        int rz = Math.floorDiv(chunkZ, 64);
        return ((long) rx << 32) | (rz & 0xFFFFFFFFL);
    }
}