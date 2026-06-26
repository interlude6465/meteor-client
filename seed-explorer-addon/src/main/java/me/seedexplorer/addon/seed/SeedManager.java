/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.seed;

import me.seedexplorer.addon.events.SeedChangeEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import net.minecraft.nbt.CompoundTag;

public class SeedManager extends System<SeedManager> {
    private long worldSeed;
    private String mcVersion;

    public SeedManager() {
        super("seed-manager");
        this.worldSeed = 0;
        this.mcVersion = "";
    }

    public static SeedManager get() {
        return Systems.get(SeedManager.class);
    }

    public long getWorldSeed() {
        return worldSeed;
    }

    public String getMcVersion() {
        return mcVersion;
    }

    public void setWorldSeed(long worldSeed) {
        if (this.worldSeed != worldSeed) {
            this.worldSeed = worldSeed;
            postChangeEvent();
            save();
        }
    }

    public void setMcVersion(String mcVersion) {
        if (!this.mcVersion.equals(mcVersion)) {
            this.mcVersion = mcVersion;
            postChangeEvent();
            save();
        }
    }

    public void set(long worldSeed, String mcVersion) {
        boolean changed = false;

        if (this.worldSeed != worldSeed) {
            this.worldSeed = worldSeed;
            changed = true;
        }

        if (!this.mcVersion.equals(mcVersion)) {
            this.mcVersion = mcVersion;
            changed = true;
        }

        if (changed) {
            postChangeEvent();
            save();
        }
    }

    private void postChangeEvent() {
        MeteorClient.EVENT_BUS.post(SeedChangeEvent.get(worldSeed, mcVersion));
    }

    @Override
    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putLong("worldSeed", worldSeed);
        tag.putString("mcVersion", mcVersion);
        return tag;
    }

    @Override
    public SeedManager fromTag(CompoundTag tag) {
        worldSeed = tag.getLongOr("worldSeed", 0);
        mcVersion = tag.getStringOr("mcVersion", "");
        return this;
    }
}