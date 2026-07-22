package com.example.addon.modules;

import com.example.addon.AddonTemplate;
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
        try {
            // Get player Y via reflection
            double playerY = getPlayerY();
            if (playerY == Double.MAX_VALUE) return;

            int y = (int) playerY;
            
            if (y > resetY.get()) {
                armed = true;
            }
            
            if (armed && y <= resetY.get()) {
                armed = false;
                stage = 1;
                timer = 0;
                setViewDistance(lowDistance.get());
            }
            
            if (stage == 1) {
                timer++;
                if (timer >= 2) {
                    setViewDistance(highDistance.get());
                    stage = 0;
                }
            }
        } catch (Exception e) {
            // Silent fail
        }
    }
    
    private double getPlayerY() {
        try {
            java.lang.reflect.Field pf = null;
            for (var f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("Entity")) {
                    pf = f;
                    break;
                }
            }
            if (pf == null) return Double.MAX_VALUE;
            pf.setAccessible(true);
            Object player = pf.get(mc);
            if (player == null) return Double.MAX_VALUE;
            
            for (var f : player.getClass().getDeclaredFields()) {
                if (f.getName().contains("y")) {
                    f.setAccessible(true);
                    return f.getDouble(player);
                }
            }
        } catch (Exception e) {}
        return Double.MAX_VALUE;
    }
    
    private void setViewDistance(int dist) {
        try {
            java.lang.reflect.Field of = null;
            for (var f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("Options")) {
                    of = f;
                    break;
                }
            }
            if (of == null) return;
            of.setAccessible(true);
            Object opts = of.get(mc);
            if (opts == null) return;
            
            for (var vf : opts.getClass().getDeclaredFields()) {
                if (vf.getType().getSimpleName().contains("Option")) {
                    vf.setAccessible(true);
                    Object opt = vf.get(opts);
                    if (opt != null) {
                        opt.getClass().getMethod("setValue", int.class).invoke(opt, dist);
                    }
                    break;
                }
            }
            
            // Reload chunks
            reloadChunks();
        } catch (Exception e) {}
    }
    
    private void reloadChunks() {
        try {
            java.lang.reflect.Field wf = null;
            for (var f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("World")) {
                    wf = f;
                    break;
                }
            }
            if (wf != null) {
                wf.setAccessible(true);
                Object world = wf.get(mc);
                if (world != null) {
                    world.getClass().getMethod("updateLevelInPlayerCache").invoke(world);
                }
            }
        } catch (Exception e) {}
    }
}
