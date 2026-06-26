/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.modules;

import me.seedexplorer.addon.SeedExplorerAddon;
import me.seedexplorer.addon.gui.SeedExplorerScreen;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.pressable.WButton;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class SeedExplorerModule extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Boolean> oreOverlay = sgGeneral.add(new BoolSetting.Builder()
        .name("ore-overlay")
        .description("Shows predicted ore locations (Diamond, Ancient Debris, etc.) on the Seed Explorer map.")
        .defaultValue(true)
        .build()
    );

    public SeedExplorerModule() {
        super(SeedExplorerAddon.SEED_EXPLORER, "seed-explorer", "Base module for the Seed Explorer addon.");
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        WButton openScreen = theme.button("Open Seed Explorer");
        openScreen.action = () -> mc.setScreen(new SeedExplorerScreen(theme));
        return openScreen;
    }
}