package mafiaprod.emstory.listeners;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerRespawnEvent;

public class NetherLogic implements Listener {

    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        World nether = Bukkit.getWorld("world_nether");
        if (nether == null) return;

        // Если игрок умер в аду, респавним его в аду, но не меняем его общий спавн
        if (event.getPlayer().getWorld().equals(nether)) {
            Location respawnLocation = findSafeLocation(new Location(nether, 0, 100, 0));
            event.setRespawnLocation(respawnLocation);
        }
    }

    private Location findSafeLocation(Location baseLocation) {
        World world = baseLocation.getWorld();
        if (world == null) return baseLocation;

        int y = baseLocation.getBlockY();
        while (y > 0 && world.getBlockAt(baseLocation.getBlockX(), y, baseLocation.getBlockZ()).getType() != Material.AIR) {
            y++;
        }

        return new Location(world, baseLocation.getX(), y, baseLocation.getZ());
    }
}
