/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.map;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.TextureFormat;
import me.seedexplorer.addon.events.SeedChangeEvent;
import me.seedexplorer.addon.seed.SeedManager;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Manages map chunk tiles, their generation, and caching. */
public class MinimapManager {
    private static final MinimapManager INSTANCE = new MinimapManager();
    private final Map<Long, ChunkTile> cache = new ConcurrentHashMap<>();

    private MinimapManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    public static MinimapManager get() {
        return INSTANCE;
    }

    @EventHandler
    private void onSeedChanged(SeedChangeEvent event) {
        clear();
    }

    /**
     * Retrieves a ChunkTile for the given chunk coordinates.
     * Creates a new tile with async generation if it doesn't exist in the cache.
     */
    public ChunkTile getTile(int x, int z) {
        long key = ((long) x << 32) | (z & 0xFFFFFFFFL);
        ChunkTile tile = cache.get(key);
        if (tile == null) {
            tile = new ChunkTile(x, z);
            cache.put(key, tile);
            generateAsync(tile);
        }
        return tile;
    }

    /**
     * Launches a background task to generate the NativeImage for a chunk tile,
     * then uploads the texture to the GPU on the main render thread.
     */
    private void generateAsync(ChunkTile tile) {
        if (!tile.markGenerating()) return;

        MeteorExecutor.execute(() -> {
            try {
                long seed = SeedManager.get().getWorldSeed();
                NativeImage image = new NativeImage(NativeImage.Format.RGBA, 16, 16, false);

                for (int cz = 0; cz < 16; cz++) {
                    for (int cx = 0; cx < 16; cx++) {
                        int worldX = tile.x * 16 + cx;
                        int worldZ = tile.z * 16 + cz;
                        int color = BiomeGenerator.getBiomeColor(worldX, worldZ);
                        image.setPixelABGR(cx, cz, color);
                    }
                }

                Minecraft.getInstance().execute(() -> {
                    if (tile.texture != null) {
                        tile.texture.close();
                    }

                    Texture texture = new Texture(16, 16, TextureFormat.RGBA8, FilterMode.NEAREST, FilterMode.NEAREST);
                    RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture.getTexture(), image);
                    tile.texture = texture;
                    tile.generating = false;
                    image.close();
                });
            } catch (Exception e) {
                tile.generating = false;
                e.printStackTrace();
            }
        });
    }

    /** Clears all cached tiles, disposing GPU resources. */
    public void clear() {
        for (ChunkTile tile : cache.values()) {
            tile.dispose();
        }
        cache.clear();
    }
}