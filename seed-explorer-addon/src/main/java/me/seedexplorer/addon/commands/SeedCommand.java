/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;

import me.seedexplorer.addon.seed.SeedManager;

public class SeedCommand extends Command {
    public SeedCommand() {
        super("seed", "Sets the world seed and version for seed exploration.");
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.then(argument("seed", StringArgumentType.word())
            .executes(context -> {
                String seedStr = context.getArgument("seed", String.class);
                long seed = parseSeed(seedStr);
                SeedManager.get().setWorldSeed(seed);
                info("Set seed to (highlight)%s(default) (numeric: (highlight)%d(default)).", seedStr, seed);
                return SINGLE_SUCCESS;
            })
            .then(argument("version", StringArgumentType.greedyString())
                .executes(context -> {
                    String seedStr = context.getArgument("seed", String.class);
                    String version = context.getArgument("version", String.class);
                    long seed = parseSeed(seedStr);
                    SeedManager.get().set(seed, version);
                    info("Set seed to (highlight)%s(default) (numeric: (highlight)%d(default)) for version (highlight)%s(default).", seedStr, seed, version);
                    return SINGLE_SUCCESS;
                })
            )
        );
    }

    private long parseSeed(String seed) {
        try {
            return Long.parseLong(seed);
        } catch (NumberFormatException e) {
            return seed.hashCode();
        }
    }
}