package com.example.playerdetector;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

public class DetectorScreen extends Screen {
    private final Screen parent;
    private ButtonWidget toggleButton;
    private ButtonWidget soundButton;
    private RangeSliderWidget rangeSlider;

    public DetectorScreen(Screen parent) {
        super(Text.translatable("screen.player-detector.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ModConfig config = PlayerDetectorClient.getConfig();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 64;
        int widgetWidth = 230;
        int widgetHeight = 20;

        this.toggleButton = ButtonWidget.builder(getToggleText(config.enabled), button -> {
            config.enabled = !config.enabled;
            config.save();
            button.setMessage(getToggleText(config.enabled));
        }).dimensions(centerX - widgetWidth / 2, startY, widgetWidth, widgetHeight).build();
        this.addDrawableChild(this.toggleButton);

        this.rangeSlider = new RangeSliderWidget(
                centerX - widgetWidth / 2,
                startY + 26,
                widgetWidth,
                widgetHeight,
                config.range
        );
        this.addDrawableChild(this.rangeSlider);

        this.soundButton = ButtonWidget.builder(getSoundText(config.soundType), button -> {
            config.soundType = config.soundType.next();
            config.save();
            button.setMessage(getSoundText(config.soundType));
        }).dimensions(centerX - widgetWidth / 2, startY + 52, widgetWidth, widgetHeight).build();
        this.addDrawableChild(this.soundButton);

        ButtonWidget testSoundButton = ButtonWidget.builder(
                Text.translatable("screen.player-detector.test_sound"),
                button -> PlayerDetectorClient.playAlertSound(this.client)
        ).dimensions(centerX - widgetWidth / 2, startY + 78, widgetWidth, widgetHeight).build();
        this.addDrawableChild(testSoundButton);

        ButtonWidget debugButton = ButtonWidget.builder(
                Text.translatable("screen.player-detector.debug_scan"),
                button -> PlayerDetectorClient.sendDebugScan(this.client)
        ).dimensions(centerX - widgetWidth / 2, startY + 104, widgetWidth, widgetHeight).build();
        this.addDrawableChild(debugButton);

        ButtonWidget doneButton = ButtonWidget.builder(
                Text.translatable("gui.done"),
                button -> this.close()
        ).dimensions(centerX - widgetWidth / 2, startY + 132, widgetWidth, widgetHeight).build();
        this.addDrawableChild(doneButton);
    }

    private Text getToggleText(boolean enabled) {
        return enabled
                ? Text.translatable("screen.player-detector.status_on")
                : Text.translatable("screen.player-detector.status_off");
    }

    private Text getSoundText(AlertSoundType soundType) {
        return Text.translatable(soundType.getTranslationKey());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(
                this.textRenderer,
                this.title,
                this.width / 2,
                this.height / 2 - 88,
                0xFFFFFF
        );
    }

    @Override
    public void removed() {
        if (this.rangeSlider != null) {
            this.rangeSlider.commitValue();
        }
        PlayerDetectorClient.getConfig().save();
        super.removed();
    }

    @Override
    public void close() {
        if (this.client != null) {
            this.client.setScreen(this.parent);
        }
    }

    private static class RangeSliderWidget extends SliderWidget {
        private int currentRange;

        public RangeSliderWidget(int x, int y, int width, int height, int initialRange) {
            super(x, y, width, height, Text.empty(), rangeToProgress(initialRange));
            this.currentRange = clamp(initialRange);
            this.updateMessage();
        }

        private static int clamp(int value) {
            return Math.max(ModConfig.MIN_RANGE, Math.min(ModConfig.MAX_RANGE, value));
        }

        private static double rangeToProgress(int range) {
            int clamped = clamp(range);
            return (double) (clamped - ModConfig.MIN_RANGE) / (double) (ModConfig.MAX_RANGE - ModConfig.MIN_RANGE);
        }

        private static int progressToRange(double progress) {
            double clampedProgress = Math.max(0.0, Math.min(1.0, progress));
            return (int) Math.round(ModConfig.MIN_RANGE + clampedProgress * (ModConfig.MAX_RANGE - ModConfig.MIN_RANGE));
        }

        @Override
        protected void updateMessage() {
            this.setMessage(Text.translatable("screen.player-detector.range", this.currentRange));
        }

        @Override
        protected void applyValue() {
            this.currentRange = progressToRange(this.value);
            this.value = rangeToProgress(this.currentRange);
            this.updateMessage();
            PlayerDetectorClient.getConfig().range = this.currentRange;
        }

        public void commitValue() {
            this.currentRange = progressToRange(this.value);
            PlayerDetectorClient.getConfig().range = this.currentRange;
            PlayerDetectorClient.getConfig().save();
        }
    }
}
