package mafiaprod.emstory;

import mafiaprod.emstory.commands.EMSCommand;
import mafiaprod.emstory.listeners.DispenserCrafting;
import mafiaprod.emstory.listeners.FurnaceLogic;
import mafiaprod.emstory.listeners.GMRDLogic;
import mafiaprod.emstory.listeners.NetherLogic;
import mafiaprod.emstory.listeners.PortalIgnite;
import mafiaprod.emstory.managers.PortalManager;
import mafiaprod.emstory.recieps.Recieps;
import mafiaprod.emstory.events.NetherInvasionEvent;
import mafiaprod.emstory.events.NetherInvasionManager;
import org.bukkit.plugin.java.JavaPlugin;

public final class EMStory extends JavaPlugin {
    private static EMStory instance;
    private PortalManager portalManager;
    private Recieps recieps;
    private NetherInvasionEvent invasionEvent;

    @Override
    public void onEnable() {
        // Создаем папку плагина, если она не существует
        if (!getDataFolder().exists()) {
            getDataFolder().mkdir();
        }

        instance = this;
        
        // Инициализируем систему рецептов (теперь хранится как поле класса)
        this.recieps = new Recieps(this);

        // Инициализируем PortalManager
        this.portalManager = new PortalManager(this, recieps);
        
        // Инициализируем систему нашествия из Ада
        this.invasionEvent = new NetherInvasionEvent(this);

        // Регистрация слушателей событий
        registerEventListeners();
        
        // Регистрация команд
        registerCommands();
        
        getLogger().info("EMStory plugin успешно загружен!");
    }
    
    // Регистрация всех слушателей событий
    private void registerEventListeners() {
        getServer().getPluginManager().registerEvents(new PortalIgnite(this, recieps, portalManager), this);
        getServer().getPluginManager().registerEvents(new GMRDLogic(this, recieps), this);
        getServer().getPluginManager().registerEvents(new DispenserCrafting(this, recieps), this);
        getServer().getPluginManager().registerEvents(new NetherLogic(), this);
        getServer().getPluginManager().registerEvents(new FurnaceLogic(this), this);
        
        // Регистрируем слушатель событий нашествия
        getServer().getPluginManager().registerEvents(invasionEvent, this);
    }
    
    // Регистрация всех команд
    private void registerCommands() {
        this.getCommand("giveitem").setExecutor(new EMSCommand(this));
        
        // Регистрируем команду для управления нашествием
        NetherInvasionManager invasionManager = new NetherInvasionManager(this, invasionEvent);
        this.getCommand("invasion").setExecutor(invasionManager);
        this.getCommand("invasion").setTabCompleter(invasionManager);
    }

    public static EMStory getInstance() {
        return instance; // Получаем ссылку на экземпляр
    }

    // Геттер для PortalManager
    public PortalManager getPortalManager() {
        return portalManager;
    }
    
    // Геттер для системы рецептов
    public Recieps getRecieps() {
        return recieps;
    }
    
    // Геттер для системы нашествия
    public NetherInvasionEvent getInvasionEvent() {
        return invasionEvent;
    }

    @Override
    public void onDisable() {
        // Менеджер порталов теперь сам заботится о сохранении данных
        getLogger().info("EMStory plugin disabled!");
    }
}