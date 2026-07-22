package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class ResetHome extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> homeNumber = sgGeneral.add(new IntSetting.Builder()
        .name("home-number")
        .description("Which home number to delete and re-set.")
        .defaultValue(1)
        .range(1, 20)
        .sliderMin(1)
        .sliderMax(20)
        .build()
    );

    public ResetHome() {
        super(AddonTemplate.CATEGORY, "reset-home", "Deletes and re-sets a numbered home at your current position via keybind.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null) {
            toggle();
            return;
        }

        String homeName = " " + homeNumber.get();
        ChatUtils.info("Resetting " + homeName + "...");
        
        // Send delhome command
        sendCommand("delhome " + homeName);
        
        MeteorExecutor.execute(() -> {
            try {
                Thread.sleep(600);
            } catch (InterruptedException ignored) {}
            
            if (mc.player != null) {
                sendCommand("sethome " + homeName);
                ChatUtils.info(homeName + " reset.");
            }
            toggle();
        });
    }
    
    private void sendCommand(String command) {
        try {
            mc.player.getClass().getMethod("method_45730", String.class).invoke(mc.player, command);
        } catch (Exception e) {
            // Fallback
        }
    }
}
