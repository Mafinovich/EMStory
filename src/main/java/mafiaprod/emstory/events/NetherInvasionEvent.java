package mafiaprod.emstory.events;

import mafiaprod.emstory.EMStory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.Sound;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * Менеджер событий "Нашествие" из Ада
 * Отвечает за создание разломов по миру после первого открытия портала
 * и за запуск нашествий адских мобов через разломы
 */
public class NetherInvasionEvent implements Listener {
    private final EMStory plugin;
    private final Random random = new Random();
    private final List<Location> fractures = new ArrayList<>();
    private final Map<UUID, BukkitTask> activeInvasions = new HashMap<>();
    private final File fracturesFile;
    private final int worldBorder = 4000; // Размер мира 4000x4000
    private boolean portalOpened = false;
    
    // Мобы для нашествия
    private final List<EntityType> netherMobs = Arrays.asList(
            EntityType.GHAST, EntityType.PIGLIN, EntityType.ZOMBIFIED_PIGLIN,
            EntityType.BLAZE, EntityType.MAGMA_CUBE, EntityType.HOGLIN,
            EntityType.WITHER_SKELETON, EntityType.STRIDER
    );
    
    // Биомы, в которых не должны появляться разломы
    private final List<Biome> excludedBiomes = Arrays.asList(
            Biome.SWAMP, Biome.MANGROVE_SWAMP
    );
    
    /**
     * Конструктор менеджера нашествий
     * @param plugin Основной класс плагина
     */
    public NetherInvasionEvent(EMStory plugin) {
        this.plugin = plugin;
        this.fracturesFile = new File(plugin.getDataFolder(), "fractures.yml");
        loadFractures();
    }
    
    /**
     * Обработчик события создания портала
     * При первом создании портала в Ад запускает создание разломов по миру
     */
    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (portalOpened || event.isCancelled()) {
            return;
        }
        
        // Проверяем, что это портал в Ад (в мире обычно это связано с направлением портала)
        if (event.getReason() == PortalCreateEvent.CreateReason.FIRE) {
            // Ждем пару секунд после создания портала, чтобы игрок успел увидеть его
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                Location portalLocation = event.getBlocks().get(0).getLocation();
                World world = portalLocation.getWorld();
                
                if (world != null && world.getEnvironment() == World.Environment.NORMAL) {
                    // Отмечаем, что портал уже открыт
                    portalOpened = true;
                    
                    // Сохраняем информацию о том, что портал открыт
                    savePortalState();
                    
                    // Запускаем создание разломов
                    createFractures();
                    
                    // Создаем первое нашествие у портала
                    spawnPortalInvasion(portalLocation);
                }
            }, 40L); // Задержка в 2 секунды (40 тиков)
        }
    }
    
    /**
     * Создает разломы в случайных местах мира
     */
    private void createFractures() {
        plugin.getLogger().info("Создание разломов после открытия портала в Ад...");
        
        // Оповещаем всех игроков о начале события
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
        }
        
        // Создаем 20-30 разломов в случайных местах мира
        int count = 20 + random.nextInt(11);
        World overworld = Bukkit.getWorlds().get(0); // Обычно это основной мир
        
        new BukkitRunnable() {
            int created = 0;
            
            @Override
            public void run() {
                if (created >= count) {
                    plugin.getLogger().info("Создано " + fractures.size() + " разломов");
                    saveFractures();
                    
                    cancel();
                    return;
                }
                
                // Генерируем случайную позицию в мире
                int x = random.nextInt(worldBorder * 2) - worldBorder;
                int z = random.nextInt(worldBorder * 2) - worldBorder;
                
                // Проверяем загрузку чанка
                int chunkX = x >> 4;
                int chunkZ = z >> 4;
                
                if (!overworld.isChunkLoaded(chunkX, chunkZ)) {
                    overworld.loadChunk(chunkX, chunkZ, true);
                }
                
                // Находим самый высокий блок на этой позиции
                int y = overworld.getHighestBlockYAt(x, z);
                Location location = new Location(overworld, x, y, z);
                Block block = location.getBlock();
                
                // Проверяем, не исключенный ли это биом
                if (excludedBiomes.contains(block.getBiome())) {
                    return; // Пропускаем этот биом, попробуем в следующий раз
                }
                
                // Создаем разлом
                createFracture(location);
                fractures.add(location);
                created++;
                
                // Логируем создание разлома
                plugin.getLogger().info("Создан разлом #" + created + " на координатах: " + 
                        location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ());
                
                // Уведомляем ближайших игроков
                for (Player player : overworld.getPlayers()) {
                    if (player.getLocation().distance(location) < 200) {
                        player.playSound(player.getLocation(), Sound.AMBIENT_CAVE, 1.0f, 0.5f);
                    }
                }
            }
        }.runTaskTimer(plugin, 20L, 60L); // Каждые 3 секунды создаем новый разлом
    }
    
    /**
     * Создает разлом на указанной локации
     * @param location Локация для создания разлома
     */
    private void createFracture(Location location) {
        World world = location.getWorld();
        if (world == null) return;
        
        // Создаем случайный шаблон разлома
        int width = 2 + random.nextInt(4); // Ширина 2-5
        int height = 2 + random.nextInt(4); // Высота 2-5
        int depth = 1 + random.nextInt(3);  // Глубина 1-3
        
        // Генерируем шаблон разлома с вероятностью для каждого блока
        for (int x = -width/2; x <= width/2; x++) {
            for (int y = -height/2; y <= height/2; y++) {
                for (int z = -depth/2; z <= depth/2; z++) {
                    if (random.nextDouble() < 0.4) { // 40% шанс для каждого блока
                        Block block = world.getBlockAt(
                                location.getBlockX() + x, 
                                location.getBlockY() + y, 
                                location.getBlockZ() + z
                        );
                        
                        // Проверяем, что блок можно заменить (не бедрок и т.п.)
                        if (!block.getType().isSolid() || block.getType() == Material.STONE || 
                                block.getType() == Material.DIRT || block.getType() == Material.GRASS_BLOCK) {
                            block.setType(Material.NETHER_PORTAL);
                        }
                    }
                }
            }
        }
        
        // Создаем эффект при появлении разлома
        world.spawnParticle(Particle.FLAME, location, 30, 2, 2, 2, 0.1);
        world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 2.0f, 0.5f);
        world.playSound(location, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.5f);
    }
    
    /**
     * Запускает нашествие адских мобов в локации первого портала
     * @param location Локация портала
     */
    private void spawnPortalInvasion(Location location) {
        plugin.getLogger().info("Запуск вторжения адских мобов у первого портала...");
        
        // Оповещаем всех игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                    ChatColor.DARK_RED + "Вторжение началось!",
                    ChatColor.RED + "Адские создания проникают в наш мир",
                    20, 60, 20
            );
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0f, 0.7f);
        }
        
        // Создаем волны мобов
        new BukkitRunnable() {
            int wave = 0;
            final int totalWaves = 5;
            
            @Override
            public void run() {
                if (wave >= totalWaves) {
                    cancel();
                    return;
                }
                
                // Увеличиваем количество мобов с каждой волной
                int mobCount = 5 + wave * 3;
                
                for (int i = 0; i < mobCount; i++) {
                    // Определяем случайное место рядом с порталом
                    double offsetX = (random.nextDouble() - 0.5) * 10;
                    double offsetZ = (random.nextDouble() - 0.5) * 10;
                    Location spawnLoc = location.clone().add(offsetX, 1, offsetZ);
                    
                    // Выбираем случайного моба
                    EntityType mobType = netherMobs.get(random.nextInt(netherMobs.size()));
                    
                    // Спавним моба
                    Entity entity = location.getWorld().spawnEntity(spawnLoc, mobType);
                    
                    // Делаем мобов агрессивными и сильнее
                    if (entity instanceof LivingEntity) {
                        LivingEntity mob = (LivingEntity) entity;
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.STRENGTH, 99999, 1));
                        mob.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 99999, 0));
                        
                        // Даем мобам уникальное имя для этого события
                        if (mob instanceof Monster) {
                            mob.setCustomName(ChatColor.RED + "Адский " + mobType.name().toLowerCase());
                            mob.setCustomNameVisible(true);
                        }
                    }
                }
                
                // Воспроизводим эффекты
                location.getWorld().spawnParticle(Particle.PORTAL, location, 50, 3, 3, 3, 0.1);
                location.getWorld().playSound(location, Sound.ENTITY_GHAST_SCREAM, 1.0f, 0.5f);
                
                wave++;
            }
        }.runTaskTimer(plugin, 20L, 300L); // Первая волна через 1 секунду, потом каждые 15 секунд
    }
    
    /**
     * Запускает нашествие адских мобов из указанного разлома
     * @param fractureIndex Индекс разлома в списке
     * @param intensity Интенсивность нашествия (1-5)
     * @return true, если нашествие успешно запущено
     */
    public boolean startInvasion(int fractureIndex, int intensity) {
        if (fractureIndex < 0 || fractureIndex >= fractures.size()) {
            return false;
        }
        
        // Получаем локацию разлома
        Location location = fractures.get(fractureIndex);
        World world = location.getWorld();
        
        if (world == null) {
            return false;
        }
        
        // Ограничиваем интенсивность от 1 до 5
        final int invasionIntensity = Math.max(1, Math.min(5, intensity));
        
        // Создаем уникальный идентификатор для этого нашествия
        final UUID invasionId = UUID.randomUUID();
        
        // Логируем начало нашествия
        plugin.getLogger().info("Запуск нашествия #" + invasionId + " из разлома #" + fractureIndex + 
                " с интенсивностью " + invasionIntensity);
        
        // Оповещаем всех игроков
        for (Player player : world.getPlayers()) {
            if (player.getLocation().distance(location) < 200) {
                player.sendMessage(ChatColor.DARK_RED + "[ТРЕВОГА] " + 
                        ChatColor.RED + "Разлом активирован! Началось вторжение адских созданий!");
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            }
        }
        
        // Запускаем волны мобов
        BukkitTask task = new BukkitRunnable() {
            int wave = 0;
            final int totalWaves = 3 + invasionIntensity * 2; // От 5 до 13 волн
            
            @Override
            public void run() {
                // Проверяем, что мир существует
                if (location.getWorld() == null) {
                    cancel();
                    activeInvasions.remove(invasionId);
                    return;
                }
                
                if (wave >= totalWaves) {
                    // Нашествие закончилось
                    plugin.getLogger().info("Нашествие #" + invasionId + " завершено");
                    
                    // Оповещаем ближайших игроков
                    for (Player player : world.getPlayers()) {
                        if (player.getLocation().distance(location) < 200) {
                            player.sendMessage(ChatColor.GREEN + "[Статус] " + 
                                    ChatColor.YELLOW + "Вторжение из разлома остановлено.");
                            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
                        }
                    }
                    
                    cancel();
                    activeInvasions.remove(invasionId);
                    return;
                }
                
                // Расчет количества мобов на основе интенсивности и текущей волны
                int mobCount = 3 + invasionIntensity + (wave / 2);
                
                for (int i = 0; i < mobCount; i++) {
                    // Определяем случайное место рядом с разломом
                    double radius = 3 + random.nextDouble() * 7; // Радиус спауна 3-10 блоков
                    double angle = random.nextDouble() * 2 * Math.PI;
                    double offsetX = Math.cos(angle) * radius;
                    double offsetZ = Math.sin(angle) * radius;
                    
                    Location spawnLoc = location.clone().add(offsetX, 1, offsetZ);
                    
                    // Проверяем, загружен ли чанк
                    if (!spawnLoc.getChunk().isLoaded()) {
                        continue;
                    }
                    
                    // Выбираем случайного моба с учетом интенсивности
                    // Более сильные мобы на высокой интенсивности
                    EntityType mobType;
                    double chance = random.nextDouble();
                    
                    if (invasionIntensity >= 4 && chance < 0.2) {
                        // 20% шанс на сильных мобов при высокой интенсивности
                        mobType = EntityType.GHAST;
                    } else if (invasionIntensity >= 3 && chance < 0.3) {
                        // 30% шанс на средних мобов
                        mobType = random.nextBoolean() ? EntityType.BLAZE : EntityType.WITHER_SKELETON;
                    } else {
                        // Обычные мобы
                        mobType = netherMobs.get(random.nextInt(netherMobs.size()));
                    }
                    
                    // Спавним моба
                    Entity entity = world.spawnEntity(spawnLoc, mobType);
                    
                    // Делаем мобов сильнее в зависимости от интенсивности
                    if (entity instanceof LivingEntity) {
                        LivingEntity mob = (LivingEntity) entity;
                        
                        // Увеличиваем силу на основе интенсивности
                        mob.addPotionEffect(new PotionEffect(
                            PotionEffectType.STRENGTH, 
                            99999, 
                            Math.max(0, invasionIntensity - 2)
                        ));
                        
                        // Увеличиваем скорость
                        if (invasionIntensity > 2) {
                            mob.addPotionEffect(new PotionEffect(
                                PotionEffectType.SPEED, 
                                99999, 
                                Math.max(0, invasionIntensity - 3)
                            ));
                        }
                        
                        // Делаем мобов огнестойкими
                        if (invasionIntensity >= 4) {
                            mob.addPotionEffect(new PotionEffect(
                                PotionEffectType.FIRE_RESISTANCE, 
                                99999, 
                                0
                            ));
                        }
                        
                        // Даем мобам уникальное имя для этого события
                        if (mob instanceof Monster) {
                            String prefix;
                            if (invasionIntensity >= 5) {
                                prefix = ChatColor.DARK_RED + "Элитный адский ";
                            } else if (invasionIntensity >= 3) {
                                prefix = ChatColor.RED + "Опасный адский ";
                            } else {
                                prefix = ChatColor.GOLD + "Адский ";
                            }
                            
                            mob.setCustomName(prefix + mobType.name().toLowerCase().replace("_", " "));
                            mob.setCustomNameVisible(true);
                        }
                    }
                }
                
                // Воспроизводим эффекты
                world.spawnParticle(Particle.PORTAL, location, 30, 3, 3, 3, 0.1);
                world.playSound(location, Sound.ENTITY_GHAST_SHOOT, 1.0f, 0.5f);
                
                // Оповещаем ближайших игроков о новой волне
                for (Player player : world.getPlayers()) {
                    if (player.getLocation().distance(location) < 100) {
                        player.sendMessage(ChatColor.RED + "[Волна " + (wave + 1) + "/" + totalWaves + "] " + 
                                ChatColor.GOLD + "Новая волна адских созданий появляется из разлома!");
                    }
                }
                
                wave++;
            }
        }.runTaskTimer(plugin, 20L, 20L * (10 - invasionIntensity)); // Чем выше интенсивность, тем чаще волны
        
        // Сохраняем задачу
        activeInvasions.put(invasionId, task);
        
        return true;
    }
    
    /**
     * Останавливает все активные нашествия
     */
    public void stopAllInvasions() {
        for (BukkitTask task : activeInvasions.values()) {
            task.cancel();
        }
        
        activeInvasions.clear();
        plugin.getLogger().info("Все активные нашествия остановлены");
    }
    
    /**
     * Загружает сохраненные разломы из файла
     */
    private void loadFractures() {
        if (!fracturesFile.exists()) {
            return;
        }
        
        YamlConfiguration config = YamlConfiguration.loadConfiguration(fracturesFile);
        
        // Загружаем статус портала
        portalOpened = config.getBoolean("portal_opened", false);
        
        // Загружаем разломы
        List<Map<?, ?>> fracturesList = config.getMapList("fractures");
        for (Map<?, ?> map : fracturesList) {
            try {
                String worldName = (String) map.get("world");
                World world = Bukkit.getWorld(worldName);
                
                if (world != null) {
                    double x = ((Number) map.get("x")).doubleValue();
                    double y = ((Number) map.get("y")).doubleValue();
                    double z = ((Number) map.get("z")).doubleValue();
                    
                    Location location = new Location(world, x, y, z);
                    fractures.add(location);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка при загрузке разлома", e);
            }
        }
        
        plugin.getLogger().info("Загружено " + fractures.size() + " разломов");
    }
    
    /**
     * Сохраняет разломы в файл
     */
    private void saveFractures() {
        YamlConfiguration config = new YamlConfiguration();
        
        // Сохраняем статус портала
        config.set("portal_opened", portalOpened);
        
        // Сохраняем разломы
        List<Map<String, Object>> fracturesList = new ArrayList<>();
        for (Location location : fractures) {
            Map<String, Object> map = new HashMap<>();
            map.put("world", location.getWorld().getName());
            map.put("x", location.getX());
            map.put("y", location.getY());
            map.put("z", location.getZ());
            fracturesList.add(map);
        }
        
        config.set("fractures", fracturesList);
        
        try {
            config.save(fracturesFile);
            plugin.getLogger().info("Сохранено " + fractures.size() + " разломов");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при сохранении разломов", e);
        }
    }
    
    /**
     * Сохраняет статус открытия портала
     */
    private void savePortalState() {
        YamlConfiguration config = YamlConfiguration.loadConfiguration(fracturesFile);
        config.set("portal_opened", true);
        
        try {
            config.save(fracturesFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при сохранении статуса портала", e);
        }
    }
    
    /**
     * Получает список всех разломов
     * @return Список локаций разломов
     */
    public List<Location> getFractures() {
        return new ArrayList<>(fractures);
    }
    
    /**
     * Проверяет, был ли уже открыт портал
     * @return true, если портал уже был открыт
     */
    public boolean isPortalOpened() {
        return portalOpened;
    }
    
    /**
     * Обработчик события телепортации игрока через портал
     * Предотвращает телепортацию через разломы
     */
    @EventHandler
    public void onPlayerPortal(PlayerPortalEvent event) {
        Location playerLoc = event.getPlayer().getLocation();
        
        // Проверяем, находится ли игрок вблизи одного из разломов
        for (Location fractureLoc : fractures) {
            if (playerLoc.getWorld().equals(fractureLoc.getWorld()) && 
                    playerLoc.distance(fractureLoc) < 10) { // радиус 10 блоков от центра разлома
                
                // Отменяем телепортацию
                event.setCancelled(true);
                
                // Уведомляем игрока
                event.getPlayer().playSound(playerLoc, Sound.BLOCK_PORTAL_TRIGGER, 0.5f, 0.5f);
                
                // Создаем эффект отторжения
                playerLoc.getWorld().spawnParticle(Particle.PORTAL, playerLoc, 50, 0.5, 1, 0.5, 0.1);
                
                return;
            }
        }
    }
    
    /**
     * Сбрасывает статус открытия портала
     */
    public void resetPortalState() {
        portalOpened = false;
        savePortalState();
        plugin.getLogger().info("Статус открытия портала сброшен. Следующее открытие портала запустит нашествие.");
    }
    
    /**
     * Закрывает все разломы и очищает их список
     */
    public void closeAllFractures() {
        plugin.getLogger().info("Закрытие всех разломов...");
        
        // Создаем копию списка, чтобы избежать ConcurrentModificationException
        List<Location> fracturesCopy = new ArrayList<>(fractures);
        
        // Закрываем каждый разлом
        for (Location location : fracturesCopy) {
            World world = location.getWorld();
            if (world == null) continue;
            
            // Находим и удаляем все блоки портала в радиусе разлома
            int radius = 5;
            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        Block block = world.getBlockAt(
                                location.getBlockX() + x, 
                                location.getBlockY() + y, 
                                location.getBlockZ() + z
                        );
                        
                        if (block.getType() == Material.NETHER_PORTAL) {
                            // Добавляем эффекты закрытия
                            world.spawnParticle(Particle.PORTAL, block.getLocation().add(0.5, 0.5, 0.5), 10, 0.5, 0.5, 0.5, 0.1);
                            world.playSound(block.getLocation(), Sound.BLOCK_GLASS_BREAK, 0.5f, 0.5f);
                            
                            // Удаляем блок портала
                            block.setType(Material.AIR);
                        }
                    }
                }
            }
            
            // Добавляем финальный эффект в центре разлома
            world.spawnParticle(Particle.FLAME, location, 20, 1, 1, 1, 0.1);
            world.playSound(location, Sound.ENTITY_GENERIC_EXPLODE, 1.0f, 0.5f);
        }
        
        // Очищаем список разломов
        fractures.clear();
        
        // Сохраняем пустой список
        saveFractures();
        
        // Останавливаем все активные нашествия
        stopAllInvasions();
        
        plugin.getLogger().info("Все разломы успешно закрыты.");
    }
} 