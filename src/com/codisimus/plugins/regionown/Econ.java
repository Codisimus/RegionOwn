package com.codisimus.plugins.regionown;

import com.codisimus.plugins.regionown.Region.AddOn;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

/**
 * Manages payment of buying/selling Regions/Add-ons
 * 
 * @author Codisimus
 */
public class Econ {
    static Economy economy;
    static double buyPrice;
    static double moneyBack;
    
    /* Add-on Prices */
    static double blockPvP;
    static double blockPvE;
    static double blockExplosions;
    static double lockChests;
    static double lockDoors;
    static double disableButtons;
    static double disablePistons;
    static double denyAccess;
    static double alarm;
    static double heal;
    static double feed;

    /**
     * Charges a Player for purchasing the given Region
     * The price is calculated using the Regions size
     * 
     * @param player The name of the Player to be charged
     * @param region The Region being purchased
     * @return True if the transaction was successful
     */
    public static boolean buy(Player player, Region region) {
        String name = player.getName();
        double price = getBuyPrice(name, region);
        
        if (economy != null) {
            //Cancel if the Player cannot afford the transaction
            if (!economy.has(name, price)) {
                player.sendMessage(RegionOwnMessages.insufficientFunds.replace("<amount>", format(price)));
                return false;
            }

            economy.withdrawPlayer(name, price);
        }
        
        player.sendMessage(RegionOwnMessages.buy.replace("<amount>", format(price)));
        return true;
    }
    
    /**
     * Charges the Player the given amount of money
     * 
     * @param player The Player being charged
     * @param amount The amount being charged
     * @return true if the transaction was successful;
     */
    public static boolean charge(Player player, double amount) {
        String name = player.getName();
        
        if (economy != null) {
            //Cancel if the Player cannot afford the transaction
            if (!economy.has(name, amount)) {
                player.sendMessage(RegionOwnMessages.insufficientFunds.replace("<amount>", format(amount)));
                return false;
            }

            economy.withdrawPlayer(name, amount);
        }
        
        if (amount > 0) {
            player.sendMessage(RegionOwnMessages.charge.replace("<amount>", format(amount)));
        }
        return true;
    }
    
    /**
     * Adds the sellPrice to the Player's total balance
     * 
     * @param player The Player who is selling
     */
    public static void sell(Player player, Region region) {
        String name = player.getName();
        String price = format(getSellPrice(name, region));
        sell(name, region);
        player.sendMessage(RegionOwnMessages.sell.replace("<amount>", price));
    }
    
    /**
     * Adds the sellPrice to the Player's total balance
     * 
     * @param name The name of the Player who is selling
     */
    public static void sell(String name, Region region) {
        if (economy != null) {
            economy.depositPlayer(name, getSellPrice(name, region));
        }
    }
    
    /**
     * Forces the Player to sell their land
     * 
     * @param admin The Player who is forcing the sale
     * @param seller The Player who is being forced to sell
     */
    public static void sell(Player admin, String owner, Region region) {
        sell(owner, region);
        String price = format(getSellPrice(owner, region));

        //Notify the Seller
        Player seller = RegionOwn.server.getPlayer(owner);
        if (seller != null) {
            seller.sendMessage(RegionOwnMessages.adminSold.replace("<amount>", price));
        }
        
        admin.sendMessage(RegionOwnMessages.adminSell.replace("<amount>", price));
    }
    
    /**
     * Refunds the Player the given amount of money
     * 
     * @param player The Player being refunded
     * @param amount The amount being refunded
     */
    public static void refund(Player player, double amount) {
        String name = player.getName();
        
        if (economy != null) {
            economy.depositPlayer(name, amount);
        }
        
        player.sendMessage(RegionOwnMessages.refund.replace("<amount>", format(amount)));
    }
    
    /**
     * Returns the BuyPrice for the given Region
     * 
     * @param player The given Player
     * @return The calculated BuyPrice
     */
    public static double getBuyPrice(String player, Region region) {
        return RegionOwn.hasPermission(player, "free")
                ? 0
                : buyPrice * region.size();
    }
    
    /**
     * Returns the SellPrice for the given Region
     * 
     * @param player The given Player
     * @return The calculated SellPrice
     */
    public static double getSellPrice(String player, Region region) {
        return getBuyPrice(player, region) * (moneyBack / 100);
    }
    
    /**
     * Returns the BuyPrice for the given Add-on
     * 
     * @param addon The given Add-on
     * @return The BuyPrice of the add-on or 0 if the price is -1
     */
    public static double getBuyPrice(AddOn addon) {
        double price;
        switch (addon) {
        case BLOCKPVP: price = blockPvP; break;
        case BLOCKPVE: price = blockPvE; break;
        case BLOCKEXPLOSIONS: price = blockExplosions; break;
        case LOCKCHESTS: price = lockChests; break;
        case LOCKDOORS: price = lockDoors; break;
        case DISABLEBUTTONS: price = disableButtons;  break;
        case DISABLEPISTONS: price = disablePistons; break;
        case DENYACCESS: price = denyAccess; break;
        case ALARM: price = alarm; break;
        case HEAL: price = heal; break;
        case FEED: price = feed; break;
        default: price = -2; break;
        }
        
        if (price == -1) {
            price = 0;
        }
        
        return price;
    }
    
    /**
     * Returns the SellPrice for the given Add-on
     * 
     * @param addon The given Add-on
     * @return The SellPrice of the add-on
     */
    public static double getSellPrice(AddOn addon) {
        return getBuyPrice(addon) * (moneyBack / 100);
    }
    
    /**
     * Formats the money amount by adding the unit
     * 
     * @param amount The amount of money to be formatted
     * @return The String of the amount + currency name
     */
    public static String format(double amount) {
        return economy == null
                ? "free"
                : economy.format(amount).replace(".00", "");
    }
}