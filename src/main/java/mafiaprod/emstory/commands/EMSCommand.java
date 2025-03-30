package mafiaprod.emstory.commands;

import mafiaprod.emstory.EMStory;
import mafiaprod.emstory.recieps.Recieps;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.Dispenser;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class EMSCommand implements CommandExecutor, TabCompleter {
    private final Plugin plugin;
    private final List<String> availableItems = Arrays.asList(
            "dispenser", "gmrd", "fuel", "quantum_spectrometer", "mass_spectral_stabilizer", "all"
    );

    public EMSCommand(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        // Получаем доступ к системе рецептов через главный класс
        Recieps recieps = EMStory.getInstance().getRecieps();

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Вы не являетесь игроком!");
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("emstory.hiddenitem")) {
            player.sendMessage(ChatColor.RED + "У вас нет прав использовать эту команду!");
            return false;
        }

        if(!(args.length > 0)){
            player.sendMessage(ChatColor.RED + "Введите аргумент! [" + String.join(" | ", availableItems) + "]");
            return false;
        }

        String itemId = args[0].toLowerCase();

        // Выдача всех предметов
        if (itemId.equals("all")) {
            for (String id : recieps.getRegisteredItemIds()) {
                ItemStack item = recieps.createCustomItem(id);
                if (item != null) {
                    player.getInventory().addItem(item);
                }
            }
            player.sendMessage(ChatColor.GREEN + "Все предметы получены!");
            return true;
        }
        
        // Обработка по типу предмета
        switch (itemId) {
            case "dispenser":
                ItemStack crafter = recieps.createDispenserCrafting();
                player.getInventory().addItem(crafter);
                player.sendMessage(ChatColor.GREEN + "Крафтер получен!");
                return true;
                
            case "gmrd":
                ItemStack gmrd = recieps.createGMRD();
                player.getInventory().addItem(gmrd);
                player.sendMessage(ChatColor.GREEN + "ГМРД получен!");
                return true;
                
            case "fuel":
                ItemStack fuel = recieps.createPortalFuel();
                player.getInventory().addItem(fuel);
                player.sendMessage(ChatColor.GREEN + "Топливо получено!");
                return true;
                
            case "quantum_spectrometer":
                ItemStack quantumSpectrometer = recieps.createQuantumSpectrometer();
                player.getInventory().addItem(quantumSpectrometer);
                player.sendMessage(ChatColor.GREEN + "Квантовый Спектрометр получен!");
                return true;
                
            case "mass_spectral_stabilizer":
                ItemStack stabilizer = recieps.createMassSpectralStabilizer();
                player.getInventory().addItem(stabilizer);
                player.sendMessage(ChatColor.GREEN + "Масс-спектральный стабилизатор получен!");
                return true;
                
            default:
                // Проверяем, есть ли кастомный предмет с таким ID
                ItemStack customItem = recieps.createCustomItem(itemId);
                if (customItem != null) {
                    player.getInventory().addItem(customItem);
                    player.sendMessage(ChatColor.GREEN + "Предмет " + itemId + " получен!");
                    return true;
                }
                
                player.sendMessage(ChatColor.RED + "Неверный аргумент! [" + String.join(" | ", availableItems) + "]");
                return false;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            String input = args[0].toLowerCase();
            return availableItems.stream()
                    .filter(item -> item.startsWith(input))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
}
