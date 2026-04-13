package ru.l1ratch.loaderwork;

import org.bukkit.Material;

public class ItemReward {

    private final Material material;
    private final int amount;

    public ItemReward(Material material, int amount) {
        this.material = material;
        this.amount = amount;
    }

    public Material getMaterial() {
        return material;
    }

    public int getAmount() {
        return amount;
    }
}
