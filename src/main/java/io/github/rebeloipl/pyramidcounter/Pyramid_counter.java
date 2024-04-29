package io.github.rebeloipl.pyramidcounter;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class Pyramid_counter extends JavaPlugin implements Listener {

    private Map<UUID, Integer> playerRunCounts = new HashMap<>();
    private Map<UUID, LocalDateTime> lastRunDates = new HashMap<>();
    private Map<UUID, Integer> itemsPickedUpToday = new HashMap<>();
    private Map<UUID, LocalDateTime> lastItemPickupTimes = new HashMap<>();

    private final int maxRunsPerDay = 10;
    private final int maxItemsPerDay = 10;
    private final int maxItemsPerPeriod = 3; // Maximum items per cooldown period
    private final Duration cooldownPeriod = Duration.ofMinutes(10); // 10 minutes cooldown period

    private int pyramidMinX = 7800;
    private int pyramidMaxX = 7860;
    private int pyramidMinY = 32;
    private int pyramidMaxY = 128;
    private int pyramidMinZ = -7730;
    private int pyramidMaxZ = -7780;

    @Override
    public void onEnable() {
        getLogger().info("Minecraft Pyramid Limit script enabled!");

        // Register event listeners
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {
        getLogger().info("Minecraft Pyramid Limit script disabled!");
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        Location chestLocation = event.getClickedBlock().getLocation();

        // Check if the player interacts with a chest in the mini-game area
        if (isChestInMiniGameArea(chestLocation)) {
            // Check if the player has exceeded the daily run limit
            if (hasExceededDailyLimit(player)) {
                player.sendMessage("You have reached the daily run limit for the pyramid mini-game.");
                event.setCancelled(true); // Cancel the interaction
                return;
            }

            // Increment the player's run count and update last run date
            updateRunCount(player);

            // Check if the player has exceeded the daily item limit
            if (hasExceededItemLimit(player)) {
                player.sendMessage("You have already picked up the maximum number of items from the pyramid today.");
                event.setCancelled(true); // Cancel the interaction
                return;
            }

            // Check if the player has exceeded the item limit per cooldown period
            if (hasExceededItemLimitPerPeriod(player)) {
                player.sendMessage("You have reached the item pickup limit for the current period. Please wait before picking up more items.");
                event.setCancelled(true); // Cancel the interaction
                return;
            }

            // Increment the player's item count and update last item pickup time
            updateItemCount(player);
            updateLastItemPickupTime(player);
        }
    }

    private boolean isChestInMiniGameArea(Location location) {
        // Check if the clicked block is within the boundaries of the pyramid area
        int blockX = location.getBlockX();
        int blockY = location.getBlockY();
        int blockZ = location.getBlockZ();

        return blockX >= pyramidMinX && blockX <= pyramidMaxX &&
                blockY >= pyramidMinY && blockY <= pyramidMaxY &&
                blockZ >= pyramidMinZ && blockZ <= pyramidMaxZ;
    }

    private boolean hasExceededDailyLimit(Player player) {
        // Check if the player has a record in the lastRunDates map
        UUID playerId = player.getUniqueId();
        if (!lastRunDates.containsKey(playerId)) {
            lastRunDates.put(playerId, LocalDateTime.now());
            playerRunCounts.put(playerId, 0); // Initialize run count for the player
            itemsPickedUpToday.put(playerId, 0); // Initialize item count for the player
            return false;
        }

        // Check if the current date is different from the last run date
        LocalDateTime currentDate = LocalDateTime.now();
        LocalDateTime lastRunDate = lastRunDates.get(playerId);
        if (!currentDate.toLocalDate().equals(lastRunDate.toLocalDate())) {
            lastRunDates.put(playerId, currentDate); // Update last run date
            playerRunCounts.put(playerId, 0); // Reset run count for the player
            itemsPickedUpToday.put(playerId, 0); // Reset item count for the player
            return false;
        }

        // Check if the player has exceeded the daily run limit
        return playerRunCounts.get(playerId) >= maxRunsPerDay;
    }

    private void updateRunCount(Player player) {
        UUID playerId = player.getUniqueId();
        int runCount = playerRunCounts.getOrDefault(playerId, 0);
        playerRunCounts.put(playerId, runCount + 1);
    }

    private boolean hasExceededItemLimit(Player player) {
        UUID playerId = player.getUniqueId();
        return itemsPickedUpToday.getOrDefault(playerId, 0) >= maxItemsPerDay;
    }

    private void updateItemCount(Player player) {
        UUID playerId = player.getUniqueId();
        int itemCount = itemsPickedUpToday.getOrDefault(playerId, 0);
        itemsPickedUpToday.put(playerId, itemCount + 1);
    }

    private boolean hasExceededItemLimitPerPeriod(Player player) {
        UUID playerId = player.getUniqueId();
        LocalDateTime lastPickupTime = lastItemPickupTimes.getOrDefault(playerId, LocalDateTime.MIN);
        LocalDateTime currentTime = LocalDateTime.now();

        // Check if the cooldown period has elapsed since the last pickup time
        return Duration.between(lastPickupTime, currentTime).compareTo(cooldownPeriod) < 0 &&
                itemsPickedUpToday.getOrDefault(playerId, 0) % maxItemsPerPeriod == 0;
    }

    private void updateLastItemPickupTime(Player player) {
        UUID playerId = player.getUniqueId();
        lastItemPickupTimes.put(playerId, LocalDateTime.now());
    }
}
