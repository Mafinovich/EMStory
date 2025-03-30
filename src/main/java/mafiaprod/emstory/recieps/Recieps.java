package mafiaprod.emstory.recieps;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.TileState;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Recieps {
    private final Plugin plugin;
    private final Map<String, CustomItem> customItems = new HashMap<>();

    // Класс для хранения информации о кастомном предмете
    public static class CustomItem {
        private final Material baseMaterial;
        private final String displayName;
        private final List<String> lore;
        private final String id;
        private final List<Map.Entry<Material, Integer>> ingredients;
        private final ChatColor nameColor;

        public CustomItem(Material baseMaterial, String displayName, List<String> lore, String id, 
                          List<Map.Entry<Material, Integer>> ingredients, ChatColor nameColor) {
            this.baseMaterial = baseMaterial;
            this.displayName = displayName;
            this.lore = lore;
            this.id = id;
            this.ingredients = ingredients;
            this.nameColor = nameColor;
        }

        public Material getBaseMaterial() {
            return baseMaterial;
        }

        public String getDisplayName() {
            return displayName;
        }

        public List<String> getLore() {
            return lore;
        }

        public String getId() {
            return id;
        }

        public List<Map.Entry<Material, Integer>> getIngredients() {
            return ingredients;
        }

        public ChatColor getNameColor() {
            return nameColor;
        }
    }

    public Recieps(Plugin plugin) {
        this.plugin = plugin;
        registerDefaultItems();
    }

    // Регистрация всех кастомных предметов
    private void registerDefaultItems() {
        // ГМРД
        registerCustomItem(
            new CustomItem(
                Material.CHEST,
                "ГМРД",
                Arrays.asList("Генератор Межпространственной Резонансной Декомпрессии"),
                "gmrd",
                Arrays.asList(
                    Map.entry(Material.IRON_INGOT, 8),
                    Map.entry(Material.REDSTONE, 1)
                ),
                ChatColor.GOLD
            )
        );

        // Крафтер
        registerCustomItem(
            new CustomItem(
                Material.DISPENSER,
                "Крафтер",
                Arrays.asList("Крутой крафтер"),
                "crafter",
                Arrays.asList(), // Нет ингредиентов для крафта (административный предмет)
                ChatColor.AQUA
            )
        );

        // Портальное топливо
        registerCustomItem(
            new CustomItem(
                Material.BLAZE_POWDER,
                "Портальное топливо",
                Arrays.asList("Используется для работы портала ГМРД"),
                "portal_fuel",
                Arrays.asList(),
                ChatColor.GOLD
            )
        );
        
        // Огненный Пыль
        registerCustomItem(
            new CustomItem(
                Material.BLAZE_POWDER,
                "Огненный Пыль",
                Arrays.asList(
                    ChatColor.GRAY + "Концентрированная энергия огня",
                    ChatColor.GRAY + "Используется для создания высокотемпературных сплавов"
                ),
                "fire_dust",
                Arrays.asList(
                    Map.entry(Material.COAL, 4),
                    Map.entry(Material.GUNPOWDER, 3),
                    Map.entry(Material.LAVA_BUCKET, 1)
                ),
                ChatColor.RED
            )
        );
        
        // Незерит
        registerCustomItem(
            new CustomItem(
                Material.NETHERITE_INGOT,
                "Незерит",
                Arrays.asList(
                    ChatColor.GRAY + "Прочный и термостойкий сплав",
                    ChatColor.GRAY + "Используется для изготовления компонентов ГМРД"
                ),
                "netherite",
                Arrays.asList(
                    Map.entry(Material.GOLD_INGOT, 3),
                    Map.entry(Material.IRON_INGOT, 2),
                    Map.entry(Material.COPPER_INGOT, 2)
                    // Огненный пыль добавляется отдельно
                ),
                ChatColor.DARK_GRAY
            )
        );
        
        // Светящийся Кристалл
        registerCustomItem(
            new CustomItem(
                Material.AMETHYST_SHARD,
                "Светящийся Кристалл",
                Arrays.asList(
                    ChatColor.GRAY + "Кристалл, наполненный светящейся энергией",
                    ChatColor.GRAY + "Компонент для создания Резонансного Катализатора"
                ),
                "glowing_crystal",
                Arrays.asList(
                    Map.entry(Material.AMETHYST_SHARD, 2),
                    Map.entry(Material.REDSTONE, 2),
                    Map.entry(Material.GLOWSTONE_DUST, 1)
                ),
                ChatColor.LIGHT_PURPLE
            )
        );
        
        // Углеродный Сплав
        registerCustomItem(
            new CustomItem(
                Material.COAL_BLOCK,
                "Углеродный Сплав",
                Arrays.asList(
                    ChatColor.GRAY + "Прочный многослойный материал",
                    ChatColor.GRAY + "Компонент для создания Резонансного Катализатора"
                ),
                "carbon_alloy",
                Arrays.asList(
                    Map.entry(Material.COAL_BLOCK, 1),
                    Map.entry(Material.IRON_INGOT, 1),
                    Map.entry(Material.COPPER_INGOT, 1)
                ),
                ChatColor.DARK_GRAY
            )
        );
        
        // Глубокосланцевый Каркас
        registerCustomItem(
            new CustomItem(
                Material.DEEPSLATE,
                "Глубокосланцевый Каркас",
                Arrays.asList(
                    ChatColor.GRAY + "Прочная конструкция из глубинного сланца",
                    ChatColor.GRAY + "Компонент для создания Резонансного Катализатора"
                ),
                "deepslate_frame",
                Arrays.asList(
                    Map.entry(Material.DEEPSLATE, 2),
                    Map.entry(Material.OBSIDIAN, 1),
                    Map.entry(Material.STONE_BRICKS, 2)
                ),
                ChatColor.GRAY
            )
        );
        
        // Тлеющее Ядро
        registerCustomItem(
            new CustomItem(
                Material.MAGMA_BLOCK,
                "Тлеющее Ядро",
                Arrays.asList(
                    ChatColor.GRAY + "Нестабильный энергетический источник",
                    ChatColor.GRAY + "Компонент для создания Резонансного Катализатора"
                ),
                "smoldering_core",
                Arrays.asList(
                    Map.entry(Material.LAVA_BUCKET, 1),
                    Map.entry(Material.ENDER_EYE, 1),
                    Map.entry(Material.GUNPOWDER, 2)
                ),
                ChatColor.GOLD
            )
        );
        
        // Резонансный Катализатор (топливо для ГМРД)
        registerCustomItem(
            new CustomItem(
                Material.NETHER_STAR,
                "Резонансный Катализатор",
                Arrays.asList(
                    ChatColor.GRAY + "Высокоэффективное топливо для ГМРД",
                    ChatColor.GRAY + "Обеспечивает стабильную работу портала"
                ),
                "resonance_catalyst",
                Arrays.asList(
                    Map.entry(Material.NETHER_STAR, 1) // Заглушка для отображения в интерфейсе
                ),
                ChatColor.AQUA
            )
        );

        // Квантовый Спектрометр
        registerCustomItem(
            new CustomItem(
                Material.QUARTZ,
                "Квантовый Спектрометр",
                Arrays.asList(
                    ChatColor.GRAY + "Часть стабилизатора, регулирующая спектральные параметры",
                    ChatColor.GRAY + "Компонент для создания масс-спектрального стабилизатора"
                ),
                "quantum_spectrometer",
                Arrays.asList(
                    Map.entry(Material.LAPIS_LAZULI, 4),
                    Map.entry(Material.ENDER_EYE, 1),
                    Map.entry(Material.IRON_INGOT, 3)
                ),
                ChatColor.BLUE
            )
        );
        
        // Эндриевый Конденсатор
        registerCustomItem(
            new CustomItem(
                Material.ENDER_PEARL,
                "Эндриевый Конденсатор",
                Arrays.asList(
                    ChatColor.GRAY + "Часть стабилизатора, регулирующая энергетический баланс",
                    ChatColor.GRAY + "Компонент для создания масс-спектрального стабилизатора"
                ),
                "ender_capacitor",
                Arrays.asList(
                    Map.entry(Material.ENDER_PEARL, 3),
                    Map.entry(Material.GOLD_BLOCK, 3),
                    Map.entry(Material.REDSTONE, 3)
                ),
                ChatColor.DARK_PURPLE
            )
        );
        
        // Аметистовый Контур
        registerCustomItem(
            new CustomItem(
                Material.AMETHYST_SHARD,
                "Аметистовый Контур",
                Arrays.asList(
                    ChatColor.GRAY + "Часть стабилизатора, стабилизирующая частоту",
                    ChatColor.GRAY + "Компонент для создания масс-спектрального стабилизатора"
                ),
                "amethyst_circuit",
                Arrays.asList(
                    Map.entry(Material.AMETHYST_SHARD, 4),
                    Map.entry(Material.AMETHYST_BLOCK, 2),
                    Map.entry(Material.COPPER_INGOT, 4)
                ),
                ChatColor.LIGHT_PURPLE
            )
        );
        
        // Редстоуновый Чип
        registerCustomItem(
            new CustomItem(
                Material.REDSTONE,
                "Редстоуновый Чип",
                Arrays.asList(
                    ChatColor.GRAY + "Часть стабилизатора, элемент управления",
                    ChatColor.GRAY + "Используется во многих высокотехнологичных устройствах"
                ),
                "redstone_chip",
                Arrays.asList(
                    Map.entry(Material.REDSTONE, 5),
                    Map.entry(Material.COPPER_INGOT, 1),
                    Map.entry(Material.GOLD_INGOT, 1),
                    Map.entry(Material.DIAMOND, 1),
                    Map.entry(Material.IRON_INGOT, 1)
                ),
                ChatColor.RED
            )
        );

        // Масс-спектральный стабилизатор
        registerCustomItem(
            new CustomItem(
                Material.END_CRYSTAL,
                "Масс-спектральный стабилизатор",
                Arrays.asList(
                    ChatColor.GRAY + "Регулирует частоту квантовых осцилляций обсидиана, сдерживает портал",
                    ChatColor.GRAY + "Компонент для создания ГМРД"
                ),
                "mass_spectral_stabilizer",
                Arrays.asList(
                    Map.entry(Material.END_CRYSTAL, 1), // Заглушка для отображения в интерфейсе
                    Map.entry(Material.ENDER_PEARL, 1)  // Настоящие ингредиенты задаются отдельно
                ), 
                ChatColor.AQUA
            )
        );
        
        // Нестабильный Незеритовый Сплав
        registerCustomItem(
            new CustomItem(
                Material.NETHERITE_INGOT,
                "Нестабильный Незеритовый Сплав",
                Arrays.asList(
                    ChatColor.GRAY + "Часть камеры, реагент для пространственного разрыва",
                    ChatColor.GRAY + "Компонент для создания Аннигиляционной Камеры"
                ),
                "unstable_netherite_alloy",
                Arrays.asList(
                    Map.entry(Material.NETHERITE_INGOT, 2),
                    Map.entry(Material.QUARTZ, 3),
                    Map.entry(Material.AMETHYST_SHARD, 2),
                    Map.entry(Material.ENDER_EYE, 1)
                ),
                ChatColor.DARK_RED
            )
        );
        
        // Генератор Античастиц
        registerCustomItem(
            new CustomItem(
                Material.CRYING_OBSIDIAN,
                "Генератор Античастиц",
                Arrays.asList(
                    ChatColor.GRAY + "Часть камеры, создает антиматерию",
                    ChatColor.GRAY + "Компонент для создания Аннигиляционной Камеры"
                ),
                "antiparticle_generator",
                Arrays.asList(
                    Map.entry(Material.OBSIDIAN, 3),
                    Map.entry(Material.LAPIS_BLOCK, 1)
                    // Редстоуновый Чип добавляется отдельно
                ),
                ChatColor.DARK_PURPLE
            )
        );
        
        // Кристалл Эндер-Пустоты
        registerCustomItem(
            new CustomItem(
                Material.END_CRYSTAL,
                "Кристалл Эндер-Пустоты",
                Arrays.asList(
                    ChatColor.GRAY + "Часть камеры, стабилизирует пустотные процессы",
                    ChatColor.GRAY + "Компонент для создания Аннигиляционной Камеры"
                ),
                "ender_void_crystal",
                Arrays.asList(
                    Map.entry(Material.ENDER_PEARL, 2),
                    Map.entry(Material.AMETHYST_SHARD, 3),
                    Map.entry(Material.REDSTONE, 3)
                ),
                ChatColor.DARK_AQUA
            )
        );
        
        // Обсидиановая Оболочка
        registerCustomItem(
            new CustomItem(
                Material.OBSIDIAN,
                "Обсидиановая Оболочка",
                Arrays.asList(
                    ChatColor.GRAY + "Часть камеры, структурная защита",
                    ChatColor.GRAY + "Компонент для создания Аннигиляционной Камеры"
                ),
                "obsidian_shell",
                Arrays.asList(
                    Map.entry(Material.OBSIDIAN, 4),
                    Map.entry(Material.NETHERITE_INGOT, 1),
                    Map.entry(Material.IRON_INGOT, 4)
                ),
                ChatColor.DARK_GRAY
            )
        );
        
        // Аннигиляционная Камера
        registerCustomItem(
            new CustomItem(
                Material.RESPAWN_ANCHOR,
                "Аннигиляционная Камера",
                Arrays.asList(
                    ChatColor.GRAY + "Контролируемый разрыв пространственной мембраны",
                    ChatColor.GRAY + "Компонент для создания ГМРД"
                ),
                "annihilation_chamber",
                Arrays.asList(
                    Map.entry(Material.RESPAWN_ANCHOR, 1) // Заглушка для отображения в интерфейсе
                ),
                ChatColor.DARK_RED
            )
        );
        
        // Гравитационное Ядро
        registerCustomItem(
            new CustomItem(
                Material.IRON_BLOCK,
                "Гравитационное Ядро",
                Arrays.asList(
                    ChatColor.GRAY + "Часть компенсатора, регулирует гравитационное поле",
                    ChatColor.GRAY + "Компонент для создания Гравитационного компенсатора"
                ),
                "gravitational_core",
                Arrays.asList(
                    Map.entry(Material.IRON_BLOCK, 3),
                    Map.entry(Material.COPPER_BLOCK, 2),
                    Map.entry(Material.NETHERITE_INGOT, 1),
                    Map.entry(Material.REDSTONE, 3)
                ),
                ChatColor.GRAY
            )
        );
        
        // Резонатор Пространства
        registerCustomItem(
            new CustomItem(
                Material.AMETHYST_BLOCK,
                "Резонатор Пространства",
                Arrays.asList(
                    ChatColor.GRAY + "Часть компенсатора, стабилизирует пространственные колебания",
                    ChatColor.GRAY + "Компонент для создания Гравитационного компенсатора"
                ),
                "space_resonator",
                Arrays.asList(
                    Map.entry(Material.AMETHYST_BLOCK, 3),
                    Map.entry(Material.CRYING_OBSIDIAN, 3)
                ),
                ChatColor.LIGHT_PURPLE
            )
        );
        
        // Обсидиановая Рамка
        registerCustomItem(
            new CustomItem(
                Material.OBSIDIAN,
                "Обсидиановая Рамка",
                Arrays.asList(
                    ChatColor.GRAY + "Часть компенсатора, фиксирует структуру портала",
                    ChatColor.GRAY + "Компонент для создания Гравитационного компенсатора"
                ),
                "obsidian_frame",
                Arrays.asList(
                    Map.entry(Material.OBSIDIAN, 6),
                    Map.entry(Material.IRON_INGOT, 3)
                ),
                ChatColor.DARK_GRAY
            )
        );
        
        // Гравитационный компенсатор
        registerCustomItem(
            new CustomItem(
                Material.LODESTONE,
                "Гравитационный компенсатор",
                Arrays.asList(
                    ChatColor.GRAY + "Поддерживает стабильность межпространственного тоннеля",
                    ChatColor.GRAY + "Компонент для создания ГМРД"
                ),
                "gravity_compensator",
                Arrays.asList(
                    Map.entry(Material.LODESTONE, 1) // Заглушка для отображения в интерфейсе
                ),
                ChatColor.YELLOW
            )
        );
        
        // Кварцевый Экран
        registerCustomItem(
            new CustomItem(
                Material.QUARTZ_BLOCK,
                "Кварцевый Экран",
                Arrays.asList(
                    ChatColor.GRAY + "Часть интерфейса, визуальный дисплей",
                    ChatColor.GRAY + "Компонент для создания Контрольного интерфейса"
                ),
                "quartz_display",
                Arrays.asList(
                    Map.entry(Material.QUARTZ, 3),
                    Map.entry(Material.GLASS, 1),
                    Map.entry(Material.REDSTONE, 3),
                    Map.entry(Material.IRON_INGOT, 1),
                    Map.entry(Material.GOLD_INGOT, 1)
                ),
                ChatColor.WHITE
            )
        );
        
        // Контрольный интерфейс
        registerCustomItem(
            new CustomItem(
                Material.DAYLIGHT_DETECTOR,
                "Контрольный интерфейс",
                Arrays.asList(
                    ChatColor.GRAY + "Позволяет управлять параметрами портала",
                    ChatColor.GRAY + "Компонент для создания ГМРД"
                ),
                "control_interface",
                Arrays.asList(
                    Map.entry(Material.DAYLIGHT_DETECTOR, 1) // Заглушка для отображения в интерфейсе
                ),
                ChatColor.AQUA
            )
        );
        
        // Квантовый Пьезоэлемент
        registerCustomItem(
            new CustomItem(
                Material.QUARTZ_BLOCK,
                "Квантовый Пьезоэлемент",
                Arrays.asList(
                    ChatColor.GRAY + "Часть генератора, создает квантовые колебания",
                    ChatColor.GRAY + "Компонент для создания Генератора Плазменного Резонанса"
                ),
                "quantum_piezo",
                Arrays.asList(
                    Map.entry(Material.QUARTZ_BLOCK, 3),
                    Map.entry(Material.BLAZE_POWDER, 2),
                    Map.entry(Material.REDSTONE, 2),
                    Map.entry(Material.LAPIS_LAZULI, 2)
                ),
                ChatColor.WHITE
            )
        );
        
        // Сверхпроводящий Магнит
        registerCustomItem(
            new CustomItem(
                Material.NETHERITE_BLOCK,
                "Сверхпроводящий Магнит",
                Arrays.asList(
                    ChatColor.GRAY + "Часть генератора, создает мощное магнитное поле",
                    ChatColor.GRAY + "Компонент для создания Генератора Плазменного Резонанса"
                ),
                "superconducting_magnet",
                Arrays.asList(
                    Map.entry(Material.NETHERITE_INGOT, 2),
                    Map.entry(Material.IRON_BLOCK, 3),
                    Map.entry(Material.PACKED_ICE, 3)
                ),
                ChatColor.DARK_GRAY
            )
        );
        
        // Кристалл Излучения
        registerCustomItem(
            new CustomItem(
                Material.AMETHYST_SHARD,
                "Кристалл Излучения",
                Arrays.asList(
                    ChatColor.GRAY + "Часть генератора, концентрирует энергию",
                    ChatColor.GRAY + "Компонент для создания Генератора Плазменного Резонанса"
                ),
                "radiation_crystal",
                Arrays.asList(
                    Map.entry(Material.AMETHYST_SHARD, 3),
                    Map.entry(Material.GLOWSTONE, 2),
                    Map.entry(Material.REDSTONE, 1),
                    Map.entry(Material.ENDER_PEARL, 3)
                ),
                ChatColor.LIGHT_PURPLE
            )
        );
        
        // Медный Катализатор
        registerCustomItem(
            new CustomItem(
                Material.COPPER_INGOT,
                "Медный Катализатор",
                Arrays.asList(
                    ChatColor.GRAY + "Часть генератора, усиливает реакцию",
                    ChatColor.GRAY + "Компонент для создания Генератора Плазменного Резонанса"
                ),
                "copper_catalyst",
                Arrays.asList(
                    Map.entry(Material.COPPER_INGOT, 5),
                    Map.entry(Material.BLAZE_POWDER, 2),
                    Map.entry(Material.LAPIS_LAZULI, 2)
                ),
                ChatColor.GOLD
            )
        );
        
        // Обсидиановый Резонатор
        registerCustomItem(
            new CustomItem(
                Material.OBSIDIAN,
                "Обсидиановый Резонатор",
                Arrays.asList(
                    ChatColor.GRAY + "Часть генератора, стабилизирует резонанс",
                    ChatColor.GRAY + "Компонент для создания Генератора Плазменного Резонанса"
                ),
                "obsidian_resonator",
                Arrays.asList(
                    Map.entry(Material.OBSIDIAN, 4),
                    Map.entry(Material.DIAMOND_BLOCK, 1),
                    Map.entry(Material.AMETHYST_SHARD, 4)
                ),
                ChatColor.DARK_PURPLE
            )
        );
        
        // Генератор Плазменного Резонанса
        registerCustomItem(
            new CustomItem(
                Material.BEACON,
                "Генератор Плазменного Резонанса",
                Arrays.asList(
                    ChatColor.GRAY + "Создает электромагнитные волны для возбуждения плазмы",
                    ChatColor.GRAY + "Компонент для создания ГМРД"
                ),
                "plasma_resonance_generator",
                Arrays.asList(
                    Map.entry(Material.BEACON, 1) // Заглушка для отображения в интерфейсе
                ),
                ChatColor.BLUE
            )
        );
    }

    // Регистрация кастомного предмета
    public void registerCustomItem(CustomItem item) {
        customItems.put(item.getId(), item);
    }

    // Получение списка всех ID зарегистрированных кастомных предметов
    public Set<String> getRegisteredItemIds() {
        return customItems.keySet();
    }

    // Получение информации о кастомном предмете
    public CustomItem getCustomItemInfo(String id) {
        return customItems.get(id);
    }

    // Проверка кастомного предмета по ID
    public boolean isCustomItem(ItemStack item, String id) {
        if (item == null || item.getItemMeta() == null) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        NamespacedKey key = new NamespacedKey(plugin, id);
        
        // Особое поведение для portal_fuel, который заменен на resonance_catalyst
        if (id.equals("portal_fuel")) {
            NamespacedKey resKey = new NamespacedKey(plugin, "resonance_catalyst");
            return data.has(key, PersistentDataType.STRING) || data.has(resKey, PersistentDataType.STRING);
        }
        
        return data.has(key, PersistentDataType.STRING);
    }

    // Проверка любого кастомного предмета
    public boolean isAnyCustomItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        
        for (String id : customItems.keySet()) {
            NamespacedKey key = new NamespacedKey(plugin, id);
            if (data.has(key, PersistentDataType.STRING)) {
                return true;
            }
        }
        return false;
    }

    // Получение ID кастомного предмета, если это кастомный предмет
    public String getCustomItemId(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return null;
        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer data = meta.getPersistentDataContainer();
        
        for (String id : customItems.keySet()) {
            NamespacedKey key = new NamespacedKey(plugin, id);
            if (data.has(key, PersistentDataType.STRING)) {
                return id;
            }
        }
        return null;
    }

    // Создание предмета по ID
    public ItemStack createCustomItem(String id) {
        CustomItem itemInfo = customItems.get(id);
        if (itemInfo == null) {
            Bukkit.getLogger().warning("Попытка создать несуществующий кастомный предмет: " + id);
            return null;
        }

        ItemStack item = new ItemStack(itemInfo.getBaseMaterial());
        ItemMeta meta = item.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(itemInfo.getNameColor() + itemInfo.getDisplayName());
            meta.setLore(itemInfo.getLore());
            
            NamespacedKey key = new NamespacedKey(plugin, itemInfo.getId());
            meta.getPersistentDataContainer().set(key, PersistentDataType.STRING, "true");
            
            item.setItemMeta(meta);
        }
        
        return item;
    }

    // Обратная совместимость с старыми методами
    public ItemStack createGMRD() {
        return createCustomItem("gmrd");
    }

    public boolean isGMRD(ItemStack item) {
        return isCustomItem(item, "gmrd");
    }

    public ItemStack createDispenserCrafting() {
        return createCustomItem("crafter");
    }

    public ItemStack createPortalFuel() {
        return createCustomItem("resonance_catalyst");
    }

    // Методы для создания новых компонентов
    public ItemStack createFireDust() {
        return createCustomItem("fire_dust");
    }
    
    public ItemStack createNetherite() {
        return createCustomItem("netherite");
    }
    
    public ItemStack createGlowingCrystal() {
        return createCustomItem("glowing_crystal");
    }
    
    public ItemStack createCarbonAlloy() {
        return createCustomItem("carbon_alloy");
    }
    
    public ItemStack createDeepslateFrame() {
        return createCustomItem("deepslate_frame");
    }
    
    public ItemStack createSmolderingCore() {
        return createCustomItem("smoldering_core");
    }
    
    public ItemStack createResonanceCatalyst() {
        return createCustomItem("resonance_catalyst");
    }

    public ItemStack createQuantumSpectrometer() {
        return createCustomItem("quantum_spectrometer");
    }
    
    public ItemStack createEnderCapacitor() {
        return createCustomItem("ender_capacitor");
    }
    
    public ItemStack createAmethystCircuit() {
        return createCustomItem("amethyst_circuit");
    }
    
    public ItemStack createRedstoneChip() {
        return createCustomItem("redstone_chip");
    }
    
    public ItemStack createMassSpectralStabilizer() {
        return createCustomItem("mass_spectral_stabilizer");
    }
    
    public ItemStack createUnstableNetheriteAlloy() {
        return createCustomItem("unstable_netherite_alloy");
    }
    
    public ItemStack createAntiparticleGenerator() {
        return createCustomItem("antiparticle_generator");
    }
    
    public ItemStack createEnderVoidCrystal() {
        return createCustomItem("ender_void_crystal");
    }
    
    public ItemStack createObsidianShell() {
        return createCustomItem("obsidian_shell");
    }
    
    public ItemStack createAnnihilationChamber() {
        return createCustomItem("annihilation_chamber");
    }
    
    public ItemStack createGravitationalCore() {
        return createCustomItem("gravitational_core");
    }
    
    public ItemStack createSpaceResonator() {
        return createCustomItem("space_resonator");
    }
    
    public ItemStack createObsidianFrame() {
        return createCustomItem("obsidian_frame");
    }
    
    public ItemStack createGravityCompensator() {
        return createCustomItem("gravity_compensator");
    }
    
    public ItemStack createQuartzDisplay() {
        return createCustomItem("quartz_display");
    }
    
    public ItemStack createControlInterface() {
        return createCustomItem("control_interface");
    }
    
    public ItemStack createQuantumPiezo() {
        return createCustomItem("quantum_piezo");
    }
    
    public ItemStack createSuperconductingMagnet() {
        return createCustomItem("superconducting_magnet");
    }
    
    public ItemStack createRadiationCrystal() {
        return createCustomItem("radiation_crystal");
    }
    
    public ItemStack createCopperCatalyst() {
        return createCustomItem("copper_catalyst");
    }
    
    public ItemStack createObsidianResonator() {
        return createCustomItem("obsidian_resonator");
    }
    
    public ItemStack createPlasmaResonanceGenerator() {
        return createCustomItem("plasma_resonance_generator");
    }

    // Получение ингредиентов для кастомного предмета
    public Map<Material, Integer> getIngredientsForItem(String id) {
        CustomItem itemInfo = customItems.get(id);
        if (itemInfo == null) return new HashMap<>();
        
        Map<Material, Integer> ingredients = new HashMap<>();
        for (Map.Entry<Material, Integer> entry : itemInfo.getIngredients()) {
            ingredients.put(entry.getKey(), entry.getValue());
        }
        
        return ingredients;
    }

    // Проверка рецепта для кастомного предмета
    public boolean checkRecipe(String itemId, Map<Material, Integer> availableMaterials) {
        Map<Material, Integer> required = getIngredientsForItem(itemId);
        
        // Для предметов со сложным крафтом используем специальную логику
        if (itemId.equals("mass_spectral_stabilizer")) {
            // Проверяем наличие всех компонентов для масс-спектрального стабилизатора
            Map<String, Integer> customRequired = new HashMap<>();
            customRequired.put("quantum_spectrometer", 1);
            customRequired.put("ender_capacitor", 2);
            customRequired.put("amethyst_circuit", 1);
            customRequired.put("redstone_chip", 2);
            
            return checkComplexRecipe(customRequired, availableMaterials);
        } else if (itemId.equals("annihilation_chamber")) {
            // Проверяем наличие всех компонентов для аннигиляционной камеры
            Map<String, Integer> customRequired = new HashMap<>();
            customRequired.put("unstable_netherite_alloy", 2);
            customRequired.put("antiparticle_generator", 1);
            customRequired.put("ender_void_crystal", 1);
            customRequired.put("obsidian_shell", 1);
            
            return checkComplexRecipe(customRequired, availableMaterials);
        } else if (itemId.equals("gravity_compensator")) {
            // Проверяем наличие всех компонентов для гравитационного компенсатора
            Map<String, Integer> customRequired = new HashMap<>();
            customRequired.put("gravitational_core", 1);
            customRequired.put("space_resonator", 2);
            customRequired.put("obsidian_frame", 1);
            
            return checkComplexRecipe(customRequired, availableMaterials);
        } else if (itemId.equals("control_interface")) {
            // Проверяем наличие всех компонентов для контрольного интерфейса
            Map<String, Integer> customRequired = new HashMap<>();
            customRequired.put("redstone_chip", 7);
            customRequired.put("quartz_display", 1);
            customRequired.put("ender_pearl", 1);
            
            return checkComplexRecipe(customRequired, availableMaterials);
        } else if (itemId.equals("plasma_resonance_generator")) {
            // Проверяем наличие всех компонентов для генератора плазменного резонанса
            Map<String, Integer> customRequired = new HashMap<>();
            customRequired.put("quantum_piezo", 2);
            customRequired.put("superconducting_magnet", 1);
            customRequired.put("radiation_crystal", 1);
            customRequired.put("copper_catalyst", 2);
            customRequired.put("obsidian_resonator", 1);
            
            return checkComplexRecipe(customRequired, availableMaterials);
        } else if (itemId.equals("netherite")) {
            // Проверяем наличие всех компонентов для незерита
            Map<String, Integer> customRequired = new HashMap<>();
            customRequired.put("fire_dust", 2);
            
            Map<Material, Integer> materialRequired = new HashMap<>();
            materialRequired.put(Material.GOLD_INGOT, 3);
            materialRequired.put(Material.IRON_INGOT, 2);
            materialRequired.put(Material.COPPER_INGOT, 2);
            
            return checkMixedRecipe(customRequired, materialRequired, availableMaterials);
        } else if (itemId.equals("resonance_catalyst")) {
            // Проверяем наличие всех компонентов для резонансного катализатора
            Map<String, Integer> customRequired = new HashMap<>();
            customRequired.put("glowing_crystal", 2);
            customRequired.put("carbon_alloy", 3);
            customRequired.put("deepslate_frame", 1);
            customRequired.put("smoldering_core", 1);
            
            return checkComplexRecipe(customRequired, availableMaterials);
        } else if (required.isEmpty()) {
            return false;
        }

        // Проверка точного соответствия необходимых материалов для стандартных предметов
        for (Map.Entry<Material, Integer> entry : required.entrySet()) {
            Material mat = entry.getKey();
            int requiredAmount = entry.getValue();
            int foundAmount = availableMaterials.getOrDefault(mat, 0);

            if (foundAmount < requiredAmount) {
                return false;
            }
        }

        // Проверка отсутствия лишних материалов
        if (availableMaterials.size() != required.size()) {
            return false;
        }

        return true;
    }
    
    // Проверка смешанных рецептов (кастомные предметы + обычные материалы)
    private boolean checkMixedRecipe(Map<String, Integer> customRequired, Map<Material, Integer> materialRequired, 
                                   Map<Material, Integer> availableMaterials) {
        Map<String, Integer> foundCustomItems = new HashMap<>();
        Map<Material, Integer> remainingMaterials = new HashMap<>(availableMaterials);
        
        // Ищем кастомные предметы в availableMaterials и удаляем их из remainingMaterials
        for (Map.Entry<Material, Integer> entry : availableMaterials.entrySet()) {
            Material material = entry.getKey();
            
            // Создаем временный ItemStack для проверки
            ItemStack tempItem = new ItemStack(material);
            String customId = getCustomItemId(tempItem);
            
            if (customId != null && customRequired.containsKey(customId)) {
                foundCustomItems.put(customId, entry.getValue());
                remainingMaterials.remove(material);
            }
        }
        
        // Проверяем наличие всех нужных кастомных предметов
        for (Map.Entry<String, Integer> entry : customRequired.entrySet()) {
            String customId = entry.getKey();
            int requiredAmount = entry.getValue();
            int foundAmount = foundCustomItems.getOrDefault(customId, 0);
            
            if (foundAmount < requiredAmount) {
                return false;
            }
        }
        
        // Проверяем наличие всех обычных материалов
        for (Map.Entry<Material, Integer> entry : materialRequired.entrySet()) {
            Material material = entry.getKey();
            int requiredAmount = entry.getValue();
            int availableAmount = remainingMaterials.getOrDefault(material, 0);
            
            if (availableAmount < requiredAmount) {
                return false;
            }
            
            // Удаляем материал из оставшихся
            if (availableAmount == requiredAmount) {
                remainingMaterials.remove(material);
            } else {
                remainingMaterials.put(material, availableAmount - requiredAmount);
            }
        }
        
        // Проверяем что нет лишних материалов
        return remainingMaterials.isEmpty();
    }
    
    // Метод для проверки сложных рецептов с кастомными предметами
    private boolean checkComplexRecipe(Map<String, Integer> customRequired, Map<Material, Integer> availableMaterials) {
        Map<String, Integer> foundCustomItems = new HashMap<>();
        Map<Material, Integer> remainingMaterials = new HashMap<>(availableMaterials);
        
        // Ищем кастомные предметы в availableMaterials и удаляем их из remainingMaterials
        for (Map.Entry<Material, Integer> entry : availableMaterials.entrySet()) {
            Material material = entry.getKey();
            
            // Создаем временный ItemStack для проверки
            ItemStack tempItem = new ItemStack(material);
            String customId = getCustomItemId(tempItem);
            
            if (customId != null && customRequired.containsKey(customId)) {
                foundCustomItems.put(customId, entry.getValue());
                remainingMaterials.remove(material);
            }
        }
        
        // Проверяем наличие всех нужных кастомных предметов
        for (Map.Entry<String, Integer> entry : customRequired.entrySet()) {
            String customId = entry.getKey();
            int requiredAmount = entry.getValue();
            
            // Если это не кастомный предмет, а обычный материал
            if (customId.equals("ender_pearl") || customId.equals("crying_obsidian") || 
                customId.equals("lapis_block")) {
                Material material = Material.valueOf(customId.toUpperCase());
                int foundAmount = availableMaterials.getOrDefault(material, 0);
                
                if (foundAmount < requiredAmount) {
                    return false;
                }
                
                continue;
            }
            
            int foundAmount = foundCustomItems.getOrDefault(customId, 0);
            
            if (foundAmount < requiredAmount) {
                return false;
            }
        }
        
        // Проверяем что нет лишних материалов
        return remainingMaterials.isEmpty() || 
               (customRequired.containsKey("ender_pearl") && remainingMaterials.size() == 1 && 
                remainingMaterials.containsKey(Material.ENDER_PEARL)) ||
               (customRequired.containsKey("crying_obsidian") && remainingMaterials.size() == 1 && 
                remainingMaterials.containsKey(Material.CRYING_OBSIDIAN)) ||
               (customRequired.containsKey("lapis_block") && remainingMaterials.size() == 1 && 
                remainingMaterials.containsKey(Material.LAPIS_BLOCK));
    }
}
