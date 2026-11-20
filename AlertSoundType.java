package com.example.playerdetector;

import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

public enum AlertSoundType {
    EXPERIENCE_ORB("screen.player-detector.sound_orb", SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.2f),
    NOTE_BLOCK_PLING("screen.player-detector.sound_pling", SoundEvents.BLOCK_NOTE_BLOCK_PLING, 1.5f);

    private final String translationKey;
    private final SoundEvent soundEvent;
    private final float pitch;

    AlertSoundType(String translationKey, SoundEvent soundEvent, float pitch) {
        this.translationKey = translationKey;
        this.soundEvent = soundEvent;
        this.pitch = pitch;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public SoundEvent getSoundEvent() {
        return soundEvent;
    }

    public float getPitch() {
        return pitch;
    }

    public AlertSoundType next() {
        AlertSoundType[] values = values();
        return values[(this.ordinal() + 1) % values.length];
    }
}
