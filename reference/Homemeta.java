/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.modules.Module
 */
package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Homemeta
extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<Integer> homeNumber;

    public Homemeta() {
        super(AddonTemplate.CATEGORY, "Home meta", "Runs my home reset sequence.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.homeNumber = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("home-number")).description("Which home slot to use.")).defaultValue((Object)3)).min(1).max(10).sliderMin(1).sliderMax(10).build());
    }

    public void onActivate() {
        if (this.mc.field_1724 == null) {
            this.toggle();
            return;
        }
        int home = (Integer)this.homeNumber.get();
        String[] commands = new String[]{"delhome " + home, "sethome " + home, "rtp", "home " + home};
        Thread worker = new Thread(() -> {
            for (String command : commands) {
                this.mc.execute(() -> {
                    if (this.mc.field_1724 != null) {
                        this.mc.field_1724.field_3944.method_45730(command);
                    }
                });
                try {
                    Thread.sleep(450L);
                }
                catch (InterruptedException interruptedException) {
                    // empty catch block
                }
            }
            this.mc.execute(() -> ((Homemeta)this).toggle());
        });
        worker.start();
    }
}
