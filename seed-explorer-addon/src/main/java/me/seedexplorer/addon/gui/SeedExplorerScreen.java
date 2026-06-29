/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.gui;

import me.seedexplorer.addon.map.BiomeGenerator;
import me.seedexplorer.addon.map.MinimapManager;
import me.seedexplorer.addon.modules.SeedExplorerModule;
import me.seedexplorer.addon.ore.OreCache;
import me.seedexplorer.addon.render.MapLayer;
import me.seedexplorer.addon.render.MapRenderContext;
import me.seedexplorer.addon.render.MapRenderResult;
import me.seedexplorer.addon.render.MapViewport;
import me.seedexplorer.addon.render.SeedRenderer;
import me.seedexplorer.addon.render.StructureColors;
import me.seedexplorer.addon.render.StructureIcons;
import me.seedexplorer.addon.render.UiIcons;
import me.seedexplorer.addon.structures.GeneratedStructure;
import me.seedexplorer.addon.structures.StructureCache;
import me.seedexplorer.addon.structures.StructureType;
import me.seedexplorer.addon.seed.SeedManager;
import me.seedexplorer.addon.waypoints.SeedWaypoint;
import me.seedexplorer.addon.waypoints.WaypointManager;
import me.seedexplorer.addon.worldgen.VanillaStructurePredictor;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.gui.widgets.input.WTextBox;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.Texture;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Fullscreen infinite map screen for seed exploration.
 * Supports panning with left-click drag and zooming with scroll wheel.
 * Renders map layers through SeedRenderer.
 * Provides click popups for structures and waypoints.
 */
public class SeedExplorerScreen extends WidgetScreen {
    private static final int KEY_ENTER = 257;
    private static final int KEY_KP_ENTER = 335;

    private static final int DIMENSION_BUTTON_WIDTH = 80;
    private static final int DIMENSION_BUTTON_HEIGHT = 20;
    private static final int DIMENSION_BUTTON_X = 10;
    private static final int DIMENSION_BUTTON_Y = 10;
    private static final int LAYER_BUTTON_WIDTH = 104;
    private static final int LAYER_BUTTON_COMPACT_WIDTH = 84;
    private static final int LAYER_BUTTON_HEIGHT = 22;
    private static final int LAYER_BUTTON_GAP = 5;
    private static final int LAYER_BUTTON_X = 10;
    private static final int LAYER_BUTTON_Y = 40;
    private static final int RIGHT_CONTROLS_WIDTH = 270;
    private static final int RIGHT_CONTROLS_PADDING = 10;
    private static final int STRUCTURE_POPUP_WIDTH = 220;
    private static final int STRUCTURE_POPUP_TOP_HEIGHT = 94;
    private static final int STRUCTURE_POPUP_GAP = 6;
    private static final int STRUCTURE_POPUP_ACTION_HEIGHT = 22;
    private static final int SEARCH_SUGGESTION_HEIGHT = 23;

    private final SeedRenderer seedRenderer = new SeedRenderer();

    private double offsetX, offsetZ;
    private double zoom = 1.0;
    private double targetZoom = 1.0;

    private boolean dragging;
    private double dragStartX, dragStartY;
    private double dragOffsetX, dragOffsetZ;

    // Dimension selector
    private int selectedDimension = 0; // 0=Overworld, -1=Nether, 1=End

    // Structure data
    private List<GeneratedStructure> visibleStructures = new ArrayList<>();
    private GeneratedStructure hoveredStructure;
    private GeneratedStructure selectedStructure;

    // Waypoint data
    private List<SeedWaypoint> visibleWaypoints = new ArrayList<>();
    private SeedWaypoint hoveredWaypoint;
    private SeedWaypoint selectedWaypoint;

    // Context menu
    private boolean contextMenuOpen;
    private double contextMenuX, contextMenuY;
    private boolean isWaypointContextMenu; // true if context menu is for a waypoint, false for structure
    private boolean structurePanelOpen;

    // Top controls
    private WTextBox seedBox;
    private WTextBox searchBar;
    private boolean suppressNextEnterAction;
    private boolean applyingSeed;
    private String lastSearchText = "";
    private StructureType searchStructureFilter;
    private String searchSuggestion = "";

    // Biome name to ID mapping for search
    private static final Map<String, Integer> BIOME_NAME_TO_ID = new HashMap<>();
    static {
        BIOME_NAME_TO_ID.put("ocean", 0);
        BIOME_NAME_TO_ID.put("deep ocean", 1);
        BIOME_NAME_TO_ID.put("warm ocean", 2);
        BIOME_NAME_TO_ID.put("lukewarm ocean", 3);
        BIOME_NAME_TO_ID.put("cold ocean", 4);
        BIOME_NAME_TO_ID.put("deep lukewarm ocean", 5);
        BIOME_NAME_TO_ID.put("deep cold ocean", 6);
        BIOME_NAME_TO_ID.put("deep frozen ocean", 7);
        BIOME_NAME_TO_ID.put("plains", 8);
        BIOME_NAME_TO_ID.put("sunflower plains", 9);
        BIOME_NAME_TO_ID.put("forest", 10);
        BIOME_NAME_TO_ID.put("dark forest", 11);
        BIOME_NAME_TO_ID.put("birch forest", 12);
        BIOME_NAME_TO_ID.put("old growth birch forest", 13);
        BIOME_NAME_TO_ID.put("taiga", 14);
        BIOME_NAME_TO_ID.put("old growth taiga", 15);
        BIOME_NAME_TO_ID.put("snowy taiga", 16);
        BIOME_NAME_TO_ID.put("snowy plains", 17);
        BIOME_NAME_TO_ID.put("snowy slopes", 18);
        BIOME_NAME_TO_ID.put("ice spikes", 19);
        BIOME_NAME_TO_ID.put("savanna", 20);
        BIOME_NAME_TO_ID.put("savanna plateau", 21);
        BIOME_NAME_TO_ID.put("desert", 22);
        BIOME_NAME_TO_ID.put("badlands", 23);
        BIOME_NAME_TO_ID.put("windswept hills", 24);
        BIOME_NAME_TO_ID.put("windswept gravelly hills", 25);
        BIOME_NAME_TO_ID.put("windswept forest", 26);
        BIOME_NAME_TO_ID.put("stony peaks", 27);
        BIOME_NAME_TO_ID.put("meadow", 28);
        BIOME_NAME_TO_ID.put("jungle", 29);
        BIOME_NAME_TO_ID.put("sparse jungle", 30);
        BIOME_NAME_TO_ID.put("bamboo jungle", 31);
        BIOME_NAME_TO_ID.put("swamp", 32);
        BIOME_NAME_TO_ID.put("beach", 33);
        BIOME_NAME_TO_ID.put("river", 34);
        BIOME_NAME_TO_ID.put("mushroom fields", 35);
        BIOME_NAME_TO_ID.put("nether wastes", 36);
        BIOME_NAME_TO_ID.put("crimson forest", 37);
        BIOME_NAME_TO_ID.put("warped forest", 38);
        BIOME_NAME_TO_ID.put("soul sand valley", 39);
        BIOME_NAME_TO_ID.put("basalt deltas", 40);
        BIOME_NAME_TO_ID.put("the end", 41);
        BIOME_NAME_TO_ID.put("end midlands", 42);
        BIOME_NAME_TO_ID.put("end barrens", 43);
        BIOME_NAME_TO_ID.put("small end islands", 44);
        BIOME_NAME_TO_ID.put("cherry grove", 45);
        BIOME_NAME_TO_ID.put("pale garden", 46);
        BIOME_NAME_TO_ID.put("frozen ocean", 47);
        BIOME_NAME_TO_ID.put("frozen river", 48);
    }

    public SeedExplorerScreen(GuiTheme theme) {
        super(theme, "Seed Explorer");
        centerOnCurrentPlayer();
    }

    @Override
    public void initWidgets() {
        WVerticalList topRightControls = theme.verticalList();
        topRightControls.spacing = 4;

        seedBox = theme.textBox(seedBoxInitialValue(), "Seed (number or text)...");
        seedBox.actionOnUnfocused = this::onSeedChanged;
        topRightControls.add(seedBox).padTop(10).padRight(RIGHT_CONTROLS_PADDING).minWidth(RIGHT_CONTROLS_WIDTH);

        searchBar = theme.textBox("", "Search (X Z, structure, biome)...");
        searchBar.action = this::updateSearchState;
        searchBar.actionOnUnfocused = this::updateSearchState;
        enterAction = this::onSearch;
        topRightControls.add(searchBar).padRight(RIGHT_CONTROLS_PADDING).minWidth(RIGHT_CONTROLS_WIDTH);

        add(topRightControls).top().right();
    }

    private String seedBoxInitialValue() {
        long seed = SeedManager.get().getWorldSeed();
        return seed == 0 ? "" : Long.toString(seed);
    }

    @Override
    protected void onRenderBefore(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        Minecraft mc = Minecraft.getInstance();
        double screenWidth = mc.getWindow().getWidth();
        double screenHeight = mc.getWindow().getHeight();
        SeedExplorerModule module = Modules.get().get(SeedExplorerModule.class);

        syncDimensionToPlayer(mc);
        applyPlayerAutoCenter(mc, module);
        updateSmoothZoom(delta);
        updateSearchState();

        MapViewport viewport = MapViewport.create(offsetX, offsetZ, zoom, screenWidth, screenHeight);
        // The viewport is built in physical (framebuffer) pixels, so the mouse must be
        // converted from GUI-scaled coordinates the same way mouseClicked() does. Otherwise
        // hover detection and the coordinate readout drift at any GUI scale other than 1.
        MapRenderContext context = new MapRenderContext(
            viewport,
            (int) Math.round(toRenderX(mouseX)),
            (int) Math.round(toRenderY(mouseY)),
            delta,
            selectedDimension,
            getEnabledLayers(module),
            shouldShowPlayerInfo(module),
            generationMargin(module),
            biomeTileMargin(module),
            biomeOpacity(module),
            oreOpacity(module),
            oreMarkerScale(module),
            structureMarkerScale(module),
            waypointMarkerScale(module),
            dimWaypointStructures(module),
            loadedTerrainOnMap(module),
            searchStructureFilter
        );
        MapRenderResult result = seedRenderer.render(context, theme);

        visibleStructures = result.structures();
        visibleWaypoints = result.waypoints();
        hoveredStructure = result.hoveredStructure();
        hoveredWaypoint = result.hoveredWaypoint();

        drawTopControlBackground(screenWidth);
        drawDimensionSelector();
        drawLayerToggles(screenWidth);
        drawSearchSuggestion();
        if (structurePanelOpen) drawStructurePanel(screenWidth, screenHeight);

        if (hoveredWaypoint != null && !contextMenuOpen && shouldShowWaypointTooltips(module)) {
            String tooltip = "Waypoint: " + hoveredWaypoint.name + " [" + hoveredWaypoint.x + ", " + hoveredWaypoint.z + "]";
            theme.textRenderer().begin(1.0);
            double tx = mouseX + 12;
            double ty = mouseY + 12;
            if (tx + 200 > screenWidth) tx = mouseX - 200;
            if (ty + 20 > screenHeight) ty = mouseY - 30;
            theme.textRenderer().render(tooltip, tx, ty, SeedRenderer.WAYPOINT_MARKER_COLOR);
            theme.textRenderer().end();
        }

        if (contextMenuOpen) {
            drawContextMenu();
        }
    }

    private EnumSet<MapLayer> getEnabledLayers(SeedExplorerModule module) {
        EnumSet<MapLayer> layers = EnumSet.allOf(MapLayer.class);
        if (module == null) return layers;

        for (MapLayer layer : MapLayer.values()) {
            if (!module.isLayerEnabled(layer)) layers.remove(layer);
        }
        return layers;
    }

    private boolean shouldShowPlayerInfo(SeedExplorerModule module) {
        return module == null || module.showPlayerInfo.get();
    }

    private int generationMargin(SeedExplorerModule module) {
        return module == null ? 10 : module.generationMargin.get();
    }

    private int biomeTileMargin(SeedExplorerModule module) {
        return module == null ? 2 : module.biomeTileMargin.get();
    }

    private int biomeOpacity(SeedExplorerModule module) {
        return module == null ? 255 : module.biomeOpacity.get();
    }

    private int oreOpacity(SeedExplorerModule module) {
        return module == null ? 200 : module.oreOpacity.get();
    }

    private double oreMarkerScale(SeedExplorerModule module) {
        return module == null ? 1.0 : module.oreMarkerScale.get();
    }

    private double structureMarkerScale(SeedExplorerModule module) {
        return module == null ? 1.0 : module.structureMarkerScale.get();
    }

    private double waypointMarkerScale(SeedExplorerModule module) {
        return module == null ? 1.0 : module.waypointMarkerScale.get();
    }

    private boolean dimWaypointStructures(SeedExplorerModule module) {
        return module == null || module.dimWaypointStructures.get();
    }

    private boolean loadedTerrainOnMap(SeedExplorerModule module) {
        return module != null && module.loadedTerrainOnMap.get();
    }

    private boolean shouldShowWaypointTooltips(SeedExplorerModule module) {
        return module == null || module.waypointTooltips.get();
    }

    private void syncDimensionToPlayer(Minecraft mc) {
        int playerDimension = currentDimensionId(mc);
        if (selectedDimension == playerDimension) return;

        selectedDimension = playerDimension;
        if (mc.player != null) {
            offsetX = mc.player.getX();
            offsetZ = mc.player.getZ();
        }
        selectedStructure = null;
        selectedWaypoint = null;
        contextMenuOpen = false;
        lastSearchText = "";
        searchStructureFilter = null;
        searchSuggestion = "";
        StructureCache.get().clear();
        OreCache.get().clear();
    }

    private void centerOnCurrentPlayer() {
        Minecraft mc = Minecraft.getInstance();
        selectedDimension = currentDimensionId(mc);
        if (mc.player == null) return;

        offsetX = mc.player.getX();
        offsetZ = mc.player.getZ();
    }

    private void applyPlayerAutoCenter(Minecraft mc, SeedExplorerModule module) {
        if (module == null || !module.autoCenterPlayer.get() || mc.player == null || mc.level == null) return;
        if (currentDimensionId(mc) != selectedDimension) return;

        offsetX = mc.player.getX();
        offsetZ = mc.player.getZ();
    }

    private int currentDimensionId(Minecraft mc) {
        if (mc.level == null) return 0;
        if (mc.level.dimension() == Level.NETHER) return -1;
        if (mc.level.dimension() == Level.END) return 1;
        return 0;
    }

    private void drawDimensionSelector() {
        int wpCount = WaypointManager.get().getSeedWaypoints(selectedDimension).size();
        String label = dimensionName(selectedDimension) + "  |  WP: " + wpCount;
        int width = DIMENSION_BUTTON_WIDTH * 2 + 28;

        Renderer2D r = Renderer2D.COLOR;
        r.begin();
        r.quad(DIMENSION_BUTTON_X + 2, DIMENSION_BUTTON_Y + 2, width, DIMENSION_BUTTON_HEIGHT, new Color(0, 0, 0, 95));
        r.quad(DIMENSION_BUTTON_X, DIMENSION_BUTTON_Y, width, DIMENSION_BUTTON_HEIGHT, new Color(27, 45, 66, 232));
        r.quad(DIMENSION_BUTTON_X, DIMENSION_BUTTON_Y, width, 2, new Color(77, 134, 172, 210));
        r.boxLines(DIMENSION_BUTTON_X, DIMENSION_BUTTON_Y, width, DIMENSION_BUTTON_HEIGHT, new Color(122, 192, 210, 210));
        r.render();
        theme.textRenderer().begin(1.0);
        theme.textRenderer().render(label, DIMENSION_BUTTON_X + 8, DIMENSION_BUTTON_Y + 4, Color.WHITE);
        theme.textRenderer().end();
    }

    private void drawLayerToggles(double screenWidth) {
        SeedExplorerModule module = Modules.get().get(SeedExplorerModule.class);
        if (module == null) return;

        int maxPerRow = maxLayerButtonsPerRow(screenWidth);
        int buttonWidth = layerButtonWidth(screenWidth);
        Renderer2D r = Renderer2D.COLOR;
        r.begin();

        int i = 0;
        for (MapLayer layer : MapLayer.values()) {
            int row = i / maxPerRow;
            int col = i % maxPerRow;
            int x = LAYER_BUTTON_X + col * (buttonWidth + LAYER_BUTTON_GAP);
            int y = LAYER_BUTTON_Y + row * (LAYER_BUTTON_HEIGHT + LAYER_BUTTON_GAP);
            boolean enabled = module.isLayerEnabled(layer);
            boolean panelOpen = layer == MapLayer.STRUCTURES && structurePanelOpen;
            Color shadow = new Color(0, 0, 0, 80);
            Color bgColor = enabled ? new Color(26, 58, 66, 228) : new Color(25, 27, 33, 205);
            Color topLine = enabled ? new Color(74, 146, 162, 225) : new Color(64, 68, 78, 170);
            Color border = panelOpen
                ? new Color(230, 205, 95, 235)
                : enabled ? new Color(84, 178, 188, 215) : new Color(76, 79, 88, 165);

            r.quad(x + 1, y + 2, buttonWidth, LAYER_BUTTON_HEIGHT, shadow);
            r.quad(x, y, buttonWidth, LAYER_BUTTON_HEIGHT, bgColor);
            r.quad(x, y, buttonWidth, 2, topLine);
            r.boxLines(x, y, buttonWidth, LAYER_BUTTON_HEIGHT, border);
            i++;
        }

        r.render();

        drawLayerIcons(maxPerRow, module, buttonWidth);

        theme.textRenderer().begin(1.0);
        i = 0;
        for (MapLayer layer : MapLayer.values()) {
            int row = i / maxPerRow;
            int col = i % maxPerRow;
            int x = LAYER_BUTTON_X + col * (buttonWidth + LAYER_BUTTON_GAP);
            int y = LAYER_BUTTON_Y + row * (LAYER_BUTTON_HEIGHT + LAYER_BUTTON_GAP);
            boolean enabled = module.isLayerEnabled(layer);
            Color textColor = enabled ? Color.WHITE : new Color(150, 150, 155);
            double textX = x + (layerIcon(layer) == null ? 8 : 27);
            double maxTextWidth = buttonWidth - (layerIcon(layer) == null ? 15 : 34);
            theme.textRenderer().render(trimToWidth(layerLabel(layer), maxTextWidth), textX, y + 5, textColor);
            i++;
        }
        theme.textRenderer().end();
    }

    private void drawLayerIcons(int maxPerRow, SeedExplorerModule module, int buttonWidth) {
        Renderer2D tex = Renderer2D.TEXTURE;
        int i = 0;
        for (MapLayer layer : MapLayer.values()) {
            Texture icon = layerIcon(layer);
            if (icon == null) {
                i++;
                continue;
            }

            int row = i / maxPerRow;
            int col = i % maxPerRow;
            int x = LAYER_BUTTON_X + col * (buttonWidth + LAYER_BUTTON_GAP);
            int y = LAYER_BUTTON_Y + row * (LAYER_BUTTON_HEIGHT + LAYER_BUTTON_GAP);
            boolean enabled = module.isLayerEnabled(layer);
            Color tint = enabled ? Color.WHITE : new Color(130, 130, 135, 120);
            tex.begin();
            tex.texQuad(x + 6, y + 4, 14, 14, tint);
            tex.render(icon.getTextureView(), icon.getSampler());
            i++;
        }
    }

    private String layerLabel(MapLayer layer) {
        return switch (layer) {
            case CHUNK_BORDERS -> "Chunks";
            case COORDINATES -> "Coords";
            default -> layer.title;
        };
    }

    private Texture layerIcon(MapLayer layer) {
        return switch (layer) {
            case TERRAIN -> UiIcons.get("terrain");
            case ORES -> UiIcons.get("ores");
            case WAYPOINTS -> UiIcons.get("waypoints");
            case PLAYER -> UiIcons.get("player");
            default -> null;
        };
    }

    private void drawTopControlBackground(double screenWidth) {
        Renderer2D r = Renderer2D.COLOR;
        r.begin();
        double height = topControlHeight(screenWidth);
        r.quad(0, 0, screenWidth, height, new Color(10, 12, 18, 190));
        r.quad(0, height - 1, screenWidth, 1, new Color(72, 128, 145, 145));
        r.render();
    }

    private void drawSearchSuggestion() {
        if (searchBar == null || searchStructureFilter == null || searchSuggestion.isBlank()) return;

        double x = searchBar.x;
        double y = searchBar.y + searchBar.height + 3;
        double width = Math.max(searchBar.width, RIGHT_CONTROLS_WIDTH);

        Renderer2D r = Renderer2D.COLOR;
        r.begin();
        r.quad(x, y, width, SEARCH_SUGGESTION_HEIGHT, new Color(16, 20, 27, 235));
        r.boxLines(x, y, width, SEARCH_SUGGESTION_HEIGHT, new Color(85, 170, 180, 185));
        r.render();

        Texture icon = StructureIcons.get(searchStructureFilter);
        if (icon != null) {
            Renderer2D tex = Renderer2D.TEXTURE;
            tex.begin();
            tex.texQuad(x + 5, y + 4, 15, 15, Color.WHITE);
            tex.render(icon.getTextureView(), icon.getSampler());
        }

        theme.textRenderer().begin(1.0);
        theme.textRenderer().render(searchSuggestion, x + (icon == null ? 8 : 25), y + 6, Color.WHITE);
        theme.textRenderer().end();
    }

    private boolean handleSearchSuggestionClick(double mouseX, double mouseY) {
        if (searchBar == null || searchStructureFilter == null || searchSuggestion.isBlank()) return false;

        double x = searchBar.x;
        double y = searchBar.y + searchBar.height + 3;
        double width = Math.max(searchBar.width, RIGHT_CONTROLS_WIDTH);
        if (mouseX < x || mouseX > x + width || mouseY < y || mouseY > y + SEARCH_SUGGESTION_HEIGHT) return false;

        jumpToSearchResult();
        return true;
    }

    private int topControlHeight(double screenWidth) {
        int rows = (MapLayer.values().length + maxLayerButtonsPerRow(screenWidth) - 1) / maxLayerButtonsPerRow(screenWidth);
        return Math.max(68, LAYER_BUTTON_Y + rows * (LAYER_BUTTON_HEIGHT + LAYER_BUTTON_GAP) + 8);
    }

    private int maxLayerButtonsPerRow(double screenWidth) {
        int buttonWidth = layerButtonWidth(screenWidth);
        double availableWidth = screenWidth - LAYER_BUTTON_X - RIGHT_CONTROLS_WIDTH - RIGHT_CONTROLS_PADDING * 2;
        if (availableWidth < buttonWidth) availableWidth = screenWidth - LAYER_BUTTON_X * 2;
        return Math.max(1, (int) ((availableWidth + LAYER_BUTTON_GAP) / (buttonWidth + LAYER_BUTTON_GAP)));
    }

    private int layerButtonWidth(double screenWidth) {
        return screenWidth < 760 ? LAYER_BUTTON_COMPACT_WIDTH : LAYER_BUTTON_WIDTH;
    }

    private void drawContextMenu() {
        if (!isWaypointContextMenu && selectedStructure != null) {
            drawStructurePopup();
            return;
        }

        String[] items;
        int itemHeight = 20;
        int menuWidth;

        if (isWaypointContextMenu) {
            items = new String[]{"Copy Coords", "Delete Waypoint", "Center Map"};
            menuWidth = 160;
        } else {
            boolean hasWaypoint = selectedStructure != null && WaypointManager.get().hasWaypointAt(
                selectedStructure.x, selectedStructure.z, selectedDimension);
            if (hasWaypoint) {
                items = new String[]{"Copy Coords", "Remove Waypoint", "Center Map", "TP"};
            } else {
                items = new String[]{"Copy Coords", "Create Waypoint", "Center Map", "TP"};
            }
            menuWidth = 160;
        }

        int menuHeight = items.length * itemHeight;

        // Clamp to screen
        Minecraft mc = Minecraft.getInstance();
        double screenWidth = mc.getWindow().getWidth();
        double screenHeight = mc.getWindow().getHeight();

        double mx = Math.min(contextMenuX, screenWidth - menuWidth);
        double my = Math.min(contextMenuY, screenHeight - menuHeight);
        if (mx < 0) mx = 0;
        if (my < 0) my = 0;

        // Background
        Renderer2D r = Renderer2D.COLOR;
        r.begin();
        r.quad(mx, my, menuWidth, menuHeight, new Color(20, 20, 30, 230));
        r.render();

        // Items
        theme.textRenderer().begin(1.0);
        for (int i = 0; i < items.length; i++) {
            double iy = my + i * itemHeight;
            Color itemColor = new Color(200, 200, 200);
            theme.textRenderer().render(items[i], mx + 8, iy + 4, itemColor);
        }
        theme.textRenderer().end();

        // Item dividers
        r.begin();
        for (int i = 1; i < items.length; i++) {
            double iy = my + i * itemHeight;
            r.quad(mx, iy, menuWidth, 1, new Color(60, 60, 80, 200));
        }
        r.render();
    }

    private void drawStructurePopup() {
        GeneratedStructure structure = selectedStructure;
        if (structure == null) return;

        boolean hasWaypoint = WaypointManager.get().hasWaypointAt(structure.x, structure.z, selectedDimension);
        boolean completed = isStructureCompleted(structure);
        String[] actions = structurePopupActions(hasWaypoint);
        int menuWidth = STRUCTURE_POPUP_WIDTH;
        int menuHeight = STRUCTURE_POPUP_TOP_HEIGHT + STRUCTURE_POPUP_GAP + actions.length * STRUCTURE_POPUP_ACTION_HEIGHT;

        Minecraft mc = Minecraft.getInstance();
        double screenWidth = mc.getWindow().getWidth();
        double screenHeight = mc.getWindow().getHeight();
        double mx = Math.min(contextMenuX - menuWidth / 2.0, screenWidth - menuWidth - 4);
        double my = Math.min(contextMenuY - menuHeight - 12, screenHeight - menuHeight - 4);
        if (mx < 4) mx = 4;
        if (my < 4) my = 4;

        Renderer2D r = Renderer2D.COLOR;
        r.begin();
        r.quad(mx, my, menuWidth, STRUCTURE_POPUP_TOP_HEIGHT, new Color(248, 248, 248, 245));
        r.boxLines(mx, my, menuWidth, STRUCTURE_POPUP_TOP_HEIGHT, new Color(210, 210, 218, 230));
        double pointerX = Math.max(mx + 22, Math.min(mx + menuWidth - 22, contextMenuX));
        r.triangle(pointerX - 8, my + STRUCTURE_POPUP_TOP_HEIGHT, pointerX + 8, my + STRUCTURE_POPUP_TOP_HEIGHT, pointerX, my + STRUCTURE_POPUP_TOP_HEIGHT + 8, new Color(248, 248, 248, 245));
        r.render();

        Texture icon = StructureIcons.get(structure);
        if (icon != null) {
            Renderer2D tex = Renderer2D.TEXTURE;
            tex.begin();
            tex.texQuad(mx + 8, my + 8, 20, 20, Color.WHITE);
            tex.render(icon.getTextureView(), icon.getSampler());
        }

        String title = trimToWidth(structure.displayName(), menuWidth - 42);
        String coords = "X: " + structure.x + " Z: " + structure.z;
        theme.textRenderer().begin(1.0);
        theme.textRenderer().render(title, mx + 32, my + 9, new Color(25, 25, 28));
        theme.textRenderer().render(coords, mx + (menuWidth - theme.textWidth(coords)) / 2.0, my + 31, new Color(25, 25, 28));
        theme.textRenderer().end();

        double completedY = my + 52;
        r.begin();
        r.quad(mx + 10, completedY, menuWidth - 20, 25, completed ? new Color(34, 118, 96, 230) : new Color(232, 234, 238, 235));
        r.boxLines(mx + 10, completedY, menuWidth - 20, 25, completed ? new Color(75, 220, 174, 230) : new Color(178, 182, 190, 230));
        r.boxLines(mx + 20, completedY + 7, 11, 11, completed ? Color.WHITE : new Color(95, 98, 106));
        if (completed) {
            r.quad(mx + 23, completedY + 10, 5, 5, Color.WHITE);
        }
        r.render();

        String completedText = "Completed";
        theme.textRenderer().begin(1.0);
        theme.textRenderer().render(completedText, mx + 40, completedY + 8, completed ? Color.WHITE : new Color(42, 44, 50));
        theme.textRenderer().end();

        double actionY = my + STRUCTURE_POPUP_TOP_HEIGHT + STRUCTURE_POPUP_GAP;
        for (int i = 0; i < actions.length; i++) {
            double y = actionY + i * STRUCTURE_POPUP_ACTION_HEIGHT;
            Color bg = i == 1 && hasWaypoint ? new Color(72, 42, 48, 220) : new Color(22, 26, 34, 225);
            r.begin();
            r.quad(mx, y, menuWidth, STRUCTURE_POPUP_ACTION_HEIGHT - 1, bg);
            r.boxLines(mx, y, menuWidth, STRUCTURE_POPUP_ACTION_HEIGHT - 1, new Color(68, 104, 116, 150));
            r.render();
            theme.textRenderer().begin(1.0);
            theme.textRenderer().render(actions[i], mx + 9, y + 5, Color.WHITE);
            theme.textRenderer().end();
        }
    }

    private String[] structurePopupActions(boolean hasWaypoint) {
        return new String[]{"Copy Coordinates", hasWaypoint ? "Remove Waypoint" : "Create Waypoint", "Center Map", "TP"};
    }

    private String trimToWidth(String text, double maxWidth) {
        if (theme.textWidth(text) <= maxWidth) return text;
        String suffix = "...";
        for (int i = text.length() - 1; i > 0; i--) {
            String candidate = text.substring(0, i) + suffix;
            if (theme.textWidth(candidate) <= maxWidth) return candidate;
        }
        return suffix;
    }

    private void drawStructurePanel(double screenWidth, double screenHeight) {
        List<StructureType> types = structureTypesForDimension(selectedDimension);
        int rowHeight = 23;
        int panelWidth = 260;
        int panelX = 10;
        int panelY = topControlHeight(screenWidth) + 8;
        int panelHeight = Math.min((int) screenHeight - panelY - 12, 30 + types.size() * rowHeight);

        Renderer2D r = Renderer2D.COLOR;
        r.begin();
        r.quad(panelX, panelY, panelWidth, panelHeight, new Color(15, 18, 24, 230));
        r.boxLines(panelX, panelY, panelWidth, panelHeight, new Color(78, 150, 160, 190));
        r.render();

        theme.textRenderer().begin(1.0);
        theme.textRenderer().render(dimensionName(selectedDimension) + " Structures", panelX + 10, panelY + 8, Color.WHITE);
        theme.textRenderer().end();

        int visibleRows = Math.max(0, (panelHeight - 30) / rowHeight);
        for (int i = 0; i < Math.min(types.size(), visibleRows); i++) {
            StructureType type = types.get(i);
            int y = panelY + 28 + i * rowHeight;
            boolean enabled = SeedManager.get().getProfileStructure(type.name(), true);
            Color rowColor = enabled ? new Color(36, 48, 56, 160) : new Color(30, 31, 36, 135);
            r.begin();
            r.quad(panelX + 6, y, panelWidth - 12, rowHeight - 2, rowColor);
            r.boxLines(panelX + 12, y + 4, 10, 10, enabled ? new Color(80, 210, 170, 230) : new Color(120, 120, 126, 170));
            if (enabled) r.quad(panelX + 15, y + 7, 4, 4, new Color(110, 235, 185, 245));
            r.render();

            Texture icon = StructureIcons.get(type);
            if (icon != null) {
                Renderer2D tex = Renderer2D.TEXTURE;
                tex.begin();
                tex.texQuad(panelX + 30, y + 3, 16, 16, enabled ? Color.WHITE : new Color(135, 135, 140, 115));
                tex.render(icon.getTextureView(), icon.getSampler());
            }

            theme.textRenderer().begin(1.0);
            theme.textRenderer().render(type.displayName, panelX + 52, y + 5, enabled ? Color.WHITE : new Color(150, 150, 155));
            theme.textRenderer().end();
        }
    }

    private void handleContextMenuClick(double mouseX, double mouseY) {
        if (!isWaypointContextMenu && selectedStructure != null) {
            handleStructurePopupClick(mouseX, mouseY);
            return;
        }

        int itemHeight = 20;
        int menuWidth = 160;

        String[] items;
        if (isWaypointContextMenu) {
            items = new String[]{"Copy Coords", "Delete Waypoint", "Center Map"};
        } else {
            boolean hasWaypoint = selectedStructure != null && WaypointManager.get().hasWaypointAt(
                selectedStructure.x, selectedStructure.z, selectedDimension);
            items = new String[]{"Copy Coords", hasWaypoint ? "Remove Waypoint" : "Create Waypoint", "Center Map", "TP"};
        }
        int menuHeight = items.length * itemHeight;

        double mx = Math.min(contextMenuX, Minecraft.getInstance().getWindow().getWidth() - menuWidth);
        double my = Math.min(contextMenuY, Minecraft.getInstance().getWindow().getHeight() - menuHeight);
        if (mx < 0) mx = 0;
        if (my < 0) my = 0;

        int clickedIndex = -1;
        for (int i = 0; i < items.length; i++) {
            double ix = mx;
            double iy = my + i * itemHeight;
            if (mouseX >= ix && mouseX <= ix + menuWidth && mouseY >= iy && mouseY <= iy + itemHeight) {
                clickedIndex = i;
                break;
            }
        }

        if (clickedIndex >= 0) {
            if (isWaypointContextMenu) {
                handleWaypointContextAction(clickedIndex);
            } else {
                handleStructureContextAction(clickedIndex);
            }
        }

        contextMenuOpen = false;
        selectedStructure = null;
        selectedWaypoint = null;
    }

    private void handleStructurePopupClick(double mouseX, double mouseY) {
        boolean hasWaypoint = WaypointManager.get().hasWaypointAt(selectedStructure.x, selectedStructure.z, selectedDimension);
        String[] actions = structurePopupActions(hasWaypoint);
        int menuWidth = STRUCTURE_POPUP_WIDTH;
        int menuHeight = STRUCTURE_POPUP_TOP_HEIGHT + STRUCTURE_POPUP_GAP + actions.length * STRUCTURE_POPUP_ACTION_HEIGHT;

        Minecraft mc = Minecraft.getInstance();
        double mx = Math.min(contextMenuX - menuWidth / 2.0, mc.getWindow().getWidth() - menuWidth - 4);
        double my = Math.min(contextMenuY - menuHeight - 12, mc.getWindow().getHeight() - menuHeight - 4);
        if (mx < 4) mx = 4;
        if (my < 4) my = 4;

        double completedY = my + 52;
        if (mouseX >= mx + 10 && mouseX <= mx + menuWidth - 10 && mouseY >= completedY && mouseY <= completedY + 25) {
            toggleStructureCompleted(selectedStructure);
            return;
        }

        double actionY = my + STRUCTURE_POPUP_TOP_HEIGHT + STRUCTURE_POPUP_GAP;
        int clickedIndex = -1;
        for (int i = 0; i < actions.length; i++) {
            double y = actionY + i * STRUCTURE_POPUP_ACTION_HEIGHT;
            if (mouseX >= mx && mouseX <= mx + menuWidth && mouseY >= y && mouseY <= y + STRUCTURE_POPUP_ACTION_HEIGHT) {
                clickedIndex = i;
                break;
            }
        }

        if (clickedIndex >= 0) {
            handleStructureContextAction(clickedIndex);
            if (clickedIndex == 1) return;
        }

        contextMenuOpen = false;
        selectedStructure = null;
        selectedWaypoint = null;
    }

    private void handleWaypointContextAction(int index) {
        if (selectedWaypoint == null) return;

        switch (index) {
            case 0 -> copyCoords(selectedWaypoint.x, selectedWaypoint.z);
            case 1 ->
                // The visibleWaypoints list is rebuilt every frame from the render result and
                // may be an immutable List.of() when the waypoint layer is disabled, so removing
                // from it here would throw. Removing from the manager is enough.
                WaypointManager.get().removeWaypoint(selectedWaypoint);
            case 2 -> centerMap(selectedWaypoint.x, selectedWaypoint.z);
        }
    }

    private void handleStructureContextAction(int index) {
        if (selectedStructure == null) return;

        switch (index) {
            case 0 -> copyCoords(selectedStructure.x, selectedStructure.z);
            case 1 -> {
                if (WaypointManager.get().hasWaypointAt(selectedStructure.x, selectedStructure.z, selectedDimension)) {
                    WaypointManager.get().removeWaypointAt(selectedStructure.x, selectedStructure.z, selectedDimension);
                } else {
                    createWaypoint(selectedStructure);
                }
            }
            case 2 -> centerMap(selectedStructure.x, selectedStructure.z);
            case 3 -> teleportToStructure(selectedStructure);
        }
    }

    private boolean isStructureCompleted(GeneratedStructure structure) {
        return SeedManager.get().getCompletedStructure(structure.type.name(), selectedDimension, structure.x, structure.z);
    }

    private void toggleStructureCompleted(GeneratedStructure structure) {
        boolean completed = SeedManager.get().toggleCompletedStructure(structure.type.name(), selectedDimension, structure.x, structure.z);
        ChatUtils.info("Seed Explorer: Marked (highlight)" + structure.displayName() + "(default) as " + (completed ? "(highlight)completed(default)." : "(highlight)not completed(default)."));
    }

    private void teleportToStructure(GeneratedStructure gs) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.player.connection == null) return;

        mc.player.connection.sendCommand("tp " + gs.x + " 100 " + gs.z);
        ChatUtils.info("Seed Explorer: Sent teleport to (highlight)" + gs.x + ", 100, " + gs.z + "(default).");
    }

    private void copyCoords(int x, int z) {
        String coords = x + ", " + z;
        Minecraft.getInstance().keyboardHandler.setClipboard(coords);
    }

    private void createWaypoint(GeneratedStructure gs) {
        String name = gs.displayName() + " [" + gs.x + ", " + gs.z + "]";
        String icon = switch (gs.type) {
            case VILLAGE -> "square";
            case DESERT_PYRAMID, JUNGLE_TEMPLE, WITCH_HUT, IGLOO -> "triangle";
            case OUTPOST -> "skull";
            case MONUMENT, MANSION -> "diamond";
            case ANCIENT_CITY -> "star";
            case TRIAL_CHAMBER -> "diamond";
            case TRAIL_RUINS, RUINED_PORTAL, OCEAN_RUIN, MINESHAFT -> "circle";
            case FORTRESS, BASTION -> "skull";
            case END_CITY -> "star";
            default -> "circle";
        };
        String structureType = gs.displayName();
        WaypointManager.get().createWaypoint(name, gs.x, gs.z, selectedDimension, icon, structureType);
    }

    private void centerMap(int x, int z) {
        offsetX = x;
        offsetZ = z;
    }

    private double distanceSquared(int x1, int z1, int x2, int z2) {
        double dx = x1 - x2;
        double dz = z1 - z2;
        return dx * dx + dz * dz;
    }

    // ---- Search functionality ----

    private void updateSearchState() {
        if (searchBar == null) return;

        String query = searchBar.get().trim();
        if (query.equals(lastSearchText)) return;
        lastSearchText = query;

        if (query.isEmpty() || isCoordinateQuery(query)) {
            searchStructureFilter = null;
            searchSuggestion = "";
            return;
        }

        StructureType match = bestStructureMatch(query);
        if (match == null) {
            searchStructureFilter = null;
            searchSuggestion = "";
            return;
        }

        searchStructureFilter = match;
        searchSuggestion = "Nearest " + match.displayName;
    }

    private void onSearch() {
        if (searchBar == null) return;
        updateSearchState();
        String query = searchBar.get().trim();
        if (query.isEmpty()) return;

        // Try coordinate search first
        if (tryCoordinateSearch(query)) return;

        // Try structure search
        if (jumpToSearchResult()) return;

        // Try biome search
        if (tryBiomeSearch(query)) return;

        // No results found
        ChatUtils.info("Seed Explorer: No results found for \"(highlight)" + query + "(default)\".");
    }

    private void onSeedChanged() {
        if (applyingSeed) return;
        if (seedBox == null) return;

        applyingSeed = true;
        try {
            String seedText = seedBox.get().trim();
            if (seedText.isEmpty()) return;

            long seed = parseSeed(seedText);
            if (SeedManager.get().getWorldSeed() == seed) return;

            SeedManager.get().setWorldSeed(seed);
            StructureCache.get().clear();
            OreCache.get().clear();
            MinimapManager.get().clear();

            ChatUtils.info("Seed Explorer: Set seed to (highlight)" + seedText + "(default) (numeric: (highlight)" + seed + "(default)).");
        } finally {
            applyingSeed = false;
        }
    }

    private long parseSeed(String seedText) {
        try {
            return Long.parseLong(seedText);
        } catch (NumberFormatException ignored) {
            return seedText.hashCode();
        }
    }

    private boolean tryCoordinateSearch(String query) {
        int[] coords = parseCoordinateSearch(query);
        if (coords == null) return false;

        centerMap(coords[0], coords[1]);
        ChatUtils.info("Seed Explorer: Centered on coordinates (highlight)" + coords[0] + ", " + coords[1] + "(default).");
        return true;
    }

    private boolean isCoordinateQuery(String query) {
        return parseCoordinateSearch(query) != null;
    }

    private int[] parseCoordinateSearch(String query) {
        String normalized = query.trim()
            .replace(",", " ")
            .replace("~", " ");
        if (normalized.isEmpty()) return null;

        List<Integer> numbers = new ArrayList<>();
        for (String part : normalized.split("\\s+")) {
            if (part.isBlank()) continue;
            try {
                numbers.add(Integer.parseInt(part));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }

        if (numbers.size() == 2) return new int[]{numbers.get(0), numbers.get(1)};
        if (numbers.size() == 3) return new int[]{numbers.get(0), numbers.get(2)};
        return null;
    }

    private StructureType bestStructureMatch(String query) {
        String normalizedQuery = normalizeSearchText(query);
        if (normalizedQuery.isBlank()) return null;

        StructureType best = null;
        int bestScore = Integer.MIN_VALUE;
        for (StructureType type : StructureType.values()) {
            if (!type.hasMapPrediction()) continue;
            if (type.dimension != selectedDimension) continue;

            int score = structureMatchScore(normalizedQuery, type);
            if (score > bestScore) {
                bestScore = score;
                best = type;
            }
        }

        return bestScore >= 18 ? best : null;
    }

    private int structureMatchScore(String normalizedQuery, StructureType type) {
        int best = Integer.MIN_VALUE;
        for (String alias : structureAliases(type)) {
            String normalizedAlias = normalizeSearchText(alias);
            if (normalizedAlias.isBlank()) continue;
            if (normalizedAlias.equals(normalizedQuery)) return 1000;
            if (normalizedAlias.startsWith(normalizedQuery)) best = Math.max(best, 260 - normalizedAlias.length());
            if (normalizedAlias.contains(normalizedQuery)) best = Math.max(best, 210 - normalizedAlias.length());
            if (isSubsequence(normalizedQuery, normalizedAlias)) best = Math.max(best, 120 - normalizedAlias.length());

            int distance = levenshtein(normalizedQuery, normalizedAlias);
            int maxLength = Math.max(normalizedQuery.length(), normalizedAlias.length());
            int fuzzy = 100 - distance * 12 - Math.max(0, maxLength - normalizedQuery.length()) * 2;
            best = Math.max(best, fuzzy);
        }
        return best;
    }

    private List<String> structureAliases(StructureType type) {
        List<String> aliases = new ArrayList<>();
        aliases.add(type.displayName);
        aliases.add(type.name());
        aliases.add(type.name().replace('_', ' '));

        switch (type) {
            case DESERT_PYRAMID -> {
                aliases.add("desert temple");
                aliases.add("temple");
                aliases.add("pyramid");
            }
            case JUNGLE_TEMPLE -> {
                aliases.add("jungle pyramid");
                aliases.add("jungle temple");
                aliases.add("temple");
            }
            case WITCH_HUT -> aliases.add("swamp hut");
            case OUTPOST -> aliases.add("pillager tower");
            case MONUMENT -> aliases.add("ocean monument");
            case MANSION -> aliases.add("woodland mansion");
            case TREASURE -> aliases.add("buried treasure chest");
            case FORTRESS -> aliases.add("nether fortress");
            case BASTION -> {
                aliases.add("bastion");
                aliases.add("bastion remnant");
            }
            case NETHER_RUINED_PORTAL -> aliases.add("nether portal");
            default -> {
            }
        }

        return aliases;
    }

    private String normalizeSearchText(String text) {
        return text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private boolean isSubsequence(String query, String target) {
        int qi = 0;
        for (int ti = 0; ti < target.length() && qi < query.length(); ti++) {
            if (query.charAt(qi) == target.charAt(ti)) qi++;
        }
        return qi == query.length();
    }

    private int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) prev[j] = j;

        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] temp = prev;
            prev = curr;
            curr = temp;
        }

        return prev[b.length()];
    }

    private boolean jumpToSearchResult() {
        StructureType matchedType = searchStructureFilter;
        if (matchedType == null && searchBar != null) matchedType = bestStructureMatch(searchBar.get());
        if (matchedType == null) return false;

        long seed = SeedManager.get().getWorldSeed();
        if (seed == 0) {
            ChatUtils.info("Seed Explorer: No seed set.");
            return false;
        }
        // Start from current view center and search expanding regions
        int centerBlockX = (int) offsetX;
        int centerBlockZ = (int) offsetZ;
        int radiusChunks = matchedType == StructureType.STRONGHOLD ? 1024 : 320;
        List<GeneratedStructure> candidates = VanillaStructurePredictor.predictDimension(
            seed,
            selectedDimension,
            Math.floorDiv(centerBlockX, 16) - radiusChunks,
            Math.floorDiv(centerBlockZ, 16) - radiusChunks,
            Math.floorDiv(centerBlockX, 16) + radiusChunks,
            Math.floorDiv(centerBlockZ, 16) + radiusChunks,
            true
        );
        StructureType searchType = matchedType;
        GeneratedStructure closest = candidates.stream()
            .filter(s -> s.type == searchType)
            .min((a, b) -> Double.compare(distanceSquared(a.x, a.z, centerBlockX, centerBlockZ), distanceSquared(b.x, b.z, centerBlockX, centerBlockZ)))
            .orElse(null);

        if (closest != null) {
            centerMap(closest.x, closest.z);
            searchStructureFilter = matchedType;
            searchSuggestion = "Nearest " + matchedType.displayName;
            ChatUtils.info("Seed Explorer: Found (highlight)" + closest.displayName() + "(default) at (highlight)" + closest.x + ", " + closest.z + "(default).");
            return true;
        }

        ChatUtils.info("Seed Explorer: No (highlight)" + matchedType.displayName + "(default) found within search range.");
        return false;
    }

    private boolean tryBiomeSearch(String query) {
        // Match query against biome name map (case-insensitive)
        String lowerQuery = query.toLowerCase();
        Integer biomeId = null;

        // Try exact match first
        if (BIOME_NAME_TO_ID.containsKey(lowerQuery)) {
            biomeId = BIOME_NAME_TO_ID.get(lowerQuery);
        } else {
            // Try partial match
            for (Map.Entry<String, Integer> entry : BIOME_NAME_TO_ID.entrySet()) {
                if (entry.getKey().contains(lowerQuery)) {
                    biomeId = entry.getValue();
                    break;
                }
            }
        }

        if (biomeId == null) return false;

        long seed = SeedManager.get().getWorldSeed();
        if (seed == 0) {
            ChatUtils.info("Seed Explorer: No seed set.");
            return false;
        }

        int centerBlockX = (int) offsetX;
        int centerBlockZ = (int) offsetZ;

        // Search in expanding squares with step of 16 blocks (1 chunk)
        int maxRadius = 10000; // Max search radius in blocks
        int step = 16; // Check every chunk

        for (int radius = 0; radius <= maxRadius; radius += step) {
            // Search the perimeter of the current radius
            // Top and bottom edges
            for (int bx = -radius; bx <= radius; bx += step) {
                if (radius == 0) {
                    int id = BiomeGenerator.getBiome(centerBlockX, centerBlockZ, seed);
                    if (id == biomeId) {
                        centerMap(centerBlockX, centerBlockZ);
                        ChatUtils.info("Seed Explorer: Found biome at (highlight)" + centerBlockX + ", " + centerBlockZ + "(default).");
                        return true;
                    }
                    continue;
                }

                // Top edge: z = -radius
                int idTop = BiomeGenerator.getBiome(centerBlockX + bx, centerBlockZ - radius, seed);
                if (idTop == biomeId) {
                    centerMap(centerBlockX + bx, centerBlockZ - radius);
                    ChatUtils.info("Seed Explorer: Found biome at (highlight)" + (centerBlockX + bx) + ", " + (centerBlockZ - radius) + "(default).");
                    return true;
                }

                // Bottom edge: z = radius
                int idBottom = BiomeGenerator.getBiome(centerBlockX + bx, centerBlockZ + radius, seed);
                if (idBottom == biomeId) {
                    centerMap(centerBlockX + bx, centerBlockZ + radius);
                    ChatUtils.info("Seed Explorer: Found biome at (highlight)" + (centerBlockX + bx) + ", " + (centerBlockZ + radius) + "(default).");
                    return true;
                }
            }

            // Left and right edges (excluding corners already checked)
            for (int bz = -radius + step; bz <= radius - step; bz += step) {
                // Left edge: x = -radius
                int idLeft = BiomeGenerator.getBiome(centerBlockX - radius, centerBlockZ + bz, seed);
                if (idLeft == biomeId) {
                    centerMap(centerBlockX - radius, centerBlockZ + bz);
                    ChatUtils.info("Seed Explorer: Found biome at (highlight)" + (centerBlockX - radius) + ", " + (centerBlockZ + bz) + "(default).");
                    return true;
                }

                // Right edge: x = radius
                int idRight = BiomeGenerator.getBiome(centerBlockX + radius, centerBlockZ + bz, seed);
                if (idRight == biomeId) {
                    centerMap(centerBlockX + radius, centerBlockZ + bz);
                    ChatUtils.info("Seed Explorer: Found biome at (highlight)" + (centerBlockX + radius) + ", " + (centerBlockZ + bz) + "(default).");
                    return true;
                }
            }
        }

        ChatUtils.info("Seed Explorer: No (highlight)" + query + "(default) biome found within " + maxRadius + " blocks.");
        return false;
    }

    @Override
    public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent click, boolean doubled) {
        double mouseX = toRenderX(click.x());
        double mouseY = toRenderY(click.y());

        if (contextMenuOpen) {
            handleContextMenuClick(mouseX, mouseY);
            return true;
        }

        if (click.button() == 0 && handleSearchSuggestionClick(mouseX, mouseY)) {
            return true;
        }

        if (click.button() == 0 && handleDimensionSelectorClick(mouseX, mouseY)) {
            return true;
        }

        if (click.button() == 0 && handleLayerToggleClick(mouseX, mouseY)) {
            return true;
        }

        if (click.button() == 0 && handleStructurePanelClick(mouseX, mouseY)) {
            return true;
        }

        GeneratedStructure clickedStructure = pickStructureAt(mouseX, mouseY);
        if (click.button() == 0 && clickedStructure != null) {
            selectedStructure = clickedStructure;
            selectedWaypoint = null;
            isWaypointContextMenu = false;
            contextMenuOpen = true;
            contextMenuX = mouseX;
            contextMenuY = mouseY;
            return true;
        }

        // Let widgets (e.g. seed and search boxes) handle clicks after map controls/icons.
        if (super.mouseClicked(click, doubled)) {
            return true;
        }

        if (click.button() == 0) { // LEFT button - pan
            dragging = true;
            dragStartX = mouseX;
            dragStartY = mouseY;
            dragOffsetX = offsetX;
            dragOffsetZ = offsetZ;
            return true;
        }

        if (click.button() == 1) { // RIGHT button - context menu
            if (clickedStructure != null) {
                selectedStructure = clickedStructure;
                selectedWaypoint = null;
                isWaypointContextMenu = false;
                contextMenuOpen = true;
                contextMenuX = mouseX;
                contextMenuY = mouseY;
                return true;
            }
            if (hoveredWaypoint != null) {
                selectedWaypoint = hoveredWaypoint;
                selectedStructure = null;
                isWaypointContextMenu = true;
                contextMenuOpen = true;
                contextMenuX = mouseX;
                contextMenuY = mouseY;
                return true;
            }
        }

        return false;
    }

    private GeneratedStructure pickStructureAt(double mouseX, double mouseY) {
        if (visibleStructures.isEmpty()) return hoveredStructure;

        Minecraft mc = Minecraft.getInstance();
        MapViewport viewport = MapViewport.create(offsetX, offsetZ, zoom, mc.getWindow().getWidth(), mc.getWindow().getHeight());
        SeedExplorerModule module = Modules.get().get(SeedExplorerModule.class);
        MapRenderContext context = new MapRenderContext(
            viewport,
            (int) Math.round(mouseX),
            (int) Math.round(mouseY),
            0,
            selectedDimension,
            getEnabledLayers(module),
            shouldShowPlayerInfo(module),
            generationMargin(module),
            biomeTileMargin(module),
            biomeOpacity(module),
            oreOpacity(module),
            oreMarkerScale(module),
            structureMarkerScale(module),
            waypointMarkerScale(module),
            dimWaypointStructures(module),
            loadedTerrainOnMap(module),
            searchStructureFilter
        );

        return seedRenderer.pickStructure(context, visibleStructures);
    }

    private boolean handleDimensionSelectorClick(double mouseX, double mouseY) {
        return false;
    }

    private boolean handleLayerToggleClick(double mouseX, double mouseY) {
        SeedExplorerModule module = Modules.get().get(SeedExplorerModule.class);
        if (module == null) return false;

        double screenWidth = Minecraft.getInstance().getWindow().getWidth();
        int maxPerRow = maxLayerButtonsPerRow(screenWidth);
        int buttonWidth = layerButtonWidth(screenWidth);

        int i = 0;
        for (MapLayer layer : MapLayer.values()) {
            int row = i / maxPerRow;
            int col = i % maxPerRow;
            int x = LAYER_BUTTON_X + col * (buttonWidth + LAYER_BUTTON_GAP);
            int y = LAYER_BUTTON_Y + row * (LAYER_BUTTON_HEIGHT + LAYER_BUTTON_GAP);
            if (mouseX >= x && mouseX <= x + buttonWidth && mouseY >= y && mouseY <= y + LAYER_BUTTON_HEIGHT) {
                if (layer == MapLayer.STRUCTURES) {
                    structurePanelOpen = !structurePanelOpen;
                } else {
                    module.setLayerEnabled(layer, !module.isLayerEnabled(layer));
                }
                return true;
            }
            i++;
        }

        return false;
    }

    private boolean handleStructurePanelClick(double mouseX, double mouseY) {
        if (!structurePanelOpen) return false;
        Minecraft mc = Minecraft.getInstance();
        double screenWidth = mc.getWindow().getWidth();
        double screenHeight = mc.getWindow().getHeight();
        List<StructureType> types = structureTypesForDimension(selectedDimension);
        int rowHeight = 23;
        int panelWidth = 260;
        int panelX = 10;
        int panelY = topControlHeight(screenWidth) + 8;
        int panelHeight = Math.min((int) screenHeight - panelY - 12, 30 + types.size() * rowHeight);
        if (mouseX < panelX || mouseX > panelX + panelWidth || mouseY < panelY || mouseY > panelY + panelHeight) return false;

        int index = (int) ((mouseY - panelY - 28) / rowHeight);
        if (index >= 0 && index < types.size()) {
            StructureType type = types.get(index);
            boolean enabled = SeedManager.get().getProfileStructure(type.name(), true);
            SeedManager.get().setProfileStructure(type.name(), !enabled);
            return true;
        }

        return true;
    }

    private List<StructureType> structureTypesForDimension(int dimension) {
        List<StructureType> result = new ArrayList<>();
        for (StructureType type : StructureType.values()) {
            if (type.dimension == dimension && type.hasMapPrediction()) result.add(type);
        }
        return result;
    }

    private String dimensionName(int dimension) {
        return switch (dimension) {
            case -1 -> "Nether";
            case 1 -> "End";
            default -> "Overworld";
        };
    }

    @Override
    public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent click) {
        if (click.button() == 0) {
            dragging = false;
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        super.mouseMoved(mouseX, mouseY);

        if (dragging) {
            double renderX = toRenderX(mouseX);
            double renderY = toRenderY(mouseY);
            double dx = renderX - dragStartX;
            double dy = renderY - dragStartY;
            offsetX = dragOffsetX - dx / zoom;
            offsetZ = dragOffsetZ - dy / zoom;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double renderX = toRenderX(mouseX);
        double renderY = toRenderY(mouseY);
        SeedExplorerModule module = Modules.get().get(SeedExplorerModule.class);
        double zoomStep = module == null ? 1.2 : module.zoomStep.get();
        double oldZoom = targetZoom;
        if (verticalAmount > 0) targetZoom *= zoomStep;
        else targetZoom /= zoomStep;
        double minZoom = module == null ? 0.01 : module.minZoom.get();
        double maxZoom = module == null ? 100.0 : module.maxZoom.get();
        targetZoom = Mth.clamp(targetZoom, Math.min(minZoom, maxZoom), Math.max(minZoom, maxZoom));

        Minecraft mc = Minecraft.getInstance();
        double screenWidth = mc.getWindow().getWidth();
        double screenHeight = mc.getWindow().getHeight();

        // Adjust offset so we zoom into the mouse position
        offsetX += (renderX - screenWidth / 2.0) / oldZoom - (renderX - screenWidth / 2.0) / targetZoom;
        offsetZ += (renderY - screenHeight / 2.0) / oldZoom - (renderY - screenHeight / 2.0) / targetZoom;

        return true;
    }

    private void updateSmoothZoom(float delta) {
        if (Math.abs(targetZoom - zoom) < 0.0001) {
            zoom = targetZoom;
            return;
        }

        double t = Math.min(1.0, Math.max(0.1, delta * 0.35));
        zoom += (targetZoom - zoom) * t;
    }

    private double toRenderX(double guiX) {
        return guiX * Minecraft.getInstance().getWindow().getGuiScale();
    }

    private double toRenderY(double guiY) {
        return guiY * Minecraft.getInstance().getWindow().getGuiScale();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent key) {
        if (contextMenuOpen) {
            contextMenuOpen = false;
            selectedStructure = null;
            selectedWaypoint = null;
            return true;
        }

        if (seedBox != null && seedBox.isFocused() && isEnterKey(key)) {
            suppressNextEnterAction = true;
        }

        if (searchBar != null && searchBar.isFocused() && isEnterKey(key)) {
            suppressNextEnterAction = true;
            onSearch();
            searchBar.setFocused(false);
            return true;
        }

        // Let widgets handle keys first
        if (super.keyPressed(key)) {
            return true;
        }

        return false;
    }

    @Override
    public boolean keyReleased(net.minecraft.client.input.KeyEvent key) {
        if (suppressNextEnterAction && isEnterKey(key)) {
            suppressNextEnterAction = false;
            return true;
        }

        return super.keyReleased(key);
    }

    private boolean isEnterKey(net.minecraft.client.input.KeyEvent key) {
        return key.key() == KEY_ENTER || key.key() == KEY_KP_ENTER;
    }
}
