/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.modules;

import me.seedexplorer.addon.SeedExplorerAddon;
import meteordevelopment.meteorclient.systems.modules.Module;

public class SeedExplorerModule extends Module {
    public SeedExplorerModule() {
        super(SeedExplorerAddon.SEED_EXPLORER, "seed-explorer", "Base module for the Seed Explorer addon.");
    }
}