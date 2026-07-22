package com.example.addon.modules;

import com.example.addon.AddonTemplate;
import com.example.addon.utils.MinecraftAccess;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class Homemeta extends Module {
    private final SettingGroup sgGeneral = this.settings.getDefaultGroup();
    private final Setting<Integer> homeNumber = sgGeneral.add(new IntSetting.Builder()
        .name("home-number")
        .description("Which home slot to use.")
        .defaultValue(3)
        .range(1, 10)
        .sliderMin(1)
        .sliderMax(10)
        .build()
    );

    public Homemeta() {
        super(AddonTemplate.CATEGORY, "home-meta", "Runs my home reset sequence.");
    }

    @Override
    public void onActivate() {
        if (MinecraftAccess.getPlayer(mc) == null) {
            toggle();
            return;
        }
        
        int home = homeNumber.get();
        String[] commands = {"delhome " + home, "sethome " + home, "rtp", "home " + home};
        Thread worker = new Thread(() -> {
            for (String command : commands) {
                mc.execute(() -> MinecraftAccess.sendCommand(mc, command));
                try {
                    Thread.sleep(450);
                } catch (InterruptedException ignored) {}
            }
            mc.execute(this::toggle);
        });
        worker.start();
    }
}
