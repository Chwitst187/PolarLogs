package at.shorty.polar.addon.util;

import at.shorty.polar.addon.PolarLogs;
import lombok.Getter;
import org.bukkit.Bukkit;

@Getter
public class SpecialUtilityJustForFoliaSpecialNeeds {

    private final PolarLogs plugin;

    public SpecialUtilityJustForFoliaSpecialNeeds(PolarLogs plugin) {
        this.plugin = plugin;
    }

    public void runAsyncNow(Runnable runnable) {
        if (runnable == null) return;
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }

}
