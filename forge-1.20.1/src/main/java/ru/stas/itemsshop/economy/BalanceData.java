package ru.stas.itemsshop.economy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class BalanceData extends SavedData {
    public static final long STARTING_BALANCE = 0L;
    private static final String DATA_NAME = "items_shop_balances";
    private final Map<UUID, Long> balances = new HashMap<>();

    public static BalanceData get(ServerPlayer player) {
        MinecraftServer server = player.getServer();
        if (server == null) throw new IllegalStateException("Player has no server");
        return server.overworld().getDataStorage().computeIfAbsent(BalanceData::load, BalanceData::new, DATA_NAME);
    }

    public long getBalance(UUID playerId) {
        return balances.getOrDefault(playerId, STARTING_BALANCE);
    }

    public boolean withdraw(UUID playerId, long amount) {
        if (amount < 0) return false;
        long current = getBalance(playerId);
        if (current < amount) return false;
        balances.put(playerId, current - amount);
        setDirty();
        return true;
    }

    public long deposit(UUID playerId, long amount) {
        if (amount <= 0) return getBalance(playerId);
        long current = getBalance(playerId);
        long updated = current > Long.MAX_VALUE - amount ? Long.MAX_VALUE : current + amount;
        balances.put(playerId, updated);
        setDirty();
        return updated;
    }

    private static BalanceData load(CompoundTag root) {
        BalanceData data = new BalanceData();
        ListTag entries = root.getList("players", Tag.TAG_COMPOUND);
        for (int i = 0; i < entries.size(); i++) {
            CompoundTag entry = entries.getCompound(i);
            if (entry.hasUUID("id")) data.balances.put(entry.getUUID("id"), Math.max(0L, entry.getLong("balance")));
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag root) {
        ListTag entries = new ListTag();
        balances.forEach((id, balance) -> {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("id", id);
            entry.putLong("balance", balance);
            entries.add(entry);
        });
        root.put("players", entries);
        return root;
    }
}
