package ru.l1ratch.loaderwork;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LoaderWork extends JavaPlugin {

    private final Map<String, JobProfile> jobProfiles = new LinkedHashMap<String, JobProfile>();

    private PluginSettings pluginSettings;
    private MessageService messageService;
    private RegionService regionService;
    private RewardService rewardService;
    private LoaderController controller;
    private Economy economy;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        setupEconomy();
        reloadPluginState();

        controller = new LoaderController(this);
        getServer().getPluginManager().registerEvents(controller, this);

        PluginCommand command = getCommand("loader");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(this);
        }

        getLogger().info("LoaderWork включён");
    }

    @Override
    public void onDisable() {
        if (controller != null) {
            controller.shutdown();
        }
        getLogger().info("LoaderWork выключен");
    }

    public void reloadPluginState() {
        reloadConfig();

        pluginSettings = loadPluginSettings();
        messageService = new MessageService(this);
        regionService = new RegionService(this);
        loadJobs();
        rewardService = new RewardService(this);

        if (controller != null) {
            controller.handleReload();
        }
    }

    public void reloadSettingsFromConfig() {
        reloadConfig();
        pluginSettings = loadPluginSettings();
        if (controller != null) {
            controller.rebuildHeartbeat();
        }
    }

    public void reloadJobsFromConfig() {
        reloadConfig();
        loadJobs();
    }

    public void reloadRegionsFromConfig() {
        reloadConfig();
        if (regionService == null) {
            regionService = new RegionService(this);
        } else {
            regionService.reload();
        }
    }

    private PluginSettings loadPluginSettings() {
        FileConfiguration config = getConfig();
        ConfigurationSection settings = config.getConfigurationSection("settings");

        boolean autoSelectByRegion = getBoolean(settings, "auto-select-job-by-region", true);
        int updateIntervalTicks = clamp(getInt(settings, "update-interval-ticks", 2), 1, 20);
        double holdMaxMoveDistance = Math.max(0.05D, getDouble(settings, "hold-max-move-distance", 0.18D));
        int carryParticleIntervalTicks = clamp(getInt(settings, "carry-particle-interval-ticks", 10), 2, 40);

        return new PluginSettings(autoSelectByRegion, updateIntervalTicks, holdMaxMoveDistance, carryParticleIntervalTicks);
    }

    private void loadJobs() {
        jobProfiles.clear();

        ConfigurationSection jobsSection = getConfig().getConfigurationSection("jobs");
        if (jobsSection == null) {
            return;
        }

        for (String jobId : jobsSection.getKeys(false)) {
            ConfigurationSection jobSection = jobsSection.getConfigurationSection(jobId);
            if (jobSection == null) {
                continue;
            }

            boolean enabled = jobSection.getBoolean("enabled", true);
            String displayName = jobSection.getString("display-name", jobId);
            String permission = emptyToNull(jobSection.getString("permission", ""));
            String pickupRegion = jobSection.getString("pickup-region", "");
            String dropoffRegion = jobSection.getString("dropoff-region", "");

            int pickupHoldTicks = Math.max(10, jobSection.getInt("pickup-hold-ticks", 40));
            int dropoffHoldTicks = Math.max(10, jobSection.getInt("dropoff-hold-ticks", 30));
            int respawnDelayTicks = Math.max(0, jobSection.getInt("respawn-delay-ticks", 600));

            ConfigurationSection carrySection = jobSection.getConfigurationSection("carry");
            int navigationIntervalTicks = clamp(getInt(carrySection, "navigation-interval-ticks", 20), 0, 200);
            int slownessAmplifier = clamp(getInt(carrySection, "slowness-amplifier", 2), 0, 10);
            int fatigueAmplifier = clamp(getInt(carrySection, "fatigue-amplifier", 0), 0, 10);
            double displayHeight = getDouble(carrySection, "display-height", 0.35D);
            double displayBehindOffset = getDouble(carrySection, "display-behind-offset", 0.95D);

            List<String> blockNames = jobSection.getStringList("allowed-blocks");
            Map<Material, RewardDefinition> blockRewards = new LinkedHashMap<Material, RewardDefinition>();
            RewardDefinition defaultReward = loadReward(jobSection.getConfigurationSection("rewards.default"));

            ConfigurationSection byBlockSection = jobSection.getConfigurationSection("rewards.by-block");
            if (byBlockSection != null) {
                for (String materialKey : byBlockSection.getKeys(false)) {
                    Material material = Material.getMaterial(materialKey.toUpperCase());
                    if (material == null) {
                        getLogger().warning("Неизвестный материал награды '" + materialKey + "' в работе '" + jobId + "'.");
                        continue;
                    }
                    blockRewards.put(material, loadReward(byBlockSection.getConfigurationSection(materialKey)));
                }
            }

            JobProfile profile = new JobProfile(
                    jobId,
                    enabled,
                    displayName,
                    permission,
                    pickupRegion,
                    dropoffRegion,
                    pickupHoldTicks,
                    dropoffHoldTicks,
                    respawnDelayTicks,
                    navigationIntervalTicks,
                    slownessAmplifier,
                    fatigueAmplifier,
                    displayHeight,
                    displayBehindOffset,
                    parseMaterials(blockNames, jobId),
                    defaultReward,
                    blockRewards
            );

            jobProfiles.put(jobId, profile);
        }
    }

    private RewardDefinition loadReward(ConfigurationSection section) {
        if (section == null) {
            return RewardDefinition.empty();
        }

        double money = section.getDouble("money", 0.0D);
        int experience = Math.max(0, section.getInt("experience", 0));
        List<String> commands = new ArrayList<String>(section.getStringList("commands"));
        List<ItemReward> items = new ArrayList<ItemReward>();

        List<Map<?, ?>> rawItems = section.getMapList("items");
        for (Map<?, ?> rawItem : rawItems) {
            Object materialValue = rawItem.get("material");
            if (materialValue == null) {
                continue;
            }

            Material material = Material.getMaterial(String.valueOf(materialValue).toUpperCase());
            if (material == null || !material.isItem()) {
                getLogger().warning("Неизвестный предмет награды '" + materialValue + "'.");
                continue;
            }

            int amount = 1;
            Object amountValue = rawItem.get("amount");
            if (amountValue instanceof Number) {
                amount = ((Number) amountValue).intValue();
            } else if (amountValue != null) {
                try {
                    amount = Integer.parseInt(String.valueOf(amountValue));
                } catch (NumberFormatException ignored) {
                    amount = 1;
                }
            }

            items.add(new ItemReward(material, Math.max(1, amount)));
        }

        return new RewardDefinition(money, experience, commands, items);
    }

    private List<Material> parseMaterials(List<String> blockNames, String jobId) {
        List<Material> result = new ArrayList<Material>();
        for (String blockName : blockNames) {
            Material material = Material.getMaterial(blockName.toUpperCase());
            if (material == null || !material.isBlock()) {
                getLogger().warning("Неизвестный блок '" + blockName + "' в работе '" + jobId + "'.");
                continue;
            }
            result.add(material);
        }
        return result;
    }

    private void setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return;
        }

        RegisteredServiceProvider<Economy> registration = getServer().getServicesManager().getRegistration(Economy.class);
        economy = registration == null ? null : registration.getProvider();
    }

    private boolean getBoolean(ConfigurationSection section, String path, boolean fallback) {
        return section == null ? fallback : section.getBoolean(path, fallback);
    }

    private int getInt(ConfigurationSection section, String path, int fallback) {
        return section == null ? fallback : section.getInt(path, fallback);
    }

    private double getDouble(ConfigurationSection section, String path, double fallback) {
        return section == null ? fallback : section.getDouble(path, fallback);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String emptyToNull(String value) {
        return value == null || value.trim().isEmpty() ? null : value;
    }

    public PluginSettings getPluginSettings() {
        return pluginSettings;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public RegionService getRegionService() {
        return regionService;
    }

    public RewardService getRewardService() {
        return rewardService;
    }

    public Economy getEconomy() {
        return economy;
    }

    public Map<String, JobProfile> getJobProfiles() {
        return Collections.unmodifiableMap(jobProfiles);
    }

    public JobProfile getJobProfile(String jobId) {
        if (jobId == null) {
            return null;
        }

        JobProfile directMatch = jobProfiles.get(jobId);
        if (directMatch != null) {
            return directMatch;
        }

        for (Map.Entry<String, JobProfile> entry : jobProfiles.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(jobId)) {
                return entry.getValue();
            }
        }

        return null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return controller != null && controller.handleCommand(sender, args);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return controller == null ? Collections.<String>emptyList() : controller.handleTabComplete(sender, args);
    }
}
