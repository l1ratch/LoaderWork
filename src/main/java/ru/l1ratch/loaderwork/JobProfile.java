package ru.l1ratch.loaderwork;

import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class JobProfile {

    private final String id;
    private final boolean enabled;
    private final String displayName;
    private final String permission;
    private final String pickupRegion;
    private final String dropoffRegion;
    private final int pickupHoldTicks;
    private final int dropoffHoldTicks;
    private final int respawnDelayTicks;
    private final int navigationIntervalTicks;
    private final int slownessAmplifier;
    private final int fatigueAmplifier;
    private final double displayHeight;
    private final double displayBehindOffset;
    private final Set<Material> allowedBlocks;
    private final RewardDefinition defaultReward;
    private final Map<Material, RewardDefinition> blockRewards;

    public JobProfile(
            String id,
            boolean enabled,
            String displayName,
            String permission,
            String pickupRegion,
            String dropoffRegion,
            int pickupHoldTicks,
            int dropoffHoldTicks,
            int respawnDelayTicks,
            int navigationIntervalTicks,
            int slownessAmplifier,
            int fatigueAmplifier,
            double displayHeight,
            double displayBehindOffset,
            Iterable<Material> allowedBlocks,
            RewardDefinition defaultReward,
            Map<Material, RewardDefinition> blockRewards
    ) {
        this.id = id;
        this.enabled = enabled;
        this.displayName = displayName;
        this.permission = permission;
        this.pickupRegion = pickupRegion;
        this.dropoffRegion = dropoffRegion;
        this.pickupHoldTicks = pickupHoldTicks;
        this.dropoffHoldTicks = dropoffHoldTicks;
        this.respawnDelayTicks = respawnDelayTicks;
        this.navigationIntervalTicks = navigationIntervalTicks;
        this.slownessAmplifier = slownessAmplifier;
        this.fatigueAmplifier = fatigueAmplifier;
        this.displayHeight = displayHeight;
        this.displayBehindOffset = displayBehindOffset;

        LinkedHashSet<Material> loadedBlocks = new LinkedHashSet<Material>();
        for (Material material : allowedBlocks) {
            loadedBlocks.add(material);
        }
        this.allowedBlocks = Collections.unmodifiableSet(loadedBlocks);
        this.defaultReward = defaultReward == null ? RewardDefinition.empty() : defaultReward;
        this.blockRewards = Collections.unmodifiableMap(new LinkedHashMap<Material, RewardDefinition>(blockRewards));
    }

    public String getId() {
        return id;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public String getDisplayName() {
        return displayName == null || displayName.trim().isEmpty() ? id : displayName;
    }

    public String getPermission() {
        return permission;
    }

    public String getPickupRegion() {
        return pickupRegion;
    }

    public String getDropoffRegion() {
        return dropoffRegion;
    }

    public int getPickupHoldTicks() {
        return pickupHoldTicks;
    }

    public int getDropoffHoldTicks() {
        return dropoffHoldTicks;
    }

    public int getRespawnDelayTicks() {
        return respawnDelayTicks;
    }

    public int getNavigationIntervalTicks() {
        return navigationIntervalTicks;
    }

    public int getSlownessAmplifier() {
        return slownessAmplifier;
    }

    public int getFatigueAmplifier() {
        return fatigueAmplifier;
    }

    public double getDisplayHeight() {
        return displayHeight;
    }

    public double getDisplayBehindOffset() {
        return displayBehindOffset;
    }

    public Set<Material> getAllowedBlocks() {
        return allowedBlocks;
    }

    public boolean isAllowed(Material material) {
        return allowedBlocks.isEmpty() || allowedBlocks.contains(material);
    }

    public boolean canUse(Player player) {
        return permission == null || permission.trim().isEmpty() || player.hasPermission(permission);
    }

    public RewardDefinition getReward(Material material) {
        RewardDefinition reward = blockRewards.get(material);
        return reward != null ? reward : defaultReward;
    }
}
