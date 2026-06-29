/*
 * This file is part of the Meteor Seed Explorer Addon distribution (https://github.com/SeedExplorer/meteor-seed-explorer).
 * Copyright (c) SeedExplorer Team.
 */

package me.seedexplorer.addon.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import me.seedexplorer.addon.map.BiomeGenerator;
import me.seedexplorer.addon.ore.OrePatch;
import me.seedexplorer.addon.ore.OrePredictor;
import me.seedexplorer.addon.ore.OreType;
import me.seedexplorer.addon.seed.SeedManager;
import me.seedexplorer.addon.structures.GeneratedStructure;
import me.seedexplorer.addon.structures.PredictionStatus;
import me.seedexplorer.addon.workers.WorkerManager;
import me.seedexplorer.addon.worldgen.PredictedBiome;
import me.seedexplorer.addon.worldgen.VanillaStructurePredictor;
import me.seedexplorer.addon.worldgen.WorldgenEngine;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.BlockPos;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

public class SeedExplorerCommand extends Command {
    private static final double MINE_REACH_DISTANCE_SQUARED = 25.0;
    private static final int TARGET_SKIP_TIMEOUT_TICKS = 20 * 90;
    private static final int PATH_REFRESH_TICKS = 100;

    private final AtomicInteger minePredictionJobIds = new AtomicInteger();
    private List<OrePatch> predictedMineTargets = List.of();
    private OreType predictedMineType;
    private int predictedMineDimension;
    private int predictedMineIndex;
    private int predictedMinePathTargetIndex = -1;
    private int predictedMinePathRefreshTicks;
    private int predictedMineTargetTicks;
    private boolean predictedMineSawOre;
    private boolean predictedMineActive;
    private boolean predictedMineBreaking;

    private static final List<String> OVERWORLD_LOCATE_BATCH = List.of(
        "minecraft:village_plains",
        "minecraft:village_desert",
        "minecraft:village_savanna",
        "minecraft:village_snowy",
        "minecraft:village_taiga",
        "minecraft:desert_pyramid",
        "minecraft:jungle_pyramid",
        "minecraft:swamp_hut",
        "minecraft:igloo",
        "minecraft:pillager_outpost",
        "minecraft:monument",
        "minecraft:mansion",
        "minecraft:ancient_city",
        "minecraft:trial_chambers",
        "minecraft:trail_ruins",
        "minecraft:ruined_portal",
        "minecraft:ruined_portal_desert",
        "minecraft:ruined_portal_jungle",
        "minecraft:ruined_portal_swamp",
        "minecraft:ruined_portal_mountain",
        "minecraft:ruined_portal_ocean",
        "minecraft:shipwreck",
        "minecraft:shipwreck_beached",
        "minecraft:ocean_ruin_cold",
        "minecraft:ocean_ruin_warm"
    );

    public SeedExplorerCommand() {
        super("seed-explorer", "Base command for the Seed Explorer addon.");
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @Override
    public void build(LiteralArgumentBuilder<ClientSuggestionProvider> builder) {
        builder.executes(context -> {
            info("Seed Explorer addon loaded. Use sub-commands for specific functionality.");
            return SINGLE_SUCCESS;
        });

        builder.then(literal("biome-debug").executes(context -> {
            if (mc.player == null || mc.level == null) {
                error("Join a world first.");
                return SINGLE_SUCCESS;
            }

            long seed = SeedManager.get().getWorldSeed();
            if (seed == 0) {
                error("No seed set. Use .seed <seed> or the Seed Explorer seed box first.");
                return SINGLE_SUCCESS;
            }

            BlockPos pos = mc.player.blockPosition();
            int dimension = dimensionId();
            PredictedBiome predicted = BiomeGenerator.getPredictedBiome(pos.getX(), pos.getZ(), seed, dimension);
            PredictedBiome actual = WorldgenEngine.fromRuntimeBiome(mc.level.getBiome(pos));

            info("Seed Explorer biome debug at (highlight)%d, %d, %d(default):", pos.getX(), pos.getY(), pos.getZ());
            info("Predicted: (highlight)%s(default) [%s]", predicted.displayName(), predicted.id());
            info("Loaded world: (highlight)%s(default) [%s]", actual.displayName(), actual.id());
            if (!predicted.id().equals(actual.id())) {
                warning("Mismatch. This means the backend still needs more work for this seed/dimension.");
            }
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("structure-debug").executes(context -> {
            if (mc.player == null || mc.level == null) {
                error("Join a world first.");
                return SINGLE_SUCCESS;
            }

            long seed = SeedManager.get().getWorldSeed();
            if (seed == 0) {
                error("No seed set. Use .seed <seed> or the Seed Explorer seed box first.");
                return SINGLE_SUCCESS;
            }

            if (dimensionId() != 0) {
                error("Overworld structure debug only supports the Overworld right now.");
                return SINGLE_SUCCESS;
            }

            BlockPos pos = mc.player.blockPosition();
            int chunkX = Math.floorDiv(pos.getX(), 16);
            int chunkZ = Math.floorDiv(pos.getZ(), 16);
            String version = SeedManager.get().getMcVersion();
            StringBuilder report = new StringBuilder();
            appendLine(report, "Seed Explorer structure-debug");
            appendLine(report, "seed=" + seed);
            appendLine(report, "version=" + (version == null || version.isBlank() ? "unknown" : version));
            appendLine(report, "player=" + pos.getX() + "," + pos.getY() + "," + pos.getZ());
            appendLine(report, "chunk=" + chunkX + "," + chunkZ);
            info("Structure debug using seed (highlight)%d(default)%s at chunk (highlight)%d, %d(default).",
                seed,
                version == null || version.isBlank() ? "" : " version " + version,
                chunkX,
                chunkZ);
            List<GeneratedStructure> structures = VanillaStructurePredictor.predictOverworld(seed, chunkX - 160, chunkZ - 160, chunkX + 160, chunkZ + 160);

            if (structures.isEmpty()) {
                List<VanillaStructurePredictor.DebugCandidate> candidates = VanillaStructurePredictor.debugOverworld(seed, chunkX - 160, chunkZ - 160, chunkX + 160, chunkZ + 160);
                if (candidates.isEmpty()) {
                    warning("No vanilla placement candidates found nearby.");
                    appendLine(report, "result=no vanilla placement candidates nearby");
                } else {
                    VanillaStructurePredictor.DebugCandidate closestCandidate = candidates.stream()
                        .min(Comparator.comparingLong(s -> distanceSquared(pos.getX(), pos.getZ(), s.x(), s.z())))
                        .orElse(null);
                    if (closestCandidate != null) {
                        int distance = (int) Math.round(Math.sqrt(distanceSquared(pos.getX(), pos.getZ(), closestCandidate.x(), closestCandidate.z())));
                        warning("No valid supported structure nearby. Closest raw candidate:");
                        info("(highlight)%s(default) at (highlight)%d, %d(default), biome %s, valid=%s, distance %d blocks.",
                            closestCandidate.structureId(), closestCandidate.x(), closestCandidate.z(),
                            closestCandidate.biomeId(), closestCandidate.validBiome(), distance);
                        appendLine(report, "result=no valid supported structure nearby");
                        appendLine(report, "closest_raw=" + closestCandidate.structureId()
                            + " x=" + closestCandidate.x()
                            + " z=" + closestCandidate.z()
                            + " biome=" + closestCandidate.biomeId()
                            + " valid=" + closestCandidate.validBiome()
                            + " distance=" + distance);
                    }
                }
                DebugReportWriter.copyAndSave("structure-debug", report.toString());
                return SINGLE_SUCCESS;
            }

            GeneratedStructure closest = structures.stream()
                .min(Comparator.comparingLong(s -> distanceSquared(pos.getX(), pos.getZ(), s.x, s.z)))
                .orElse(null);
            if (closest == null) return SINGLE_SUCCESS;

            long distanceSquared = distanceSquared(pos.getX(), pos.getZ(), closest.x, closest.z);
            int distance = (int) Math.round(Math.sqrt(distanceSquared));
            info("Closest supported prediction: (highlight)%s(default) at (highlight)%d, %d(default), distance %d blocks.",
                closest.displayName(), closest.x, closest.z, distance);
            info("Biome there: (highlight)%s(default)", VanillaStructurePredictor.biomeAt(seed, closest.x, closest.z));
            appendLine(report, "closest=" + closest.displayName()
                + " x=" + closest.x
                + " z=" + closest.z
                + " distance=" + distance
                + " biome=" + VanillaStructurePredictor.biomeAt(seed, closest.x, closest.z));
            DebugReportWriter.copyAndSave("structure-debug", report.toString());
            return SINGLE_SUCCESS;
        }));

        builder.then(literal("structure-check")
            .then(argument("x", IntegerArgumentType.integer())
                .then(argument("z", IntegerArgumentType.integer())
                    .executes(context -> {
                        int x = IntegerArgumentType.getInteger(context, "x");
                        int z = IntegerArgumentType.getInteger(context, "z");
                        return runStructureCheck(x, z);
                    })
                )
            )
        );

        builder.then(literal("ore-debug")
            .executes(context -> runOreDebug(3))
            .then(argument("radius", IntegerArgumentType.integer(0, 8))
                .executes(context -> runOreDebug(IntegerArgumentType.getInteger(context, "radius")))
            )
            .then(literal("stop")
                .executes(context -> {
                    OreDebugRunner.get().stop();
                    info("Stopped ore-debug.");
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("mine-predicted")
            .then(literal("stop")
                .executes(context -> stopBaritonePathing())
            )
            .then(argument("ore", StringArgumentType.word())
                .executes(context -> runMinePredicted(StringArgumentType.getString(context, "ore"), 8, 24))
                .then(argument("radius", IntegerArgumentType.integer(1, 12))
                    .executes(context -> runMinePredicted(
                        StringArgumentType.getString(context, "ore"),
                        IntegerArgumentType.getInteger(context, "radius"),
                        24
                    ))
                    .then(argument("targets", IntegerArgumentType.integer(1, 64))
                        .executes(context -> runMinePredicted(
                            StringArgumentType.getString(context, "ore"),
                            IntegerArgumentType.getInteger(context, "radius"),
                            IntegerArgumentType.getInteger(context, "targets")
                        ))
                    )
                )
            )
        );

        builder.then(literal("locate-batch")
            .executes(context -> runLocateBatch())
            .then(literal("stop")
                .executes(context -> {
                    LocateBatchRunner.get().stop();
                    info("Stopped locate batch.");
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("validate-overworld")
            .executes(context -> runValidateOverworld())
            .then(literal("stop")
                .executes(context -> {
                    LocateBatchRunner.get().stop();
                    info("Stopped overworld validation scan.");
                    return SINGLE_SUCCESS;
                })
            )
        );

        builder.then(literal("prediction-status")
            .executes(context -> runPredictionStatus())
        );
    }

    private int runPredictionStatus() {
        StringBuilder report = new StringBuilder();
        appendLine(report, "Seed Explorer Overworld prediction status");
        appendLine(report, "");
        for (PredictionStatus.Entry entry : PredictionStatus.overworld()) {
            String line = entry.name() + " = " + entry.status();
            if (!entry.note().isBlank()) line += " (" + entry.note() + ")";
            appendLine(report, line);
            info(line);
        }
        DebugReportWriter.copyAndSave("prediction-status", report.toString());
        return SINGLE_SUCCESS;
    }

    private int runOreDebug(int radiusChunks) {
        if (mc.player == null || mc.level == null) {
            error("Join a world first.");
            return SINGLE_SUCCESS;
        }

        long seed = SeedManager.get().getWorldSeed();
        if (seed == 0) {
            error("No seed set. Use .seed set <seed> or the Seed Explorer seed box first.");
            return SINGLE_SUCCESS;
        }

        OreDebugRunner.get().start(radiusChunks);
        return SINGLE_SUCCESS;
    }

    public int runMinePredictedFromChat(String oreName, int radiusChunks, int targetLimit) {
        return runMinePredicted(oreName, radiusChunks, targetLimit);
    }

    public int stopMinePredictedFromChat() {
        return stopBaritonePathing();
    }

    private int runMinePredicted(String oreName, int radiusChunks, int targetLimit) {
        if (mc.player == null || mc.level == null) {
            error("Join a world first.");
            return SINGLE_SUCCESS;
        }

        long seed = SeedManager.get().getWorldSeed();
        if (seed == 0) {
            error("No seed set. Use .seed set <seed> or the Seed Explorer seed box first.");
            return SINGLE_SUCCESS;
        }

        OreType oreType = parseOreType(oreName);
        if (oreType == null) {
            error("Unknown ore '%s'. Try diamond, ancient_debris, quartz, nether_gold, iron, gold, redstone, lapis, coal, copper, or emerald.", oreName);
            return SINGLE_SUCCESS;
        }

        int dimension = dimensionId();
        if (oreType.dimension != dimension) {
            error("%s prediction is for the %s, but you are in the %s.",
                oreType.displayName,
                dimensionName(oreType.dimension),
                dimensionName(dimension));
            return SINGLE_SUCCESS;
        }

        BlockPos playerPos = mc.player.blockPosition();
        int radius = Math.max(1, Math.min(radiusChunks, 12));
        int maxTargets = Math.max(1, Math.min(targetLimit, 64));
        int centerChunkX = Math.floorDiv(playerPos.getX(), 16);
        int centerChunkZ = Math.floorDiv(playerPos.getZ(), 16);
        int centerX = playerPos.getX();
        int centerY = playerPos.getY();
        int centerZ = playerPos.getZ();
        int jobId = minePredictionJobIds.incrementAndGet();

        info("Searching predicted %s targets within %d chunk%s...",
            oreType.displayName,
            radius,
            radius == 1 ? "" : "s");

        if (!WorkerManager.get().submit(() -> runMinePredictionJob(
            jobId,
            seed,
            dimension,
            oreType,
            radius,
            maxTargets,
            centerChunkX,
            centerChunkZ,
            centerX,
            centerY,
            centerZ
        ))) {
            error("Seed Explorer worker queue is full. Try again in a moment.");
        }

        return SINGLE_SUCCESS;
    }

    private void runMinePredictionJob(int jobId, long seed, int dimension, OreType oreType, int radius,
                                      int maxTargets, int centerChunkX, int centerChunkZ,
                                      int centerX, int centerY, int centerZ) {
        List<OrePatch> candidates;
        try {
            candidates = OrePredictor.predictInChunkRadius(
                centerChunkX,
                centerChunkZ,
                radius,
                dimension,
                oreType,
                maxTargets * 3,
                centerX,
                centerY,
                centerZ,
                seed
            );
        } catch (Throwable throwable) {
            mc.execute(() -> error("Predicted ore search failed: %s: %s",
                throwable.getClass().getSimpleName(),
                throwable.getMessage()));
            return;
        }

        List<OrePatch> targets = filterMineTargets(candidates, dimension, maxTargets);

        mc.execute(() -> finishMinePredictionJob(jobId, oreType, radius, targets));
    }

    private void finishMinePredictionJob(int jobId, OreType oreType, int radius, List<OrePatch> targets) {
        if (jobId != minePredictionJobIds.get()) return;

        if (targets.isEmpty()) {
            warning("No uncleared predicted %s blocks found within %d chunks.", oreType.displayName, radius);
            return;
        }

        startPredictedMineSession(oreType, targets);
    }

    private void startPredictedMineSession(OreType oreType, List<OrePatch> targets) {
        if (predictedMineActive) stopPredictedMineSession(false);
        else cancelBaritonePathing(false);

        predictedMineTargets = List.copyOf(targets);
        predictedMineType = oreType;
        predictedMineDimension = oreType.dimension;
        predictedMineIndex = 0;
        predictedMinePathTargetIndex = -1;
        predictedMinePathRefreshTicks = 0;
        predictedMineTargetTicks = 0;
        predictedMineSawOre = false;
        predictedMineBreaking = false;
        predictedMineActive = true;

        if (!pathToCurrentPredictedOre()) {
            predictedMineActive = false;
            return;
        }

        OrePatch first = predictedMineTargets.getFirst();
        info("Mining predicted %s queue: %d target%s, first at %d, %d, %d.",
            oreType.displayName,
            predictedMineTargets.size(),
            predictedMineTargets.size() == 1 ? "" : "s",
            first.x,
            first.y,
            first.z);
    }

    private boolean pathToCurrentPredictedOre() {
        OrePatch target = currentPredictedMineTarget();
        if (target == null) {
            finishPredictedMineSession();
            return false;
        }

        BaritoneResult result = startBaritonePathing(List.of(target), true);
        if (!result.success()) {
            error(result.message());
            return false;
        }

        predictedMinePathTargetIndex = predictedMineIndex;
        predictedMinePathRefreshTicks = 0;
        return true;
    }

    private OrePatch currentPredictedMineTarget() {
        if (!predictedMineActive || predictedMineIndex < 0 || predictedMineIndex >= predictedMineTargets.size()) return null;
        return predictedMineTargets.get(predictedMineIndex);
    }

    @EventHandler
    private void onTickPre(TickEvent.Pre event) {
        tickPredictedMineSession();
    }

    private void tickPredictedMineSession() {
        if (!predictedMineActive) return;

        if (mc.player == null || mc.level == null || mc.gameMode == null) {
            clearPredictedMineSession();
            return;
        }

        if (dimensionId() != predictedMineDimension) {
            warning("Stopped predicted mining because you changed dimension.");
            stopPredictedMineSession(false);
            return;
        }

        OrePatch target = currentPredictedMineTarget();
        if (target == null) {
            finishPredictedMineSession();
            return;
        }

        predictedMineTargetTicks++;
        BlockPos targetPos = new BlockPos(target.x, target.y, target.z);
        BlockState state = mc.level.getBlockState(targetPos);
        boolean targetIsOre = predictedMineType.matches(state);
        if (targetIsOre) predictedMineSawOre = true;

        if (predictedMineSawOre && !targetIsOre) {
            markCurrentPredictedOreCleared();
            advancePredictedMineTarget();
            return;
        }

        if (mc.player.distanceToSqr(target.x + 0.5, target.y + 0.5, target.z + 0.5) <= MINE_REACH_DISTANCE_SQUARED) {
            if (targetIsOre) {
                if (!predictedMineBreaking) cancelBaritonePathing(false);
                predictedMineBreaking = true;
                BlockUtils.breakBlock(targetPos, true);
                return;
            }

            if (predictedMineTargetTicks >= TARGET_SKIP_TIMEOUT_TICKS) {
                warning("Skipping predicted %s at %d, %d, %d because it was not found after reaching it.",
                    target.type.displayName,
                    target.x,
                    target.y,
                    target.z);
                markCurrentPredictedOreCleared();
                advancePredictedMineTarget();
                return;
            }
        }

        predictedMineBreaking = false;
        predictedMinePathRefreshTicks++;

        if (predictedMinePathTargetIndex != predictedMineIndex || predictedMinePathRefreshTicks >= PATH_REFRESH_TICKS) {
            pathToCurrentPredictedOre();
        }
    }

    private void markCurrentPredictedOreCleared() {
        OrePatch target = currentPredictedMineTarget();
        if (target == null) return;

        int cleared = SeedManager.get().markClearedOrePatches(predictedMineDimension, List.of(target));
        if (cleared > 0) {
            info("Mined predicted %s at %d, %d, %d.",
                target.type.displayName,
                target.x,
                target.y,
                target.z);
        }
    }

    private void advancePredictedMineTarget() {
        predictedMineIndex++;
        predictedMineBreaking = false;
        predictedMineTargetTicks = 0;
        predictedMineSawOre = false;

        if (predictedMineIndex >= predictedMineTargets.size()) {
            finishPredictedMineSession();
            return;
        }

        pathToCurrentPredictedOre();
    }

    private void finishPredictedMineSession() {
        int total = predictedMineTargets.size();
        OreType type = predictedMineType;
        clearPredictedMineSession();
        cancelBaritonePathing(false);
        if (type != null) info("Finished predicted %s mining queue: %d target%s.", type.displayName, total, total == 1 ? "" : "s");
    }

    private void stopPredictedMineSession(boolean announce) {
        OreType type = predictedMineType;
        int remaining = Math.max(0, predictedMineTargets.size() - predictedMineIndex);
        clearPredictedMineSession();
        cancelBaritonePathing(false);
        if (announce && type != null) info("Stopped predicted %s mining queue with %d target%s remaining.", type.displayName, remaining, remaining == 1 ? "" : "s");
    }

    private void clearPredictedMineSession() {
        predictedMineTargets = List.of();
        predictedMineType = null;
        predictedMineDimension = 0;
        predictedMineIndex = 0;
        predictedMinePathTargetIndex = -1;
        predictedMinePathRefreshTicks = 0;
        predictedMineTargetTicks = 0;
        predictedMineSawOre = false;
        predictedMineActive = false;
        predictedMineBreaking = false;
    }

    private List<OrePatch> filterMineTargets(List<OrePatch> candidates, int dimension, int maxTargets) {
        List<OrePatch> targets = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        for (OrePatch patch : candidates) {
            if (SeedManager.get().isClearedOre(patch, dimension)) continue;

            long key = BlockPos.asLong(patch.x, patch.y, patch.z);
            if (!seen.add(key)) continue;

            targets.add(patch);
            if (targets.size() >= maxTargets) break;
        }
        return targets;
    }

    private OreType parseOreType(String oreName) {
        String normalized = oreName.toLowerCase(Locale.ROOT)
            .replace("minecraft:", "")
            .replace("-", "_")
            .replace(" ", "_");
        if (normalized.endsWith("_ore")) normalized = normalized.substring(0, normalized.length() - 4);
        if (normalized.endsWith("_ores")) normalized = normalized.substring(0, normalized.length() - 5);

        return switch (normalized) {
            case "diamond", "diamonds" -> OreType.DIAMOND;
            case "emerald", "emeralds" -> OreType.EMERALD;
            case "coal" -> OreType.COAL;
            case "iron" -> OreType.IRON;
            case "copper" -> OreType.COPPER;
            case "gold" -> OreType.GOLD;
            case "lapis", "lapis_lazuli" -> OreType.LAPIS;
            case "redstone" -> OreType.REDSTONE;
            case "quartz", "nether_quartz" -> OreType.QUARTZ;
            case "nether_gold", "nethergold" -> OreType.NETHER_GOLD;
            case "ancient_debris", "debris" -> OreType.ANCIENT_DEBRIS;
            default -> null;
        };
    }

    private BaritoneResult startBaritonePathing(List<OrePatch> targets) {
        return startBaritonePathing(targets, false);
    }

    private BaritoneResult startBaritonePathing(List<OrePatch> targets, boolean exactBlockGoal) {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Class<?> providerClass = Class.forName("baritone.api.IBaritoneProvider");
            Class<?> baritoneClass = Class.forName("baritone.api.IBaritone");
            Class<?> goalClass = Class.forName("baritone.api.pathing.goals.Goal");
            Class<?> getToBlockGoalClass = Class.forName("baritone.api.pathing.goals.GoalGetToBlock");
            Class<?> blockGoalClass = Class.forName("baritone.api.pathing.goals.GoalBlock");
            Class<?> compositeGoalClass = Class.forName("baritone.api.pathing.goals.GoalComposite");
            Class<?> customGoalProcessClass = Class.forName("baritone.api.process.ICustomGoalProcess");

            Object provider = apiClass.getMethod("getProvider").invoke(null);
            if (provider == null) return new BaritoneResult(false, "Baritone is not available.");

            Object baritone = providerClass.getMethod("getPrimaryBaritone").invoke(provider);
            if (baritone == null) return new BaritoneResult(false, "Baritone is not available.");

            Object goal = createBaritoneGoal(targets, goalClass, exactBlockGoal ? blockGoalClass : getToBlockGoalClass, compositeGoalClass);
            Object customGoalProcess = baritoneClass.getMethod("getCustomGoalProcess").invoke(baritone);
            customGoalProcessClass.getMethod("setGoalAndPath", goalClass).invoke(customGoalProcess, goal);
            return new BaritoneResult(true, "started");
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            return new BaritoneResult(false, "Baritone is not installed or not loaded.");
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            return new BaritoneResult(false, "Could not start Baritone pathing: " + cause.getClass().getSimpleName() + ": " + cause.getMessage());
        } catch (Throwable throwable) {
            return new BaritoneResult(false, "Could not start Baritone pathing: " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        }
    }

    private Object createBaritoneGoal(List<OrePatch> targets, Class<?> goalClass, Class<?> targetGoalClass, Class<?> compositeGoalClass)
        throws ReflectiveOperationException {
        if (targets.size() == 1) {
            OrePatch patch = targets.getFirst();
            return targetGoalClass.getConstructor(BlockPos.class).newInstance(new BlockPos(patch.x, patch.y, patch.z));
        }

        Object goalArray = Array.newInstance(goalClass, targets.size());
        for (int i = 0; i < targets.size(); i++) {
            OrePatch patch = targets.get(i);
            Object goal = targetGoalClass.getConstructor(BlockPos.class).newInstance(new BlockPos(patch.x, patch.y, patch.z));
            Array.set(goalArray, i, goal);
        }

        return compositeGoalClass.getConstructor(goalArray.getClass()).newInstance(new Object[] { goalArray });
    }

    private int stopBaritonePathing() {
        return stopBaritonePathing(true);
    }

    private int stopBaritonePathing(boolean announce) {
        minePredictionJobIds.incrementAndGet();
        if (predictedMineActive) {
            stopPredictedMineSession(announce);
            return SINGLE_SUCCESS;
        }

        return cancelBaritonePathing(announce);
    }

    private int cancelBaritonePathing(boolean announce) {
        try {
            Class<?> apiClass = Class.forName("baritone.api.BaritoneAPI");
            Class<?> providerClass = Class.forName("baritone.api.IBaritoneProvider");
            Class<?> baritoneClass = Class.forName("baritone.api.IBaritone");
            Class<?> pathingBehaviorClass = Class.forName("baritone.api.behavior.IPathingBehavior");

            Object provider = apiClass.getMethod("getProvider").invoke(null);
            Object baritone = providerClass.getMethod("getPrimaryBaritone").invoke(provider);
            Object pathingBehavior = baritoneClass.getMethod("getPathingBehavior").invoke(baritone);
            pathingBehaviorClass.getMethod("cancelEverything").invoke(pathingBehavior);
            if (announce) info("Stopped Baritone pathing.");
        } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
            if (announce) error("Baritone is not installed or not loaded.");
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception.getCause() == null ? exception : exception.getCause();
            if (announce) error("Could not stop Baritone: %s: %s", cause.getClass().getSimpleName(), cause.getMessage());
        } catch (Throwable throwable) {
            if (announce) error("Could not stop Baritone: %s: %s", throwable.getClass().getSimpleName(), throwable.getMessage());
        }
        return SINGLE_SUCCESS;
    }

    private List<OrePatch> scanLoadedChunk(LevelChunk chunk, OreType type) {
        List<OrePatch> results = new ArrayList<>();
        int minY = Math.max(mc.level.getMinY(), type.minY);
        int maxY = Math.min(mc.level.getMaxY() - 1, type.maxY);
        if (minY > maxY) return results;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int baseX = chunk.getPos().x() << 4;
        int baseZ = chunk.getPos().z() << 4;
        for (int y = minY; y <= maxY; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    pos.set(baseX + x, y, baseZ + z);
                    if (type.matches(chunk.getBlockState(pos))) {
                        results.add(new OrePatch(pos.getX(), pos.getY(), pos.getZ(), type, true));
                    }
                }
            }
        }
        return results;
    }

    private boolean inChunkBounds(OrePatch patch, int minChunkX, int minChunkZ, int maxChunkX, int maxChunkZ) {
        int chunkX = Math.floorDiv(patch.x, 16);
        int chunkZ = Math.floorDiv(patch.z, 16);
        return chunkX >= minChunkX && chunkX <= maxChunkX && chunkZ >= minChunkZ && chunkZ <= maxChunkZ;
    }

    private MatchSummary compareOre(List<OrePatch> actual, List<OrePatch> predicted, int tolerance) {
        Set<Integer> usedPredicted = new HashSet<>();
        List<OrePatch> matchedPredicted = new ArrayList<>();
        List<OrePatch> missingActual = new ArrayList<>();
        int matchedActual = 0;

        for (OrePatch actualPatch : actual) {
            int bestIndex = -1;
            int bestDistance = Integer.MAX_VALUE;
            for (int i = 0; i < predicted.size(); i++) {
                if (usedPredicted.contains(i)) continue;
                OrePatch predictedPatch = predicted.get(i);
                int distance = manhattan(actualPatch, predictedPatch);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestIndex = i;
                }
            }

            if (bestIndex >= 0 && bestDistance <= tolerance) {
                usedPredicted.add(bestIndex);
                matchedPredicted.add(predicted.get(bestIndex));
                matchedActual++;
            } else {
                missingActual.add(actualPatch);
            }
        }

        List<OrePatch> extraPredicted = new ArrayList<>();
        for (int i = 0; i < predicted.size(); i++) {
            if (!usedPredicted.contains(i)) extraPredicted.add(predicted.get(i));
        }

        return new MatchSummary(matchedActual, matchedPredicted, missingActual, extraPredicted);
    }

    private int manhattan(OrePatch a, OrePatch b) {
        return Math.abs(a.x - b.x) + Math.abs(a.y - b.y) + Math.abs(a.z - b.z);
    }

    private void appendOreList(StringBuilder report, String label, List<OrePatch> patches, int limit) {
        appendLine(report, label + "_count=" + patches.size());
        int count = Math.min(limit, patches.size());
        for (int i = 0; i < count; i++) {
            OrePatch patch = patches.get(i);
            appendLine(report, "  " + patch.type.displayName + " " + patch.x + "," + patch.y + "," + patch.z
                + " chunk=" + Math.floorDiv(patch.x, 16) + "," + Math.floorDiv(patch.z, 16)
                + " source=" + patch.source);
        }
        if (patches.size() > limit) {
            appendLine(report, "  ... " + (patches.size() - limit) + " more");
        }
    }

    private void appendSourceSummary(StringBuilder report, String label, List<OrePatch> patches) {
        appendLine(report, label + "=");
        if (patches.isEmpty()) {
            appendLine(report, "  none");
            return;
        }

        patches.stream()
            .collect(java.util.stream.Collectors.groupingBy(patch -> patch.source, java.util.TreeMap::new, java.util.stream.Collectors.counting()))
            .forEach((source, count) -> appendLine(report, "  " + source + "=" + count));
    }

    private void appendYHistogram(StringBuilder report, String label, List<OrePatch> patches) {
        appendLine(report, label + "=");
        if (patches.isEmpty()) {
            appendLine(report, "  none");
            return;
        }

        patches.stream()
            .collect(java.util.stream.Collectors.groupingBy(patch -> Math.floorDiv(patch.y, 16) * 16, java.util.TreeMap::new, java.util.stream.Collectors.counting()))
            .forEach((bucket, count) -> appendLine(report, "  " + bucket + ".." + (bucket + 15) + "=" + count));
    }

    private void appendBlockStateDiagnostics(StringBuilder report, String label, List<OrePatch> patches,
                                             boolean includeRuntime, long seed, int dimension, int sampleLimit) {
        appendLine(report, label + "=");
        if (patches.isEmpty()) {
            appendLine(report, "  none");
            return;
        }

        Map<String, Long> predictedBaseCounts = patches.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                patch -> blockStateId(WorldgenEngine.baseBlock(seed, dimension, patch.x, patch.y, patch.z)),
                java.util.TreeMap::new,
                java.util.stream.Collectors.counting()
            ));
        appendLine(report, "  predicted_base=");
        predictedBaseCounts.forEach((block, count) -> appendLine(report, "    " + block + "=" + count));

        if (includeRuntime) {
            Map<String, Long> runtimeCounts = patches.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                    patch -> blockStateId(runtimeBlock(patch.x, patch.y, patch.z)),
                    java.util.TreeMap::new,
                    java.util.stream.Collectors.counting()
                ));
            appendLine(report, "  runtime=");
            runtimeCounts.forEach((block, count) -> appendLine(report, "    " + block + "=" + count));
        }

        appendLine(report, "  samples=");
        int count = Math.min(sampleLimit, patches.size());
        for (int i = 0; i < count; i++) {
            OrePatch patch = patches.get(i);
            String line = "    " + patch.x + "," + patch.y + "," + patch.z
                + " predicted_base=" + blockStateId(WorldgenEngine.baseBlock(seed, dimension, patch.x, patch.y, patch.z));
            if (includeRuntime) {
                line += " runtime=" + blockStateId(runtimeBlock(patch.x, patch.y, patch.z));
            }
            appendLine(report, line);
        }
        if (patches.size() > sampleLimit) appendLine(report, "    ... " + (patches.size() - sampleLimit) + " more");
    }

    private BlockState runtimeBlock(int x, int y, int z) {
        if (mc.level == null || y < mc.level.getMinY() || y >= mc.level.getMaxY()) return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
        return mc.level.getBlockState(new BlockPos(x, y, z));
    }

    private String blockStateId(BlockState state) {
        if (state == null) return "unknown";
        return BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
    }

    private int runLocateBatch() {
        if (mc.player == null || mc.level == null) {
            error("Join a world first.");
            return SINGLE_SUCCESS;
        }

        if (dimensionId() != 0) {
            error("Locate batch only supports the Overworld right now.");
            return SINGLE_SUCCESS;
        }

        long seed = SeedManager.get().getWorldSeed();
        if (seed == 0) {
            error("No seed set. Use .seed set <seed> first.");
            return SINGLE_SUCCESS;
        }

        if (LocateBatchRunner.get().isRunning()) {
            warning("A locate batch is already running. Use .seed-explorer locate-batch stop first.");
            return SINGLE_SUCCESS;
        }

        LocateBatchRunner.get().start(seed, OVERWORLD_LOCATE_BATCH);
        return SINGLE_SUCCESS;
    }

    private int runValidateOverworld() {
        if (mc.player == null || mc.level == null) {
            error("Join a world first.");
            return SINGLE_SUCCESS;
        }

        if (dimensionId() != 0) {
            error("Overworld validation only supports the Overworld right now.");
            return SINGLE_SUCCESS;
        }

        long seed = SeedManager.get().getWorldSeed();
        if (seed == 0) {
            error("No seed set. Use .seed set <seed> first.");
            return SINGLE_SUCCESS;
        }

        if (LocateBatchRunner.get().isRunning()) {
            warning("A locate/validation batch is already running. Use .seed-explorer validate-overworld stop first.");
            return SINGLE_SUCCESS;
        }

        LocateBatchRunner.get().start(
            seed,
            "validate-overworld",
            "Seed Explorer Overworld validation scan",
            mc.player.blockPosition(),
            overworldValidationTargets()
        );
        return SINGLE_SUCCESS;
    }

    private List<LocateBatchRunner.Target> overworldValidationTargets() {
        List<LocateBatchRunner.Target> targets = new ArrayList<>();
        targets.add(target("Village Plains", "minecraft:village_plains", true, "Villages"));
        targets.add(target("Village Desert", "minecraft:village_desert", true, "Villages"));
        targets.add(target("Village Savanna", "minecraft:village_savanna", true, "Villages"));
        targets.add(target("Village Snowy", "minecraft:village_snowy", true, "Villages"));
        targets.add(target("Village Taiga", "minecraft:village_taiga", true, "Villages"));
        targets.add(target("Pillager Outpost", "minecraft:pillager_outpost", true, ""));
        targets.add(target("Woodland Mansion", "minecraft:mansion", true, ""));
        targets.add(target("Desert Pyramid", "minecraft:desert_pyramid", true, ""));
        targets.add(target("Jungle Pyramid", "minecraft:jungle_pyramid", true, ""));
        targets.add(target("Swamp Hut", "minecraft:swamp_hut", true, ""));
        targets.add(target("Igloo", "minecraft:igloo", true, ""));
        targets.add(target("Trial Chambers", "minecraft:trial_chambers", true, ""));
        targets.add(target("Ancient City", "minecraft:ancient_city", true, ""));
        targets.add(target("Stronghold", "minecraft:stronghold", true, "Strongholds"));
        targets.add(target("Mineshaft", "minecraft:mineshaft", true, "validation only: dense placement"));
        targets.add(target("Mineshaft Mesa", "minecraft:mineshaft_mesa", true, "validation only: dense placement"));
        targets.add(skip("Dungeon", "not locatable with /locate structure; needs monster-room feature scan"));
        targets.add(target("Ocean Monument", "minecraft:monument", true, ""));
        targets.add(target("Shipwreck", "minecraft:shipwreck", true, ""));
        targets.add(target("Shipwreck Beached", "minecraft:shipwreck_beached", true, ""));
        targets.add(target("Ocean Ruin Cold", "minecraft:ocean_ruin_cold", true, ""));
        targets.add(target("Ocean Ruin Warm", "minecraft:ocean_ruin_warm", true, ""));
        targets.add(target("Buried Treasure", "minecraft:buried_treasure", true, ""));
        targets.add(target("Trail Ruins", "minecraft:trail_ruins", true, ""));
        targets.add(target("Ruined Portal", "minecraft:ruined_portal", true, ""));
        targets.add(target("Ruined Portal Desert", "minecraft:ruined_portal_desert", true, ""));
        targets.add(target("Ruined Portal Jungle", "minecraft:ruined_portal_jungle", true, ""));
        targets.add(target("Ruined Portal Swamp", "minecraft:ruined_portal_swamp", true, ""));
        targets.add(target("Ruined Portal Mountain", "minecraft:ruined_portal_mountain", true, ""));
        targets.add(target("Ruined Portal Ocean", "minecraft:ruined_portal_ocean", true, ""));
        targets.add(skip("Amethyst Geode", "not locatable with /locate structure; needs configured-feature geode scan"));
        return targets;
    }

    private LocateBatchRunner.Target target(String name, String structureId, boolean comparePrediction, String note) {
        return new LocateBatchRunner.Target(name, structureId, comparePrediction, note);
    }

    private LocateBatchRunner.Target skip(String name, String note) {
        return new LocateBatchRunner.Target(name, "", false, note);
    }

    private int runStructureCheck(int x, int z) {
        long seed = SeedManager.get().getWorldSeed();
        if (seed == 0) {
            error("No seed set. Use .seed set <seed> first.");
            return SINGLE_SUCCESS;
        }

        if (dimensionId() != 0) {
            error("Overworld structure check only supports the Overworld right now.");
            return SINGLE_SUCCESS;
        }

        int chunkX = Math.floorDiv(x, 16);
        int chunkZ = Math.floorDiv(z, 16);
        StringBuilder report = new StringBuilder();
        appendLine(report, "Seed Explorer structure-check");
        appendLine(report, "seed=" + seed);
        appendLine(report, "target=" + x + "," + z);
        appendLine(report, "chunk=" + chunkX + "," + chunkZ);
        info("Checking structures near (highlight)%d, %d(default) using seed (highlight)%d(default).", x, z, seed);

        List<GeneratedStructure> structures = VanillaStructurePredictor.predictOverworld(seed, chunkX - 32, chunkZ - 32, chunkX + 32, chunkZ + 32);
        if (!structures.isEmpty()) {
            structures.stream()
                .sorted(Comparator.comparingLong(s -> distanceSquared(x, z, s.x, s.z)))
                .limit(5)
                .forEach(s -> {
                    int distance = (int) Math.round(Math.sqrt(distanceSquared(x, z, s.x, s.z)));
                    info("(highlight)%s(default) at (highlight)%d, %d(default), distance %d, biome %s.",
                        s.displayName(), s.x, s.z, distance, VanillaStructurePredictor.biomeAt(seed, s.x, s.z));
                    appendLine(report, "prediction=" + s.displayName()
                        + " x=" + s.x
                        + " z=" + s.z
                        + " distance=" + distance
                        + " biome=" + VanillaStructurePredictor.biomeAt(seed, s.x, s.z));
                });
            DebugReportWriter.copyAndSave("structure-check", report.toString());
            return SINGLE_SUCCESS;
        }

        warning("No valid supported structure prediction within 32 chunks.");
        appendLine(report, "result=no valid supported structure prediction within 32 chunks");
        List<VanillaStructurePredictor.DebugCandidate> candidates = VanillaStructurePredictor.debugOverworld(seed, chunkX - 32, chunkZ - 32, chunkX + 32, chunkZ + 32);
        candidates.stream()
            .sorted(Comparator.comparingLong(s -> distanceSquared(x, z, s.x(), s.z())))
            .limit(5)
            .forEach(s -> {
                int distance = (int) Math.round(Math.sqrt(distanceSquared(x, z, s.x(), s.z())));
                info("Raw (highlight)%s(default) at (highlight)%d, %d(default), distance %d, biome %s, valid=%s.",
                    s.structureId(), s.x(), s.z(), distance, s.biomeId(), s.validBiome());
                appendLine(report, "raw=" + s.structureId()
                    + " x=" + s.x()
                    + " z=" + s.z()
                    + " distance=" + distance
                    + " biome=" + s.biomeId()
                    + " valid=" + s.validBiome());
            });
        DebugReportWriter.copyAndSave("structure-check", report.toString());
        return SINGLE_SUCCESS;
    }

    private void appendLine(StringBuilder report, String line) {
        report.append(line).append(System.lineSeparator());
    }

    private long distanceSquared(int x1, int z1, int x2, int z2) {
        long dx = x2 - x1;
        long dz = z2 - z1;
        return dx * dx + dz * dz;
    }

    private int dimensionId() {
        if (mc.level == null) return 0;
        if (mc.level.dimension() == Level.NETHER) return -1;
        if (mc.level.dimension() == Level.END) return 1;
        return 0;
    }

    private String dimensionName(int dimension) {
        return switch (dimension) {
            case -1 -> "nether";
            case 1 -> "end";
            default -> "overworld";
        };
    }

    private record MatchSummary(int matchedActual, List<OrePatch> matchedPredicted, List<OrePatch> missingActual, List<OrePatch> extraPredicted) {
    }

    private record BaritoneResult(boolean success, String message) {
    }
}
