/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  meteordevelopment.meteorclient.events.game.GameLeftEvent
 *  meteordevelopment.meteorclient.events.render.Render3DEvent
 *  meteordevelopment.meteorclient.events.world.TickEvent$Post
 *  meteordevelopment.meteorclient.renderer.ShapeMode
 *  meteordevelopment.meteorclient.settings.ColorSetting$Builder
 *  meteordevelopment.meteorclient.settings.EnumSetting$Builder
 *  meteordevelopment.meteorclient.settings.IntSetting$Builder
 *  meteordevelopment.meteorclient.settings.Setting
 *  meteordevelopment.meteorclient.settings.SettingGroup
 *  meteordevelopment.meteorclient.systems.modules.Module
 *  meteordevelopment.meteorclient.utils.render.color.Color
 *  meteordevelopment.meteorclient.utils.render.color.SettingColor
 *  meteordevelopment.orbit.EventHandler
 *  net.minecraft.class_1923
 *  net.minecraft.class_2636
 *  net.minecraft.class_2818
 */
package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import java.util.HashSet;
import java.util.Set;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.class_1923;
import net.minecraft.class_2636;
import net.minecraft.class_2818;

public class SpawnerChunks
extends Module {
    private final SettingGroup sgGeneral;
    private final Setting<SettingColor> sideColor;
    private final Setting<SettingColor> lineColor;
    private final Setting<ShapeMode> shapeMode;
    private final Setting<Integer> rescanInterval;
    private final Set<class_1923> spawnerChunks;
    private int tickCounter;

    public SpawnerChunks() {
        super(AddonTemplate.CATEGORY, "spawner-chunks", "Highlights chunks that contain a mob spawner.");
        this.sgGeneral = this.settings.getDefaultGroup();
        this.sideColor = this.sgGeneral.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("side-color")).description("Fill color for spawner chunks.")).defaultValue(new SettingColor(255, 0, 0, 60)).build());
        this.lineColor = this.sgGeneral.add((Setting)((ColorSetting.Builder)((ColorSetting.Builder)new ColorSetting.Builder().name("line-color")).description("Outline color for spawner chunks.")).defaultValue(new SettingColor(255, 0, 0, 255)).build());
        this.shapeMode = this.sgGeneral.add((Setting)((EnumSetting.Builder)((EnumSetting.Builder)new EnumSetting.Builder().name("shape-mode")).defaultValue((Object)ShapeMode.Both)).build());
        this.rescanInterval = this.sgGeneral.add((Setting)((IntSetting.Builder)((IntSetting.Builder)((IntSetting.Builder)new IntSetting.Builder().name("rescan-ticks")).description("How often (in ticks) to rescan nearby chunks for spawners.")).defaultValue((Object)20)).min(5).sliderMax(100).build());
        this.spawnerChunks = new HashSet<class_1923>();
        this.tickCounter = 0;
    }

    public void onActivate() {
        this.spawnerChunks.clear();
        this.rescan();
    }

    @EventHandler
    private void onTick(GameLeftEvent event) {
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        ++this.tickCounter;
        if (this.tickCounter >= (Integer)this.rescanInterval.get()) {
            this.tickCounter = 0;
            this.rescan();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        for (class_1923 pos : this.spawnerChunks) {
            event.renderer.box((double)pos.method_8326(), (double)this.mc.field_1687.method_31607(), (double)pos.method_8328(), (double)(pos.method_8327() + 1), (double)(this.mc.field_1687.method_31607() + this.mc.field_1687.method_31605()), (double)(pos.method_8329() + 1), (Color)this.sideColor.get(), (Color)this.lineColor.get(), (ShapeMode)this.shapeMode.get(), 0);
        }
    }

    private void rescan() {
        if (this.mc.field_1687 == null || this.mc.field_1724 == null) {
            return;
        }
        this.spawnerChunks.clear();
        int viewDist = (Integer)this.mc.field_1690.method_42503().method_41753();
        class_1923 playerChunk = this.mc.field_1724.method_31476();
        for (int cx = playerChunk.field_9181 - viewDist; cx <= playerChunk.field_9181 + viewDist; ++cx) {
            for (int cz = playerChunk.field_9180 - viewDist; cz <= playerChunk.field_9180 + viewDist; ++cz) {
                class_2818 chunk = this.mc.field_1687.method_8497(cx, cz);
                boolean hasSpawner = chunk.method_12214().values().stream().anyMatch(be -> be instanceof class_2636);
                if (!hasSpawner) continue;
                this.spawnerChunks.add(new class_1923(cx, cz));
            }
        }
    }
}
