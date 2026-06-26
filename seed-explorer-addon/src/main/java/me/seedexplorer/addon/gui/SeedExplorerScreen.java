/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.gui;

import me.seedexplorer.addon.map.ChunkTile;
import me.seedexplorer.addon.map.MinimapManager;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.WidgetScreen;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.utils.render.color.Color;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.Mth;

import static org.lwjgl.glfw.GLFW.*;

/** Fullscreen map screen for seed exploration. */
public class SeedExplorerScreen extends WidgetScreen {
    private double offsetX, offsetZ;
    private double zoom = 1.0;

    private boolean lastMouseLeftPressed;
    private double lastMouseX, lastMouseY;

    public SeedExplorerScreen(GuiTheme theme) {
        super(theme, "Seed Explorer");
    }

    @Override
    public void initWidgets() {
    }

    @Override
    protected void onRenderBefore(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        // Handle panning
        boolean mouseLeftPressed = glfwGetMouseButton(mc.getWindow().getHandle(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        if (mouseLeftPressed) {
            if (lastMouseLeftPressed) {
                offsetX -= (mouseX - lastMouseX) / zoom;
                offsetZ -= (mouseY - lastMouseY) / zoom;
            }
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        } else {
            lastMouseX = mouseX;
            lastMouseY = mouseY;
        }
        lastMouseLeftPressed = mouseLeftPressed;

        // Render map
        renderMap(mouseX, mouseY, delta);

        // Draw coordinates overlay
        double worldX = offsetX + (mouseX - mc.getWindow().getGuiScaledWidth() / 2.0) / zoom;
        double worldZ = offsetZ + (mouseY - mc.getWindow().getGuiScaledHeight() / 2.0) / zoom;
        String coords = String.format("X: %.1f, Z: %.1f (Zoom: %.2f)", worldX, worldZ, zoom);
        theme.textRenderer().begin(1.0);
        theme.textRenderer().render(coords, 10, mc.getWindow().getGuiScaledHeight() - 20, theme.textColor());
        theme.textRenderer().end();
    }

    private void renderMap(int mouseX, int mouseY, float delta) {
        double screenWidth = mc.getWindow().getGuiScaledWidth();
        double screenHeight = mc.getWindow().getGuiScaledHeight();

        int startX = (int) Math.floor((offsetX - (screenWidth / 2 / zoom)) / 16);
        int startZ = (int) Math.floor((offsetZ - (screenHeight / 2 / zoom)) / 16);
        int endX = (int) Math.ceil((offsetX + (screenWidth / 2 / zoom)) / 16);
        int endZ = (int) Math.ceil((offsetZ + (screenHeight / 2 / zoom)) / 16);

        Renderer2D r = Renderer2D.TEXTURE;

        for (int cz = startZ; cz <= endZ; cz++) {
            for (int cx = startX; cx <= endX; cx++) {
                ChunkTile tile = MinimapManager.get().getTile(cx, cz);
                if (tile.texture != null) {
                    double x = (cx * 16 - offsetX) * zoom + screenWidth / 2;
                    double y = (cz * 16 - offsetZ) * zoom + screenHeight / 2;
                    double size = 16 * zoom;

                    r.begin();
                    r.texQuad(x, y, size, size, Color.WHITE);
                    r.render(tile.texture.getTextureView(), tile.texture.getSampler());
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        double zoomStep = 1.2;
        double oldZoom = zoom;
        if (verticalAmount > 0) zoom *= zoomStep;
        else zoom /= zoomStep;
        zoom = Mth.clamp(zoom, 0.01, 100.0);

        double screenWidth = mc.getWindow().getGuiScaledWidth();
        double screenHeight = mc.getWindow().getGuiScaledHeight();

        // Adjust offset so we zoom into the mouse position
        offsetX += (mouseX - screenWidth / 2.0) / oldZoom - (mouseX - screenWidth / 2.0) / zoom;
        offsetZ += (mouseY - screenHeight / 2.0) / oldZoom - (mouseY - screenHeight / 2.0) / zoom;

        return true;
    }
}
