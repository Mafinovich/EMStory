package mafiaprod.emstory.listeners;

import mafiaprod.emstory.EMStory;
import org.bukkit.Material;
import org.bukkit.block.Furnace;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.FurnaceBurnEvent;
import org.bukkit.event.inventory.FurnaceSmeltEvent;
import org.bukkit.inventory.FurnaceInventory;
import org.bukkit.inventory.ItemStack;

public class FurnaceLogic implements Listener {
    
    private final EMStory plugin;
    
    public FurnaceLogic(EMStory plugin) {
        this.plugin = plugin;
    }
    
    @EventHandler
    public void onFurnaceBurn(FurnaceBurnEvent event) {
        ItemStack fuel = event.getFuel();
        
        // Обрабатываем только для угля и древесного угля
        if (fuel.getType() == Material.COAL) {
            // Обычный уголь тратится в 2 раза медленнее (горит дольше)
            event.setBurnTime(event.getBurnTime() * 2);
        } else if (fuel.getType() == Material.CHARCOAL) {
            // Древесный уголь тратится в 2 раза быстрее (горит меньше)
            event.setBurnTime(event.getBurnTime() / 2);
        }
    }
    
    @EventHandler
    public void onFurnaceSmelt(FurnaceSmeltEvent event) {
        // Проверяем, является ли предмет рудой
        if (isOre(event.getSource().getType())) {
            // Получаем печь
            Furnace furnace = (Furnace) event.getBlock().getState();
            FurnaceInventory inventory = furnace.getInventory();
            
            // Проверяем топливо
            ItemStack fuel = inventory.getFuel();
            if (fuel == null || fuel.getType() != Material.COAL) {
                // Отменяем плавку, если топливо не обычный уголь
                event.setCancelled(true);
            }
        }
    }
    
    // Проверка, является ли материал рудой
    private boolean isOre(Material material) {
        switch (material) {
            case IRON_ORE:
            case GOLD_ORE:
            case COPPER_ORE:
            case DEEPSLATE_IRON_ORE:
            case DEEPSLATE_GOLD_ORE:
            case DEEPSLATE_COPPER_ORE:
            case NETHER_GOLD_ORE:
            case ANCIENT_DEBRIS:
                return true;
            default:
                return false;
        }
    }
}
