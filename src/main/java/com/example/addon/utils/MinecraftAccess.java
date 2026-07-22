package com.example.addon.utils;

import net.minecraft.core.BlockPos;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MinecraftAccess {
    
    public static Object getPlayer(Object mc) {
        try {
            var field = mc.getClass().getDeclaredField("field_1724");
            field.setAccessible(true);
            return field.get(mc);
        } catch (Exception e) { return null; }
    }
    
    public static Object getWorld(Object mc) {
        try {
            var field = mc.getClass().getDeclaredField("field_1687");
            field.setAccessible(true);
            return field.get(mc);
        } catch (Exception e) { return null; }
    }
    
    public static Object getWorldRenderer(Object mc) {
        try {
            var field = mc.getClass().getDeclaredField("field_1769");
            field.setAccessible(true);
            return field.get(mc);
        } catch (Exception e) { return null; }
    }
    
    public static Object getViewDistanceOption(Object options) {
        try {
            var field = options.getClass().getDeclaredField("field_4213");
            field.setAccessible(true);
            return field.get(options);
        } catch (Exception e) { return null; }
    }
    
    public static void setViewDistance(Object mc, int distance) {
        try {
            var options = mc.getClass().getField("options").get(mc);
            if (options != null) {
                Object option = getViewDistanceOption(options);
                if (option != null) {
                    var method = option.getClass().getMethod("setValue", int.class);
                    method.invoke(option, distance);
                }
            }
        } catch (Exception e) {}
    }
    
    public static void reloadChunks(Object mc) {
        try {
            Object world = getWorld(mc);
            if (world != null) {
                var method = world.getClass().getMethod("updateLevelInPlayerCache");
                method.invoke(world);
            }
            Object renderer = getWorldRenderer(mc);
            if (renderer != null) {
                try {
                    var method = renderer.getClass().getMethod("method_3292");
                    method.invoke(renderer);
                } catch (Exception e) {
                    try {
                        var method = renderer.getClass().getMethod("reload");
                        method.invoke(renderer);
                    } catch (Exception e2) {}
                }
            }
        } catch (Exception e) {}
    }
    
    public static void sendCommand(Object mc, String command) {
        try {
            Object player = getPlayer(mc);
            if (player != null) {
                for (var field : player.getClass().getDeclaredFields()) {
                    if (field.getType().getName().contains("ServerPlayNetworkHandler")) {
                        field.setAccessible(true);
                        Object handler = field.get(player);
                        var method = handler.getClass().getMethod("method_45730", String.class);
                        method.invoke(handler, command);
                        return;
                    }
                }
            }
        } catch (Exception e) {}
    }
    
    public static double getPlayerY(Object mc) {
        try {
            Object player = getPlayer(mc);
            if (player != null) {
                try {
                    var field = player.getClass().getSuperclass().getDeclaredField("field_9280");
                    field.setAccessible(true);
                    return field.getDouble(player);
                } catch (Exception e) {
                    var method = player.getClass().getMethod("method_31478");
                    return (double) method.invoke(player);
                }
            }
        } catch (Exception e) {}
        return Double.MAX_VALUE;
    }
    
    public static double getPlayerX(Object mc) {
        try {
            Object player = getPlayer(mc);
            if (player != null) {
                try {
                    var field = player.getClass().getSuperclass().getDeclaredField("field_9226");
                    field.setAccessible(true);
                    return field.getDouble(player);
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        return 0;
    }
    
    public static double getPlayerZ(Object mc) {
        try {
            Object player = getPlayer(mc);
            if (player != null) {
                try {
                    var field = player.getClass().getSuperclass().getDeclaredField("field_9234");
                    field.setAccessible(true);
                    return field.getDouble(player);
                } catch (Exception e) {}
            }
        } catch (Exception e) {}
        return 0;
    }
    
    public static int getWorldMinY(Object mc) {
        try {
            Object world = getWorld(mc);
            if (world != null) {
                var method = world.getClass().getMethod("method_31607");
                return (int) method.invoke(world);
            }
        } catch (Exception e) {}
        return 0;
    }
    
    public static int getWorldHeight(Object mc) {
        try {
            Object world = getWorld(mc);
            if (world != null) {
                var method = world.getClass().getMethod("method_31605");
                return (int) method.invoke(world);
            }
        } catch (Exception e) {}
        return 320;
    }
    
    @SuppressWarnings("unchecked")
    public static Set<BlockPos> findSpawnerChunks(Object mc) {
        Set<BlockPos> spawnerChunks = new HashSet<>();
        try {
            Object world = getWorld(mc);
            if (world == null) return spawnerChunks;
            
            Object player = getPlayer(mc);
            if (player == null) return spawnerChunks;
            
            int viewDist = 8;
            try {
                var options = mc.getClass().getField("options").get(mc);
                if (options != null) {
                    Object option = getViewDistanceOption(options);
                    if (option != null) {
                        var method = option.getClass().getMethod("getValue");
                        viewDist = (int) method.invoke(option);
                    }
                }
            } catch (Exception e) {}
            
            int playerChunkX = (int) Math.floor(getPlayerX(mc) / 16.0);
            int playerChunkZ = (int) Math.floor(getPlayerZ(mc) / 16.0);
            
            var getChunk = world.getClass().getMethod("method_8497", int.class, int.class);
            
            for (int cx = playerChunkX - viewDist; cx <= playerChunkX + viewDist; cx++) {
                for (int cz = playerChunkZ - viewDist; cz <= playerChunkZ + viewDist; cz++) {
                    Object chunk = getChunk.invoke(world, cx, cz);
                    if (chunk != null) {
                        var getBlockEntities = chunk.getClass().getMethod("method_12214");
                        Map<?, ?> blockEntities = (Map<?, ?>) getBlockEntities.invoke(chunk);
                        boolean hasSpawner = false;
                        for (Object blockEntity : blockEntities.values()) {
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
            }
        } catch (Exception e) {}
        return spawnerChunks;
    }
    
    public static void disconnect(Object mc) {
        try {
            Object world = getWorld(mc);
            if (world != null) {
                var method = world.getClass().getMethod("method_29634");
                method.invoke(world);
            }
        } catch (Exception e) {
            // Fallback - try client method
            try {
                var method = mc.getClass().getMethod("method_29634");
                method.invoke(mc);
            } catch (Exception e2) {
                // Another fallback - disconnect via integrated server
                try {
                    var field = mc.getClass().getDeclaredField("field_1699"); // integratedServer
                    field.setAccessible(true);
                    var server = field.get(mc);
                    if (server != null) {
                        var method = server.getClass().getMethod("method_29634");
                        method.invoke(server);
                    }
                } catch (Exception e3) {
                    // Last resort - schedule disconnect
                    try {
                        var method = mc.getClass().getMethod("stop");
                        method.invoke(mc);
                    } catch (Exception e4) {}
                }
            }
        }
    }
    
    public static void connectToLastServer(Object mc) {
        try {
            // Get the client connection and reconnect
            var method = mc.getClass().getMethod("method_29635");
            method.invoke(mc);
        } catch (Exception e) {
            // Fallback - try to connect to saved server info
            try {
                var field = mc.getClass().getDeclaredField("field_1698"); // currentServerEntry
                field.setAccessible(true);
                var serverEntry = field.get(mc);
                if (serverEntry != null) {
                    var method = mc.getClass().getMethod("method_29636", serverEntry.getClass());
                    method.invoke(mc, serverEntry);
                }
            } catch (Exception e2) {
                // Fallback - use standard connect method
                try {
                    var method = mc.getClass().getMethod("connect");
                    method.invoke(mc);
                } catch (Exception e3) {}
            }
        }
    }
}
