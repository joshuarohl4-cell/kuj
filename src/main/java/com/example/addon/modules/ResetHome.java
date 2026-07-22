package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.MinecraftAccess;
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
        if (MinecraftAccess.getPlayer(mc) == null) {
            toggle();
            return;
        }

        String homeName = " " + homeNumber.get();
        ChatUtils.info("Resetting " + homeName + "...");
        
        MinecraftAccess.sendCommand(mc, "delhome " + homeName);
        
        MeteorExecutor.execute(() -> {
            try {
                Thread.sleep(600);
            } catch (InterruptedException ignored) {}
            
            if (MinecraftAccess.getPlayer(mc) != null) {
                MinecraftAccess.sendCommand(mc, "sethome " + homeName);
                ChatUtils.info(homeName + " reset.");
            }
            toggle();
        });
    }
}
