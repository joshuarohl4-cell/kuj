package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.MinecraftAccess;
import meteordevelopment.meteorclient.systems.modules.Module;

public class AutoRelog extends Module {

    public AutoRelog() {
        super(AddonTemplate.CATEGORY, "auto-relog", "Disconnects and automatically reconnects to the server.");
    }

    @Override
    public void onActivate() {
        // Disconnect from server
        MinecraftAccess.disconnect(mc);
        
        // Schedule reconnection after short delay
        new Thread(() -> {
            try {
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}
            
            mc.execute(() -> {
                MinecraftAccess.connectToLastServer(mc);
                toggle();
            });
        }).start();
    }
}
