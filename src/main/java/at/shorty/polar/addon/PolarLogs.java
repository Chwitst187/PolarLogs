package at.shorty.polar.addon;

import at.shorty.polar.addon.config.*;
import at.shorty.polar.addon.util.SpecialUtilityJustForFoliaSpecialNeeds;
import lombok.Getter;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import top.polar.api.loader.LoaderApi;

import java.util.Set;

public class PolarLogs extends JavaPlugin {

    @Getter
    private static SpecialUtilityJustForFoliaSpecialNeeds specialUtilityJustForFoliaSpecialNeeds;
    private PolarApiHook polarApiHook;
    @Getter
    private Logs logs;
    public static String prefix = "§bLogs §7| §r";

    @Override
    public void onLoad() {
        specialUtilityJustForFoliaSpecialNeeds = new SpecialUtilityJustForFoliaSpecialNeeds(this);
        updateConfig();
        loadConfigAndApply();
        if (logs == null) {
            getLogger().severe("Invalid config, disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onEnable() {
        loadLogs();
    }

    private void loadConfigAndApply() {
        ConfigurationSection mitigationSection = getSection("mitigation");
        ConfigurationSection detectionSection = getSection("detection");
        ConfigurationSection cloudDetectionSection = getSection("cloud_detection");
        ConfigurationSection punishmentSection = getSection("punishment");
        ConfigurationSection logsSection = getSection("logs");

        if (mitigationSection == null || detectionSection == null || cloudDetectionSection == null || punishmentSection == null || logsSection == null) {
            logs = null;
            return;
        }

        Mitigation mitigation = Mitigation.loadFromConfigSection(mitigationSection);
        Detection detection = Detection.loadFromConfigSection(detectionSection);
        CloudDetection cloudDetection = CloudDetection.loadFromConfigSection(cloudDetectionSection);
        Punishment punishment = Punishment.loadFromConfigSection(punishmentSection);
        logs = Logs.loadFromConfigSection(logsSection);

        if (polarApiHook == null) {
            polarApiHook = new PolarApiHook(this, mitigation, detection, cloudDetection, punishment, logs);
            LoaderApi.registerEnableCallback(polarApiHook);
        } else {
            polarApiHook.reloadConfig(mitigation, detection, cloudDetection, punishment, logs);
        }
    }

    private ConfigurationSection getSection(String path) {
        ConfigurationSection section = getConfig().getConfigurationSection(path);
        if (section == null) {
            getLogger().severe("Missing config section: " + path);
        }
        return section;
    }

    private void loadLogs() {
        if (logs == null) {
            getLogger().severe("Logs config is missing. Plugin startup is incomplete.");
            return;
        }

        if (logs.isEnabled()) {
            if (!logs.getContext().matches("^[a-zA-Z0-9_]+$") || logs.getContext().isEmpty()) {
                getLogger().severe("Invalid log context name, must be [a-zA-Z0-9_]");
                getLogger().severe("Falling back to default context name: global");
                logs.setContext("global");
            }
            logs.establishConnection().thenAccept(established -> {
                if (established) {
                    getLogger().info("Connected to database.");
                } else {
                    getLogger().severe("Failed to establish connection to database.");
                }
            });
        }
    }

    @Override
    public void onDisable() {
        if (logs != null && logs.isEnabled()) {
            logs.dropConnection();
        }
    }

    public void testWebhook() {
        if (polarApiHook != null) {
            polarApiHook.testWebhook();
        }
    }

    public void reloadPluginConfig() {
        reloadConfig();
        if (logs != null) {
            logs.dropConnection();
        }

        loadConfigAndApply();
        if (logs == null) {
            getLogger().severe("Reloaded config is invalid. Skipping log reload.");
            return;
        }

        loadLogs();
    }

    // https://www.spigotmc.org/threads/solved-replacing-a-config-file.40420/#post-462207, accessed 1st April 2024
    public void updateConfig() {
        saveDefaultConfig();
        Configuration defaults = getConfig().getDefaults();
        if (defaults == null) {
            return;
        }
        Set<String> options = defaults.getKeys(false);
        Set<String> currentOptions = getConfig().getKeys(false);
        boolean changed = false;
        for (String option : options) {
            if (!currentOptions.contains(option)) {
                getConfig().set(option, defaults.get(option));
                changed = true;
            }
        }
        for (String currentOption : currentOptions) {
            if (!options.contains(currentOption)) {
                getConfig().set(currentOption, null);
                changed = true;
            }
        }
        if (changed) saveConfig();
    }
}
