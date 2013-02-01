package com.codisimus.plugins.regionown;

import com.codisimus.plugins.regionown.Region.AddOn;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executes Player Commands
 *
 * @author Codisimus
 */
public class RegionOwnCommand implements CommandExecutor {
    public static String command = "region";
    static EnumSet<Material> clearIDs;
    private static enum Action {
        HELP, SEL, SELECT, DONE, SAVE, MOBREGION, TNT,
        BIO, BIOME, CLEAR, THAW, REPLACE, CUT, GROW,
        FILL, BACKUP, REVERT, UNDO, BUY, SELL, NAME,
        EXPAND, LIST, INFO, COOWNER, SELLALL
    }

    /**
     * Listens for ChunkOwn commands to execute them
     *
     * @param sender The CommandSender who may not be a Player
     * @param command The command that was executed
     * @param alias The alias that the sender used
     * @param args The arguments for the command
     * @return true always
     */
    @Override
    public boolean onCommand(CommandSender sender, Command command, String alias, String[] args) {
        //Cancel if the command is not from a Player
        if (!(sender instanceof Player)) {
            return true;
        }

        Player player = (Player) sender;

        //Cancel if the Player is in a disabled World
        if (!RegionOwn.enabledInWorld(player.getWorld())) {
            player.sendMessage("§4RegionOwn is disabled in your current World");
            return true;
        }

        //Display help page if the Player did not add any arguments
        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        Action action;

        try {
            action = Action.valueOf(args[0].toUpperCase());
        } catch (IllegalArgumentException notEnum) {
            sendHelp(player);
            return true;
        }

        switch (action) {
        /* Region Selection */
        case SEL: //Fall through
        case SELECT:
            if (RegionOwn.hasPermission(player, "own") || RegionOwn.hasPermission(player, "save") || RegionOwn.hasPermission(player, "tools")) {
                if (args.length == 2) {
                    if (args[1].equals("biome")) {
                        RegionSelector.selectBiome(player);
                    } else {
                        Region region = RegionOwn.findRegion(args[1]);
                        if (region == null) {
                            player.sendMessage(RegionOwnMessages.regionNotFound.replace("<name>", args[1]));
                        } else if (!region.isCoOwner(player)) {
                            player.sendMessage(RegionOwnMessages.doNotOwn);
                        } else {
                            RegionSelector.setSelection(player, region);
                            player.sendMessage("§5Region §6" + region.name + "§5 has been selected");
                        }
                    }
                } else {
                    RegionSelector.startSelection(player);
                }
            } else {
                player.sendMessage(RegionOwnMessages.permission);
            }
            return true;

        case DONE:
            RegionSelector.endSelection(player);
            return true;

        case EXPAND:
            if (RegionSelector.isSelecting(player)) {
            RegionSelector.endSelection(player);
        }

            if (RegionSelector.hasSelection(player)) {
                Region region = RegionSelector.getSelection(player);
                if (region.owner != null) {
                    player.sendMessage("§4You cannot expand an Owned Region");
                    return true;
                }

                int oldSize = region.size();

                switch (args.length) {
                case 1:
                    int y = player.getLocation().getBlockY();
                    if (y < region.y1) {
                        region.y1 = y;
                    } else if (y > region.y2) {
                        region.y2 = y;
                    } else {
                        player.sendMessage("§4You must be standing above or below your current selection");
                    }
                    break;

                case 2:
                    if (args[1].equals("all")) {
                        region.y1 = 0;
                        region.y2 = player.getWorld().getMaxHeight();
                    } else if (args[1].equals("surface")) {
                        World world = player.getWorld();
                        region.y1 = region.y2;
                        int index = 0;
                        for (int x = region.x1; x <= region.x2; x++) {
                            for (int z = region.z1; z <= region.z2; z++) {
                                if (region.bitSet.get(index)) {
                                    Block block = world.getHighestBlockAt(x, z);
                                    while (block.getType().isOccluding()) {
                                        block = block.getRelative(BlockFace.DOWN);
                                    }
                                    int surface = block.getLocation().getBlockY();
                                    if (surface < region.y1) {
                                        region.y1 = surface;
                                    }
                                }
                            }
                        }
                    } else if (args[1].equals("highest")) {
                        World world = player.getWorld();
                        region.y2 = region.y1;
                        int index = 0;
                        for (int x = region.x1; x <= region.x2; x++) {
                            for (int z = region.z1; z <= region.z2; z++) {
                                if (region.bitSet.get(index)) {
                                    Block block = world.getHighestBlockAt(x, z).getRelative(BlockFace.DOWN);
                                    int highest = block.getLocation().getBlockY();
                                    if (highest > region.y2) {
                                        region.y2 = highest;
                                    }
                                }
                            }
                        }
                    } else if (args[1].equals("down")) {
                        region.y1 = 0;
                    } else if (args[1].equals("up")) {
                        region.y2 = player.getWorld().getMaxHeight();
                    } else {
                        sendHelp(player);
                        return true;
                    }
                    break;

                default:
                    sendHelp(player);
                    return true;
                }

                region.height = region.y2 - region.y1 + 1;

                player.sendMessage("§5Your selection has been expanded by §6" + (region.size() - oldSize) + " §5Blocks and is now §6"+region.size()+" §5Blocks");
            } else {
                player.sendMessage("§4You do not have a Region Selected");
            }
            return true;

        case SAVE:
            //Cancel if the Player does not have permission to use the command
            if (RegionOwn.hasPermission(player, "save")) {
                if (args.length != 2) {
                    player.sendMessage("§4Please include a §6name for you new Region");
                    return true;
                }

                if (RegionSelector.isSelecting(player)) {
                    RegionSelector.endSelection(player);
                }

                if (!RegionSelector.hasSelection(player)) {
                    player.sendMessage("§4You do not have a Region Selected");
                    return true;
                }

                Region region = RegionSelector.getSelection(player);
                region.name = args[1];

                RegionOwn.addRegion(region);
            } else {
                player.sendMessage(RegionOwnMessages.permission);
            }
            return true;

        case MOBREGION:
            //Cancel if the Player does not have permission to use the command
            if (!RegionOwn.hasPermission(player, "mobregion")) {
                player.sendMessage(RegionOwnMessages.permission);
                return true;
            }

            switch (args.length) {
            case 2:
                if (args[1].equals("list")) {
                    listMobRegions(player);
                } else {
                    sendHelp(player);
                }
                break;
            case 3:
                if (args[1].equals("add")) {
                    addMobRegion(player, args[1]);
                } else if (args[1].equals("remove")) {
                    removeMobRegion(player, args[1]);
                } else {
                    sendHelp(player);
                }
                break;
            default:
                sendHelp(player);
                break;
            }
            return true;

        /* Region Tools */
        case TNT:
            if (args.length != 1) {
                sendToolsHelp(player);
            } else {
                //Cancel if the Player does not have permission to use the command
                if (RegionOwn.hasPermission(player, "tools")) {
                    if (RegionSelector.isSelecting(player)) {
                        RegionSelector.endSelection(player);
                    }

                    if (!RegionSelector.hasSelection(player)) {
                        player.sendMessage("§4You do not have a Region Selected");
                        return true;
                    }

                    Region region = RegionSelector.getSelection(player);

                    World world = player.getWorld();
                    int index = 0;
                    int amount = 0;
                    for (int x = region.x1; x <= region.x2; x++) {
                        for (int z = region.z1; z <= region.z2; z++) {
                            if (region.bitSet.get(index)) {
                                world.getHighestBlockAt(x, z).setType(Material.TNT);
                                amount++;
                            }
                            index++;
                        }
                    }
                    player.sendMessage("§6" + amount + "§5 Blocks of TNT have been placed");
                } else {
                    player.sendMessage(RegionOwnMessages.permission);
                }
            }
            return true;

        case BIO: //Fall through
        case BIOME:
            if (args.length != 2) {
                sendToolsHelp(player);
            } else {
                Biome biome;
                try {
                    biome = Biome.valueOf(args[1].toUpperCase());
                } catch (IllegalArgumentException notEnum) {
                    player.sendMessage("§6" + args[1] + "§4 is not a valid Biome");
                    return true;
                }

                //Cancel if the Player does not have permission to use the command
                if (RegionOwn.hasPermission(player, "tools")) {
                    if (RegionSelector.isSelecting(player)) {
                        RegionSelector.endSelection(player);
                    }

                    if (!RegionSelector.hasSelection(player)) {
                        player.sendMessage("§4You do not have a Region Selected");
                        return true;
                    }

                    Region region = RegionSelector.getSelection(player);

                    HashSet<Chunk> needsUpdate = new HashSet<Chunk>();
                    World world = player.getWorld();
                    int index = 0;
                    for (int x = region.x1; x <= region.x2; x++) {
                        for (int z = region.z1; z <= region.z2; z++) {
                            if (region.bitSet.get(index)) {
                                world.setBiome(x, z, biome);
                                needsUpdate.add(world.getBlockAt(x, 0, z).getChunk());
                            }
                            index++;
                        }
                    }

                    for (Chunk chunk: needsUpdate) {
                        world.refreshChunk(chunk.getX(), chunk.getZ());
                    }

                    player.sendMessage("§5The selected Region has been set to a §6" + biome.name() + "§5 biome");
                    player.sendMessage("§5You must relog in order to see changes");
                } else {
                    player.sendMessage(RegionOwnMessages.permission);
                }
            }
            return true;

        case CUT:
            //Cancel if the Player does not have permission to use the command
            if (RegionOwn.hasPermission(player, "tools")) {
                if (RegionSelector.isSelecting(player)) {
                    RegionSelector.endSelection(player);
                }

                if (!RegionSelector.hasSelection(player)) {
                    player.sendMessage("§4You do not have a Region Selected");
                    return true;
                }

                Region region = RegionSelector.getSelection(player);
                region.saveUndoSnapshot();

                World world = player.getWorld();
                int index = 0;
                int amount = 0;
                for (int x = region.x1; x <= region.x2; x++) {
                    for (int z = region.z1; z <= region.z2; z++) {
                        if (region.bitSet.get(index)) {
                            for (int y = region.y1; y <= region.y2; y++) {
                                Block block = world.getBlockAt(x, y, z);
                                switch (block.getType()) {
                                case SAPLING: //Fall through
                                case LOG: //Fall through
                                case LEAVES: //Fall through
                                case LONG_GRASS: //Fall through
                                case DEAD_BUSH: //Fall through
                                case YELLOW_FLOWER: //Fall through
                                case RED_ROSE: //Fall through
                                case BROWN_MUSHROOM: //Fall through
                                case RED_MUSHROOM: //Fall through
                                case CROPS: //Fall through
                                case CACTUS: //Fall through
                                case SUGAR_CANE_BLOCK: //Fall through
                                case PUMPKIN: //Fall through
                                case HUGE_MUSHROOM_1: //Fall through
                                case HUGE_MUSHROOM_2: //Fall through
                                case MELON_BLOCK: //Fall through
                                case PUMPKIN_STEM: //Fall through
                                case MELON_STEM: //Fall through
                                case VINE: //Fall through
                                case WATER_LILY: //Fall through
                                case WEB: //Fall through
                                //case COCOA_PLANT:
                                    block.setTypeId(0);
                                    amount++;
                                    break;
                                default: break;
                                }
                            }
                        }
                        index++;
                    }
                }
                player.sendMessage("§6" + amount + "§5 Blocks have been cut");
            } else {
                player.sendMessage(RegionOwnMessages.permission);
            }
            return true;

        case THAW:
            //Cancel if the Player does not have permission to use the command
            if (RegionOwn.hasPermission(player, "tools")) {
                if (RegionSelector.isSelecting(player)) {
                    RegionSelector.endSelection(player);
                }

                if (!RegionSelector.hasSelection(player)) {
                    player.sendMessage("§4You do not have a Region Selected");
                    return true;
                }

                Region region = RegionSelector.getSelection(player);
                region.saveUndoSnapshot();

                World world = player.getWorld();
                int index = 0;
                int amount = 0;
                for (int x = region.x1; x <= region.x2; x++) {
                    for (int z = region.z1; z <= region.z2; z++) {
                        if (region.bitSet.get(index)) {
                            for (int y = region.y1; y <= region.y2; y++) {
                                Block block = world.getBlockAt(x, y, z);
                                switch (block.getType()) {
                                case ICE: block.setTypeId(8); amount++; break;
                                case SNOW: //Fall through
                                case SNOW_BLOCK: block.setTypeId(0); amount++; break;
                                default: break;
                                }
                            }
                        }
                        index++;
                    }
                }
                player.sendMessage("§6" + amount + "§5 Blocks have been thawed");
            } else {
                player.sendMessage(RegionOwnMessages.permission);
            }
            return true;

        case CLEAR:
            //Cancel if the Player does not have permission to use the command
            if (RegionOwn.hasPermission(player, "tools")) {
                if (RegionSelector.isSelecting(player)) {
                    RegionSelector.endSelection(player);
                }

                if (!RegionSelector.hasSelection(player)) {
                    player.sendMessage("§4You do not have a Region Selected");
                    return true;
                }

                Region region = RegionSelector.getSelection(player);
                region.saveUndoSnapshot();

                World world = player.getWorld();
                int index = 0;
                int amount = 0;

                EnumSet<Material> materialSet;
                switch (args.length) {
                case 1:
                    materialSet = EnumSet.copyOf(clearIDs);
                    break;

                case 2:
                    if (args[1].equals("all")) {
                        materialSet = EnumSet.allOf(Material.class);
                        break;
                    }
                    //Fall through
                default:
                    materialSet = EnumSet.noneOf(Material.class);
                    for (int i = 1; i < args.length; i++) {
                        Material material = Material.matchMaterial(args[i]);
                        if (material != null) {
                            materialSet.add(material);
                        }
                    }
                }

                for (int x = region.x1; x <= region.x2; x++) {
                    for (int z = region.z1; z <= region.z2; z++) {
                        if (region.bitSet.get(index)) {
                            for (int y = region.y1; y <= region.y2; y++) {
                                Block block = world.getBlockAt(x, y, z);
                                if (materialSet.contains(block.getType())) {
                                    block.setTypeId(0);
                                    amount++;
                                }
                            }
                        }
                        index++;
                    }
                }
                player.sendMessage("§6" + amount + "§5 Blocks have been cleared");
            } else {
                player.sendMessage(RegionOwnMessages.permission);
            }
            return true;

        case REPLACE:
            if (args.length != 3) {
                sendToolsHelp(player);
            } else {
                Material newMaterial = Material.matchMaterial(args[2]);
                if (newMaterial == null) {
                    player.sendMessage("§6" + args[2] + "§4 is not a valid §6Material");
                    return true;
                }

                EnumSet<Material> toReplace;
                if (args[1].equals("all")) {
                    toReplace = EnumSet.allOf(Material.class);
                } else {
                    toReplace = EnumSet.noneOf(Material.class);
                    for (String string: args[1].split(",")) {
                        Material material = Material.matchMaterial(string);
                        if (material == null) {
                            player.sendMessage("§6" + string + "§4 is not a valid §6Material");
                            return true;
                        }
                        toReplace.add(material);
                    }
                }

                //Cancel if the Player does not have permission to use the command
                if (RegionOwn.hasPermission(player, "tools")) {
                    if (RegionSelector.isSelecting(player)) {
                        RegionSelector.endSelection(player);
                    }

                    if (!RegionSelector.hasSelection(player)) {
                        player.sendMessage("§4You do not have a Region Selected");
                        return true;
                    }

                    Region region = RegionSelector.getSelection(player);
                    region.saveUndoSnapshot();

                    World world = player.getWorld();
                    int index = 0;
                    int amount = 0;
                    for (int x = region.x1; x <= region.x2; x++) {
                        for (int z = region.z1; z <= region.z2; z++) {
                            if (region.bitSet.get(index)) {
                                for (int y = region.y1; y <= region.y2; y++) {
                                    Block block = world.getBlockAt(x, y, z);
                                    if (toReplace.contains(block.getType())) {
                                        block.setType(newMaterial);
                                        amount++;
                                    }
                                }
                            }
                            index++;
                        }
                    }
                    player.sendMessage("§6" + amount + "§5 Blocks have been changed to §6" + newMaterial.name());
                } else {
                    player.sendMessage(RegionOwnMessages.permission);
                }
            }
            return true;

        case GROW:
            //Cancel if the Player does not have permission to use the command
            if (RegionOwn.hasPermission(player, "tools")) {
                if (RegionSelector.isSelecting(player)) {
                    RegionSelector.endSelection(player);
                }

                if (!RegionSelector.hasSelection(player)) {
                    player.sendMessage("§4You do not have a Region Selected");
                    return true;
                }

                Region region = RegionSelector.getSelection(player);
                region.saveUndoSnapshot();

                World world = player.getWorld();
                int index = 0;
                int amount = 0;

                for (int x = region.x1; x <= region.x2; x++) {
                    for (int z = region.z1; z <= region.z2; z++) {
                        if (region.bitSet.get(index)) {
                            Block block = world.getHighestBlockAt(x, z);
                            while (block.getType().isOccluding()) {
                                block = block.getRelative(BlockFace.DOWN);
                            }

                            //Change DIRT to GRASS_BLOCK
                            if (block.getTypeId() == 3) {
                                block.setTypeId(2);
                                amount++;
                            }
                        }
                        index++;
                    }
                }
                player.sendMessage("§6" + amount + "§5 Blocks have been set to Grass");
            } else {
                player.sendMessage(RegionOwnMessages.permission);
            }
            return true;

        case FILL:
            //Cancel if the Player does not have permission to use the command
            if (RegionOwn.hasPermission(player, "tools")) {
                if (RegionSelector.isSelecting(player)) {
                    RegionSelector.endSelection(player);
                }

                if (!RegionSelector.hasSelection(player)) {
                    player.sendMessage("§4You do not have a Region Selected");
                    return true;
                }

                Region region = RegionSelector.getSelection(player);
                region.saveUndoSnapshot();

                World world = player.getWorld();
                int index = 0;
                int amount = 0;

                switch (args.length) {
                case 1:
                    for (int x = region.x1; x <= region.x2; x++) {
                        for (int z = region.z1; z <= region.z2; z++) {
                            if (region.bitSet.get(index)) {
                                Block block = world.getBlockAt(x, region.y1, z);
                                if (block.getTypeId() == 0) {
                                    block.setTypeId(8);
                                    amount++;
                                }
                            }
                            index++;
                        }
                    }
                    player.sendMessage("§6" + amount + "§5 Blocks have been set to Water");
                    break;

                case 2:
                    Material material = Material.matchMaterial(args[1]);
                    if (material == null) {
                        player.sendMessage("§6" + args[1] + "§5 is not a valid Material");
                        return true;
                    }

                    for (int x = region.x1; x <= region.x2; x++) {
                        for (int z = region.z1; z <= region.z2; z++) {
                            if (region.bitSet.get(index)) {
                                for (int y = region.y1; y <= region.y2; y++){
                                    world.getBlockAt(x, y, z).setType(material);
                                    amount++;
                                }
                            }
                            index++;
                        }
                    }
                    player.sendMessage("§6" + amount + "§5 Blocks have been set to §6" + material.name());
                    return true;

                default:
                    sendHelp(player);
                    return true;
                }
            } else {
                player.sendMessage(RegionOwnMessages.permission);
            }
            return true;


        /* Region Backup */
        case BACKUP:
            if (RegionOwn.hasPermission(player, "backup")) {
                Region region;
                switch (args.length) {
                case 2:
                    region = RegionSelector.getSelection(player);
                    if (region == null) {
                        region = RegionOwn.findRegion(player.getLocation());
                    }
                    if (region == null) {
                        player.sendMessage("§4You must be standing in a Region, or include the §6RegionName");
                    } else {
                        if (region.saveSnapshot(args[1])) {
                            player.sendMessage("§6" + region.size() + "§5 Blocks have been Saved");
                        } else {
                            player.sendMessage("§4Saving of region §6" + region.name + "§4 has failed (More details printed to console)");
                        }
                    }
                    return true;

                case 3:
                    region = RegionOwn.findRegion(args[1]);
                    if (region == null) {
                        player.sendMessage(RegionOwnMessages.regionNotFound.replace("<name>", args[1]));
                    } else {
                        if (region.saveSnapshot(args[2])) {
                            player.sendMessage("§6" + region.size() + "§5 Blocks have been Saved");
                        } else {
                            player.sendMessage("§4Saving of region §6" + region.name + "§4 has failed (More details printed to console)");
                        }
                    }
                    return true;

                default:
                    sendHelp(player);
                    return true;
                }
            } else {
                player.sendMessage(RegionOwnMessages.permission);
            }
            return true;

        case REVERT:
            if (RegionOwn.hasPermission(player, "revert")) {
                Region region;
                switch (args.length) {
                case 2:
                    region = RegionOwn.findRegion(player.getLocation());
                    if (region == null) {
                        player.sendMessage("§4You must be standing in a Region or include the §6RegionName");
                    } else {
                        if (region.revert(args[1])) {
                            player.sendMessage("§6" + region.size() + "§5 Blocks have been Reverted");
                        } else {
                            player.sendMessage("§4Reverting of region has failed (More details printed to console)");
                        }
                    }
                    return true;

                case 3:
                    region = RegionOwn.findRegion(args[1]);
                    if (region == null) {
                        player.sendMessage(RegionOwnMessages.regionNotFound.replace("<name>", args[1]));
                    } else {
                        if (region.revert(args[2])) {
                            player.sendMessage("§6" + region.size() + "§5 Blocks have been Reverted");
                        } else {
                            player.sendMessage("§4Reverting of region §6" + region.name + "§4 has failed (More details printed to console)");
                        }
                    }
                    return true;

                default:
                    sendHelp(player);
                    return true;
                }
            } else {
                player.sendMessage(RegionOwnMessages.permission);
            }
            return true;

        case UNDO:
            if (RegionOwn.hasPermission(player, "revert")) {
                Region region;
                switch (args.length) {
                case 1:
                    region = RegionSelector.getSelection(player);
                    if (region == null) {
                        player.sendMessage("§4There is no memory of a recently modified Region");
                    } else {
                        if (region.undo()) {
                            player.sendMessage("§6" + region.size() + "§5 Blocks have been Reverted");
                        } else {
                            player.sendMessage("§4Reverting of region has failed (More details printed to console)");
                        }
                    }
                    return true;

                case 2:
                    region = RegionOwn.findRegion(args[1]);
                    if (region == null) {
                        player.sendMessage(RegionOwnMessages.regionNotFound.replace("<name>", args[1]));
                    } else {
                        if (region.undo()) {
                            player.sendMessage("§6" + region.size() + "§5 Blocks have been Reverted");
                        } else {
                            player.sendMessage("§4Reverting of region §6"+region.name+"§4 has failed (More details printed to console)");
                        }
                    }
                    return true;

                default:
                    sendHelp(player);
                    return true;
                }
            } else {
                player.sendMessage(RegionOwnMessages.permission);
            }
            return true;


        /* Owned Regions */
        case BUY:
            switch (args.length) {
            case 1:
                player.sendMessage("§4Please include a §6name for you new Region");
                return true;

            case 2:
                try {
                    buyAddOn(player, null, AddOn.valueOf(args[1].toUpperCase()));
                } catch (IllegalArgumentException notAddOn) {
                    buy(player, args[1]);
                }
                return true;

            case 3:
                try {
                    sellAddOn(player, args[1], AddOn.valueOf(args[2].toUpperCase()));
                } catch (IllegalArgumentException notAddOn) {
                    player.sendMessage("§6" + args[2] + "§4 is not a valid Add-on");
                }
                return true;

            default: break;
            }

        case SELL:
            switch (args.length) {
            case 1:
                sell(player, null);
                return true;

            case 2:
                try {
                    sellAddOn(player, null, AddOn.valueOf(args[1].toUpperCase()));
                } catch (IllegalArgumentException notAddOn) {
                    sell(player, args[1]);
                }
                return true;

            case 3:
                try {
                    sellAddOn(player, args[1], AddOn.valueOf(args[2].toUpperCase()));
                } catch (IllegalArgumentException notAddOn) {
                    player.sendMessage("§6" + args[2] + "§4 is not a valid Add-on");
                }
                return true;

            default: break;
            }

        case SELLALL:
            sellAll(player);
            return true;

        case LIST:
            switch (args.length) {
            case 1:
                if (!RegionOwn.hasPermission(player, "own")) {
                    player.sendMessage(RegionOwnMessages.permission);
                } else {
                    for (Region owned: RegionOwn.getOwnedRegions(player.getName())) {
                        player.sendMessage(owned.toString());
                    }
                }
                return true;

            case 2:
                if (args[1].equals("addons")) {
                    listAddOns(player, null);
                } else {
                    sendAddOnHelp(player);
                }
                return true;

            case 3:
                if (args[1].equals("addons")) {
                    listAddOns(player, args[2]);
                } else {
                    sendAddOnHelp(player);
                }
                return true;

            default: break;
            }

        case INFO:
            if (args.length == 2) {
                info(player, args[1]);
            }
            else {
                sendHelp(player);
            }
            return true;

        case COOWNER:
            switch (args.length) {
            case 4:
                regionCoowner(player,  null, args[2], args[1], args[3]);
                return true;

            case 5:
                if (args[1].equals("all")) {
                    coowner(player, args[3], args[2], args[4]);
                } else {
                    regionCoowner(player,  args[1], args[3], args[2], args[4]);
                }
                return true;

            default: break;
            }

        case HELP:
            if (args.length == 2) {
                if (args[1].equals("addons")) {
                    sendAddOnHelp(player);
                } else if (args[1].equals("modify")) {
                    sendModifyHelp(player);
                } else if (args[1].equals("tools")) {
                    sendToolsHelp(player);
                } else {
                    break;
                }
            } else {
                break;
            }
            return true;

        default: break;
        }

        sendHelp(player);
        return true;
    }

    /**
     * Adds the selected RegionOwn Region as a Mob Region
     *
     * @param player The Player who has a Region selected
     * @param name The name of the Mob Region
     */
    public static void addMobRegion(Player player, String name) {
        if (RegionSelector.isSelecting(player)) {
            RegionSelector.endSelection(player);
        }

        if (!RegionSelector.hasSelection(player)) {
            player.sendMessage("You must first select a Region");
            return;
        }

        Region region = RegionSelector.getSelection(player);
        region.name = name;
        region.setOwner("PhatLoots");

        RegionOwn.mobRegions.put(name, region);
        player.sendMessage("§6" + name + "§5 has been removed as a Mob Region");
        region.save();
    }

    /**
     * Removes the specified Mob Region
     *
     * @param player The Player removing the Region
     * @param name The name of the Mob Region
     */
    public static void removeMobRegion(Player player, String name) {
        RegionOwn.removeRegion(RegionOwn.mobRegions.remove(name));
        player.sendMessage("§6" + name + "§5 has been removed as a Mob Region");
    }

    /**
     * Lists all Mob Regions
     *
     * @param player The Player to send the list to
     */
    public static void listMobRegions(Player player) {
        String list = "§5Current Mob Regions: §6";

        //Concat each PhatLoot
        for (String string : RegionOwn.mobRegions.keySet()) {
            list += string + "§0, ";
        }

        player.sendMessage(list.substring(0, list.length() - 2));
    }

    /**
     * Gives ownership of the selected Region to the Player
     * Regions are trimmed if they overlap an existing OwnedRegion
     *
     * @param player The Player buying the Region
     */
    public static void buy(Player player, String regionName) {
        //Cancel if the Player does not have permission to use the command
        if (!RegionOwn.hasPermission(player, "own")) {
            player.sendMessage(RegionOwnMessages.permission);
            return;
        }

        if (RegionOwn.findRegion(regionName) != null) {
            player.sendMessage("Region "+regionName+" already exists");
            return;
        }

        if (RegionSelector.isSelecting(player)) {
            RegionSelector.endSelection(player);
        }

        if (!RegionSelector.hasSelection(player)) {
            player.sendMessage("§4You do not have a Region Selected so no purchase was made");
            return;
        }

        Region region = RegionSelector.getSelection(player);
        if (region.owner != null) {
            player.sendMessage("§4The selected Region is already owned");
            return;
        }

        int pretrim = region.size();
        if (region.trim()) {
            player.sendMessage("§4The selection overlaps with a preexisting Region, Your Selection has been trimmed from §6"
                    + pretrim + "§4 Blocks to §6" + region.size() + "§4 Blocks to allow purchase");
            player.sendMessage("§4If you still wish to purchase the Region please repeat the buy command");
            return;
        }

        region.name = regionName;
        region.setOwner(player.getName());

        //Charge the Player only if they don't have the 'regionown.free' node
        if (!Econ.buy(player, region)) {
            return;
        }

        RegionOwn.addRegion(region);
    }

    /**
     * Gives the Add-on to the Region
     *
     * @param player The Player buying the Add-on
     */
    public static void buyAddOn(Player player, String regionName, AddOn addOn) {
        Region region = regionName == null ? RegionOwn.findRegion(player.getLocation()) : RegionOwn.findRegion(regionName);
        if (region == null) {
            player.sendMessage(RegionOwnMessages.regionNotFound.replace("<name>", regionName));
            return;
        }

        //Cancel if the Player does not have permission to buy the Add-on
        if (!RegionOwn.hasPermission(player, addOn)) {
            player.sendMessage(RegionOwnMessages.permission);
            return;
        }

        //Cancel if the Player already has the Add-on
        if (region.hasAddOn(addOn)) {
            player.sendMessage("§4You already have that Add-on enabled");
            return;
        }

        //Cancel if the Player could not afford the transaction
        if (!Econ.charge(player, Econ.getBuyPrice(addOn))) {
            return;
        }

        region.setAddOn(player, addOn, true);
        region.save();
    }

    /**
     * Removes the given Add-on from the Region
     *
     * @param player The Player selling the Add-on
     */
    public static void sellAddOn(Player player, String regionName, AddOn addOn) {
        Region region = regionName == null ? RegionOwn.findRegion(player.getLocation()) : RegionOwn.findRegion(regionName);
        if (region == null) {
            player.sendMessage(RegionOwnMessages.regionNotFound.replace("<name>", regionName));
            return;
        }

        //Cancel if the Player already has the Add-on
        if (!region.hasAddOn(addOn)) {
            player.sendMessage("§4You already have that Add-on disabled");
            return;
        }

        Econ.refund(player, Econ.getSellPrice(addOn));
        region.setAddOn(player, addOn, false);
        region.save();
    }

    /**
     * Removes ownership of the specified Region
     *
     * @param player The Player selling the Region
     */
    public static void sell(Player player, String regionName) {
        //Cancel if the Player does not have permission to use the command
        if (!RegionOwn.hasPermission(player, "own")) {
            player.sendMessage(RegionOwnMessages.permission);
            return;
        }

        Region region = regionName == null ? RegionOwn.findRegion(player.getLocation()) : RegionOwn.findRegion(regionName);
        if (region == null) {
            player.sendMessage(RegionOwnMessages.regionNotFound.replace("<name>", regionName));
            return;
        }

        String name = player.getName();

        //Allow Admins to sell a Region owned by someone else
        if (!region.isOwner(name)) {
            if (RegionOwn.hasPermission(player, "admin")) {
                Econ.sell(player, name, region);
            } else {
                player.sendMessage(RegionOwnMessages.doNotOwn);
                return;
            }
        } else {
            Econ.sell(player, region);
        }

        RegionOwn.removeRegion(region);
    }

    /**
     * Display to the Player all of the Add-ons for the Region
     *
     * @param player The Player requesting the list
     */
    public static void listAddOns(Player player, String regionName) {
        Region region = regionName == null ? RegionOwn.findRegion(player.getLocation()) : RegionOwn.findRegion(regionName);
        if (region == null) {
            player.sendMessage(RegionOwnMessages.regionNotFound.replace("<name>", regionName));
            return;
        }

        String list = "§5Enabled Add-ons§f: ";
        if (Econ.blockPvP != -2 && region.blockPvP) {
            list = list.concat("§6BlockPvP§f, ");
        }
        if (Econ.blockPvE != -2 && region.blockPvE) {
            list = list.concat("§6BlockPvE§f, ");
        }
        if (Econ.blockExplosions != -2 && region.blockExplosions) {
            list = list.concat("§6BlockExplosions§f, ");
        }
        if (Econ.lockChests != -2 && region.lockChests) {
            list = list.concat("§6LockChests§f, ");
        }
        if (Econ.lockDoors != -2 && region.lockDoors) {
            list = list.concat("§6LockDoors§f, ");
        }
        if (Econ.disableButtons != -2 && region.disableButtons) {
            list = list.concat("§6DisableButtons§f, ");
        }
        if (Econ.disablePistons != -2 && region.disablePistons) {
            list = list.concat("§6DisablePistons§f, ");
        }
        if (Econ.alarm != -2 && region.alarm) {
            list = list.concat("§6AlarmSystem§f, ");
        }
        if (Econ.heal != -2 && region.heal) {
            list = list.concat("§6Heal§f, ");
        }
        if (Econ.feed != -2 && region.feed) {
            list = list.concat("§6Feed§f, ");
        }

        player.sendMessage(list.substring(0, list.length()-2));
    }

    /**
     * Display to the Player the info of the current Region
     * Info displayed is the Location, Owner, size, and current Co-owners
     *
     * @param player The Player requesting the info
     */
    public static void info(Player player, String regionName) {
        //Cancel if the Player does not have permission to use the command
        if (!RegionOwn.hasPermission(player, "info")) {
            player.sendMessage(RegionOwnMessages.permission);
            return;
        }

        Region region = regionName == null ? RegionOwn.findRegion(player.getLocation()) : RegionOwn.findRegion(regionName);
        if (region == null) {
            player.sendMessage(RegionOwnMessages.regionNotFound.replace("<name>", regionName));
            return;
        }

        //Display the world and coordinates of the Region to the Player
        player.sendMessage("§5Region §6" + region.toString() + "§5 belongs to §6"
                + (region.owner == null ? "noone" : region.owner.name)
                + "§5 and is §6" + region.size() + "§5 Blocks");

        //Display Co-owners of OwnedChunk to Player
        String coOwners = "§5Co-owners§f:  ";
        for (String coOwner: region.coOwners) {
            coOwners = coOwners.concat("§6" + coOwner + "§f, ");
        }
        player.sendMessage(coOwners.substring(0, coOwners.length() - 2));

        //Display Co-owner Groups of OwnedChunk to Player
        String groups = "§5Co-owner Groups§f:  ";
        for (String group: region.groups) {
            groups = groups.concat("§6" + group + "§f, ");
        }
        player.sendMessage(groups.substring(0, groups.length() - 2));
    }

    /**
     * Manages Co-ownership of the given Region/RegionOwner
     *
     * @param player The given Player who may be the Owner
     * @param regionName The name of the Region
     * @param type The given type: 'player' or 'group'
     * @param action The given action: 'add' or 'remove'
     * @param coOwner The given Co-owner
     */
    public static void regionCoowner(Player player, String regionName, String type, String action, String coOwner) {
        //Cancel if the Player does not have permission to use the command
        if (!RegionOwn.hasPermission(player, "coowner")) {
            player.sendMessage(RegionOwnMessages.permission);
            return;
        }

        Region region = regionName == null ? RegionOwn.findRegion(player.getLocation()) : RegionOwn.findRegion(regionName);
        if (region == null) {
            player.sendMessage(RegionOwnMessages.regionNotFound.replace("<name>", regionName));
            return;
        }

        //Cancel if the OwnedChunk is owned by someone else
        if (!region.isOwner(player.getName())) {
            player.sendMessage(RegionOwnMessages.doNotOwn);
            return;
        }

        //Determine the command to execute
        if (type.equals("player")) {
            if (action.equals("add")) {
                //Cancel if the Player is already a CoOwner
                if (region.coOwners.contains(coOwner)) {
                    player.sendMessage("§6" + coOwner + "§4 is already a Co-owner");
                    return;
                }

                region.coOwners.add(coOwner);
                player.sendMessage("§4" + coOwner + "§5 added as a Co-owner");
            } else if (action.equals("remove")) {
                region.coOwners.remove(coOwner);
            } else {
                sendHelp(player);
                return;
            }
        } else if (type.equals("group")) {
            if (action.equals("add")) {
                //Cancel if the Group is already a CoOwner
                if (region.groups.contains(coOwner)) {
                    player.sendMessage("§6" + coOwner + "§4 is already a Co-owner");
                    return;
                }

                region.groups.add(coOwner);
                player.sendMessage("§6" + coOwner + "§5 added as a Co-owner");
            } else if (action.equals("remove")) {
                region.groups.remove(coOwner);
            } else {
                sendHelp(player);
                return;
            }
        } else {
            sendHelp(player);
            return;
        }

        region.save();
    }

    /**
     * Manages Co-ownership of the given Region/RegionOwner
     *
     * @param player The given Player who may be the Owner
     * @param type The given type: 'player' or 'group'
     * @param action The given action: 'add' or 'remove'
     * @param coOwner The given Co-owner
     */
    public static void coowner(Player player, String type, String action, String coOwner) {
        //Cancel if the Player does not have permission to use the command
        if (!RegionOwn.hasPermission(player, "coowner")) {
            player.sendMessage(RegionOwnMessages.permission);
            return;
        }

        RegionOwner owner = RegionOwn.getOwner(player.getName());

        //Determine the command to execute
        if (type.equals("player")) {
            if (action.equals("add")) {
                //Cancel if the Player is already a CoOwner
                if (owner.coOwners.contains(coOwner)) {
                    player.sendMessage("§6" + coOwner + "§4 is already a Co-owner");
                    return;
                }

                owner.coOwners.add(coOwner);
                player.sendMessage("§6" + coOwner + "§5 added as a Co-owner");
            } else if (action.equals("remove")) {
                owner.coOwners.remove(coOwner);
            } else {
                sendHelp(player);
                return;
            }
        } else if (type.equals("group")) {
            if (action.equals("add")) {
                //Cancel if the Group is already a CoOwner
                if (owner.groups.contains(coOwner)) {
                    player.sendMessage("§6" + coOwner + "§4 is already a Co-owner");
                    return;
                }

                owner.groups.add(coOwner);
                player.sendMessage("§6" + coOwner + "§5 added as a Co-owner");
            } else if (action.equals("remove")) {
                owner.groups.remove(coOwner);
            } else {
                sendHelp(player);
                return;
            }
        } else {
            sendHelp(player);
            return;
        }

        owner.save();
    }

    /**
     * Removes all the Regions that are owned by the given Player
     *
     * @param player The given Player
     */
    public static void sellAll(Player player) {
        sellAll(player, player.getName());
    }

    /**
     * Removes all the Regions that are owned by the given Player
     *
     * @param player The name of the given Player
     */
    public static void sellAll(String player) {
        sellAll(null, player);
    }

    /**
     * Removes all the Region that are owned by the given Player
     *
     * @param player The Admin who is selling the Regions
     * @param player The name of the given Player
     */
    private static void sellAll(Player player, String name) {
        Iterator<Region> itr = RegionOwn.getOwnedRegions(name).iterator();
        LinkedList<Region> toRemove = new LinkedList<Region>();

        while (itr.hasNext()) {
            Region region = itr.next();

            //Sell the Chunk if it is owned by the given Player
            if (region.isOwner(name)) {
                if (player == null) {
                    Econ.sell(name, region);
                } else {
                    Econ.sell(player, region);
                }

                toRemove.add(region);
            }
        }

        for (Region region: toRemove) {
            RegionOwn.removeRegion(region);
        }

        RegionOwner owner = RegionOwn.findOwner(name);
        owner.save();
    }

    /**
     * Displays the RegionOwn Main Help Page to the given Player
     *
     * @param Player The Player needing help
     */
    public static void sendHelp(Player player) {
        player.sendMessage("§e     RegionOwn Help Page:");
        player.sendMessage("§5If the §6Name§5 is not specified, the current Region will be used");
        player.sendMessage("§2/"+command+" help addons §f=§b Display the Region Add-on Help Page");
        player.sendMessage("§2/"+command+" help modify §f=§b Display the Modify Region Help Page");
        if (RegionOwn.hasPermission(player, "tools")) {
            player.sendMessage("§2/"+command+" help tools §f=§b View a list of Region Tools");
        }
        player.sendMessage("§2/"+command+" select §f<§6Name§f> =§b Select the specified Region");
        player.sendMessage("§2/"+command+" select §f=§b Start selection of a new Region");
        if (RegionOwn.hasPermission(player, "selectbiome")) {
            player.sendMessage("§2/"+command+" select biome §f=§b Select the entire Biome you are in");
        }
        player.sendMessage("§2/"+command+" done §f=§b Finish selecting a Region");
        player.sendMessage("§2/"+command+" expand §f=§b Expand the selected Region to your y-coord");
        if (RegionOwn.hasPermission(player, "save")) {
            player.sendMessage("§2/"+command+" save §f<§6Name§f> =§b Save the selected Region");
        }
        if (RegionOwn.hasPermission(player, "mobregion")) {
            player.sendMessage("§2/"+command+" mobregion <add|remove> <RegionName>§b Save the selected Region for region based mob loot");
            player.sendMessage("§2/"+command+" mobregion list§b List all Mob Regions");
        }
        if (RegionOwn.hasPermission(player, "backup")) {
            player.sendMessage("§2/"+command+" backup §f[§6Name§f] <§6File§f> =§b Backup the specified Region");
        }
        if (RegionOwn.hasPermission(player, "revert")) {
            player.sendMessage("§2/"+command+" revert §f[§6Name§f] <§6File§f> =§b Revert the specified Region");
            player.sendMessage("§2/"+command+" undo §f=§b Undo your last modification");
            player.sendMessage("§2/"+command+" undo §f<§6Name§f> =§b Undo the last reversion of the Region");
        }
        player.sendMessage("§2/"+command+" buy §f<§6Name§f> =§b Purchase the selected Region");
        player.sendMessage("§2/"+command+" sell §f[§6Name§f] =§b Sell the specified Region");
        player.sendMessage("§2/"+command+" list §f=§b List the Regions that you own");
        player.sendMessage("§2/"+command+" sellall §f=§b Sell all owned Regions");
    }

    /**
     * Displays the Modify Region Help Page to the given Player
     *
     * @param Player The Player needing help
     */
    public static void sendModifyHelp(Player player) {
        player.sendMessage("§e     Modify Region Help Page:");
        player.sendMessage("§5If the §6Name§5 is not specified, the current Region will be used");
        player.sendMessage("§2/"+command+" help addons §f=§b Display the Region Add-on Help Page");
        player.sendMessage("§2/"+command+" name §f[§6Name§f] §f<§6NewName§f> =§b Rename the specified Region");
        if (RegionOwn.hasPermission(player, "info")) {
            player.sendMessage("§2/"+command+" info §f[§6Name§f] =§b List Owner and Co-owners of the specified Region");
        }
        if (RegionOwn.hasPermission(player, "coowner")) {
            player.sendMessage("§2/"+command+" coowner §f[§6Name§f] <§6Action§f> <§6Type§f> <§6Co-owner§f> =§b Co-owner for specified Region");
            player.sendMessage("§2/"+command+" coowner all §f<§6Action§f> <§6Type§f> <§6Name§f> =§b Co-owner for all Regions");
            player.sendMessage("§6Action§f = 'add§f' or §f'remove§f'");
            player.sendMessage("§6Type§f = 'player§f' or §f'group§f'");
            player.sendMessage("§6Name§f = The group name or the Player's name");
        }
    }

    /**
     * Displays the Region Tools Help Page to the given Player
     *
     * @param Player The Player needing help
     */
    public static void sendToolsHelp(Player player) {
        player.sendMessage("§e     Region Tools Help Page:");
        player.sendMessage("§2/"+command+" biome §f<§6Biome§f> =§b Set the Biome of the selected Region");
        player.sendMessage("§2/"+command+" cut §f=§b Remove all foilage from the selected Region");
        player.sendMessage("§2/"+command+" thaw §f=§b Remove all snow/ice from the selected Region");
        player.sendMessage("§2/"+command+" clear §f[§6Material§f] [§6Material§f] etc. =§b Remove all of the specified Materials");
        player.sendMessage("§2/"+command+" clear all §f=§b Remove all Materials in your selection");
        player.sendMessage("§2/"+command+" replace §f<§6Material,Material,Material etc§f> <§6NewMaterial§f> =§b Replace all of a specified Materials");
        player.sendMessage("§2/"+command+" grow §f=§b Change surface Dirt to Grass");
        player.sendMessage("§2/"+command+" fill §f=§b Fill the bottom layer of the selected Region with water");
        player.sendMessage("§2/"+command+" fill §f<§6Material§f> =§b Fill the selected Region with the specified Material");
        player.sendMessage("§6Material§5 may be the Item ID or it's name");
    }

    /**
     * Displays the Add-on Help Page to the given Player
     *
     * @param Player The Player needing help
     */
    public static void sendAddOnHelp(Player player) {
        player.sendMessage("§e     Region Add-on Help Page:");
        player.sendMessage("§5If the §6RegionName§5 is not specified, the current Region will be used");
        player.sendMessage("§2/"+command+" list addons §f[RegionName§f] =§b List your current add-ons");

        //Display available Add-ons
        if (RegionOwn.hasPermission(player, AddOn.BLOCKPVP)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 blockpvp §f=§b No damage from Players: §6"+Econ.format(Econ.getBuyPrice(AddOn.BLOCKPVP)));
        }
        if (RegionOwn.hasPermission(player, AddOn.BLOCKPVE)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 blockpve §f=§b No damage from Mobs: §6"+Econ.format(Econ.getBuyPrice(AddOn.BLOCKPVE)));
        }
        if (RegionOwn.hasPermission(player, AddOn.BLOCKEXPLOSIONS)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 blockexplosions §f=§b No TNT/Creeper griefing: §6"+Econ.format(Econ.getBuyPrice(AddOn.BLOCKEXPLOSIONS)));
        }
        if (RegionOwn.hasPermission(player, AddOn.LOCKCHESTS)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 lockchests §f=§b Non-Owners can't open Chests/Furnaces/Dispensers: §6"+Econ.format(Econ.getBuyPrice(AddOn.LOCKCHESTS)));
        }
        if (RegionOwn.hasPermission(player, AddOn.LOCKDOORS)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 lockdoors §f=§b Non-Owners can't open Doors: §6"+Econ.format(Econ.getBuyPrice(AddOn.LOCKDOORS)));
        }
        if (RegionOwn.hasPermission(player, AddOn.DISABLEBUTTONS)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 disablebuttons §f=§b Non-Owners can't use Buttons/Levers/Plates: §6"+Econ.format(Econ.getBuyPrice(AddOn.DISABLEBUTTONS)));
        }
        if (RegionOwn.hasPermission(player, AddOn.DENYACCESS)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 denyaccess §f=§b Players cannot enter your land: §6"+Econ.format(Econ.getBuyPrice(AddOn.DENYACCESS)));
        }
        if (RegionOwn.hasPermission(player, AddOn.DISABLEPISTONS)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 disablepistons §f=§b Pistons will no longer function: §6"+Econ.format(Econ.getBuyPrice(AddOn.DISABLEPISTONS)));
        }
        if (RegionOwn.hasPermission(player, AddOn.ALARM)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 alarm §f=§b Be alerted when a Player enters your land: §6"+Econ.format(Econ.getBuyPrice(AddOn.ALARM)));
        }
        if (RegionOwn.hasPermission(player, AddOn.HEAL)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 heal §f=§b Players gain half a heart every §6"+RegionOwnMovementListener.rate+"§b seconds: §6"+Econ.format(Econ.getBuyPrice(AddOn.HEAL)));
        }
        if (RegionOwn.hasPermission(player, AddOn.FEED)) {
            player.sendMessage("§2/"+command+" buy §f[§6RegionName§f]§2 feed §f=§b Players gain half a food every §6"+RegionOwnMovementListener.rate+"§b seconds: §6"+Econ.format(Econ.getBuyPrice(AddOn.FEED)));
        }

        player.sendMessage("§2/"+command+" sell §f[§6RegionName§f] <§6addon§f> =§b Sell an addon for §6"+Econ.moneyBack+"%§b of its buy price");
    }
}