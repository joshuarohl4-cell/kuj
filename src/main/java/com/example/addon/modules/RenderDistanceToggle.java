package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.MinecraftAccess;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

public class RenderDistanceToggle extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> lowDistance = sgGeneral.add(new IntSetting.Builder()
        .name("low-distance")
        .description("Low render distance when triggered.")
        .defaultValue(2)
        .range(2, 32)
        .sliderRange(2, 32)
        .build()
    );
    private final Setting<Integer> highDistance = sgGeneral.add(new IntSetting.Builder()
        .name("high-distance")
        .description("High render distance after reset.")
        .defaultValue(14)
        .range(2, 32)
        .sliderRange(2, 32)
        .build()
    );
    private final Setting<Integer> resetY = sgGeneral.add(new IntSetting.Builder()
        .name("reset-y")
        .description("Y level to trigger the reset.")
        .defaultValue(-1)
        .range(-64, 320)
        .sliderRange(-64, 320)
        .build()
    );

    private boolean armed = true;
    private int stage = 0;
    private int timer = 0;

    public RenderDistanceToggle() {
        super(AddonTemplate.CATEGORY, "render-distance-toggle", "Automatically refreshes chunks when going below the configured Y level.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        double playerY = MinecraftAccess.getPlayerY(mc);
        if (playerY == Double.MAX_VALUE) return;

        int y = (int) playerY;
        
        if (y > resetY.get()) {
            armed = true;
        }
        
        if (armed && y <= resetY.get()) {
            armed = false;
            stage = 1;
            timer = 0;
            MinecraftAccess.setViewDistance(mc, lowDistance.get());
        }
        
        if (stage == 1) {
            timer++;
            if (timer >= 2) {
                MinecraftAccess.setViewDistance(mc, highDistance.get());
                stage = 0;
            }
        }
    }
}
