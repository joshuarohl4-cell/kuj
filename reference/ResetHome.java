/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.utils.network.MeteorExecutor
 *  meteordevelopment.meteorclient.utils.player.ChatUtils
 */
package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.concurrent.TimeUnit;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.network.MeteorExecutor;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

public class ResetHome
extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<Integer> homeNumber;

    public ResetHome() {
        super(AddonTemplate.CATEGORY, "reset-home", "Deletes and re-sets a numbered home at your current position via keybind.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.homeNumber = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("home-number")).description("Which home number to delete and re-set.")).defaultValue((Object)1)).min(1).sliderMax(20).build());
    }

    public void onActivate() {
        if (this.mc.field_1724 == null) {
            this.toggle();
            return;
        }
        String homeName = " " + String.valueOf(this.homeNumber.get());
        ChatUtils.info((String)("Resetting " + homeName + "..."), (Object[])new Object[0]);
        this.mc.field_1724.field_3944.method_45730("delhome " + homeName);
        MeteorExecutor.execute(() -> {
            try {
                TimeUnit.MILLISECONDS.sleep(600L);
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
            if (this.mc.field_1724 != null) {
                this.mc.field_1724.field_3944.method_45730("sethome " + homeName);
                ChatUtils.info((String)(homeName + " reset."), (Object[])new Object[0]);
            }
            this.toggle();
        });
    }
}
