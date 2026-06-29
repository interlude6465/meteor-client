package me.seedexplorer.addon.tools;

import me.seedexplorer.addon.ore.OrePatch;
import me.seedexplorer.addon.ore.OrePredictor;
import me.seedexplorer.addon.ore.OreType;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/** Offline ore ESP check runner. This does not require launching Minecraft. */
public final class OreEspOfflineCheck {
    private OreEspOfflineCheck() {
    }

    public static void main(String[] args) {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        long seed = args.length > 0 ? Long.parseLong(args[0]) : 4717879387438598985L;
        int x = args.length > 1 ? Integer.parseInt(args[1]) : 5289;
        int y = args.length > 2 ? Integer.parseInt(args[2]) : 70;
        int z = args.length > 3 ? Integer.parseInt(args[3]) : -6879;
        int radius = args.length > 4 ? Integer.parseInt(args[4]) : 2;
        int maxBoxes = args.length > 5 ? Integer.parseInt(args[5]) : 80;
        OreType type = args.length > 6 ? OreType.valueOf(args[6].toUpperCase()) : OreType.DIAMOND;

        List<OrePatch> patches = OrePredictor.predictInChunkRadius(
            Math.floorDiv(x, 16),
            Math.floorDiv(z, 16),
            radius,
            type.dimension,
            type,
            maxBoxes,
            x,
            y,
            z,
            seed
        );
        List<OrePatch> fullRangePatches = OrePredictor.predictInChunkRange(
                Math.floorDiv(x, 16) - radius,
                Math.floorDiv(z, 16) - radius,
                Math.floorDiv(x, 16) + radius,
                Math.floorDiv(z, 16) + radius,
                seed,
                type.dimension
            ).stream()
            .filter(patch -> patch.type == type)
            .sorted(Comparator.comparingLong(patch -> distanceSquared(patch, x, y, z)))
            .limit(maxBoxes)
            .toList();

        StringBuilder report = new StringBuilder();
        append(report, "seed=" + seed);
        append(report, "center=" + x + "," + y + "," + z);
        append(report, "dimension=" + dimensionName(type.dimension));
        append(report, "type=" + type.displayName + " radius_chunks=" + radius + " max_boxes=" + maxBoxes);
        append(report, "patches=" + patches.size());
        append(report, "full_range_patches=" + fullRangePatches.size());
        patches.stream()
            .sorted(Comparator.comparingLong(patch -> distanceSquared(patch, x, y, z)))
            .limit(20)
            .forEach(patch -> append(report, patch.type.displayName + " " + patch.x + "," + patch.y + "," + patch.z + " source=" + patch.source));
        append(report, "full_range_samples=");
        fullRangePatches.stream()
            .limit(20)
            .forEach(patch -> append(report, patch.type.displayName + " " + patch.x + "," + patch.y + "," + patch.z + " source=" + patch.source));
        append(report, "");
        append(report, "placement_trace=");
        for (String line : OrePredictor.debugOrePlacementTrace(seed, type.dimension, type, Math.floorDiv(x, 16), Math.floorDiv(z, 16), radius, 40)) {
            append(report, "  " + line);
        }

        append(report, "");
        append(report, "feature_summary=");
        for (String line : OrePredictor.debugFeatureSummary(seed, type.dimension)) {
            append(report, "  " + line);
        }

        String text = report.toString();
        System.out.print(text);
        writeReport(type, text);
    }

    private static long distanceSquared(OrePatch patch, int x, int y, int z) {
        long dx = patch.x - x;
        long dy = patch.y - y;
        long dz = patch.z - z;
        return dx * dx + dy * dy + dz * dz;
    }

    private static void append(StringBuilder report, String line) {
        report.append(line).append(System.lineSeparator());
    }

    private static String dimensionName(int dimension) {
        return switch (dimension) {
            case -1 -> "nether";
            case 1 -> "end";
            default -> "overworld";
        };
    }

    private static void writeReport(OreType type, String text) {
        try {
            Path dir = Path.of("build", "reports", "seed-explorer");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve("offline-ore-esp-" + type.name().toLowerCase() + ".txt"), text);
        } catch (IOException ignored) {
        }
    }
}
