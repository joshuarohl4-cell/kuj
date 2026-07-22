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

    // Cached method/field references
    private static Object worldField;
    private static Object playerField;
    private static Object optionsField;
    private static Object viewDistanceField;
    private static Object worldMinYMethod;
    private static Object worldHeightMethod;
    private static boolean reflectionInitialized = false;

    public SpawnerChunks() {
        super(AddonTemplate.CATEGORY, "spawner-chunks", "Highlights chunks that contain a mob spawner.");
    }

    private static void initReflection() {
        if (reflectionInitialized) return;
        try {
            Class<?> mcClass = Class.forName("net.minecraft.client.MinecraftClient");
            
            // Get fields
            for (var f : mcClass.getDeclaredFields()) {
                String typeName = f.getType().getName();
                if (typeName.contains("ClientWorld") || typeName.contains("World")) {
                    worldField = f;
                    f.setAccessible(true);
                } else if (typeName.contains("PlayerEntity") || typeName.contains("ClientPlayerEntity")) {
                    playerField = f;
                    f.setAccessible(true);
                } else if (typeName.contains("GameOptions")) {
                    optionsField = f;
                    f.setAccessible(true);
                }
            }
            
            // Get methods for world
            Class<?> worldClass = null;
            for (Object wf : new Object[]{worldField}) {
                if (wf != null) {
                    worldClass = ((java.lang.reflect.Field)wf).getType();
                    break;
                }
            }
            
            if (worldClass != null) {
                for (var m : worldClass.getDeclaredMethods()) {
                    String name = m.getName();
                    if (name.toLowerCase().contains("bottom") && m.getParameterCount() == 0) {
                        worldMinYMethod = m;
                        m.setAccessible(true);
                    }
                    if ((name.toLowerCase().contains("height") || name.toLowerCase().contains("top")) && m.getParameterCount() == 0) {
                        worldHeightMethod = m;
                        m.setAccessible(true);
                    }
                }
            }
            
            reflectionInitialized = true;
        } catch (Exception e) {
            // Reflection failed
        }
    }

    private Object getWorld() {
        try {
            if (worldField == null) return null;
            return ((java.lang.reflect.Field)worldField).get(mc);
        } catch (Exception e) { return null; }
    }

    private Object getPlayer() {
        try {
            if (playerField == null) return null;
            return ((java.lang.reflect.Field)playerField).get(mc);
        } catch (Exception e) { return null; }
    }

    private int getWorldMinY() {
        try {
            Object world = getWorld();
            if (world == null || worldMinYMethod == null) return 0;
            return (int) ((java.lang.reflect.Method)worldMinYMethod).invoke(world);
        } catch (Exception e) { return 0; }
    }

    private int getWorldHeight() {
        try {
            Object world = getWorld();
            if (world == null || worldHeightMethod == null) return 320;
            return (int) ((java.lang.reflect.Method)worldHeightMethod).invoke(world);
        } catch (Exception e) { return 320; }
    }

    private int getViewDistance() {
        try {
            if (optionsField == null) return 8;
            Object options = ((java.lang.reflect.Field)optionsField).get(mc);
            if (options == null) return 8;
            
            // Find view distance option
            for (var f : options.getClass().getDeclaredFields()) {
                String typeName = f.getType().getName();
                if (typeName.contains("Option") || typeName.contains("OptionSlider")) {
                    viewDistanceField = f;
                    f.setAccessible(true);
                    Object opt = f.get(options);
                    if (opt != null) {
                        var method = opt.getClass().getMethod("getValue");
                        return (int) method.invoke(opt);
                    }
                }
            }
        } catch (Exception e) {}
        return 8;
    }

    private double getPlayerPos(Object player, String axis) {
        try {
            // Try field approach
            for (var f : player.getClass().getSuperclass().getDeclaredFields()) {
                String fname = f.getName();
                if (axis.equals("x") && fname.contains("x")) {
                    f.setAccessible(true);
                    return f.getDouble(player);
                }
                if (axis.equals("y") && fname.contains("y")) {
                    f.setAccessible(true);
                    return f.getDouble(player);
                }
                if (axis.equals("z") && fname.contains("z")) {
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
                    m.setAccessible(true);
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

    @Override
    public void onActivate() {
        initReflection();
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
            
            int minY = getWorldMinY();
            int maxY = minY + getWorldHeight();
            
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
            double px = getPlayerPos(player, "x");
            double pz = getPlayerPos(player, "z");
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
}
