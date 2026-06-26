/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.ore;

import meteordevelopment.meteorclient.utils.render.color.Color;

/**
 * Minecraft ore types supported by the ore predictor.
 * Each type has a display name, render color, dimension restriction,
 * salt for RNG seeding, and a generation chance (0-100).
 */
public enum OreType {
    DIAMOND("Diamond", 0, 20083233, 9, new Color(0, 200, 255, 200)),
    ANCIENT_DEBRIS("Ancient Debris", -1, 30084233, 5, new Color(120, 60, 40, 200)),
    EMERALD("Emerald", 0, 20083234, 7, new Color(0, 220, 0, 200)),
    GOLD("Gold", 0, 20083235, 12, new Color(255, 200, 0, 200));

    public final String displayName;
    public final int dimension;  // 0=Overworld, -1=Nether, 1=End
    public final int salt;
    public final int chance;     // Percent chance (0-100) of a patch generating in a given chunk
    public final Color color;

    OreType(String displayName, int dimension, int salt, int chance, Color color) {
        this.displayName = displayName;
        this.dimension = dimension;
        this.salt = salt;
        this.chance = chance;
        this.color = color;
    }
}