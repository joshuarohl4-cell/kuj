package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import meteordevelopment.meteorclient.systems.modules.Module;

public class AutoRelog extends Module {

    public AutoRelog() {
        super(AddonTemplate.CATEGORY, "auto-relog", "Disconnects and automatically reconnects to the server.");
    }

    @Override
    public void onActivate() {
        new Thread(() -> {
            try {
                // Find disconnect method
                Class<?> mcClass = mc.getClass();
                java.lang.reflect.Field connectionField = null;
                java.lang.reflect.Field serverField = null;
                
                for (var f : mcClass.getDeclaredFields()) {
                    String t = f.getType().getName();
                    if (t.contains("ClientPlayerNetworkHandler")) {
                        connectionField = f;
                        f.setAccessible(true);
                    }
                    if (t.contains("ServerInfo")) {
                        serverField = f;
                        f.setAccessible(true);
                    }
                }
                
                // Disconnect
                if (connectionField != null) {
                    Object handler = connectionField.get(mc);
                    if (handler != null) {
                        handler.getClass().getMethod("disconnect").invoke(handler);
                    }
                }
                
                Thread.sleep(500);
                
                // Reconnect
                if (serverField != null) {
                    Object serverInfo = serverField.get(mc);
                    if (serverInfo != null) {
                        mc.getClass().getMethod("method_29636", serverInfo.getClass()).invoke(mc, serverInfo);
                    }
                }
                
            } catch (Exception e) {
                // Silent fail
            }
            
            mc.execute(this::toggle);
        }).start();
    }
}
