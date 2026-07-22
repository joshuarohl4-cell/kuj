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
import java.util.Map;
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
            Object world = getWorld();
            if (world == null) return;
            
            int minY = getWorldMinY(world);
            int maxY = minY + getWorldHeight(world);
            
            for (BlockPos chunkPos : spawnerChunks) {
                int startX = chunkPos.getX() << 4;
                int startZ = chunkPos.getZ() << 4;
                
                AABB box = new AABB(startX, minY, startZ, startX + 16, maxY, startZ + 16);
                event.renderer.box(box, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
            }
        } catch (Exception e) {
            // Silent fail
        }
    }

    private void rescan() {
        try {
            spawnerChunks.clear();
            
            Object world = getWorld();
            Object player = getPlayer();
            if (world == null || player == null) return;
            
            int viewDist = getViewDistance();
            double px = getPlayerX(player);
            double pz = getPlayerZ(player);
            int playerChunkX = (int) Math.floor(px / 16.0);
            int playerChunkZ = (int) Math.floor(pz / 16.0);
            
            for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
                for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                    Object chunk = getChunk(world, cx, cz);
                    if (chunk == null) continue;
                    
                    Map<?, ?> blockEntities = getBlockEntities(chunk);
                    if (blockEntities == null) continue;
                    
                    boolean hasSpawner = false;
                    for (Object blockEntity : blockEntities.values()) {
                        String name = blockEntity.getClass().getSimpleName();
                        if (name.contains("MobSpawner") || name.contains("SpawnerBlockEntity") || name.contains("Spawner")) {
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
            // Silent fail
        }
    }
    
    private Object getWorld() {
        try {
            for (var f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("World")) {
                    f.setAccessible(true);
                    return f.get(mc);
                }
            }
        } catch (Exception e) {}
        return null;
    }
    
    private Object getPlayer() {
        try {
            for (var f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("Entity")) {
                    f.setAccessible(true);
                    return f.get(mc);
                }
            }
        } catch (Exception e) {}
        return null;
    }
    
    private int getWorldMinY(Object world) {
        try {
            for (var m : world.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && m.getName().toLowerCase().contains("bottom")) {
                    return (int) m.invoke(world);
                }
            }
        } catch (Exception e) {}
        return 0;
    }
    
    private int getWorldHeight(Object world) {
        try {
            for (var m : world.getClass().getMethods()) {
                if (m.getParameterCount() == 0 && (m.getName().toLowerCase().contains("height") || m.getName().toLowerCase().contains("top"))) {
                    return (int) m.invoke(world);
                }
            }
        } catch (Exception e) {}
        return 320;
    }
    
    private int getViewDistance() {
        try {
            for (var f : mc.getClass().getDeclaredFields()) {
                if (f.getType().getName().contains("Options")) {
                    f.setAccessible(true);
                    Object opts = f.get(mc);
                    if (opts != null) {
                        for (var vf : opts.getClass().getDeclaredFields()) {
                            if (vf.getType().getSimpleName().contains("Option")) {
                                vf.setAccessible(true);
                                Object opt = vf.get(opts);
                                if (opt != null) {
                                    return (int) opt.getClass().getMethod("getValue").invoke(opt);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {}
        return 8;
    }
    
    private double getPlayerX(Object player) {
        try {
            for (var f : player.getClass().getDeclaredFields()) {
                if (f.getName().contains("x")) {
                    f.setAccessible(true);
                    return f.getDouble(player);
                }
            }
        } catch (Exception e) {}
        return 0;
    }
    
    private double getPlayerZ(Object player) {
        try {
            for (var f : player.getClass().getDeclaredFields()) {
                if (f.getName().contains("z")) {
                    f.setAccessible(true);
                    return f.getDouble(player);
                }
            }
        } catch (Exception e) {}
        return 0;
    }
    
    private Object getChunk(Object world, int cx, int cz) {
        try {
            for (var m : world.getClass().getMethods()) {
                if (m.getName().toLowerCase().contains("getchunk") && m.getParameterCount() == 2) {
                    return m.invoke(world, cx, cz);
                }
            }
        } catch (Exception e) {}
        return null;
    }
    
    private Map<?, ?> getBlockEntities(Object chunk) {
        try {
            for (var f : chunk.getClass().getDeclaredFields()) {
                if (f.getType().getSimpleName().contains("Map")) {
                    f.setAccessible(true);
                    return (Map<?, ?>) f.get(chunk);
                }
            }
        } catch (Exception e) {}
        return null;
    }
}
