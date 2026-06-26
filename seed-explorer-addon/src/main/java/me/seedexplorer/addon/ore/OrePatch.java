/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.ore;

/**
 * Represents a predicted ore patch location.
 * Contains the block position and ore type.
 */
public class OrePatch {
    public final int x, z;
    public final OreType type;

    public OrePatch(int x, int z, OreType type) {
        this.x = x;
        this.z = z;
        this.type = type;
    }

    public String getCoordsString() {
        return x + ", " + z;
    }
}