package com.example.addon.modules;

import com.example.addon.AddonTemplate;
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
        try {
            if (mc.world == null) return;
            
            int minY = mc.world.getBottomY();
            int maxY = mc.world.getTopY();
            
            for (BlockPos chunkPos : spawnerChunks) {
                int startX = chunkPos.getX() << 4;
                int startZ = chunkPos.getZ() << 4;
                int endX = startX + 16;
                int endZ = startZ + 16;
                
                AABB box = new AABB(startX, minY, startZ, endX, maxY, endZ);
                event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        } catch (Exception e) {
            // Ignore render errors
        }
    }

    private void rescan() {
        try {
            spawnerChunks.clear();
            
            if (mc.world == null || mc.player == null) return;
            
            int viewDist = mc.options.getViewDistance().getValue();
            int playerChunkX = (int) Math.floor(mc.player.getX() / 16.0);
            int playerChunkZ = (int) Math.floor(mc.player.getZ() / 16.0);
            
            for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
                for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                    var chunk = mc.world.getChunk(cx, cz);
                    boolean hasSpawner = false;
                    
                    for (var blockEntity : chunk.getBlockEntities().values()) {
                        String name = blockEntity.getClass().getSimpleName();
                        if (name.contains("MobSpawner") || name.contains("SpawnerBlockEntity")) {
                            hasSpawner = true;
                            break;
                        }
                    }
                    
                    if (hasSpawner) {
                        spawnerChunks.add(new BlockPos(cx, 0, cz));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore scan errors
        }
    }
}
