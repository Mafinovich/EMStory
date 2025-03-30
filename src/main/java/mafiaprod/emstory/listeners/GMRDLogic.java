package mafiaprod.emstory.listeners;

import mafiaprod.emstory.EMStory;
import mafiaprod.emstory.recieps.Recieps;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.TileState;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;


public class GMRDLogic implements Listener {
    private final NamespacedKey gmrKey;
    private final Plugin plugin;
    private final Recieps recieps;

    public GMRDLogic(Plugin plugin, Recieps recieps) {
        this.recieps = recieps;
        this.gmrKey =  new NamespacedKey(plugin, "gmrd");
        this.plugin = plugin;
    }

    // Обработчик установки ГМРД
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.CHEST) return;

        ItemStack item = event.getItemInHand();
        if (!isGMRD(item)) return;

        Chest chest = (Chest) event.getBlockPlaced().getState();
        TileState tileState = (TileState) chest;
        PersistentDataContainer data = tileState.getPersistentDataContainer();

        // Устанавливаем NBT-тег "gmrd" при установке сундука
        data.set(gmrKey, PersistentDataType.STRING, "true");
        tileState.update();
    }

    // Обработчик разрушения ГМРД
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        if (block.getType() != Material.CHEST) return;

        Chest chest = (Chest) block.getState();
        TileState tileState = (TileState) chest;
        PersistentDataContainer data = tileState.getPersistentDataContainer();

        if (data.has(gmrKey, PersistentDataType.STRING)) {
            event.setDropItems(false); // Отключаем стандартный дроп сундука

            // Выбрасываем содержимое сундука
            for (ItemStack item : chest.getInventory().getContents()) {
                if (item != null && item.getType() != Material.AIR) {
                    block.getWorld().dropItemNaturally(block.getLocation(), item);
                }
            }

            // Используем Recieps для создания ГМРД
            Recieps recieps = new Recieps(plugin);
            ItemStack gmrd = recieps.createGMRD();

            // Дропаем предмет с сохранённым тегом
            block.getWorld().dropItemNaturally(block.getLocation(), gmrd);
        }
    }


    // Проверяем, является ли предмет ГМРД
    private boolean isGMRD(ItemStack item) {
        if (item == null || item.getType() != Material.CHEST) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(gmrKey, PersistentDataType.STRING);
    }
}
