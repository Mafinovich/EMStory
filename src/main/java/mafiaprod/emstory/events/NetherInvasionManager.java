package mafiaprod.emstory.events;

import mafiaprod.emstory.EMStory;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Sound;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Класс для управления командами для события "Нашествие"
 */
public class NetherInvasionManager implements CommandExecutor, TabCompleter {
    private final EMStory plugin;
    private final NetherInvasionEvent invasionEvent;
    
    /**
     * Конструктор менеджера команд нашествия
     * @param plugin Основной класс плагина
     * @param invasionEvent Объект события нашествия
     */
    public NetherInvasionManager(EMStory plugin, NetherInvasionEvent invasionEvent) {
        this.plugin = plugin;
        this.invasionEvent = invasionEvent;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("emstory.invasion")) {
            sender.sendMessage(ChatColor.RED + "У вас нет прав для управления нашествиями!");
            return false;
        }
        
        if (args.length == 0) {
            showHelp(sender);
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "list":
                listFractures(sender);
                return true;
                
            case "start":
                if (args.length < 3) {
                    sender.sendMessage(ChatColor.RED + "Использование: /invasion start <номер_разлома> <интенсивность>");
                    return false;
                }
                
                try {
                    int fractureIndex = Integer.parseInt(args[1]);
                    int intensity = Integer.parseInt(args[2]);
                    startInvasion(sender, fractureIndex, intensity);
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Неверные числовые параметры!");
                    return false;
                }
                
            case "stop":
                stopInvasions(sender);
                return true;
                
            case "status":
                showStatus(sender);
                return true;
                
            case "startall":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Использование: /invasion startall <интенсивность>");
                    return false;
                }
                
                try {
                    int intensity = Integer.parseInt(args[1]);
                    startAllInvasions(sender, intensity);
                    return true;
                } catch (NumberFormatException e) {
                    sender.sendMessage(ChatColor.RED + "Неверный параметр интенсивности!");
                    return false;
                }
                
            case "reset":
                resetPortalState(sender);
                return true;
                
            case "endevent":
                endInvasionEvent(sender);
                return true;
                
            default:
                showHelp(sender);
                return true;
        }
    }
    
    /**
     * Показывает справку по командам
     * @param sender Отправитель команды
     */
    private void showHelp(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "==== Справка по командам Нашествия ====");
        sender.sendMessage(ChatColor.YELLOW + "/invasion list " + ChatColor.WHITE + "- Показать список всех разломов");
        sender.sendMessage(ChatColor.YELLOW + "/invasion start <номер> <интенсивность> " + ChatColor.WHITE + "- Запустить нашествие из разлома");
        sender.sendMessage(ChatColor.YELLOW + "/invasion startall <интенсивность> " + ChatColor.WHITE + "- Запустить глобальное нашествие из всех разломов");
        sender.sendMessage(ChatColor.YELLOW + "/invasion stop " + ChatColor.WHITE + "- Остановить все активные нашествия");
        sender.sendMessage(ChatColor.YELLOW + "/invasion status " + ChatColor.WHITE + "- Показать статус нашествий");
        sender.sendMessage(ChatColor.YELLOW + "/invasion reset " + ChatColor.WHITE + "- Сбросить статус открытия портала");
        sender.sendMessage(ChatColor.YELLOW + "/invasion endevent " + ChatColor.WHITE + "- Закрыть все разломы и завершить событие");
    }
    
    /**
     * Показывает список всех разломов
     * @param sender Отправитель команды
     */
    private void listFractures(CommandSender sender) {
        List<String> fractureLocations = new ArrayList<>();
        List<org.bukkit.Location> fractures = invasionEvent.getFractures();
        
        if (fractures.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Разломов пока не существует!");
            return;
        }
        
        sender.sendMessage(ChatColor.GOLD + "==== Список разломов (" + fractures.size() + ") ====");
        
        for (int i = 0; i < fractures.size(); i++) {
            org.bukkit.Location loc = fractures.get(i);
            String locString = String.format("%d: %s [%d, %d, %d]", 
                    i, loc.getWorld().getName(), 
                    loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
            
            fractureLocations.add(locString);
            
            if (sender instanceof Player) {
                Player player = (Player) sender;
                double distance = player.getLocation().distance(loc);
                sender.sendMessage(ChatColor.YELLOW + locString + ChatColor.GRAY + " (расстояние: " + 
                        String.format("%.1f", distance) + " блоков)");
            } else {
                sender.sendMessage(ChatColor.YELLOW + locString);
            }
        }
    }
    
    /**
     * Запускает нашествие из указанного разлома
     * @param sender Отправитель команды
     * @param fractureIndex Индекс разлома
     * @param intensity Интенсивность нашествия (1-5)
     */
    private void startInvasion(CommandSender sender, int fractureIndex, int intensity) {
        List<org.bukkit.Location> fractures = invasionEvent.getFractures();
        
        if (fractureIndex < 0 || fractureIndex >= fractures.size()) {
            sender.sendMessage(ChatColor.RED + "Разлом с индексом " + fractureIndex + " не существует!");
            return;
        }
        
        if (intensity < 1 || intensity > 5) {
            sender.sendMessage(ChatColor.RED + "Интенсивность должна быть от 1 до 5!");
            return;
        }
        
        if (invasionEvent.startInvasion(fractureIndex, intensity)) {
            org.bukkit.Location loc = fractures.get(fractureIndex);
            
            sender.sendMessage(ChatColor.GREEN + "Нашествие начато из разлома #" + fractureIndex + 
                    " [" + loc.getBlockX() + ", " + loc.getBlockY() + ", " + loc.getBlockZ() + "]" +
                    " с интенсивностью " + intensity);
            
            // Логируем событие
            plugin.getLogger().info(sender.getName() + " запустил нашествие из разлома #" + fractureIndex + 
                    " с интенсивностью " + intensity);
        } else {
            sender.sendMessage(ChatColor.RED + "Не удалось запустить нашествие из разлома #" + fractureIndex);
        }
    }
    
    /**
     * Запускает нашествие из всех разломов одновременно
     * @param sender Отправитель команды
     * @param intensity Интенсивность нашествия (1-5)
     */
    private void startAllInvasions(CommandSender sender, int intensity) {
        List<org.bukkit.Location> fractures = invasionEvent.getFractures();
        
        if (fractures.isEmpty()) {
            sender.sendMessage(ChatColor.RED + "Разломов пока не существует!");
            return;
        }
        
        if (intensity < 1 || intensity > 5) {
            sender.sendMessage(ChatColor.RED + "Интенсивность должна быть от 1 до 5!");
            return;
        }
        
        int successCount = 0;
        
        // Запускаем нашествие из каждого разлома
        for (int i = 0; i < fractures.size(); i++) {
            if (invasionEvent.startInvasion(i, intensity)) {
                successCount++;
            }
        }
        
        // Уведомляем о результатах
        if (successCount > 0) {
            sender.sendMessage(ChatColor.GREEN + "Глобальное нашествие начато из " + 
                    successCount + " разломов с интенсивностью " + intensity + "!");
                
            // Оповещаем всех игроков
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendTitle(
                        ChatColor.DARK_RED + "ГЛОБАЛЬНОЕ НАШЕСТВИЕ!",
                        ChatColor.RED + "Адские создания атакуют из всех разломов!",
                        20, 80, 20
                );
                player.playSound(player.getLocation(), Sound.ENTITY_WITHER_SPAWN, 1.0f, 0.5f);
            }
            
            // Логируем событие
            plugin.getLogger().info(sender.getName() + " запустил глобальное нашествие из " + 
                    successCount + " разломов с интенсивностью " + intensity);
        } else {
            sender.sendMessage(ChatColor.RED + "Не удалось запустить ни одного нашествия!");
        }
    }
    
    /**
     * Останавливает все активные нашествия
     * @param sender Отправитель команды
     */
    private void stopInvasions(CommandSender sender) {
        invasionEvent.stopAllInvasions();
        sender.sendMessage(ChatColor.GREEN + "Все активные нашествия остановлены!");
        
        // Логируем событие
        plugin.getLogger().info(sender.getName() + " остановил все активные нашествия");
    }
    
    /**
     * Показывает текущий статус порталов и нашествий
     * @param sender Отправитель команды
     */
    private void showStatus(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "==== Статус Нашествия ====");
        sender.sendMessage(ChatColor.YELLOW + "Портал в Ад открыт: " + 
                (invasionEvent.isPortalOpened() ? ChatColor.GREEN + "Да" : ChatColor.RED + "Нет"));
        
        sender.sendMessage(ChatColor.YELLOW + "Количество разломов: " + ChatColor.WHITE + 
                invasionEvent.getFractures().size());
    }
    
    /**
     * Сбрасывает статус открытия портала
     * @param sender Отправитель команды
     */
    private void resetPortalState(CommandSender sender) {
        invasionEvent.resetPortalState();
        sender.sendMessage(ChatColor.GREEN + "Статус открытия портала сброшен! Следующее открытие портала запустит нашествие.");
        
        // Логируем событие
        plugin.getLogger().info(sender.getName() + " сбросил статус открытия портала");
    }
    
    /**
     * Завершает событие нашествия, закрывает все разломы
     * @param sender Отправитель команды
     */
    private void endInvasionEvent(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "Завершение события нашествия...");
        
        // Оповещаем всех игроков
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendTitle(
                    ChatColor.GREEN + "Разломы закрываются!",
                    ChatColor.YELLOW + "Измерения восстанавливают стабильность",
                    20, 60, 20
            );
            player.playSound(player.getLocation(), Sound.ENTITY_WITHER_DEATH, 1.0f, 1.0f);
        }
        
        // Закрываем все разломы
        invasionEvent.closeAllFractures();
        
        // Оповещаем о завершении
        for (Player player : Bukkit.getOnlinePlayers()) {
            player.sendMessage(ChatColor.GREEN + "[Событие] " + 
                    ChatColor.YELLOW + "Межпространственные разломы закрыты. Нашествие окончено!");
        }
        
        // Сбрасываем статус открытия портала для будущих событий
        invasionEvent.resetPortalState();
        
        sender.sendMessage(ChatColor.GREEN + "Событие нашествия успешно завершено!");
        
        // Логируем событие
        plugin.getLogger().info(sender.getName() + " завершил событие нашествия");
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("emstory.invasion")) {
            return new ArrayList<>();
        }
        
        if (args.length == 1) {
            return Arrays.asList("list", "start", "startall", "stop", "status", "reset", "endevent").stream()
                    .filter(cmd -> cmd.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        } else if (args.length == 2 && "start".equalsIgnoreCase(args[0])) {
            // Предлагаем индексы разломов
            int fractureCount = invasionEvent.getFractures().size();
            List<String> indices = new ArrayList<>();
            
            for (int i = 0; i < fractureCount; i++) {
                indices.add(String.valueOf(i));
            }
            
            return indices.stream()
                    .filter(index -> index.startsWith(args[1]))
                    .collect(Collectors.toList());
        } else if ((args.length == 3 && "start".equalsIgnoreCase(args[0])) || 
                   (args.length == 2 && "startall".equalsIgnoreCase(args[0]))) {
            // Предлагаем интенсивность
            return Arrays.asList("1", "2", "3", "4", "5").stream()
                    .filter(intensity -> intensity.startsWith(args[args.length - 1]))
                    .collect(Collectors.toList());
        }
        
        return new ArrayList<>();
    }
} 