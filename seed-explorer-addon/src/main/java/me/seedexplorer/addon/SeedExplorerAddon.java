/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon;

import me.seedexplorer.addon.commands.SeedCommand;
import me.seedexplorer.addon.commands.SeedExplorerCommand;
import me.seedexplorer.addon.modules.SeedExplorerModule;
import me.seedexplorer.addon.seed.SeedManager;
import meteordevelopment.meteorclient.addons.GithubRepo;
import meteordevelopment.meteorclient.addons.MeteorAddon;
import meteordevelopment.meteorclient.commands.Commands;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.systems.modules.Category;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.world.item.Items;

public class SeedExplorerAddon extends MeteorAddon {
    public static final Category SEED_EXPLORER = new Category("Seed Explorer", () -> Items.COMPASS.getDefaultInstance());

    @Override
    public void onInitialize() {
        // Initialize seed manager
        SeedManager seedManager = new SeedManager();
        Systems.add(seedManager);
        seedManager.load();

        // Register modules
        Modules.get().add(new SeedExplorerModule());

        // Register commands
        Commands.add(new SeedExplorerCommand());
        Commands.add(new SeedCommand());
    }

    @Override
    public void onRegisterCategories() {
        Modules.registerCategory(SEED_EXPLORER);
    }

    @Override
    public String getPackage() {
        return "me.seedexplorer.addon";
    }

    @Override
    public GithubRepo getRepo() {
        return new GithubRepo("SeedExplorer", "meteor-seed-explorer");
    }
}