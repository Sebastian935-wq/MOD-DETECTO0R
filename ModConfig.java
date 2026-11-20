package com.example.playerdetector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class ModConfig {
    public static final int MIN_RANGE = 5;
    public static final int MAX_RANGE = 128;
    public static final int DEFAULT_RANGE = 32;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("player-detector.json");

    public boolean enabled = true;
    public int range = DEFAULT_RANGE;
    public AlertSoundType soundType = AlertSoundType.EXPERIENCE_ORB;

    public void clampRange() {
        if (this.range < MIN_RANGE) {
            this.range = MIN_RANGE;
        } else if (this.range > MAX_RANGE) {
            this.range = MAX_RANGE;
        }
        if (this.soundType == null) {
            this.soundType = AlertSoundType.EXPERIENCE_ORB;
        }
    }

    public static ModConfig load() {
        ModConfig config = new ModConfig();
        if (Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH, StandardCharsets.UTF_8)) {
                ModConfig loaded = GSON.fromJson(reader, ModConfig.class);
                if (loaded != null) {
                    config.enabled = loaded.enabled;
                    config.range = loaded.range;
                    if (loaded.soundType != null) {
                        config.soundType = loaded.soundType;
                    }
                }
            } catch (IOException | RuntimeException e) {
                PlayerDetectorClient.LOGGER.warn("Failed to load config from {}, using defaults.", CONFIG_PATH, e);
            }
        }
        config.clampRange();
        config.save();
        return config;
    }

    public void save() {
        this.clampRange();
        try {
            Path parent = CONFIG_PATH.getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH, StandardCharsets.UTF_8)) {
                GSON.toJson(this, writer);
            }
        } catch (IOException e) {
            PlayerDetectorClient.LOGGER.error("Failed to save config to {}", CONFIG_PATH, e);
        }
    }
}
