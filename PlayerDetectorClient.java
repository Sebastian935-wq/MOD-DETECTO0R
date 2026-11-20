package com.example.playerdetector;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PlayerDetectorClient implements ClientModInitializer {
    public static final String MOD_ID = "player-detector";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final double EXIT_HYSTERESIS_BLOCKS = 3.0;

    private static ModConfig config;
    private static KeyBinding openMenuKey;
    private final Set<UUID> notifiedPlayers = new HashSet<>();

    public static ModConfig getConfig() {
        return config;
    }

    @Override
    public void onInitializeClient() {
        config = ModConfig.load();

        openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.player-detector.open_menu",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_K,
                "key.categories.player-detector"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
        LOGGER.info("Player Detector initialized with range={} blocks, enabled={}", config.range, config.enabled);
    }

    private void onClientTick(MinecraftClient client) {
        while (openMenuKey.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new DetectorScreen(client.currentScreen));
            }
        }

        if (client.world == null || client.player == null) {
            if (!notifiedPlayers.isEmpty()) {
                notifiedPlayers.clear();
            }
            return;
        }

        if (!config.enabled) {
            return;
        }

        double maxRange = config.range;
        double exitRange = maxRange + EXIT_HYSTERESIS_BLOCKS;

        Map<UUID, PlayerEntity> detectedPlayers = collectNearbyPlayers(client);
        Set<UUID> onlineUuids = new HashSet<>(detectedPlayers.keySet());

        for (Map.Entry<UUID, PlayerEntity> entry : detectedPlayers.entrySet()) {
            UUID otherUuid = entry.getKey();
            PlayerEntity otherPlayer = entry.getValue();

            double distance = client.player.distanceTo(otherPlayer);

            if (distance <= maxRange) {
                if (!notifiedPlayers.contains(otherUuid)) {
                    notifiedPlayers.add(otherUuid);
                    sendAlert(client, otherPlayer.getName().getString(), distance);
                    playAlertSound(client);
                }
            } else if (distance > exitRange) {
                notifiedPlayers.remove(otherUuid);
            }
        }

        notifiedPlayers.removeIf(uuid -> !onlineUuids.contains(uuid));
    }

    private Map<UUID, PlayerEntity> collectNearbyPlayers(MinecraftClient client) {
        Map<UUID, PlayerEntity> result = new LinkedHashMap<>();
        if (client.world == null || client.player == null) {
            return result;
        }

        UUID localUuid = client.player.getUuid();

        List<? extends AbstractClientPlayerEntity> worldPlayers = client.world.getPlayers();
        for (AbstractClientPlayerEntity player : worldPlayers) {
            if (player == null || player == client.player || player.getUuid().equals(localUuid)) {
                continue;
            }
            result.putIfAbsent(player.getUuid(), player);
        }

        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof PlayerEntity playerEntity) {
                if (playerEntity == client.player || playerEntity.getUuid().equals(localUuid)) {
                    continue;
                }
                result.putIfAbsent(playerEntity.getUuid(), playerEntity);
            }
        }

        return result;
    }

    public static void sendDebugScan(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }
        if (client.world == null) {
            client.player.sendMessage(Text.literal("[PlayerDetector DEBUG] Brak załadowanego świata.").formatted(Formatting.RED), false);
            return;
        }

        Map<UUID, PlayerEntity> players = new LinkedHashMap<>();
        UUID localUuid = client.player.getUuid();
        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player != null && player != client.player && !player.getUuid().equals(localUuid)) {
                players.putIfAbsent(player.getUuid(), player);
            }
        }
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof PlayerEntity pe && pe != client.player && !pe.getUuid().equals(localUuid)) {
                players.putIfAbsent(pe.getUuid(), pe);
            }
        }

        MutableText header = Text.literal("[PlayerDetector DEBUG] ")
                .formatted(Formatting.LIGHT_PURPLE, Formatting.BOLD)
                .append(Text.literal("Widoczni gracze w świecie: " + players.size() + " | Zasięg: " + config.range + " kr.").formatted(Formatting.GRAY));
        client.player.sendMessage(header, false);

        for (PlayerEntity p : players.values()) {
            double dist = client.player.distanceTo(p);
            String formatted = String.format(Locale.ROOT, "%.2f", dist);
            MutableText line = Text.literal(" - ")
                    .formatted(Formatting.DARK_GRAY)
                    .append(Text.literal(p.getName().getString()).formatted(Formatting.AQUA))
                    .append(Text.literal(" (" + formatted + " kr.)").formatted(Formatting.YELLOW));
            client.player.sendMessage(line, false);
        }
    }

    private void sendAlert(MinecraftClient client, String playerName, double distance) {
        if (client.player == null) {
            return;
        }

        String formattedDistance = String.format(Locale.ROOT, "%.2f", distance);

        MutableText message = Text.literal("[PlayerDetector] ")
                .formatted(Formatting.GOLD, Formatting.BOLD)
                .append(Text.literal(playerName).formatted(Formatting.AQUA, Formatting.BOLD))
                .append(Text.literal(" wszedł w zasięg (").formatted(Formatting.YELLOW))
                .append(Text.literal(formattedDistance + " kr.").formatted(Formatting.GREEN, Formatting.BOLD))
                .append(Text.literal(")").formatted(Formatting.YELLOW));

        client.player.sendMessage(message, false);
    }

    public static void playAlertSound(MinecraftClient client) {
        if (client == null || client.player == null) {
            return;
        }
        AlertSoundType type = (config != null && config.soundType != null)
                ? config.soundType
                : AlertSoundType.EXPERIENCE_ORB;
        client.player.playSound(type.getSoundEvent(), 1.0f, type.getPitch());
    }
}
