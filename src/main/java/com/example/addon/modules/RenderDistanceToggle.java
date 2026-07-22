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

    // Cached reflection refs
    private static Object playerField;
    private static Object optionsField;
    private static Object viewDistanceField;
    private static Object worldField;
    private static Object worldRendererField;
    private static boolean init = false;

    public RenderDistanceToggle() {
        super(AddonTemplate.CATEGORY, "render-distance-toggle", "Automatically refreshes chunks when going below the configured Y level.");
    }

    private static void initReflection() {
        if (init) return;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.MinecraftClient");
            for (var f : mcClass.getDeclaredFields()) {
                String t = f.getType().getName();
                if (t.contains("ClientWorld")) worldField = f;
                else if (t.contains("ClientPlayerEntity")) playerField = f;
                else if (t.contains("GameOptions")) optionsField = f;
                else if (t.contains("WorldRenderer")) worldRendererField = f;
            }
            if (playerField != null) ((java.lang.reflect.Field)playerField).setAccessible(true);
            if (worldField != null) ((java.lang.reflect.Field)worldField).setAccessible(true);
            if (optionsField != null) ((java.lang.reflect.Field)optionsField).setAccessible(true);
            if (worldRendererField != null) ((java.lang.reflect.Field)worldRendererField).setAccessible(true);
            
            // Find view distance option field
            if (optionsField != null) {
                Object opts = ((java.lang.reflect.Field)optionsField).get(null);
                if (opts != null) {
                    for (var vf : opts.getClass().getDeclaredFields()) {
                        if (vf.getType().getSimpleName().contains("Option")) {
                            viewDistanceField = vf;
                            vf.setAccessible(true);
                            break;
                        }
                    }
                }
            }
            init = true;
        } catch (Exception e) {}
    }

    private Object getPlayer() {
        try {
            if (playerField == null) return null;
            return ((java.lang.reflect.Field)playerField).get(mc);
        } catch (Exception e) { return null; }
    }

    private double getPlayerY() {
        try {
            Object player = getPlayer();
            if (player == null) return Double.MAX_VALUE;
            for (var f : player.getClass().getSuperclass().getDeclaredFields()) {
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
            if (viewDistanceField == null) return;
            Object opt = ((java.lang.reflect.Field)viewDistanceField).get(((java.lang.reflect.Field)optionsField).get(mc));
            if (opt != null) {
                opt.getClass().getMethod("setValue", int.class).invoke(opt, dist);
            }
            reloadChunks();
        } catch (Exception e) {}
    }

    private void reloadChunks() {
        try {
            Object world = ((java.lang.reflect.Field)worldField).get(mc);
            if (world != null) {
                world.getClass().getMethod("updateLevelInPlayerCache").invoke(world);
            }
            Object renderer = ((java.lang.reflect.Field)worldRendererField).get(mc);
            if (renderer != null) {
                try {
                    renderer.getClass().getMethod("reload").invoke(renderer);
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        initReflection();
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
    }
}
