package com.codisimus.plugins.regionown;

/**
 * Holds messages that are displayed to users of this plugin
 *
 * @author Codisimus
 */
public class RegionOwnMessages {
    public static String permission;
    public static String doNotOwn;
    public static String insufficientFunds;
    public static String buy;
    public static String sell;
    public static String adminSell;
    public static String adminSold;
    public static String charge;
    public static String refund;
    public static String regionNotFound = "§4Region §6<Name>§4 was not found";
    
    /**
     * Formats all messages
     */
    static void formatAll() {
        permission = format(permission);
        doNotOwn = format(doNotOwn);
        insufficientFunds = format(insufficientFunds);
        buy = format(buy);
        sell = format(sell);
        adminSell = format(adminSell);
        adminSold = format(adminSold);
        charge = format(charge);
        refund = format(refund);
        regionNotFound = format(regionNotFound);
    }
    
    /**
     * Adds various Unicode characters and colors to a string
     * 
     * @param string The string being formated
     * @return The formatted String
     */
    private static String format(String string) {
        return string.replace("&", "§").replace("<ae>", "æ").replace("<AE>", "Æ")
                .replace("<o/>", "ø").replace("<O/>", "Ø")
                .replace("<a>", "å").replace("<A>", "Å");
    }
}