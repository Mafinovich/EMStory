package mafiaprod.emstory.listeners;

import mafiaprod.emstory.EMStory;
import mafiaprod.emstory.recieps.Recieps;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Chest;
import org.bukkit.block.Dispenser;
import org.bukkit.block.TileState;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.*;

import mafiaprod.emstory.recieps.Recieps;
import mafiaprod.emstory.recieps.Recieps.CustomItem;

public class DispenserCrafting implements Listener {
    private final Plugin plugin;
    private final Recieps recieps;
    private final NamespacedKey crafterkey;

    public DispenserCrafting(Plugin plugin, Recieps recieps) {
        this.plugin = plugin;
        this.recieps = recieps;
        this.crafterkey = new NamespacedKey(plugin, "crafter");
    }

    @EventHandler
    public void onDispenserActivate(BlockDispenseEvent event) {
        Block block = event.getBlock();
        if (!(block.getState() instanceof Dispenser)) return;
        Dispenser dispenser = (Dispenser) block.getState();

        // Проверка, является ли этот диспенсер крафтером
        if (!isDispenserCrafter(block)) return;

        // ВАЖНО: Полная отмена события перед любыми манипуляциями
        event.setCancelled(true);

        // Отложенная проверка через такт
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Inventory inv = dispenser.getInventory();
            
            // Получаем список материалов в инвентаре
            Map<Material, Integer> availableMaterials = countMaterialsInInventory(inv);
            // Получаем список кастомных предметов в инвентаре
            Map<String, Integer> availableCustomItems = countCustomItemsInInventory(inv);
            
            // Пробуем создать предметы с более сложными рецептами (состоящие из кастомных компонентов)
            if (tryCraftComplexItem(block, inv, availableMaterials, availableCustomItems)) {
                return; // Выходим, так как создали сложный предмет
            }
            
            // Пробуем создать каждый из известных кастомных предметов из базовых материалов
            for (String itemId : recieps.getRegisteredItemIds()) {
                CustomItem itemInfo = recieps.getCustomItemInfo(itemId);
                
                // Проверяем, можно ли создать предмет (рецепт из базовых материалов)
                if (checkNativeMaterialsRecipe(itemId, availableMaterials)) {
                    // Удаляем использованные материалы
                    removeNativeMaterials(inv, itemId);
                    // Создаем предмет
                    ItemStack resultItem = recieps.createCustomItem(itemId);
                    // Выдаем игроку
                    block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), resultItem);
                    // Проигрываем звук и эффекты
                    playSuccessEffects(block);
                    return; // Выходим, так как создали предмет
                }
            }
            
            // Для обратной совместимости: проверяем старый рецепт ГМРД
            if (checkGMRDRecipe(inv)) {
                removeIngredients(inv);
                ItemStack gmrd = recieps.createGMRD();
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), gmrd);
                playSuccessEffects(block);
            }
        }, 1L);
    }

    // Проигрывание звуков и эффектов при успешном крафте
    private void playSuccessEffects(Block block) {
        block.getWorld().playSound(block.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        block.getWorld().spawnParticle(org.bukkit.Particle.PORTAL, 
                block.getLocation().add(0.5, 1, 0.5), 30);
    }

    // Подсчет всех материалов в инвентаре
    private Map<Material, Integer> countMaterialsInInventory(Inventory inv) {
        Map<Material, Integer> materials = new HashMap<>();
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            
            // Пропускаем кастомные предметы
            if (recieps.isAnyCustomItem(item)) continue;
            
            Material type = item.getType();
            materials.put(type, materials.getOrDefault(type, 0) + item.getAmount());
        }
        return materials;
    }
    
    // Подсчет всех кастомных предметов в инвентаре
    private Map<String, Integer> countCustomItemsInInventory(Inventory inv) {
        Map<String, Integer> customItems = new HashMap<>();
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            
            String customItemId = recieps.getCustomItemId(item);
            if (customItemId != null) {
                customItems.put(customItemId, customItems.getOrDefault(customItemId, 0) + item.getAmount());
            }
        }
        return customItems;
    }
    
    // Проверка рецепта для создания предмета из базовых материалов
    private boolean checkNativeMaterialsRecipe(String itemId, Map<Material, Integer> availableMaterials) {
        return recieps.checkRecipe(itemId, availableMaterials);
    }
    
    // Удаление нативных материалов для создания предмета
    private void removeNativeMaterials(Inventory inv, String itemId) {
        Map<Material, Integer> toRemove = recieps.getIngredientsForItem(itemId);
        
        // Создаем список слотов для удаления
        List<Integer> slotsToRemove = new ArrayList<>();
        
        // Проходим по всем слотам инвентаря
        for (int i = 0; i < inv.getContents().length; i++) {
            ItemStack item = inv.getContents()[i];
            if (item == null) continue;
            
            // Пропускаем кастомные предметы
            if (recieps.isAnyCustomItem(item)) continue;
            
            Material type = item.getType();
            if (toRemove.containsKey(type)) {
                int need = toRemove.get(type);
                int has = item.getAmount();
                
                if (has <= need) {
                    // Помечаем слот к полному удалению
                    slotsToRemove.add(i);
                    toRemove.put(type, need - has);
                } else {
                    // Уменьшаем количество предмета
                    item.setAmount(has - need);
                    toRemove.put(type, 0);
                }
            }
        }
        
        // Удаляем слоты в обратном порядке, чтобы не сбить индексацию
        Collections.reverse(slotsToRemove);
        for (int slot : slotsToRemove) {
            inv.setItem(slot, null);
        }
    }

    // Обработчик установки Крафтера
    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        if (event.getBlockPlaced().getType() != Material.DISPENSER) return;

        ItemStack item = event.getItemInHand();
        if (!isItemDispenserCrafter(item)) return;

        Dispenser dispenser = (Dispenser) event.getBlockPlaced().getState();
        TileState tileState = (TileState) dispenser;
        PersistentDataContainer data = tileState.getPersistentDataContainer();

        // Устанавливаем NBT-тег "crafter" при установке сундука
        data.set(crafterkey, PersistentDataType.STRING, "true");
        tileState.update();
    }

    // Обработчик разрушения Крафтера
    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Player player = event.getPlayer();
        if (block.getType() != Material.DISPENSER) return;

        if(isDispenserCrafter(block)){
            player.sendMessage(ChatColor.RED + "Ты че баклан, дефективный?");
            event.setCancelled(true);
        }
    }

    // УСТАРЕВШИЙ МЕТОД - для обратной совместимости
    // Проверка, есть ли правильные ингредиенты в раздатчике для крафта ГМРД
    private boolean checkGMRDRecipe(Inventory inv) {
        Map<Material, Integer> required = new HashMap<>();
        required.put(Material.IRON_INGOT, 8);
        required.put(Material.REDSTONE, 1);

        Map<Material, Integer> found = new HashMap<>();

        // Подсчет точного количества каждого ингредиента
        for (ItemStack item : inv.getContents()) {
            if (item == null) continue;
            // Пропускаем кастомные предметы
            if (recieps.isAnyCustomItem(item)) continue;
            
            found.put(item.getType(), found.getOrDefault(item.getType(), 0) + item.getAmount());
        }

        // Проверка на точное количество и отсутствие лишних материалов
        for (Material mat : required.keySet()) {
            int foundAmount = found.getOrDefault(mat, 0);
            int requiredAmount = required.get(mat);

            if (foundAmount != requiredAmount) {
                return false;
            }
        }

        // Дополнительная проверка, что больше нет других материалов
        for (Material mat : found.keySet()) {
            if (!required.containsKey(mat)) {
                return false;
            }
        }

        return true;
    }

    // УСТАРЕВШИЙ МЕТОД - для обратной совместимости
    // Удаление использованных ингредиентов
    private void removeIngredients(Inventory inv) {
        Map<Material, Integer> toRemove = new HashMap<>();
        toRemove.put(Material.IRON_INGOT, 8);
        toRemove.put(Material.REDSTONE, 1);

        // Создаем список слотов для удаления
        List<Integer> slotsToRemove = new ArrayList<>();

        // Проходим по всем слотам инвентаря
        for (int i = 0; i < inv.getContents().length; i++) {
            ItemStack item = inv.getContents()[i];
            if (item == null) continue;
            
            // Пропускаем кастомные предметы
            if (recieps.isAnyCustomItem(item)) continue;

            Material type = item.getType();
            if (toRemove.containsKey(type)) {
                int need = toRemove.get(type);
                int has = item.getAmount();

                if (has <= need) {
                    // Помечаем слот к полному удалению
                    slotsToRemove.add(i);
                    toRemove.put(type, need - has);
                } else {
                    // Уменьшаем количество предмета
                    item.setAmount(has - need);
                    toRemove.put(type, 0);
                }
            }
        }

        // Удаляем слоты в обратном порядке, чтобы не сбить индексацию
        Collections.reverse(slotsToRemove);
        for (int slot : slotsToRemove) {
            inv.setItem(slot, null);
        }
    }

    // Удаление ингредиентов для сложного рецепта
    private void removeComplexRecipeIngredients(Inventory inv, String itemId) {
        Map<String, Integer> customItemsToRemove = new HashMap<>();
        Map<Material, Integer> materialsToRemove = new HashMap<>();
        
        // Для каждого сложного предмета определяем требуемые компоненты
        if (itemId.equals("mass_spectral_stabilizer")) {
            customItemsToRemove.put("quantum_spectrometer", 1);
            customItemsToRemove.put("ender_capacitor", 2);
            customItemsToRemove.put("amethyst_circuit", 1);
            customItemsToRemove.put("redstone_chip", 2);
        } else if (itemId.equals("annihilation_chamber")) {
            customItemsToRemove.put("unstable_netherite_alloy", 2);
            customItemsToRemove.put("antiparticle_generator", 1);
            customItemsToRemove.put("ender_void_crystal", 1);
            customItemsToRemove.put("obsidian_shell", 1);
        } else if (itemId.equals("gravity_compensator")) {
            customItemsToRemove.put("gravitational_core", 1);
            customItemsToRemove.put("space_resonator", 2);
            customItemsToRemove.put("obsidian_frame", 1);
        } else if (itemId.equals("control_interface")) {
            customItemsToRemove.put("redstone_chip", 7);
            customItemsToRemove.put("quartz_display", 1);
            materialsToRemove.put(Material.ENDER_PEARL, 1);
        } else if (itemId.equals("plasma_resonance_generator")) {
            customItemsToRemove.put("quantum_piezo", 2);
            customItemsToRemove.put("superconducting_magnet", 1);
            customItemsToRemove.put("radiation_crystal", 1);
            customItemsToRemove.put("copper_catalyst", 2);
            customItemsToRemove.put("obsidian_resonator", 1);
        } else if (itemId.equals("antiparticle_generator")) {
            customItemsToRemove.put("redstone_chip", 5);
            materialsToRemove.put(Material.CRYING_OBSIDIAN, 2);
            materialsToRemove.put(Material.LAPIS_BLOCK, 1);
        } else if (itemId.equals("netherite")) {
            customItemsToRemove.put("fire_dust", 2);
            materialsToRemove.put(Material.GOLD_INGOT, 3);
            materialsToRemove.put(Material.IRON_INGOT, 2);
            materialsToRemove.put(Material.COPPER_INGOT, 2);
        } else if (itemId.equals("resonance_catalyst")) {
            customItemsToRemove.put("glowing_crystal", 2);
            customItemsToRemove.put("carbon_alloy", 3);
            customItemsToRemove.put("deepslate_frame", 1);
            customItemsToRemove.put("smoldering_core", 1);
        }
        
        // Создаем список слотов для удаления или уменьшения
        List<Integer> slotsToProcess = new ArrayList<>();
        
        // Проходим по всем слотам инвентаря
        for (int i = 0; i < inv.getContents().length; i++) {
            ItemStack item = inv.getContents()[i];
            if (item == null) continue;
            
            // Проверяем кастомные предметы
            String customItemId = recieps.getCustomItemId(item);
            if (customItemId != null && customItemsToRemove.containsKey(customItemId)) {
                int need = customItemsToRemove.get(customItemId);
                int has = item.getAmount();
                
                if (has <= need) {
                    // Полностью удаляем предмет
                    inv.setItem(i, null);
                    customItemsToRemove.put(customItemId, need - has);
                } else {
                    // Уменьшаем количество
                    item.setAmount(has - need);
                    customItemsToRemove.put(customItemId, 0);
                }
                
                continue;
            }
            
            // Проверяем обычные материалы
            Material material = item.getType();
            if (materialsToRemove.containsKey(material)) {
                int need = materialsToRemove.get(material);
                int has = item.getAmount();
                
                if (has <= need) {
                    // Полностью удаляем предмет
                    inv.setItem(i, null);
                    materialsToRemove.put(material, need - has);
                } else {
                    // Уменьшаем количество
                    item.setAmount(has - need);
                    materialsToRemove.put(material, 0);
                }
            }
        }
    }
    
    // Проверка наличия всех компонентов для сложного рецепта
    private boolean checkComplexRecipe(String itemId, Inventory inv, Map<Material, Integer> availableMaterials,
                                      Map<String, Integer> availableCustomItems) {
        Map<String, Integer> requiredCustomItems = new HashMap<>();
        Map<Material, Integer> requiredMaterials = new HashMap<>();
        
        // Для каждого сложного предмета определяем требуемые компоненты
        if (itemId.equals("mass_spectral_stabilizer")) {
            requiredCustomItems.put("quantum_spectrometer", 1);
            requiredCustomItems.put("ender_capacitor", 2);
            requiredCustomItems.put("amethyst_circuit", 1);
            requiredCustomItems.put("redstone_chip", 2);
        } else if (itemId.equals("annihilation_chamber")) {
            requiredCustomItems.put("unstable_netherite_alloy", 2);
            requiredCustomItems.put("antiparticle_generator", 1);
            requiredCustomItems.put("ender_void_crystal", 1);
            requiredCustomItems.put("obsidian_shell", 1);
        } else if (itemId.equals("gravity_compensator")) {
            requiredCustomItems.put("gravitational_core", 1);
            requiredCustomItems.put("space_resonator", 2);
            requiredCustomItems.put("obsidian_frame", 1);
        } else if (itemId.equals("control_interface")) {
            requiredCustomItems.put("redstone_chip", 7);
            requiredCustomItems.put("quartz_display", 1);
            requiredMaterials.put(Material.ENDER_PEARL, 1);
        } else if (itemId.equals("plasma_resonance_generator")) {
            requiredCustomItems.put("quantum_piezo", 2);
            requiredCustomItems.put("superconducting_magnet", 1);
            requiredCustomItems.put("radiation_crystal", 1);
            requiredCustomItems.put("copper_catalyst", 2);
            requiredCustomItems.put("obsidian_resonator", 1);
        } else if (itemId.equals("antiparticle_generator")) {
            requiredCustomItems.put("redstone_chip", 5);
            requiredMaterials.put(Material.CRYING_OBSIDIAN, 2);
            requiredMaterials.put(Material.LAPIS_BLOCK, 1);
        } else if (itemId.equals("netherite")) {
            requiredCustomItems.put("fire_dust", 2);
            requiredMaterials.put(Material.GOLD_INGOT, 3);
            requiredMaterials.put(Material.IRON_INGOT, 2);
            requiredMaterials.put(Material.COPPER_INGOT, 2);
        } else if (itemId.equals("resonance_catalyst")) {
            requiredCustomItems.put("glowing_crystal", 2);
            requiredCustomItems.put("carbon_alloy", 3);
            requiredCustomItems.put("deepslate_frame", 1);
            requiredCustomItems.put("smoldering_core", 1);
        } else if (itemId.equals("portal_fuel")) {
            // Для обратной совместимости - перенаправляем на resonance_catalyst
            return checkComplexRecipe("resonance_catalyst", inv, availableMaterials, availableCustomItems);
        }
        
        // Проверяем, есть ли все кастомные предметы
        for (Map.Entry<String, Integer> entry : requiredCustomItems.entrySet()) {
            String customId = entry.getKey();
            int requiredAmount = entry.getValue();
            int availableAmount = availableCustomItems.getOrDefault(customId, 0);
            
            if (availableAmount < requiredAmount) {
                return false;
            }
        }
        
        // Проверяем, есть ли все обычные материалы
        for (Map.Entry<Material, Integer> entry : requiredMaterials.entrySet()) {
            Material material = entry.getKey();
            int requiredAmount = entry.getValue();
            int availableAmount = availableMaterials.getOrDefault(material, 0);
            
            if (availableAmount < requiredAmount) {
                return false;
            }
        }
        
        // Проверяем, что больше ничего нет в инвентаре, кроме требуемых компонентов
        int totalRequired = requiredCustomItems.size() + requiredMaterials.size();
        int totalAvailable = 0;
        
        // Проверяем все кастомные предметы и материалы в инвентаре
        for (String customId : availableCustomItems.keySet()) {
            if (requiredCustomItems.containsKey(customId)) {
                totalAvailable++;
            } else {
                // Есть лишний кастомный предмет
                return false;
            }
        }
        
        for (Material material : availableMaterials.keySet()) {
            if (requiredMaterials.containsKey(material)) {
                totalAvailable++;
            } else {
                // Есть лишний обычный материал
                return false;
            }
        }
        
        // Проверяем, что ровно столько предметов, сколько требуется
        return totalAvailable == totalRequired;
    }
    
    // Попытка крафта сложного предмета из кастомных компонентов
    private boolean tryCraftComplexItem(Block block, Inventory inv, Map<Material, Integer> availableMaterials, 
                                       Map<String, Integer> availableCustomItems) {
        // Идентификаторы сложных предметов, требующих кастомные компоненты
        String[] complexItems = {
            "mass_spectral_stabilizer", 
            "annihilation_chamber", 
            "gravity_compensator", 
            "control_interface", 
            "plasma_resonance_generator",
            "netherite",
            "resonance_catalyst"
        };
        
        for (String itemId : complexItems) {
            // Проверяем рецепт для каждого сложного предмета
            if (checkComplexRecipe(itemId, inv, availableMaterials, availableCustomItems)) {
                // Удаляем компоненты
                removeComplexRecipeIngredients(inv, itemId);
                // Создаем предмет
                ItemStack resultItem = recieps.createCustomItem(itemId);
                // При создании portal_fuel, на самом деле создаем resonance_catalyst
                if (itemId.equals("portal_fuel")) {
                    resultItem = recieps.createCustomItem("resonance_catalyst");
                }
                // Выдаем игроку
                block.getWorld().dropItemNaturally(block.getLocation().add(0.5, 1, 0.5), resultItem);
                // Проигрываем звук и эффекты
                playSpecialSuccessEffects(block);
                return true;
            }
        }
        
        return false;
    }
    
    // Улучшенные эффекты для создания сложных предметов
    private void playSpecialSuccessEffects(Block block) {
        block.getWorld().playSound(block.getLocation(), org.bukkit.Sound.ENTITY_ENDER_DRAGON_GROWL, 0.5f, 1.0f);
        block.getWorld().playSound(block.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1.0f, 1.0f);
        block.getWorld().spawnParticle(Particle.DRAGON_BREATH, 
                block.getLocation().add(0.5, 1, 0.5), 50, 0, 0, 0, 1);
        block.getWorld().spawnParticle(Particle.PORTAL, 
                block.getLocation().add(0.5, 1, 0.5), 50);
        
        // Создаем эффект молнии без урона
        new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                if (ticks > 5) {
                    this.cancel();
                    return;
                }
                block.getWorld().spawnParticle(Particle.END_ROD, 
                        block.getLocation().add(0.5, 1 + ticks * 0.2, 0.5), 10, 0.1, 0.1, 0.1, 0.1);
                ticks++;
            }
        }.runTaskTimer(plugin, 0L, 2L);
    }

    // Проверка, является ли блок крафтером
    private boolean isDispenserCrafter(Block block) {
        if (block.getType() != Material.DISPENSER) return false;

        BlockState state = block.getState();
        if (!(state instanceof TileState)) return false;

        TileState tileState = (TileState) state;
        PersistentDataContainer data = tileState.getPersistentDataContainer();
        return data.has(crafterkey, PersistentDataType.STRING);
    }

    // Проверка, является ли предмет крафтером
    private boolean isItemDispenserCrafter(ItemStack item) {
        if (item == null || item.getType() != Material.DISPENSER) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer data = meta.getPersistentDataContainer();
        return data.has(crafterkey, PersistentDataType.STRING);
    }
}
