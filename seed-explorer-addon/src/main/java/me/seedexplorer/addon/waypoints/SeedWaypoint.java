/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.waypoints;

/** Extended waypoint data for seed-explorer waypoints. */
public class SeedWaypoint {
    public final String name;
    public final int x, z;
    public final int dimension; // 0=Overworld, -1=Nether, 1=End
    public final String icon;

    public SeedWaypoint(String name, int x, int z, int dimension, String icon) {
        this.name = name;
        this.x = x;
        this.z = z;
        this.dimension = dimension;
        this.icon = icon;
    }
}