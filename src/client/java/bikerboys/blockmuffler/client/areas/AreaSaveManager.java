package bikerboys.blockmuffler.client.areas;

import bikerboys.blockmuffler.client.areas.rules.AreaRule;
import bikerboys.blockmuffler.client.areas.rules.SoundRule;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.storage.LevelResource;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AreaSaveManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int FORMAT_VERSION = 1;
    private static final int AUTOSAVE_INTERVAL_TICKS = 100;
    private static final int DEFAULT_MINECRAFT_PORT = 25565;
    private static final Logger LOGGER = Logger.getLogger("BlockMuffler/AreaSaveManager");

    @Nullable
    private static SaveTarget activeTarget;
    private static boolean activeFileCanBeSaved = true;
    private static int ticksSinceSave;
    private static boolean initialized;

    private AreaSaveManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;

        ClientTickEvents.END_CLIENT_TICK.register(AreaSaveManager::tick);
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> saveAndUnload());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> saveAndUnload());
    }

    private static void tick(Minecraft client) {
        if (client.level == null) {
            if (activeTarget != null) {
                saveAndUnload();
            }
            return;
        }

        SaveTarget target = resolveTarget(client);
        if (target == null) {
            return;
        }

        if (activeTarget == null || !activeTarget.file.equals(target.file)) {
            switchTo(target);
        }

        if (activeTarget != null && ++ticksSinceSave >= AUTOSAVE_INTERVAL_TICKS) {
            saveCurrent();
            ticksSinceSave = 0;
        }
    }

    @Nullable
    private static SaveTarget resolveTarget(Minecraft client) {
        MinecraftServer integratedServer = client.getSingleplayerServer();
        if (integratedServer != null) {
            Path worldRoot = integratedServer.getWorldPath(LevelResource.ROOT)
                    .toAbsolutePath()
                    .normalize();
            String identity = worldRoot.toString();
            return createTarget(client, "singleplayer", identity);
        }

        ServerData serverData = client.getCurrentServer();
        if (serverData == null || serverData.ip == null || serverData.ip.isBlank()) {
            return null;
        }

        String identity = normalizeServerAddress(serverData.ip);
        return createTarget(client, "server", identity);
    }

    private static SaveTarget createTarget(Minecraft client, String type, String identity) {
        String hash = sha256(type + "\0" + identity);
        String fileName = type + "-" + hash + ".json";

        Path directory = client.gameDirectory.toPath()
                .resolve("config")
                .resolve("blockmuffler")
                .resolve("areas");

        return new SaveTarget(directory.resolve(fileName));
    }

    private static String normalizeServerAddress(String address) {
        String value = address.trim().toLowerCase(Locale.ROOT);

        if (value.startsWith("[")) {
            int closingBracket = value.indexOf(']');
            if (closingBracket > 0) {
                String host = value.substring(1, closingBracket);
                String suffix = value.substring(closingBracket + 1);
                int port = parsePortSuffix(suffix, DEFAULT_MINECRAFT_PORT);
                return "[" + host + "]:" + port;
            }
        }

        int firstColon = value.indexOf(':');
        int lastColon = value.lastIndexOf(':');

        if (firstColon >= 0 && firstColon == lastColon) {
            String host = value.substring(0, firstColon);
            String suffix = value.substring(firstColon + 1);
            int port = parsePort(suffix, DEFAULT_MINECRAFT_PORT);
            return normalizeHost(host) + ":" + port;
        }

        if (firstColon >= 0) {

            return "[" + value + "]:" + DEFAULT_MINECRAFT_PORT;
        }

        return normalizeHost(value) + ":" + DEFAULT_MINECRAFT_PORT;
    }

    private static String normalizeHost(String host) {
        String result = host.trim().toLowerCase(Locale.ROOT);
        while (result.endsWith(".")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }

    private static int parsePortSuffix(String suffix, int fallback) {
        if (suffix == null || suffix.isEmpty()) {
            return fallback;
        }
        if (suffix.charAt(0) != ':') {
            return fallback;
        }
        return parsePort(suffix.substring(1), fallback);
    }

    private static int parsePort(String value, int fallback) {
        try {
            int port = Integer.parseInt(value);
            return port >= 1 && port <= 65535 ? port : fallback;
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is not available", impossible);
        }
    }

    private static void switchTo(SaveTarget target) {
        saveCurrent();

        AreaManager.areaList.clear();
        AreaManager.selectedArea = null;

        activeTarget = target;
        activeFileCanBeSaved = true;
        ticksSinceSave = 0;
        loadCurrent();
    }

    private static void loadCurrent() {
        if (activeTarget == null) {
            return;
        }

        Path file = activeTarget.file;
        if (!Files.exists(file)) {
            activeFileCanBeSaved = true;
            return;
        }

        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            SaveData data = GSON.fromJson(reader, SaveData.class);
            if (data == null || data.version != FORMAT_VERSION || data.areas == null) {
                activeFileCanBeSaved = false;
                LOGGER.warning("Area save has an unsupported or invalid format; leaving it untouched: " + file);
                return;
            }

            List<Area> loadedAreas = new ArrayList<>();
            for (AreaData savedArea : data.areas) {
                Area area = deserializeArea(savedArea);
                if (area != null) {
                    loadedAreas.add(area);
                }
            }

            AreaManager.areaList.clear();
            AreaManager.areaList.addAll(loadedAreas);
            activeFileCanBeSaved = true;
            LOGGER.info("Loaded " + loadedAreas.size() + " areas from " + file.getFileName());
        } catch (IOException | RuntimeException exception) {
            activeFileCanBeSaved = false;
            LOGGER.log(Level.SEVERE, "Could not read area save; preserving the existing file: " + file, exception);
        }
    }

    @Nullable
    private static Area deserializeArea(@Nullable AreaData saved) {
        if (saved == null || !isBlockPos(saved.pos1) || !isBlockPos(saved.pos2)) {
            return null;
        }

        BlockPos pos1 = new BlockPos(saved.pos1[0], saved.pos1[1], saved.pos1[2]);
        BlockPos pos2 = new BlockPos(saved.pos2[0], saved.pos2[1], saved.pos2[2]);
        Area area = new Area(pos1, pos2, saved.color, saved.name == null ? "Unnamed" : saved.name);

        area.setLocked(saved.locked);
        area.setRenderPlacement(saved.renderPlacement);
        area.setRotation(saved.rotation);

        if (saved.rule != null) {
            applyRule(area.getAreaRule(), saved.rule);
        }

        return area;
    }

    private static boolean isBlockPos(@Nullable int[] position) {
        return position != null && position.length == 3;
    }

    private static void applyRule(AreaRule target, RuleData saved) {
        target.setVolMultiplier(finiteOr(saved.volumeMultiplier, 1.0f));
        target.setIncludeEntities(saved.includeEntities);

        if (saved.categoryMultipliers != null) {
            for (Map.Entry<String, Float> entry : saved.categoryMultipliers.entrySet()) {
                if (entry.getKey() == null) {
                    continue;
                }
                try {
                    SoundSource category = SoundSource.valueOf(entry.getKey());
                    Float multiplier = entry.getValue();
                    if (multiplier != null && Float.isFinite(multiplier)) {
                        target.setCategoryMultiplier(category, multiplier);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
        }

        if (saved.soundRules != null) {
            for (SoundRuleData savedRule : saved.soundRules) {
                if (savedRule == null || savedRule.soundId == null || savedRule.soundId.isBlank()) {
                    continue;
                }

                SoundRule rule = new SoundRule(savedRule.soundId);
                rule.setVolumeMul(finiteOr(savedRule.volumeMul, 1.0f));
                rule.setBlocked(savedRule.blocked);
                target.addSoundRule(rule);
            }
        }
    }

    private static float finiteOr(float value, float fallback) {
        return Float.isFinite(value) ? value : fallback;
    }

    private static void saveCurrent() {
        if (activeTarget == null || !activeFileCanBeSaved) {
            return;
        }

        Path file = activeTarget.file;
        Path tempFile = file.resolveSibling(file.getFileName() + ".tmp");

        SaveData data = new SaveData();
        data.version = FORMAT_VERSION;
        data.areas = new ArrayList<>();

        for (Area area : AreaManager.areaList) {
            AreaData savedArea = serializeArea(area);
            if (savedArea != null) {
                data.areas.add(savedArea);
            }
        }

        try {
            Files.createDirectories(file.getParent());

            try (Writer writer = Files.newBufferedWriter(
                    tempFile,
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE)) {
                GSON.toJson(data, writer);
            }

            try {
                Files.move(tempFile, file,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(tempFile, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException | RuntimeException exception) {
            LOGGER.log(Level.SEVERE, "Could not save areas to " + file, exception);
            try {
                Files.deleteIfExists(tempFile);
            } catch (IOException cleanupException) {
                LOGGER.log(Level.WARNING, "Could not remove temporary area save " + tempFile, cleanupException);
            }
        }
    }

    @Nullable
    private static AreaData serializeArea(@Nullable Area area) {
        if (area == null || area.getPos1() == null || area.getPos2() == null) {
            return null;
        }

        AreaData saved = new AreaData();
        saved.pos1 = blockPosArray(area.getPos1());
        saved.pos2 = blockPosArray(area.getPos2());
        saved.color = area.getColor();
        saved.name = area.getName();
        saved.locked = area.isLocked();
        saved.renderPlacement = area.isRenderPlacement();
        saved.rotation = area.getRotation();
        saved.rule = serializeRule(area.getAreaRule());
        return saved;
    }

    private static int[] blockPosArray(BlockPos position) {
        return new int[]{position.getX(), position.getY(), position.getZ()};
    }

    private static RuleData serializeRule(AreaRule rule) {
        RuleData saved = new RuleData();
        saved.volumeMultiplier = rule.getVolMultiplier();
        saved.includeEntities = rule.isIncludeEntities();
        saved.categoryMultipliers = new java.util.LinkedHashMap<>();
        rule.getCategoryMultipliers().forEach((category, multiplier) ->
                saved.categoryMultipliers.put(category.name(), multiplier));

        saved.soundRules = new ArrayList<>();
        for (SoundRule soundRule : rule.getSoundRules()) {
            if (soundRule == null || soundRule.soundId == null) {
                continue;
            }
            SoundRuleData sound = new SoundRuleData();
            sound.soundId = soundRule.soundId;
            sound.volumeMul = soundRule.getVolumeMul();
            sound.blocked = soundRule.blocked;
            saved.soundRules.add(sound);
        }
        return saved;
    }

    private static void saveAndUnload() {
        saveCurrent();
        activeTarget = null;
        activeFileCanBeSaved = true;
        ticksSinceSave = 0;

        AreaManager.areaList.clear();
        AreaManager.selectedArea = null;
    }

    private static final class SaveTarget {
        private final Path file;

        private SaveTarget(Path file) {
            this.file = file;
        }
    }

    private static final class SaveData {
        private int version = FORMAT_VERSION;
        private List<AreaData> areas = new ArrayList<>();
    }

    private static final class AreaData {
        private int[] pos1;
        private int[] pos2;
        private int color;
        private String name;
        private boolean locked;
        private boolean renderPlacement;
        private int rotation;
        private RuleData rule = new RuleData();
    }

    private static final class RuleData {
        private float volumeMultiplier = 1.0f;
        private boolean includeEntities = true;
        private Map<String, Float> categoryMultipliers;
        private List<SoundRuleData> soundRules;
    }

    private static final class SoundRuleData {
        private String soundId;
        private float volumeMul = 1.0f;
        private boolean blocked;
    }
}
