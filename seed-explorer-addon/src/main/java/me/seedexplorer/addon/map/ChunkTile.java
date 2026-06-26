/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.map;

import meteordevelopment.meteorclient.renderer.Texture;

/** Represents a single chunk tile on the map. */
public class ChunkTile {
    public final int x, z;
    public Texture texture;

    public ChunkTile(int x, int z) {
        this.x = x;
        this.z = z;
    }

    public boolean isLoaded() {
        return texture != null;
    }

    public void dispose() {
        if (texture != null) {
            // In Meteor Client, Texture might need disposal if it's an AbstractTexture
            // but we don't have a specific dispose method on Texture class we saw.
            // AbstractTexture has close() in newer MC versions.
            texture.close();
            texture = null;
        }
    }
}
