package mafiaprod.emstory.managers;

import mafiaprod.emstory.EMStory;
import mafiaprod.emstory.listeners.PortalIgnite;
import mafiaprod.emstory.recieps.Recieps;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.stream.Collectors;

public class PortalManager {
    private final EMStory plugin;
    private final File portalDataFile;
    private final Map<Location, PortalData> activePortals;
    private final ItemStack portalFuel;
    private final PortalIgnite portalIgnite;
    private final Recieps recieps;

    public PortalManager(EMStory plugin, Recieps recieps) {
        this.plugin = plugin;
        this.portalDataFile = new File(plugin.getDataFolder(), "portal_data.yml");
        this.activePortals = new HashMap<>();
        this.portalFuel = recieps.createPortalFuel();
        this.portalIgnite = new PortalIgnite(plugin, recieps, this);
        this.recieps = recieps;
        loadPortalData();
    }

    public void addActivePortal(Location location, PortalData portalData) {
        activePortals.put(location, portalData);
        savePortalData();
    }

    public void removeActivePortal(Location location) {
        PortalData portalData = activePortals.remove(location);
        if (portalData != null) {
            plugin.getLogger().info("Удаление активного портала в локации: " + location);
            
            // Отменяем задачу расходования топлива
            if (portalData.fuelTask != -1) {
                Bukkit.getScheduler().cancelTask(portalData.fuelTask);
                plugin.getLogger().info("Отменена задача расходования топлива #" + portalData.fuelTask);
            }

            // Логгируем информацию о застрявших игроках в других измерениях
            World portalWorld = location.getWorld();
            if (portalWorld != null) {
                for (Player player : Bukkit.getOnlinePlayers()) {
                    World playerWorld = player.getWorld();
                    // Проверяем, находится ли игрок в другом измерении
                    if (playerWorld != null && !playerWorld.equals(portalWorld) && 
                            (playerWorld.getName().endsWith("_nether") || playerWorld.getName().endsWith("_the_end"))) {
                        
                        
                        // Логгируем событие
                        plugin.getLogger().warning("Игрок " + player.getName() + 
                                " застрял в измерении " + playerWorld.getName() + 
                                " из-за закрытия портала! Координаты: " + 
                                player.getLocation().getBlockX() + ", " + 
                                player.getLocation().getBlockY() + ", " + 
                                player.getLocation().getBlockZ());
                    }
                }
            }

            // Закрываем портал асинхронно для повышения производительности
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Запускаем анимацию закрытия перед фактическим удалением портала
                playPortalCloseAnimation(location, () -> {
                    closePortal(location);
                });
            });
        } else {
            plugin.getLogger().warning("Попытка удалить несуществующий портал в локации: " + location);
        }
        savePortalData();
    }
    
    /**
     * Воспроизводит эффектную анимацию закрытия портала
     */
    private void playPortalCloseAnimation(Location portalLocation, Runnable onFinish) {
        World world = portalLocation.getWorld();
        if (world == null) {
            if (onFinish != null) onFinish.run();
            return;
        }
        
        // Центр портала для эффектов
        Location center = portalLocation.clone().add(0.5, 0.5, 0.5);
        
        // Находим все блоки портала в радиусе
        List<Block> portalBlocks = getPortalBlocks(portalLocation);
        
        if (portalBlocks.isEmpty()) {
            plugin.getLogger().warning("Не найдены блоки портала для анимации закрытия. Пропуск анимации.");
            if (onFinish != null) onFinish.run();
            return;
        }
        
        // Звуковой эффект начала деактивации
        world.playSound(center, Sound.BLOCK_END_PORTAL_FRAME_FILL, 1.0F, 0.3F);
        
        // Находим игроков в радиусе для показа эффектов
        Collection<Entity> nearbyEntities = world.getNearbyEntities(center, 50, 50, 50);
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player) {
                nearbyPlayers.add((Player) entity);
            }
        }
        
        // Отправляем сообщение о деактивации портала
        for (Player player : nearbyPlayers) {
            player.sendMessage(ChatColor.RED + "" + ChatColor.BOLD + "Портал нестабилен и начинает закрываться...");
        }
        
        // Сохраняем onFinish для гарантированного вызова даже при ошибках
        final Runnable safeOnFinish = onFinish;
        
        try {
            // Создаем основную анимацию
            new BukkitRunnable() {
                int tick = 0;
                final int maxTicks = 40; // 2 секунды при 20 тиках в секунду
                
                @Override
                public void run() {
                    try {
                        if (tick >= maxTicks) {
                            // Финальные эффекты
                            world.playSound(center, Sound.ENTITY_ENDERMAN_TELEPORT, 1.0F, 0.3F);
                            
                            // Взрыв частиц в центре портала
                            world.spawnParticle(Particle.EXPLOSION, center, 1, 0, 0, 0, 0);
                            world.spawnParticle(Particle.PORTAL, center, 30, 2, 2, 2, 0.1);
                            
                            // Уведомление игроков
                            for (Player player : nearbyPlayers) {
                                player.sendTitle(
                                        ChatColor.RED + "Портал закрыт", 
                                        ChatColor.GRAY + "Межпространственный переход деактивирован", 
                                        10, 40, 20
                                );
                            }
                            
                            // Отменяем задачу
                            cancel();
                            
                            // Запускаем следующий этап после небольшой задержки
                            // Это гарантирует, что анимация будет завершена перед удалением блоков
                            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                                try {
                                    if (safeOnFinish != null) {
                                        safeOnFinish.run();
                                    }
                                } catch (Exception e) {
                                    plugin.getLogger().log(Level.SEVERE, "Ошибка при выполнении closePortal после анимации", e);
                                    // Принудительное закрытие портала при ошибке
                                    closePortal(portalLocation);
                                }
                            }, 5L); // Задержка 5 тиков (0.25 секунды)
                            
                            return;
                        }
                    
                        // Проверяем, существуют ли ещё блоки портала
                        // Если портал был удалён другим способом, прекращаем анимацию
                        boolean anyPortalBlocksExist = false;
                        for (Block block : portalBlocks) {
                            if (block.getType() == Material.NETHER_PORTAL) {
                                anyPortalBlocksExist = true;
                                break;
                            }
                        }
                        
                        if (!anyPortalBlocksExist) {
                            plugin.getLogger().info("Блоки портала уже удалены, отмена анимации закрытия.");
                            cancel();
                            
                            // Вызываем колбэк завершения, если он есть
                            if (safeOnFinish != null) {
                                Bukkit.getScheduler().runTask(plugin, safeOnFinish);
                            }
                            return;
                        }
                        
                        // Прогресс анимации от 0 до 1
                        float progress = (float) tick / maxTicks;
                        
                        // Эффект схлопывания или разрушения портала
                        for (Block block : portalBlocks) {
                            if (block.getType() != Material.NETHER_PORTAL) continue;
                            
                            Location particleLoc = block.getLocation().add(0.5, 0.5, 0.5);
                            
                            // Создаем эффект трещин/разломов
                            if (tick % 3 == 0) {
                                // Направляем частицы от краев к центру
                                Vector direction = center.toVector().subtract(particleLoc.toVector()).normalize().multiply(0.15);
                                
                                // Трещины и разломы в пространстве
                                world.spawnParticle(Particle.PORTAL, particleLoc, 5, 0.3, 0.3, 0.3, 0.05);
                                
                                // Частицы движутся к центру
                                world.spawnParticle(Particle.CRIT, particleLoc, 1, 0, 0, 0, 0.05);
                            }
                            
                            // Добавляем звуковые эффекты
                            if (tick % 10 == 0) {
                                world.playSound(particleLoc, Sound.BLOCK_GLASS_BREAK, 0.5F, 0.5F + progress);
                            }
                        }
                        
                        // Потрескивания и нестабильность портала
                        if (tick % 5 == 0) {
                            // Искажения пространства вокруг портала
                            double distortion = 3 - progress * 2; // Постепенно уменьшаем радиус искажения
                            for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 4) {
                                double x = Math.cos(angle) * distortion;
                                double z = Math.sin(angle) * distortion;
                                
                                world.spawnParticle(
                                    Particle.PORTAL, 
                                    center.clone().add(x, (Math.random() - 0.5) * 2, z), 
                                    3, 0.1, 0.1, 0.1, 0
                                );
                            }
                        }
                        
                        tick++;
                    } catch (Exception e) {
                        plugin.getLogger().log(Level.SEVERE, "Ошибка в анимации закрытия портала", e);
                        cancel();
                        
                        // Гарантируем выполнение колбэка при ошибке
                        if (safeOnFinish != null) {
                            Bukkit.getScheduler().runTask(plugin, safeOnFinish);
                        }
                    }
                }
            }.runTaskTimer(plugin, 0, 1);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось запустить анимацию закрытия портала", e);
            // Вызываем колбэк в случае общей ошибки
            if (safeOnFinish != null) {
                safeOnFinish.run();
            }
        }
    }

    private void closePortal(Location portalLoc) {
        try {
            Location searchCenter = portalLoc.clone();
            World world = searchCenter.getWorld();

            if (world == null) {
                plugin.getLogger().warning("Мир портала не найден при закрытии");
                return;
            }

            // Находим блоки портала
            List<Block> portalBlocks = getPortalBlocks(searchCenter);

            if (portalBlocks.isEmpty()) {
                plugin.getLogger().warning("Не удалось найти блоки портала для закрытия в локации: " + portalLoc);
                return;
            }

            // Для надежности используем синхронную задачу
            Bukkit.getScheduler().runTask(plugin, () -> {
                try {
                    int countRemoved = 0;
                    // Безопасное удаление блоков портала
                    for (Block block : portalBlocks) {
                        if (block.getType() == Material.NETHER_PORTAL) {
                            block.setType(Material.AIR);
                            countRemoved++;
                        }
                    }
                    plugin.getLogger().info("Портал успешно закрыт в локации: " + portalLoc + ", удалено блоков: " + countRemoved);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.SEVERE, "Ошибка при удалении блоков портала", e);
                }
            });
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Критическая ошибка при закрытии портала", e);
        }
    }

    private List<Block> getPortalBlocks(Location center) {
        List<Block> portalBlocks = new java.util.ArrayList<>();
        World world = center.getWorld();

        if (world == null) {
            plugin.getLogger().warning("Мир не найден при поиске блоков портала");
            return portalBlocks;
        }

        // Увеличенный радиус поиска для надежности
        int radius = 25;
        int centerX = center.getBlockX();
        int centerY = center.getBlockY();
        int centerZ = center.getBlockZ();
        
        // Загружаем чанки в указанном радиусе, если они не загружены
        int chunkRadius = (radius / 16) + 1;
        for (int cx = -chunkRadius; cx <= chunkRadius; cx++) {
            for (int cz = -chunkRadius; cz <= chunkRadius; cz++) {
                int chunkX = (centerX + (cx * 16)) >> 4;
                int chunkZ = (centerZ + (cz * 16)) >> 4;
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    world.loadChunk(chunkX, chunkZ, true);
                }
            }
        }

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    Block block = world.getBlockAt(centerX + x, centerY + y, centerZ + z);
                    if (block.getType() == Material.NETHER_PORTAL) {
                        portalBlocks.add(block);
                    }
                }
            }
        }

        return portalBlocks;
    }

    public Map<Location, PortalData> getActivePortals() {
        return new HashMap<>(activePortals);
    }

    private void loadPortalData() {
        if (!portalDataFile.exists()) {
            plugin.getLogger().info("Файл данных порталов не существует");
            return;
        }

        YamlConfiguration config = YamlConfiguration.loadConfiguration(portalDataFile);
        plugin.getLogger().info("Загрузка данных порталов из файла");

        for (String key : config.getKeys(false)) {
            try {
                Location portalLocation = deserializeLocation(key);
                PortalData portalData = deserializePortalData(config, key);

                if (portalData == null || portalData.fuelChest == null) {
                    plugin.getLogger().warning("Пропуск некорректных данных портала: " + key);
                    continue;
                }

                int fuelTask = startFuelConsumption(portalLocation, portalData.fuelChest);
                portalData.fuelTask = fuelTask;

                activePortals.put(portalLocation, portalData);
                plugin.getLogger().info("Загружен портал в локации: " + portalLocation);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка загрузки данных портала: " + key, e);
            }
        }
        
        plugin.getLogger().info("Загружено активных порталов: " + activePortals.size());
    }

    private int startFuelConsumption(Location portalLocation, Chest fuelChest) {
        return Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            try {
                // Проверяем chest и блок даже если он в выгруженном чанке
                if (fuelChest == null) {
                    plugin.getLogger().warning("Сундук с топливом не найден для портала: " + portalLocation);
                    removeActivePortal(portalLocation);
                    return;
                }
                
                World world = fuelChest.getWorld();
                if (world == null) {
                    plugin.getLogger().warning("Мир сундука с топливом не найден для портала: " + portalLocation);
                    removeActivePortal(portalLocation);
                    return;
                }
                
                Location chestLoc = fuelChest.getLocation();
                
                // Проверяем загружен ли чанк сундука и при необходимости загружаем его
                int chunkX = chestLoc.getBlockX() >> 4;
                int chunkZ = chestLoc.getBlockZ() >> 4;
                if (!world.isChunkLoaded(chunkX, chunkZ)) {
                    world.loadChunk(chunkX, chunkZ, true);
                }
                
                Block chestBlock = world.getBlockAt(chestLoc);
                if (!(chestBlock.getState() instanceof Chest)) {
                    plugin.getLogger().warning("Блок сундука был удален или поврежден для портала: " + portalLocation);
                    removeActivePortal(portalLocation);
                    return;
                }

                Chest currentChest = (Chest) chestBlock.getState();
                Inventory inventory = currentChest.getInventory();
                if (inventory == null || !consumeFuel(inventory)) {
                    plugin.getLogger().info("Топливо закончилось для портала: " + portalLocation);
                    removeActivePortal(portalLocation);
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.SEVERE, "Ошибка при проверке или потреблении топлива", e);
                // Принудительно закрываем портал при ошибке для безопасности
                removeActivePortal(portalLocation);
            }
        }, 200L, 200L);
    }

    private boolean consumeFuel(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (item != null && recieps.isCustomItem(item, "portal_fuel")) {
                if (item.getAmount() > 1) {
                    item.setAmount(item.getAmount() - 1);
                } else {
                    inventory.setItem(i, null);
                }
                return true;
            }
        }
        return false;
    }

    private String serializeLocation(Location location) {
        if (location == null || location.getWorld() == null) {
            throw new IllegalArgumentException("Некорректная локация для сериализации");
        }
        
        return location.getWorld().getName() + ":" +
                location.getBlockX() + ":" +
                location.getBlockY() + ":" +
                location.getBlockZ();
    }

    private Location deserializeLocation(String serializedLocation) {
        try {
            String[] parts = serializedLocation.split(":");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Некорректный формат сериализованной локации: " + serializedLocation);
            }
            
            World world = plugin.getServer().getWorld(parts[0]);
            if (world == null) {
                throw new IllegalArgumentException("Мир не найден: " + parts[0]);
            }
            
            int x = Integer.parseInt(parts[1]);
            int y = Integer.parseInt(parts[2]);
            int z = Integer.parseInt(parts[3]);
            return new Location(world, x, y, z);
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Ошибка при десериализации локации: " + serializedLocation, e);
            throw e;
        }
    }

    private void serializePortalData(YamlConfiguration config, String key, PortalData portalData) {
        config.set(key + ".chest_world", portalData.fuelChest.getLocation().getWorld().getName());
        config.set(key + ".chest_x", portalData.fuelChest.getLocation().getBlockX());
        config.set(key + ".chest_y", portalData.fuelChest.getLocation().getBlockY());
        config.set(key + ".chest_z", portalData.fuelChest.getLocation().getBlockZ());
    }

    private PortalData deserializePortalData(YamlConfiguration config, String key) {
        try {
            World chestWorld = plugin.getServer().getWorld(config.getString(key + ".chest_world"));
            int chestX = config.getInt(key + ".chest_x");
            int chestY = config.getInt(key + ".chest_y");
            int chestZ = config.getInt(key + ".chest_z");

            if (chestWorld == null) {
                plugin.getLogger().warning("Мир для chest не найден при десериализации портала");
                return null;
            }

            Location chestLocation = new Location(chestWorld, chestX, chestY, chestZ);
            Block chestBlock = chestLocation.getBlock();

            if (!(chestBlock.getState() instanceof Chest)) {
                plugin.getLogger().warning("Блок chest не найден или поврежден при десериализации портала");
                return null;
            }

            Chest fuelChest = (Chest) chestBlock.getState();
            return new PortalData(fuelChest);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка десериализации данных портала", e);
            return null;
        }
    }

    private void savePortalData() {
        YamlConfiguration config = new YamlConfiguration();
        plugin.getLogger().info("Сохранение данных порталов, всего порталов: " + activePortals.size());

        for (Map.Entry<Location, PortalData> entry : activePortals.entrySet()) {
            try {
                String key = serializeLocation(entry.getKey());
                serializePortalData(config, key, entry.getValue());
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Ошибка при сериализации портала", e);
            }
        }

        try {
            config.save(portalDataFile);
            plugin.getLogger().info("Данные порталов успешно сохранены");
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Не удалось сохранить данные порталов", e);
        }
    }

    public static class PortalData {
        public Chest fuelChest;
        public int fuelTask = -1;

        public PortalData(Chest fuelChest) {
            this.fuelChest = fuelChest;
        }
    }
}