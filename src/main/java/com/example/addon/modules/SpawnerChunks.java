package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.MinecraftAccess;
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
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;

import java.util.HashSet;
import java.util.Set;

public class SpawnerChunks extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
        .name("side-color")
        .description("Fill color for spawner chunks.")
        .defaultValue(new SettingColor(255, 0, 0, 60))
        .build()
    );
    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
        .name("line-color")
        .description("Outline color for spawner chunks.")
        .defaultValue(new SettingColor(255, 0, 0, 255))
        .build()
    );
    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
        .name("shape-mode")
        .defaultValue(ShapeMode.Both)
        .build()
    );
    private final Setting<Integer> rescanInterval = sgGeneral.add(new IntSetting.Builder()
        .name("rescan-ticks")
        .description("How often (in ticks) to rescan nearby chunks for spawners.")
        .defaultValue(20)
        .range(5, 100)
        .sliderMin(5)
        .sliderMax(100)
        .build()
    );

    private final Set<BlockPos> spawnerChunks = new HashSet<>();
    private int tickCounter = 0;

    public SpawnerChunks() {
        super(AddonTemplate.CATEGORY, "spawner-chunks", "Highlights chunks that contain a mob spawner.");
    }

    @Override
    public void onActivate() {
        spawnerChunks.clear();
        rescan();
    }

    @EventHandler
    private void onTick(GameLeftEvent event) {}

    @EventHandler
    private void onTick(TickEvent.Post event) {
        tickCounter++;
        if (tickCounter >= rescanInterval.get()) {
            tickCounter = 0;
            rescan();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        int minY = MinecraftAccess.getWorldMinY(mc);
        int maxY = minY + MinecraftAccess.getWorldHeight(mc);
        
        for (BlockPos chunkPos : spawnerChunks) {
            int startX = chunkPos.getX() << 4;
            int startZ = chunkPos.getZ() << 4;
            int endX = startX + 16;
            int endZ = startZ + 16;
            
            AABB box = new AABB(startX, minY, startZ, endX, maxY, endZ);
            event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }

    private void rescan() {
        spawnerChunks.clear();
        Set<BlockPos> chunks = MinecraftAccess.findSpawnerChunks(mc);
        if (chunks != null) {
            spawnerChunks.addAll(chunks);
        }
    }
}
