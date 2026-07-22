/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.world.TickEvent$Post
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.class_310
 */
package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.class_310;

public class RenderDistanceToggle
extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<Integer> lowDistance;
    private final Setting<Integer> highDistance;
    private final Setting<Integer> resetY;
    private boolean armed;
    private int stage;
    private int timer;

    public RenderDistanceToggle() {
        super(AddonTemplate.CATEGORY, "render-distance-toggle", "Automatically refreshes chunks when going below the configured Y level.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.lowDistance = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("low-distance")).defaultValue((Object)2)).range(2, 32).sliderRange(2, 32).build());
        this.highDistance = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("high-distance")).defaultValue((Object)14)).range(2, 32).sliderRange(2, 32).build());
        this.resetY = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("reset-y")).defaultValue((Object)-1)).range(-64, 320).sliderRange(-64, 320).build());
        this.armed = true;
        this.stage = 0;
        this.timer = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        class_310 client = class_310.method_1551();
        if (client.field_1724 == null || client.field_1687 == null) {
            return;
        }
        int y = client.field_1724.method_31478();
        if (y > (Integer)this.resetY.get()) {
            this.armed = true;
        }
        if (this.armed && y <= (Integer)this.resetY.get()) {
            this.armed = false;
            this.stage = 1;
            this.timer = 0;
            client.field_1690.method_42503().method_41748((Object)((Integer)this.lowDistance.get()));
            client.field_1690.method_1640();
            if (client.field_1769 != null) {
                client.field_1769.method_3292();
            }
        }
        if (this.stage == 1) {
            ++this.timer;
            if (this.timer >= 2) {
                client.field_1690.method_42503().method_41748((Object)((Integer)this.highDistance.get()));
                client.field_1690.method_1640();
                if (client.field_1769 != null) {
                    client.field_1769.method_3292();
                }
                this.stage = 0;
            }
        }
    }
}
