package ru.l1ratch.loaderwork;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class RewardService {

    private final LoaderWork plugin;

    public RewardService(LoaderWork plugin) {
        this.plugin = plugin;
    }

    public RewardResult giveReward(Player player, JobProfile profile, Material blockType) {
        RewardDefinition reward = profile.getReward(blockType);
        double moneyGiven = 0.0D;
        int itemsGranted = 0;
        int commandsExecuted = 0;

        Economy economy = plugin.getEconomy();
        if (economy != null && reward.getMoney() != 0.0D) {
            economy.depositPlayer(player, reward.getMoney());
            moneyGiven = reward.getMoney();
        }

        if (reward.getExperience() > 0) {
            player.giveExp(reward.getExperience());
        }

        for (ItemReward itemReward : reward.getItems()) {
            ItemStack itemStack = new ItemStack(itemReward.getMaterial(), itemReward.getAmount());
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(itemStack);
            for (ItemStack remaining : leftover.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), remaining);
            }
            itemsGranted += itemReward.getAmount();
        }

        Map<String, String> placeholders = new HashMap<String, String>();
        placeholders.put("player", player.getName());
        placeholders.put("job", profile.getDisplayName());
        placeholders.put("job_id", profile.getId());
        placeholders.put("block", formatMaterial(blockType));
        placeholders.put("money", formatMoney(reward.getMoney()));
        placeholders.put("experience", String.valueOf(reward.getExperience()));

        for (String command : reward.getCommands()) {
            if (command == null || command.trim().isEmpty()) {
                continue;
            }

            String rendered = command;
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                rendered = rendered.replace("%" + entry.getKey() + "%", entry.getValue());
            }

            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), rendered);
            commandsExecuted++;
        }

        return new RewardResult(moneyGiven, reward.getExperience(), itemsGranted, commandsExecuted);
    }

    private String formatMaterial(Material material) {
        return material.name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private String formatMoney(double value) {
        if (Math.floor(value) == value) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.US, "%.2f", value);
    }
}
