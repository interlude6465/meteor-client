/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.structures;

/**
 * Represents a generated structure location found during seed analysis.
 * Contains position, type, and whether it has been verified.
 */
public class GeneratedStructure {
    public final int x, z;
    public final StructureType type;
    public boolean verified;

    public GeneratedStructure(int x, int z, StructureType type) {
        this.x = x;
        this.z = z;
        this.type = type;
        this.verified = false;
    }

    public String getCoordsString() {
        return x + ", " + z;
    }
}