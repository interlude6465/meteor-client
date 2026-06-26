/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.waypoints;

import meteordevelopment.meteorclient.systems.waypoints.Waypoint;
import meteordevelopment.meteorclient.systems.waypoints.Waypoints;
import meteordevelopment.meteorclient.utils.world.Dimension;
import net.minecraft.core.BlockPos;

/**
 * Manages waypoints related to seed exploration.
 * Provides methods to create waypoints at predicted structure locations.
 */
public class WaypointManager {
    private static final WaypointManager INSTANCE = new WaypointManager();

    private WaypointManager() {}

    public static WaypointManager get() {
        return INSTANCE;
    }

    /**
     * Creates a Meteor Client waypoint at the given structure position.
     *
     * @param name      The waypoint name (e.g., "Village at -500, 200")
     * @param x         Block X coordinate
     * @param z         Block Z coordinate
     * @param dimension 0=Overworld, -1=Nether, 1=End
     * @param icon      Icon name (e.g., "square", "diamond", "star")
     * @return The created Waypoint, or null if it already exists
     */
    public Waypoint createWaypoint(String name, int x, int z, int dimension, String icon) {
        Dimension dim = switch (dimension) {
            case 0 -> Dimension.Overworld;
            case -1 -> Dimension.Nether;
            case 1 -> Dimension.End;
            default -> Dimension.Overworld;
        };

        Waypoint waypoint = new Waypoint.Builder()
            .name(name)
            .pos(new BlockPos(x, 64, z))
            .dimension(dim)
            .icon(icon)
            .build();

        boolean alreadyExists = Waypoints.get().add(waypoint);
        return alreadyExists ? null : waypoint;
    }
}