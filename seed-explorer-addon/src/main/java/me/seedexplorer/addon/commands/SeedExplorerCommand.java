/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

public class SeedExplorerCommand extends Command {
    public SeedExplorerCommand() {
        super("seed-explorer", "Base command for the Seed Explorer addon.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            info("Seed Explorer addon loaded. Use sub-commands for specific functionality.");
            return SINGLE_SUCCESS;
        });
    }
}