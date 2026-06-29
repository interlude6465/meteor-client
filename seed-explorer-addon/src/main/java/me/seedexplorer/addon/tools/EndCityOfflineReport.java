package me.seedexplorer.addon.tools;

import me.seedexplorer.addon.structures.GeneratedStructure;
import me.seedexplorer.addon.structures.StructureType;
import me.seedexplorer.addon.worldgen.VanillaStructurePredictor;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/** Offline End city report runner. This does not require launching Minecraft. */
public final class EndCityOfflineReport {
    private static final DateTimeFormatter FILE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private EndCityOfflineReport() {
    }

    public static void main(String[] args) throws IOException {
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();

        long seed = args.length > 0 ? Long.parseLong(args[0]) : 4717879387438598985L;
        int centerX = args.length > 1 ? Integer.parseInt(args[1]) : 0;
        int centerZ = args.length > 2 ? Integer.parseInt(args[2]) : 0;
        int radiusChunks = args.length > 3 ? Integer.parseInt(args[3]) : 2048;

        int centerChunkX = Math.floorDiv(centerX, 16);
        int centerChunkZ = Math.floorDiv(centerZ, 16);
        int minChunkX = centerChunkX - radiusChunks;
        int minChunkZ = centerChunkZ - radiusChunks;
        int maxChunkX = centerChunkX + radiusChunks;
        int maxChunkZ = centerChunkZ + radiusChunks;

        List<VanillaStructurePredictor.EndCityCandidate> candidates = VanillaStructurePredictor.debugEndCities(
            seed,
            minChunkX,
            minChunkZ,
            maxChunkX,
            maxChunkZ
        );
        List<GeneratedStructure> predictions = VanillaStructurePredictor.predictEnd(seed, minChunkX, minChunkZ, maxChunkX, maxChunkZ);

        StringBuilder report = new StringBuilder();
        appendLine(report, "Seed Explorer offline End city report");
        appendLine(report, "seed=" + seed);
        appendLine(report, "center=" + centerX + "," + centerZ);
        appendLine(report, "radius_chunks=" + radiusChunks);
        appendLine(report, "candidate_count=" + candidates.size());
        appendLine(report, "valid_end_city_candidates=" + candidates.stream().filter(VanillaStructurePredictor.EndCityCandidate::generation).count());
        appendLine(report, "map_predictions=" + predictions.stream().filter(s -> s.type == StructureType.END_CITY).count());
        appendLine(report, "");

        appendLine(report, "Closest vanilla-valid End city candidates:");
        candidates.stream()
            .filter(VanillaStructurePredictor.EndCityCandidate::generation)
            .sorted(Comparator.comparingLong(c -> distanceSquared(centerX, centerZ, c.x(), c.z())))
            .limit(40)
            .forEach(c -> appendCandidate(report, centerX, centerZ, c));

        appendLine(report, "");
        appendLine(report, "Closest rejected End city placement candidates:");
        candidates.stream()
            .filter(c -> !c.generation())
            .sorted(Comparator.comparingLong(c -> distanceSquared(centerX, centerZ, c.x(), c.z())))
            .limit(40)
            .forEach(c -> appendCandidate(report, centerX, centerZ, c));

        appendLine(report, "");
        appendLine(report, "Closest current map predictions:");
        predictions.stream()
            .filter(s -> s.type == StructureType.END_CITY)
            .sorted(Comparator.comparingLong(s -> distanceSquared(centerX, centerZ, s.x, s.z)))
            .limit(40)
            .forEach(s -> appendLine(report, s.displayName()
                + " x=" + s.x
                + " z=" + s.z
                + " distance=" + Math.round(Math.sqrt(distanceSquared(centerX, centerZ, s.x, s.z)))));

        Path directory = Path.of("seed-explorer-reports");
        Files.createDirectories(directory);
        Path file = directory.resolve("offline-end-city-" + LocalDateTime.now().format(FILE_TIME) + ".txt");
        Files.writeString(file, report.toString(), StandardCharsets.UTF_8);
        System.out.println(file.toAbsolutePath());
    }

    private static void appendCandidate(StringBuilder report, int centerX, int centerZ, VanillaStructurePredictor.EndCityCandidate c) {
        appendLine(report, "chunk=" + c.chunkX() + "," + c.chunkZ()
            + " x=" + c.x()
            + " z=" + c.z()
            + " distance=" + Math.round(Math.sqrt(distanceSquared(centerX, centerZ, c.x(), c.z())))
            + " placement=" + c.placement()
            + " allowed=" + c.allowed()
            + " biome=" + c.biome()
            + " generation=" + c.generation()
            + " note=" + c.note());
    }

    private static void appendLine(StringBuilder report, String line) {
        report.append(line).append(System.lineSeparator());
    }

    private static long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = x2 - x1;
        long dz = z2 - z1;
        return dx * dx + dz * dz;
    }
}
