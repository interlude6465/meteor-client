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
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/** Manages map chunk tiles and their generation. */
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

    private void generateAsync(ChunkTile tile) {
        MeteorExecutor.execute(() -> {
            long seed = SeedManager.get().getWorldSeed();
            NativeImage image = new NativeImage(16, 16, false);

            for (int cz = 0; cz < 16; cz++) {
                for (int cx = 0; cx < 16; cx++) {
                    int worldX = tile.x * 16 + cx;
                    int worldZ = tile.z * 16 + cz;
                    int color = getBiomeColor(worldX, worldZ, seed);
                    image.setPixelRGBA(cx, cz, color);
                }
            }

            Minecraft.getInstance().execute(() -> {
                Texture texture = new Texture(16, 16, TextureFormat.RGBA8, FilterMode.NEAREST, FilterMode.NEAREST);
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(texture.getTexture(), image);
                tile.texture = texture;
                image.close();
            });
        });
    }

    private int getBiomeColor(int x, int z, long seed) {
        Random random = new Random(seed ^ ((long) x * 341873128712L) ^ ((long) z * 132897987541L));
        float h = random.nextFloat();

        // Biome colors (ABGR format for NativeImage)
        if (h < 0.2) return 0xFF800000; // Deep Ocean
        if (h < 0.3) return 0xFFFF0000; // Ocean
        if (h < 0.35) return 0xFFE0FFFF; // Beach
        if (h < 0.6) return 0xFF00FF00; // Plains
        if (h < 0.8) return 0xFF006400; // Forest
        if (h < 0.9) return 0xFF808080; // Mountains
        return 0xFFFFFFFF; // Snow
    }

    public void clear() {
        for (ChunkTile tile : cache.values()) {
            tile.dispose();
        }
        cache.clear();
    }
}
