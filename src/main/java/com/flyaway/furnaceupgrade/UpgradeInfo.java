package com.flyaway.furnaceupgrade;

public class UpgradeInfo {
    private final String name;
    private final int cost;

    public UpgradeInfo(String name, int cost) {
        this.name = name;
        this.cost = cost;
    }

    public String getName() { return name; }
    public int getCost() { return cost; }
}
