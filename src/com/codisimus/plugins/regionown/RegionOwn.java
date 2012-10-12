package com.codisimus.plugins.regionown;

import com.codisimus.plugins.regionown.Region.AddOn;
import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Loads Plugin and manages Data/Permissions
 *
 * @author Codisimus
 */
public class RegionOwn extends JavaPlugin {
    static Server server;
    static Logger logger;
    static Permission permission;
    static PluginManager pm;
    static Properties lastDaySeen;
    static String dataFolder;
    static Plugin plugin;
    private static Properties p;
    private static HashMap<String, Region> regions = new HashMap<String, Region>();
    private static HashMap<String, Region> ownedRegions = new HashMap<String, Region>();
    private static HashMap<String, RegionOwner> regionOwners = new HashMap<String, RegionOwner>();
    private static int disownTime;
    private static LinkedList<World> worlds = new LinkedList<World>();
    private static boolean autoRevert;

    /**
     * Calls methods to load this Plugin when it is enabled
     */
    @Override
    public void onEnable() {
        server = getServer();
        logger = getLogger();
        pm = server.getPluginManager();
        plugin = this;

        /* Disable this plugin if Vault is not present */
        if (!pm.isPluginEnabled("Vault")) {
            logger.severe("[RegionOwn] Please install Vault in order to use this plugin!");
            pm.disablePlugin(this);
            return;
        }

        /* Create data folders */
        File dir = this.getDataFolder();
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dataFolder = dir.getPath();

        dir = new File(dataFolder+"/Regions");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dir = new File(dataFolder+"/Snapshots");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dir = new File(dataFolder+"/Owners");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dir = new File(dataFolder+"/Deleted");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dir = new File(dataFolder+"/Deleted/Regions");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        dir = new File(dataFolder+"/Deleted/Snapshots");
        if (!dir.isDirectory()) {
            dir.mkdir();
        }

        /* Link Permissions/Economy */
        RegisteredServiceProvider<Permission> permissionProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.permission.Permission.class);
        if (permissionProvider != null) {
            permission = permissionProvider.getProvider();
        }

        RegisteredServiceProvider<Economy> economyProvider =
                getServer().getServicesManager().getRegistration(net.milkbowl.vault.economy.Economy.class);
        if (economyProvider != null) {
            Econ.economy = economyProvider.getProvider();
        }

        loadAll();

        /* Register Events */
        pm.registerEvents(new RegionOwnListener(), this);
        pm.registerEvents(new RegionSelector(), this);
        pm.registerEvents(new RegionOwnMovementListener(), this);
        if (autoRevert) {
            pm.registerEvents(new RegionOwnLoggerListener(), this);
        }
        if (Econ.blockPvP != -2 || Econ.blockPvE != -2) {
            pm.registerEvents(new RegionOwnDamageListener(), this);
        }
        if (Econ.blockExplosions != -2) {
            pm.registerEvents(new RegionOwnExplosionListener(), this);
        }
        if (Econ.lockChests != -2 || Econ.lockDoors != -2) {
            pm.registerEvents(new RegionOwnInteractionListener(), this);
        }
        if (Econ.disablePistons != -2) {
            pm.registerEvents(new RegionOwnPistonListener(), this);
        }

        /* Register the command found in the plugin.yml */
        String commands = this.getDescription().getCommands().toString();
        RegionOwnCommand.command = commands.substring(1, commands.indexOf("="));
        getCommand(RegionOwnCommand.command).setExecutor(new RegionOwnCommand());

        /* Schedule repeating tasks */
        scheduleDisowner();
        RegionOwnMovementListener.scheduleHealer();
        RegionOwnMovementListener.scheduleFeeder();
        RegionSelector.animateSelections();

        Properties version = new Properties();
        try {
            version.load(this.getResource("version.properties"));
        } catch (Exception ex) {
            logger.warning("[RegionOwn] version.properties file not found within jar");
        }
        logger.info("RegionOwn "+this.getDescription().getVersion()+" (Build "+version.getProperty("Build")+") is enabled!");
    }

    /**
     * Executes all methods which load data from files
     */
    private static void loadAll() {
        loadSettings();
        loadOwners();
        loadRegions();
        loadLastSeen();
    }

    /**
     * Loads settings from the config.properties file
     */
    public static void loadSettings() {
        FileInputStream fis = null;
        try {
            //Copy the file from the jar if it is missing
            File file = new File(dataFolder+"/config.properties");
            if (!file.exists()) {
                plugin.saveResource("config.properties", true);
            }

            //Load config file
            p = new Properties();
            fis = new FileInputStream(file);
            p.load(fis);

            /* Prices */
            Econ.buyPrice = Double.parseDouble(loadValue("BuyPrice"));
            Econ.moneyBack = Double.parseDouble(loadValue("MoneyBack"));
            Econ.blockPvP = Double.parseDouble(loadValue("BlockPvP"));
            Econ.blockPvE = Double.parseDouble(loadValue("BlockPvE"));
            Econ.blockExplosions = Double.parseDouble(loadValue("BlockExplosions"));
            Econ.lockChests = Double.parseDouble(loadValue("LockChests"));
            Econ.lockDoors = Double.parseDouble(loadValue("LockDoors"));
            Econ.disableButtons = Double.parseDouble(loadValue("DisableButtons"));
            Econ.disablePistons = Double.parseDouble(loadValue("DisablePistons"));
            Econ.alarm = Double.parseDouble(loadValue("AlarmSystem"));
            Econ.heal = Double.parseDouble(loadValue("RegenerateHealth"));
            Econ.feed = Double.parseDouble(loadValue("RegenerateHunger"));
            Econ.denyAccess = Double.parseDouble(loadValue("DenyAccess"));

            /* Messages */
            RegionOwnMessages.permission = loadValue("PermissionMessage");
            RegionOwnMessages.doNotOwn = loadValue("DoNotOwnMessage");
            RegionOwnMessages.insufficientFunds = loadValue("InsufficientFundsMessage");
            RegionOwnMessages.buy = loadValue("BuyMessage");
            RegionOwnMessages.sell = loadValue("SellMessage");
            RegionOwnMessages.adminSell = loadValue("AdminSellMessage");
            RegionOwnMessages.adminSold = loadValue("SoldByAdminMessage");
            RegionOwnMessages.charge = loadValue("ChargeMessage");
            RegionOwnMessages.refund = loadValue("RefundMessage");
            RegionOwnMessages.accessDenied = loadValue("AccessDeniedMessage");
            RegionOwnMessages.formatAll();

            /* Other */
            disownTime = Integer.parseInt(loadValue("AutoDisownTimer"));
            autoRevert = Boolean.parseBoolean(loadValue("AutoRevertRegions"));
            RegionOwnMovementListener.rate = Integer.parseInt(loadValue("RegenerateRate"));
            RegionOwnMovementListener.amount = Integer.parseInt(loadValue("RegenerateAmount"));

            RegionOwnCommand.clearIDs = EnumSet.noneOf(Material.class);
            String[] ids = loadValue("ClearIDs").split(" ");
            for (String id: ids) {
                RegionOwnCommand.clearIDs.add(Material.matchMaterial(id));
            }

            RegionOwnCommand.nonOpaques = EnumSet.of(Material.AIR, Material.SAPLING, Material.LEAVES, Material.GLASS,
            Material.POWERED_RAIL, Material.DETECTOR_RAIL, Material.WEB, Material.LONG_GRASS, Material.DEAD_BUSH,
            Material.PISTON_EXTENSION, Material.PISTON_MOVING_PIECE, Material.YELLOW_FLOWER, Material.RED_ROSE,
            Material.BROWN_MUSHROOM, Material.TORCH, Material.FIRE, Material.REDSTONE_WIRE, Material.CROPS,
            Material.SIGN_POST, Material.WOODEN_DOOR, Material.LADDER, Material.RAILS, Material.WALL_SIGN,
            Material.LEVER, Material.STONE_PLATE, Material.IRON_DOOR_BLOCK, Material.WOOD_PLATE,
            Material.REDSTONE_TORCH_OFF, Material.REDSTONE_TORCH_ON, Material.STONE_BUTTON, Material.CACTUS,
            Material.SUGAR_CANE_BLOCK, Material.FENCE, Material.PORTAL, Material.CAKE_BLOCK, Material.DIODE_BLOCK_OFF,
            Material.DIODE_BLOCK_ON, Material.TRAP_DOOR, Material.IRON_FENCE, Material.THIN_GLASS, Material.PUMPKIN_STEM,
            Material.MELON_STEM, Material.VINE, Material.FENCE_GATE, Material.WATER_LILY, Material.NETHER_FENCE, Material.NETHER_WARTS);

            String data = loadValue("EnabledOnlyInWorlds");
            if (!data.isEmpty()) {
                for (String string: data.split(", ")) {
                    World world = server.getWorld(string);
                    if (world != null) {
                        worlds.add(world);
                    }
                }
            }
        } catch (Exception missingProp) {
            logger.severe("[RegionOwn] Failed to load config settings. This plugin may not function properly");
            missingProp.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Loads the given key and prints an error message if the key is missing
     *
     * @param key The key to be loaded
     * @return The String value of the loaded key
     */
    private static String loadValue(String key) {
        if (p.containsKey(key)) {
            return p.getProperty(key);
        } else {
            logger.severe("[RegionOwn] Missing value for "+key);
            logger.severe("[RegionOwn] Please regenerate the config.properties file");
            return null;
        }
    }

    /**
     * Loads Regions from files
     */
    public static void loadRegions() {
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        for (File file: new File(dataFolder+"/Regions/").listFiles()) {
            String name = file.getName();
            if (name.endsWith(".properties")) {
                try {
                    //Load the Properties file for reading
                    Properties p = new Properties();
                    fis = new FileInputStream(file);
                    p.load(fis);
                    fis.close();

                    //Skip loading if the Region is in a disabled World
                    String world = p.getProperty("World");
                    if (!enabledInWorld(server.getWorld(world))) {
                        continue;
                    }

                    //Convert the array of coordinates
                    int[] coords = new int[6];
                    String[] data = p.getProperty("Coordinates").split(":");
                    for (int i = 0; i < 6; i++) {
                        coords[i] = Integer.parseInt(data[i]);
                    }

                    String regionName = name.substring(0, name.length() - 11);

                    //Read the BitSet
                    File inFile = new File(dataFolder+"/Regions/"+regionName+".bitset");
                    fis = new FileInputStream(inFile);
                    ois = new ObjectInputStream(fis);
                    BitSet bitSet = (BitSet)ois.readObject();

                    Region region = new Region(world, coords, bitSet, p.getProperty("Owner"));

                    //Convert the coOwners data into a LinkedList for the Region
                    String coOwners = p.getProperty("Co-owners");
                    if (!coOwners.equals("none")) {
                        region.coOwners = new LinkedList<String>(Arrays.asList(coOwners.split(",")));
                    }

                    //Convert the groups data into a LinkedList for the Region
                    coOwners = p.getProperty("Groups");
                    if (!coOwners.equals("none")) {
                        region.groups = new LinkedList<String>(Arrays.asList(coOwners.split(",")));
                    }

                    region.name = regionName;
                    region.welcomeMessage = p.getProperty("WelcomeMessage");
                    region.leavingMessage = p.getProperty("LeavingMessage");

                    if (p.containsKey("BLOCKPVP")) {
                        for (AddOn addOn: AddOn.values()) {
                            String key = addOn.name();
                            if (p.containsKey(key)) {
                                region.setAddOn(null, addOn, Boolean.parseBoolean(p.getProperty(key)));
                            }
                        }
                    } else { //Update from 0.1.0
                        logger.info("Updating old save file...");
                        region.blockPvP = Boolean.parseBoolean(p.getProperty("BlockPvP"));
                        region.blockPvE = Boolean.parseBoolean(p.getProperty("BlockPvE"));
                        region.blockExplosions = Boolean.parseBoolean(p.getProperty("BlockExplosions"));
                        region.lockChests = Boolean.parseBoolean(p.getProperty("LockChests"));
                        region.lockDoors = Boolean.parseBoolean(p.getProperty("LockDoors"));
                        region.disableButtons = Boolean.parseBoolean(p.getProperty("DisableButtons"));
                        region.disablePistons = Boolean.parseBoolean(p.getProperty("DisablePistons"));
                        region.alarm = Boolean.parseBoolean(p.getProperty("AlarmSystem"));
                        region.heal = Boolean.parseBoolean(p.getProperty("RegenerateHealth"));
                        region.feed = Boolean.parseBoolean(p.getProperty("RegenerateHunger"));

                        region.save();
                    }

                    if (region.owner == null) {
                        regions.put(region.name, region);
                    } else {
                        ownedRegions.put(region.name, region);
                    }
                } catch (Exception loadFailed) {
                    logger.severe("[RegionOwn] Failed to load "+name);
                    loadFailed.printStackTrace();
                } finally {
                    try {
                        fis.close();
                        ois.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }

    /**
     * Loads RegionOwners from files
     */
    public static void loadOwners() {
        FileInputStream fis = null;
        for (File file: new File(dataFolder+"/Owners/").listFiles()) {
            String name = file.getName();
            if (name.endsWith(".properties")) {
                try {
                    //Load the Properties file for reading
                    Properties p = new Properties();
                    fis = new FileInputStream(file);
                    p.load(fis);

                    //Construct a new RegionOwner using the file name
                    RegionOwner owner = new RegionOwner(name.substring(0, name.length() - 11));

                    //Convert the coOwners data into a LinkedList for the ChunkOwner
                    String data = p.getProperty("Co-owners");
                    if (!data.equals("none")) 
                        owner.coOwners = new LinkedList<String>(Arrays.asList(data.split(",")));

                    //Convert the groups data into a LinkedList for the ChunkOwner
                    data = p.getProperty("Groups");
                    if (!data.equals("none")) 
                        owner.groups = new LinkedList<String>(Arrays.asList(data.split(",")));

                    regionOwners.put(owner.name, owner);
                }
                catch (Exception loadFailed) {
                    logger.severe("[RegionOwn] Failed to load "+name);
                    loadFailed.printStackTrace();
                } finally {
                    try {
                        fis.close();
                    } catch (Exception e) {
                    }
                }
            }
        }
    }
    
    /**
     * Loads last seen data from file
     */
    public static void loadLastSeen() {
        lastDaySeen = new Properties();
        FileInputStream fis = null;
        try {
            //Create the file if it does not exist
            File file = new File(dataFolder+"/lastseen.properties");
            if (!file.exists()) {
                file.createNewFile();
            }

            fis = new FileInputStream(file);
            lastDaySeen.load(fis);
        } catch (Exception ex) {
            logger.severe("[ChunkOwn] Failed to load lastseen.properties");
        } finally {
            try {
                fis.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Saves all data
     */
    public static void saveAll() {
        for (Region region: ownedRegions.values()) {
            saveRegion(region);
        }
        for (RegionOwner owner: regionOwners.values()) {
            saveRegionOwner(owner);
        }
        saveLastSeen();
    }

    /**
     * Writes the given RegionOwner to its save file
     * If the file already exists, it is overwritten
     *
     * @param region The given Region
     */
    public static void saveRegion(Region region) {
        FileOutputStream fos = null;
        ObjectOutputStream oos = null;
        try {
            Properties p = new Properties();

            p.setProperty("World", region.world);
            p.setProperty("Coordinates", region.x1+":"+region.z1+":"+region.x2+":"+region.z2+":"+region.y1+":"+region.y2);
            p.setProperty("Owner", region.owner == null ? "noone" : region.owner.name);

            String coOwners = "";
            if (region.coOwners.isEmpty()) {
                coOwners = "none";
            } else {
                for (String coOwner: region.coOwners) {
                    coOwners = coOwners.concat(coOwner+",");
                }
            }
            p.setProperty("Co-owners", coOwners);

            String groups = "";
            if (region.groups.isEmpty()) {
                groups = "none";
            } else {
                for (String group: region.groups) {
                    groups = groups.concat(group+",");
                }
            }
            p.setProperty("Groups", groups);

            p.setProperty("WelcomeMessage", region.welcomeMessage);
            p.setProperty("LeavingMessage", region.leavingMessage);

            for (AddOn addOn: AddOn.values()) {
                p.setProperty(addOn.name(), String.valueOf(region.hasAddOn(addOn)));
            }

            //Write the Region Properties to file
            fos = new FileOutputStream(dataFolder+"/Regions/"+region.name+".properties");
            p.store(fos, null);
            fos.close();

            File file = new File(dataFolder+"/Regions/"+region.name+".bitset");
            if (!file.exists()) {
                fos = new FileOutputStream(file);
                oos = new ObjectOutputStream(fos);
                oos.writeObject(region.bitSet);
            }
        } catch (Exception saveFailed) {
            logger.severe("[RegionOwn] Save Failed!");
            saveFailed.printStackTrace();
        } finally {
            try {
                fos.close();
                oos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Writes the given RegionOwner to its save file
     * If the file already exists, it is overwritten
     *
     * @param owner The given RegionOwner
     */
    static void saveRegionOwner(RegionOwner owner) {
        FileOutputStream fos = null;
        try {
            Properties p = new Properties();

            String coOwners = "";
            if (owner.coOwners.isEmpty()) {
                coOwners = "none";
            } else {
                for (String coOwner: owner.coOwners) {
                    coOwners = coOwners.concat(coOwner+",");
                }
            }
            p.setProperty("Co-owners", coOwners);

            String groups = "";
            if (owner.groups.isEmpty()) {
                groups = "none";
            } else {
                for (String group: owner.groups) {
                    groups = groups.concat(group+",");
                }
            }
            p.setProperty("Groups", groups);

            //Write the RegionOwner Properties to file
            fos = new FileOutputStream(dataFolder+"/RegionOwners/"+owner.name+".properties");
            p.store(fos, null);
        } catch (Exception saveFailed) {
            logger.severe("[RegionOwn] Save Failed!");
            saveFailed.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Writes the Map of last seen data to the save file
     * Old file is over written
     */
    public static void saveLastSeen() {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(dataFolder.concat("/lastseen.properties"));
            lastDaySeen.store(fos, null);
        } catch (Exception ex) {
            logger.severe("[RegionOwn] Failed to save lastseen.properties");
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Returns true if Player has permission to build*
     * *build also refers to many griefing events**
     * **all events can be found in the Listener Classes
     *
     * @param player The Player who is trying to build
     * @param block The Block the Player is modifying
     * @return True if Player has permission to 'build'
     */
    public static boolean canBuild(Player player, Block block) {
        //Return true if the Block is in a disabled World
        if (!enabledInWorld(block.getWorld())) {
            return true;
        }

        Region region = findRegion(block.getLocation());

        //If unowned, deny building if the Player cannot build on unclaimed land
        if (region == null) {
            if (player != null && !hasPermission(player, "admin") && hasPermission(player, "mustowntobuild")) {
                player.sendMessage(RegionOwnMessages.doNotOwn);
                return false;
            } else {
                return true;
            }
        }

        //Deny building if no Player can be determined (Mob)
        if (player == null) {
            return false;
        }

        //Allow building if the Player is a Co-owner of the land
        if (region.isCoOwner(player)) {
            return true;
        } else {
            player.sendMessage(RegionOwnMessages.doNotOwn);
            return false;
        }
    }

    /**
     * Returns true if this plugin is enabled in the given World
     *
     * @param world The given World
     * @return True if this plugin is enabled in the given World
     */
    public static boolean enabledInWorld(World world) {
        return worlds.isEmpty() || worlds.contains(world);
    }

    /**
     * Returns true if the given Player has the specific permission
     *
     * @param player The Player who is being checked for permission
     * @param type The String of the permission, ex. admin
     * @return True if the given Player has the specific permission
     */
    static boolean hasPermission(String player, String type) {
        return permission.has(server.getWorlds().get(0), player, "regionown."+type);
    }

    /**
     * Returns true if the given Player has the specific permission
     *
     * @param player The Player who is being checked for permission
     * @param type The String of the permission, ex. admin
     * @return True if the given Player has the specific permission
     */
    public static boolean hasPermission(Player player, String type) {
        return permission.has(player, "regionown."+type);
    }

    /**
     * Returns true if the given Player has permission to buy the given Add-on 
     *
     * @param player The Player who is being checked for permission
     * @param addOn The given Add-on
     * @return True if the given Player has the specific permission
     */
    public static boolean hasPermission(Player player, AddOn addOn) {
        return Econ.getBuyPrice(addOn) == -2
                ? false
                : hasPermission(player, "addon."+addOn.name().toLowerCase());
    }

    /**
     * Adds the Region to the saved data
     *
     * @param region The Region to add
     */
    public static void addRegion(Region region) {
        //Cancel if the Region is in a disabled World
        World world = server.getWorld(region.world);
        if (!enabledInWorld(world)) {
            return;
        }

        region.saveSnapshot("auto");

        if (region.owner == null) {
            regions.put(region.name, region);
        } else {
            ownedRegions.put(region.name, region);
        }

        saveRegion(region);
    }

    /**
     * Removes the Region from the saved data
     *
     * @param region The Region to remove
     */
    public static void removeRegion(Region region) {
        if (ownedRegions.containsKey(region.name)) {
            ownedRegions.remove(region.name);
            region.owner.blockCounter = region.owner.blockCounter - region.size();
            if (autoRevert) {
                region.revert("auto");
            }
        } else if (regions.containsKey(region.name)) {
            regions.remove(region.name);
        } else {
            return;
        }

        File file = new File(RegionOwn.dataFolder+"/Regions/"+region.name+".properties");
        file.renameTo(new File(file.getAbsolutePath().replace("/Regions/", "/Deleted/Regions/")));
    }

    /**
     * Returns the Region which contains the given Location
     * returns null if no Region was found
     * 
     * @param block The Location that may be in a Region
     */
    public static Region findRegion(Location location) {
        for (Region region: ownedRegions.values()) {
            if (region.contains(location)) {
                return region;
            }
        }
        return null;
    }

    /**
     * Returns the Region with the given name
     * returns null if no Region was found
     *
     * @param name The given name
     */
    public static Region findRegion(String name) {
        return ownedRegions.containsKey(name)
                ? ownedRegions.get(name)
                : regions.get(name);
    }

    /**
     * Returns the RegionOwner object for the given Player
     * A new RegionOwner is created if one does not exist
     * 
     * @param player The name of the Player
     * @return The RegionOwner for the given Player
     */
    public static RegionOwner getOwner(String player) {
        RegionOwner owner = findOwner(player);

        if (owner == null) {
            owner = new RegionOwner(player);
            regionOwners.put(player, owner);
        }

        return owner;
    }

    /**
     * Returns the RegionOwner object for the Region which contains the given Block
     * returns null if the Region is not found
     *
     * @param block The block that may be in a Region
     * @return The Owner of the Region
     */
    public static RegionOwner findOwner(Block block) {
        Region region = findRegion(block.getLocation());
        return region == null ? null : region.owner;
    }

    /**
     * Returns the RegionOwner object for the given Player
     * returns null if the RegionOwner does not exist
     *
     * @param player The name of the Player
     * @return The RegionOwner for the given Player
     */
    public static RegionOwner findOwner(String player) {
        return regionOwners.get(player);
    }

    /**
     * Returns a Collection of all Regions
     *
     * @return A Collection of all Regions
     */
    public static Collection<Region> getRegions() {
        return ownedRegions.values();
    }

    /**
     * Retrieves a list of Regions that the given Player owns
     *
     * @param player The name of the given Player
     * @return The list of Regions
     */
    public static LinkedList<Region> getOwnedRegions(String player) {
        LinkedList<Region> regionList = new LinkedList<Region>();
        for (Region region: ownedRegions.values()) {
            if (region.isOwner(player)) {
                regionList.add(region);
            }
        }
        return regionList;
    }

    /**
     * Checks for Players who have not logged on within the given amount of time
     * These Players will have their OwnedChunks automatically disowned
     * Players that do not have any Owned Chunks are ignored
     */
    public void scheduleDisowner() {
        if (disownTime <= 0) {
            return;
        }

        //Repeat every day
    	server.getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
    	    public void run() {
                int cutoffDay = getDayAD() - disownTime;
                for (String key: lastDaySeen.stringPropertyNames()) {
                    if (Integer.parseInt(lastDaySeen.getProperty(key)) < cutoffDay) {
                        logger.info("[RegionOwn] Clearing Regions that are owned by "+key);
                        RegionOwnCommand.sellAll(key);
                        lastDaySeen.remove(key);
                        saveLastSeen();
                    }
                }
    	    }
    	}, 0L, 1728000L);
    }

    /**
     * Returns the number of the current day in the AD time period
     *
     * @return The number of the current day in the AD time period
     */
    public static int getDayAD() {
        Calendar calendar = Calendar.getInstance();
        int yearAD = calendar.get(Calendar.YEAR);
        int dayOfYear = calendar.get(Calendar.DAY_OF_YEAR);
        return (int) ((yearAD - 1) * 365.4) + dayOfYear;
    }
}
