package mafiaprod.emstory.listeners;

import mafiaprod.emstory.EMStory;
import mafiaprod.emstory.managers.PortalManager;
import mafiaprod.emstory.recieps.Recieps;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.TileState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPortalEvent;
import org.bukkit.event.world.PortalCreateEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.FireworkMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.logging.Level;

public class PortalIgnite implements Listener {
    private final EMStory plugin;
    private final NamespacedKey gmrdKey;
    private final ItemStack portalFuel;
    private final PortalManager portalManager;
    private final Recieps recieps;

    private static final List<String> IGNITION_PHRASES = Arrays.asList(
            "Че? Почему не зажигается?", "Либо я что-то не так делаю, либо да...", "Ебаные админы опять намутили сложностей", "Че ты не горишь падла", "Я точно в 3 блока сделал?",
            "Бля, ну портал же так всегда зажигался!", "Эээ, а стержни как?", "А если построить в другом месте?", "Может, проблема в высоте? Или в биоме?", "Стоп, может, надо сделать портал больше?"
    );

    public PortalIgnite(EMStory plugin, Recieps recieps, PortalManager portalManager) {
        this.plugin = plugin;
        this.gmrdKey = new NamespacedKey(plugin, "gmrd");
        this.recieps = recieps;
        this.portalFuel = recieps.createPortalFuel();
        this.portalManager = portalManager;
    }

    @EventHandler
    public void onPlayerPortalEvent(PlayerPortalEvent event) {
        // Получаем местоположение игрока и портала
        Player player = event.getPlayer();
        Location playerLoc = player.getLocation();
        
        // Проверяем, доступны ли сейчас порталы
        Map<Location, PortalManager.PortalData> activePortals = portalManager.getActivePortals();
        
        // Если нет активных порталов, игрок застревает
        if (activePortals.isEmpty()) {
            event.setCancelled(true);

            // Добавляем эффекты для застрявшего игрока
            player.playSound(playerLoc, Sound.BLOCK_PORTAL_TRIGGER, 1.0f, 0.5f);
            player.spawnParticle(Particle.PORTAL, playerLoc, 30, 0.5, 0.5, 0.5, 0.5);
            
            // Логируем событие
            plugin.getLogger().warning("Игрок " + player.getName() + 
                    " застрял в измерении " + playerLoc.getWorld().getName() + 
                    " из-за отключения портала! Координаты: " + 
                    playerLoc.getBlockX() + ", " + playerLoc.getBlockY() + ", " + playerLoc.getBlockZ());
        }
    }

    @EventHandler
    public void onPortalCreate(PortalCreateEvent event) {
        if (event.getReason() != PortalCreateEvent.CreateReason.FIRE) return;

        Location portalLocation = event.getBlocks().get(0).getLocation();
        Block gmrdBlock = findNearestGMRDBlock(portalLocation, 20);

        if (gmrdBlock == null || !(gmrdBlock.getState() instanceof Chest)) {
            event.setCancelled(true);
            return;
        }

        Chest gmrdChest = (Chest) gmrdBlock.getState();

        // Дополнительные проверки chest
        if (gmrdChest.getInventory() == null) {
            event.setCancelled(true);
            return;
        }

        if (!consumeFuel(gmrdChest.getInventory())) {
            event.setCancelled(true);
            return;
        }

        // Запускаем анимацию создания портала
        playPortalCreationAnimation(portalLocation);
        
        startFuelConsumption(portalLocation, gmrdChest);
    }

    /**
     * Воспроизводит эффектную анимацию создания портала
     */
    private void playPortalCreationAnimation(Location portalLocation) {
        World world = portalLocation.getWorld();
        if (world == null) return;
        
        // Центр портала для эффектов
        Location center = portalLocation.clone().add(0.5, 0.5, 0.5);
        
        // Находим все блоки портала в радиусе
        List<Block> portalBlocks = new ArrayList<>();
        for (int x = -5; x <= 5; x++) {
            for (int y = -5; y <= 5; y++) {
                for (int z = -5; z <= 5; z++) {
                    Block block = world.getBlockAt(center.getBlockX() + x, center.getBlockY() + y, center.getBlockZ() + z);
                    if (block.getType() == Material.NETHER_PORTAL) {
                        portalBlocks.add(block);
                    }
                }
            }
        }
        
        if (portalBlocks.isEmpty()) return;
        
        // Звуковой эффект начала активации
        world.playSound(center, Sound.BLOCK_END_PORTAL_SPAWN, 1.0F, 0.5F);
        
        // Находим игроков в радиусе для показа эффектов
        Collection<Entity> nearbyEntities = world.getNearbyEntities(center, 50, 50, 50);
        List<Player> nearbyPlayers = new ArrayList<>();
        for (Entity entity : nearbyEntities) {
            if (entity instanceof Player) {
                nearbyPlayers.add((Player) entity);
            }
        }
        
        // Отправляем сообщение об активации портала
        for (Player player : nearbyPlayers) {
            player.sendMessage(ChatColor.DARK_PURPLE + "" + ChatColor.BOLD + "Вы чувствуете, как пространство искажается...");
        }
        
        // Создаем основную анимацию
        new BukkitRunnable() {
            int tick = 0;
            final int maxTicks = 60; // 3 секунды при 20 тиках в секунду
            
            @Override
            public void run() {
                if (tick >= maxTicks) {
                    // Финальные эффекты
                    world.playSound(center, Sound.ENTITY_ENDER_DRAGON_GROWL, 1.0F, 0.5F);
                    
                    // Создаем несколько фейерверков
                    for (int i = 0; i < 3; i++) {
                        Location fireworkLoc = center.clone().add(
                                (Math.random() - 0.5) * 3,
                                (Math.random() - 0.5) * 3,
                                (Math.random() - 0.5) * 3
                        );
                        spawnCustomFirework(fireworkLoc);
                    }
                    
                    // Землетрясение/вибрация для игроков
                    for (Player player : nearbyPlayers) {
                        player.spawnParticle(Particle.FLASH, center, 1, 0, 0, 0, 0);
                        player.playSound(player.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.5F, 0.2F);
                        player.sendTitle(
                                ChatColor.DARK_PURPLE + "Портал активирован", 
                                ChatColor.LIGHT_PURPLE + "Межпространственный переход открыт", 
                                10, 70, 20
                        );
                    }
                    
                    cancel();
                    return;
                }
                
                // Прогресс анимации от 0 до 1
                float progress = (float) tick / maxTicks;
                
                // Блоки портала светятся все ярче
                for (Block block : portalBlocks) {
                    Location particleLoc = block.getLocation().add(0.5, 0.5, 0.5);
                    
                    // Спираль частиц вокруг каждого блока портала
                    double angle = progress * Math.PI * 10 + (Math.PI * 2 * tick / 10);
                    double radius = 0.6 + progress * 0.4;
                    double x = Math.cos(angle) * radius;
                    double z = Math.sin(angle) * radius;
                    
                    world.spawnParticle(Particle.PORTAL, particleLoc.clone().add(x, 0, z), 3, 0.1, 0.1, 0.1, 0.05);
                    
                    if (tick % 5 == 0) {
                        world.spawnParticle(Particle.END_ROD, particleLoc, 1, 0.3, 0.3, 0.3, 0.02);
                    }
                    
                    if (tick % 10 == 0) {
                        world.spawnParticle(Particle.DRAGON_BREATH, particleLoc, 1, 0.5, 0.5, 0.5, 0.01);
                    }
                }
                
                // Добавляем звуковые эффекты
                if (tick % 15 == 0) {
                    world.playSound(center, Sound.BLOCK_PORTAL_AMBIENT, 1.0F, 0.5F + progress * 1.0F);
                }
                
                // Расширяющаяся волна энергии вокруг портала
                if (tick % 10 == 0) {
                    double waveRadius = progress * 5;
                    for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 8) {
                        double waveX = Math.cos(angle) * waveRadius;
                        double waveZ = Math.sin(angle) * waveRadius;
                        world.spawnParticle(
                            Particle.PORTAL, 
                            center.clone().add(waveX, 0, waveZ), 
                            3, 0.2, 0.2, 0.2, 0
                        );
                    }
                }
                
                tick++;
            }
        }.runTaskTimer(plugin, 0, 1);
    }
    
    /**
     * Создает кастомный фейерверк с эффектами для портала
     */
    private void spawnCustomFirework(Location location) {
        Firework firework = (Firework) location.getWorld().spawnEntity(location, EntityType.FIREWORK_ROCKET);
        FireworkMeta meta = firework.getFireworkMeta();
        
        // Создаем случайные цвета для фейерверка
        List<Color> colors = Arrays.asList(
            Color.PURPLE, Color.FUCHSIA, Color.BLACK, Color.BLUE, Color.fromRGB(128, 0, 128)
        );
        Collections.shuffle(colors);
        
        // Создаем эффект
        FireworkEffect effect = FireworkEffect.builder()
            .with(FireworkEffect.Type.BALL_LARGE)
            .withColor(colors.subList(0, 2))
            .withFade(colors.subList(2, 3))
            .flicker(true)
            .trail(true)
            .build();
        
        meta.addEffect(effect);
        meta.setPower(1);  // Мощность взрыва
        
        firework.setFireworkMeta(meta);
    }

    private Block findNearestGMRDBlock(Location center, int radius) {
        World world = center.getWorld();
        if (world == null) return null;

        int cx = center.getBlockX();
        int cy = center.getBlockY();
        int cz = center.getBlockZ();

        int minY = Math.max(0, cy - radius);
        int maxY = Math.min(world.getMaxHeight(), cy + radius);

        // Перебираем сначала близкие блоки
        for (int r = 0; r <= radius; r++) {
            for (int x = -r; x <= r; x++) {
                for (int y = -r; y <= r; y++) {
                    for (int z = -r; z <= r; z++) {
                        int bx = cx + x;
                        int by = cy + y;
                        int bz = cz + z;

                        if (by < minY || by > maxY) continue; // Ограничение высоты

                        Block block = world.getBlockAt(bx, by, bz);
                        if (isGMRDBlock(block)) {
                            return block; // Нашли ближайший ГМРД, выходим
                        }
                    }
                }
            }
        }

        return null; // Не найдено
    }

    private boolean isGMRDBlock(Block block) {
        if (!(block.getState() instanceof TileState)) {
            return false;
        }

        TileState tileState = (TileState) block.getState();
        PersistentDataContainer container = tileState.getPersistentDataContainer();

        return container.has(gmrdKey, PersistentDataType.STRING);
    }

    private boolean consumeFuel(Inventory inventory) {
        if (inventory == null) return false;

        // Более безопасная проверка и потребление топлива
        try {
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
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Ошибка при потреблении топлива", e);
        }

        return false;
    }

    private void startFuelConsumption(Location portalLoc, Chest gmrdChest) {
        PortalManager.PortalData portalData = new PortalManager.PortalData(gmrdChest);
        portalManager.addActivePortal(portalLoc, portalData);
    }
}