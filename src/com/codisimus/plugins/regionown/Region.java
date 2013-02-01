package com.codisimus.plugins.regionown;

import java.awt.Polygon;
import java.io.*;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedList;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

/**
 * A Region is an area of land that is defined by an outlining Polygon.
 * A Region is bounded by 2 y-coordinates
 * If Owner is null then the Region is only used for backing up and reverting
 *
 * @author Codisimus
 */
public class Region {
    public static enum AddOn {
        BLOCKPVP, BLOCKPVE, BLOCKEXPLOSIONS, LOCKCHESTS, LOCKDOORS,
        DISABLEBUTTONS, DISABLEPISTONS, DENYACCESS, ALARM, HEAL, FEED
    }

    public String world;
    public BitSet bitSet = new BitSet();

    public RegionOwner owner;
    public LinkedList<String> coOwners = new LinkedList<String>();
    public LinkedList<String> groups = new LinkedList<String>();

    public String name;
    public String welcomeMessage = "Welcome to <name>! Owner: <owner>";
    public String leavingMessage = "Thanks for visiting!";

    public boolean blockPvP = Econ.blockPvP == -1;
    public boolean blockPvE = Econ.blockPvE == -1;
    public boolean blockExplosions = Econ.blockExplosions == -1;
    public boolean lockChests = Econ.lockChests == -1;
    public boolean lockDoors = Econ.lockDoors == -1;
    public boolean disableButtons = Econ.disableButtons == -1;
    public boolean disablePistons = Econ.disablePistons == -1;
    public boolean denyAccess = Econ.denyAccess == -1;
    public boolean alarm = Econ.alarm == -1;
    public boolean heal = Econ.heal == -1;
    public boolean feed = Econ.feed == -1;

    int x1, z1, x2, z2, y1, y2;
    int height;
    private int width;
    private int length;

    /**
     * Creates a new Region from the given data (for use from a save file)
     *
     * @param world The World that the Region is in
     * @param coords An Array of int in the order x1,z1,x2,z2,y1,y2
     * @param bitSet The BitSet of included Blocks
     * @param owner The name of the RegionOwner
     */
    public Region(String world, int[] coords, BitSet bitSet, String owner) {
        this.world = world;
        this.bitSet = bitSet;

        x1 = coords[0];
        z1 = coords[1];
        x2 = coords[2];
        z2 = coords[3];
        y1 = coords[4];
        y2 = coords[5];

        width = x2 - x1 + 1;
        length = z2 - z1 + 1;
        height = y2 - y1 + 1;

        if (!owner.equals("noone")) {
            setOwner(owner);
        }
    }

    /**
     * Creates a new Region from the given data (for use from a save file)
     *
     * @param coords An Array of int in the order x1,z1,x2,z2,y1,y2
     * @param bitSet The BitSet of included Blocks
     */
    public Region(String name, int[] coords, BitSet bitSet) {
        this.name = name;
        this.bitSet = bitSet;

        x1 = coords[0];
        z1 = coords[1];
        x2 = coords[2];
        z2 = coords[3];
        y1 = coords[4];
        y2 = coords[5];

        width = x2 - x1 + 1;
        length = z2 - z1 + 1;
        height = y2 - y1 + 1;
    }

    /**
     * Creates a Region from the given Cubiod corners
     *
     * @param a One corner of the Cubiod
     * @param b The other corner of the Cubiod
     */
    public Region(Block a, Block b) {
        world = a.getWorld().getName();

        x1 = Math.min(a.getX(), b.getX());
        z1 = Math.min(a.getZ(), b.getZ());
        x2 = Math.max(a.getX(), b.getX());
        z2 = Math.max(a.getZ(), b.getZ());
        y1 = Math.min(a.getY(), b.getY());
        y2 = Math.max(a.getY(), b.getY());

        width = x2 - x1 + 1;
        length = z2 - z1 + 1;
        height = y2 - y1 + 1;

        //Fill the entire BitSet with true
        bitSet.set(0, (width * length), true);
    }

    /**
     * Creates a Region from the given Region corners that form a Polygon
     *
     * @param blocks An List of Blocks (The first and last Blocks may match)
     */
    public Region(LinkedList<Block> blocks) {
        Block firstBlock = blocks.getFirst();
        world = firstBlock.getWorld().getName();

        x1 = x2 = firstBlock.getX();
        z1 = z2 = firstBlock.getZ();
        y1 = y2 = firstBlock.getY();

        Polygon polygon = new Polygon();

        for (Block block: blocks) {
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();

            if (x < x1) {
                x1 = x;
            }
            if (x > x2) {
                x2 = x;
            }
            if (y < y1) {
                y1 = y;
            }
            if (y > y2) {
                y2 = y;
            }
            if (z < z1) {
                z1 = z;
            }
            if (z > z2) {
                z2 = z;
            }

            //Construct the Polygon
            polygon.addPoint(x, z);
        }

        width = x2 - x1 + 1;
        length = z2 - z1 + 1;
        height = y2 - y1 + 1;

        //Close the Polygon
        polygon.addPoint(firstBlock.getX(), firstBlock.getZ());
        polygon.invalidate();

        //Construct the BitSet for the Polygon
        int index = 0;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (polygon.contains(x, z) || polygon.contains(x - 1, z)
                        || polygon.contains(x, z - 1)
                        || polygon.contains(x - 1, z - 1)) {
                    bitSet.set(index, true);
                }
                index++;
            }
        }
    }

    /**
     * Creates a Region from the given Blocks that the Region will contain
     *
     * @param blocks A set of Blocks
     */
    public Region(HashSet<Block> blockSet) {
        LinkedList<Block> blocks = new LinkedList(blockSet);
        Block firstBlock = blocks.getFirst();
        World w = firstBlock.getWorld();
        world = w.getName();

        x1 = x2 = firstBlock.getX();
        z1 = z2 = firstBlock.getZ();
        y1 = y2 = firstBlock.getY();

        for (Block block: blocks) {
            int x = block.getX();
            int y = block.getY();
            int z = block.getZ();

            if (x < x1) {
                x1 = x;
            }
            if (x > x2) {
                x2 = x;
            }
            if (y < y1) {
                y1 = y;
            }
            if (y > y2) {
                y2 = y;
            }
            if (z < z1) {
                z1 = z;
            }
            if (z > z2) {
                z2 = z;
            }
        }

        width = x2 - x1 + 1;
        length = z2 - z1 + 1;
        height = y2 - y1 + 1;

        //Construct the BitSet for the Polygon
        int index = 0;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (blockSet.contains(w.getBlockAt(x, y1, z))) {
                    bitSet.set(index, true);
                }
                index++;
            }
        }
    }

    /**
     * Sets the RegionOwner and increments their blockCounter by 1
     *
     * @param player The name of the owner of the Chunk
     */
    public void setOwner(String player) {
        owner = RegionOwn.getOwner(player);
        owner.blockCounter = owner.blockCounter + size();
    }

    /**
     * Trims the Region so it does not interfere with pre-existing Regions
     *
     * @return True if this Region was trimmed
     */
    public boolean trim() {
        boolean trimmed = false;
        World w = RegionOwn.server.getWorld(world);

        for (Region region: RegionOwn.getRegions()) {
            if ((x1 >= region.x1 && x1 <= region.x2 || x2 >= region.x1 && x2 <= region.x2) || (region.x1 >= x1 && region.x1 <= x2 || region.x2 >= x1 && region.x2 <= x2)) { /* if x1 or x2 is within the x range of the Region */
                if ((y1 >= region.y1 && y1 <= region.y2 || y2 >= region.y1 && y2 <= region.y2) || (region.y1 >= y1 && region.y1 <= y2 || region.y2 >= y1 && region.y2 <= y2)) { /* && if y1 or y2 is within the y range of the Region */
                    if ((z1 >= region.z1 && z1 <= region.z2 || z2 >= region.z1 && z2 <= region.z2) || (region.z1 >= z1 && region.z1 <= z2 || region.z2 >= z1 && region.z2 <= z2)) { /* && if z1 or z2 is within the z range of the Region */
                        if (world.equals(region.world)) {
                            int index = 0;
                            for (int x = x1; x <= x2; x++) {
                                for (int z = z1; z <= z2; z++) {
                                    if (bitSet.get(index)) {
                                        for (int y = y1; y <= y2; y++) {
                                            if (region.contains(w.getBlockAt(x, y, z).getLocation())) {
                                                bitSet.flip(index);
                                                trimmed = true;
                                                break;
                                            }
                                        }
                                    }
                                    index++;
                                }
                            }
                        }
                    }
                }
            }
        }

        return trimmed;
    }

    /**
     * Returns the amount of Blocks that the Region includes
     *
     * @return The size of the Region
     */
    public int size() {
        return bitSet.cardinality() * height;
    }

    /**
     * Returns whether the given player is the Owner of this Region
     *
     * @param player The Player to be check for ownership
     * @return true if the given player is the Owner of this Region
     */
    public boolean isOwner(String player) {
        return owner == null ? false : owner.name.equals(player);
    }

    /**
     * Returns whether the given player is a Co-owner
     * Co-owner includes being in a group that has Co-ownership
     * Co-owners include Co-Owners of the RegionOwner
     * Returns true if the Player is the main Owner or an Admin
     *
     * @param player The Player to be check for Co-ownership
     * @return true if the given player is a Co-owner
     */
    public boolean isCoOwner(Player player) {
        String playerName = player.getName();

        //Check if the Player is the RegionOwner
        if (isOwner(playerName)) {
            return true;
        }

        //Check if the Player is an Admin
        if (RegionOwn.hasPermission(player, "admin")) {
            return true;
        }

        //Check to see if the Player is a Co-owner
        for (String coOwner: owner.coOwners) {
            if (coOwner.equalsIgnoreCase(playerName)) {
                return true;
            }
        }

        //Return true if the Player is in a group that has Co-ownership
        for (String group: owner.groups) {
            if (RegionOwn.permission.playerInGroup(player, group)) {
                return true;
            }
        }

        //Check to see if the Player is a Co-owner
        for (String coOwner: coOwners) {
            if (coOwner.equalsIgnoreCase(playerName)) {
                return true;
            }
        }

        //Return true if the Player is in a group that has Co-ownership
        for (String group: groups) {
            if (RegionOwn.permission.playerInGroup(player, group)) {
                return true;
            }
        }

        //Return false because the Player is not a coowner
        return false;
    }

    /**
     * Returns true if the given Location is within this Region
     *
     * @param location The given Location (Player or Block)
     * @return True if the given Location is within this Region
     */
    public boolean contains(Location location) {
        int x = location.getBlockX();
        if (x < x1 || x > x2) {
            return false;
        }

        int z = location.getBlockZ();
        if (z < z1 || z > z2) {
            return false;
        }

        int y = location.getBlockY();
        if (y < y1 || y > y2) {
            return false;
        }

        int index = (length * (x - x1)) + (z - z1);
        if (!bitSet.get(index)) {
            return false;
        }

        String w = location.getBlock().getWorld().getName();
        return world.equals(w);
    }

    /**
     * Returns a List of all Blocks within this Region
     *
     * @return A List of all Blocks within this Region
     */
    public LinkedList<Block> getBlocks() {
        LinkedList<Block> blockList = new LinkedList<Block>();

        World w = RegionOwn.server.getWorld(world);

        int index = 0;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (bitSet.get(index)) {
                    for (int y = y1; y <= y2; y++) {
                        blockList.add(w.getBlockAt(x, y, z));
                    }
                }
                index++;
            }
        }

        return blockList;
    }

    /**
     * Returns a List of the top layer of Blocks for this Region
     *
     * @return A List of the top layer of Blocks for this Region
     */
    public LinkedList<Block> getLayerOfBlocks() {
        LinkedList<Block> blockList = new LinkedList<Block>();

        World w = RegionOwn.server.getWorld(world);

        int index = 0;
        for (int x = x1; x <= x2; x++) {
            for (int z = z1; z <= z2; z++) {
                if (bitSet.get(index)) {
                    blockList.add(w.getBlockAt(x, y2, z));
                }
                index++;
            }
        }

        return blockList;
    }

    /**
     * Returns true if the Region has the given AddOn enabled
     *
     * @param addOn The given AddOn
     * @return True if the Region has the given AddOn enabled
     */
    public boolean hasAddOn(AddOn addOn) {
        switch (addOn) {
        case BLOCKPVP: return blockPvP;
        case BLOCKPVE: return blockPvE;
        case BLOCKEXPLOSIONS: return blockExplosions;
        case LOCKCHESTS: return lockChests;
        case LOCKDOORS: return lockDoors;
        case DISABLEBUTTONS: return disableButtons;
        case DISABLEPISTONS: return disablePistons;
        case DENYACCESS: return denyAccess;
        case ALARM: return alarm;
        case HEAL: return heal;
        case FEED: return feed;
        default: return false;
        }
    }

    /**
     * Sets the status of the given AddOn
     *
     * @param player The Player setting the AddOn (may be null)
     * @param addOn The given AddOn
     * @param on The new status of the AddOn
     */
    public void setAddOn(Player player, AddOn addOn, boolean on) {
        switch (addOn) {
        case BLOCKPVP:
            blockPvP = on;
            if (player != null) {
                player.sendMessage("§5Players " + (on ? "can" : "cannot")
                        + " be hurt by other Players while on this property");
            }
            break;

        case BLOCKPVE:
            blockPvE = on;
            if (player != null) {
                player.sendMessage("§5Players " + (on ? "can" : "cannot")
                                + " be hurt by Mobs while on this property");
            }
            break;

        case BLOCKEXPLOSIONS:
            blockExplosions = on;
            if (player != null) {
                player.sendMessage("§5Explosions on this property will "
                                    + (on ? "be" : "not be") + " neutralized");
            }
            break;

        case LOCKCHESTS:
            lockChests = on;
            if (player != null) {
                player.sendMessage("§5Chests/Furnaces/Dispensers on this "
                        + "property will be " + (on ? "locked" : "accessible")
                        + " to non-owners");
            }
            break;

        case LOCKDOORS:
            lockDoors = on;
            if (player != null) {
                player.sendMessage("§5Doors on this property will be "
                        + (on ? "locked" : "accessible") + " to non-owners");
            }
            break;

        case DISABLEBUTTONS:
            disableButtons = on;
            if (player != null) {
                player.sendMessage("§5Buttons/Levers/Plates on this property "
                                    + "will be " + (on ? "disabled" : "enabled")
                                    + " to non-owners");
            }
            break;

        case DISABLEPISTONS:
            disablePistons = on;
            if (player != null) {
                player.sendMessage("§5Pistons on this property will be "
                                    + (on ? "non-functional" : "functional"));
            }
            break;

        case DENYACCESS:
            denyAccess = on;
            if (player != null) {
                player.sendMessage("§Players " + (on ? "will" : " will not")
                                    + " be able to enter this property");
            }
            break;

        case ALARM:
            alarm = on;
            if (player != null) {
                player.sendMessage("§5You will " + (on ? "be" : "not be")
                            + " notified when a Player enters this property");
            }
            break;

        case HEAL:
            heal = on;
            if (player != null) {
                player.sendMessage("§5Players " + (on ? "will" : " will not")
                                    + " gain health while on this property");
            }
            break;

        case FEED:
            feed = on;
            if (player != null) {
                player.sendMessage("§Players " + (on ? "will" : " will not")
                                    + " gain food while on this property");
            }
            break;
        }
    }

    public void saveUndoSnapshot() {
        saveSnapshot("undo");
    }

    public boolean undo() {
        return revert("undo");
    }

    /**
     * Saves a snapshot of the Region to file for reverting later
     *
     * @param fileName The unique File name that the snapshot will be written to
     * @return true if the snapshot was successfully saved
     */
    public boolean saveSnapshot(String fileName) {
        FileOutputStream fos = null;
        try {
            File file = new File(RegionOwn.dataFolder+"/Snapshots/"+name);
            if (!file.exists()) {
                file.mkdir();
            }

            //Return if there is already a snapshot saved for this Region
            file = new File(RegionOwn.dataFolder + "/Snapshots/"+name+"/"
                            + fileName + ".rta");
            if (file.exists() && !fileName.equals("undo")) {
                return false;
            } else {
                file.createNewFile();
            }

            World w = RegionOwn.server.getWorld(world);
            int size = bitSet.length() * height;
            byte[] typeArray = new byte[size];
            byte[] dataArray = new byte[size];

            int i = 0;
            int index = 0;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    if (bitSet.get(index)) {
                        for (int y = y1; y <= y2; y++) {
                            Block block = w.getBlockAt(x, y, z);

                            //Store the Block's type ID
                            byte type = (byte)block.getTypeId();
                            typeArray[i] = type;

                            //Store the data of the Block
                            byte data = block.getData();
                            dataArray[i] = data;

                            i++;
                        }
                    }
                    index++;
                }
            }

            //Save the Type Array to file
            fos = new FileOutputStream(file);
            fos.write(typeArray);
            fos.close();

            //Save the Data Array to file
            file = new File(RegionOwn.dataFolder+"/Snapshots/"+name+"/"+fileName+".rda");
            if (!file.exists())
                file.createNewFile();

            fos = new FileOutputStream(file);
            fos.write(dataArray);
            return true;
        } catch (Exception ex) {
            RegionOwn.logger.severe("[RegionOwn] Error when saving Snapshot...");
            ex.printStackTrace();
            return false;
        } finally {
            try {
                fos.close();
            } catch (Exception e) {
            }
        }
    }

    /**
     * Reverts The Region back to it's saved snapshot
     *
     * @param fileName The File name of the snapshot to be loaded
     * @return true if the revert was successful
     */
    public boolean revert(String fileName) {
        if (!fileName.equals("undo")) {
            saveSnapshot("undo");
        }

        RegionOwnListener.reverting.add(this);

        InputStream is = null;
        try {
            //Return if no type array was found
            File file = new File(RegionOwn.dataFolder+"/Snapshots/"+name+"/"+fileName+".rta");
            if (!file.exists()) {
                RegionOwn.logger.severe("[RegionOwn] Unable to revert Region "+name+" because no snapshot was found.");
                return false;
            }

            //Verify that the data matches the size of this Region
            int size = (int)file.length();
            if (size != bitSet.length() * height) {
                RegionOwn.logger.severe("[RegionOwn] Unable to revert Region "+name+" because the cubiod sizes are different lengths.");
                return false;
            }

            byte[] typeArray = new byte[size];
            byte[] dataArray = new byte[size];

            is = new FileInputStream(file);

            int offset = 0;
            int numRead = 0;
            while (offset < size && (numRead = is.read(typeArray, offset, size-offset)) >= 0) {
                offset += numRead;
            }

            //Ensure all the bytes have been read in
            if (offset < size) {
                throw new IOException("Could not completely read file "+file.getName());
            }

            is.close();

            //Return if no data array was found
            file = new File(RegionOwn.dataFolder+"/Snapshots/"+name+"/"+fileName+".rda");
            if (!file.exists()) {
                RegionOwn.logger.severe("[RegionOwn] Unable to revert Region "+name+" because no snapshot was found.");
                return false;
            }

            //Verify that the data matches the size of this Region
            if ((int)file.length() != size) {
                RegionOwn.logger.severe("[RegionOwn] Unable to revert Region "+name+" because the cubiod sizes are different lengths.");
                return false;
            }

            is = new FileInputStream(file);

            offset = 0;
            numRead = 0;
            while (offset < size && (numRead = is.read(dataArray, offset, size-offset)) >= 0) {
                offset += numRead;
            }

            //Ensure all the bytes have been read in
            if (offset < size) {
                throw new IOException("Could not completely read file "+file.getName());
            }

            World w = RegionOwn.server.getWorld(world);

            int i = 0;
            int index = 0;
            for (int x = x1; x <= x2; x++) {
                for (int z = z1; z <= z2; z++) {
                    if (bitSet.get(index)) {
                        for (int y = y1; y <= y2; y++) {
                            Block block = w.getBlockAt(x, y, z);
                            block.setTypeIdAndData((int)typeArray[i], dataArray[i], true);
                            i++;
                        }
                    }
                    index++;
                }
            }

            //Mark the files as deleted
            file.renameTo(new File(file.getAbsolutePath().replace("/Snapshots/", "/Deleted/Snapshots/")));
            file = new File(RegionOwn.dataFolder+"/Snapshots/"+name+"/"+fileName+".rta");
            file.renameTo(new File(file.getAbsolutePath().replace("/Snapshots/", "/Deleted/Snapshots/")));
        } catch (Exception ex) {
            RegionOwn.logger.severe("[RegionOwn] Error when reverting Region from Snapshot...");
            ex.printStackTrace();
            return false;
        } finally {
            try {
                is.close();
            } catch (Exception e) {
            }
        }

        RegionOwnListener.reverting.remove(this);
        return true;
    }

    public void save() {
        RegionOwn.saveRegion(this);
    }

    public int[] toIntArray() {
        int[] intArray = new int[6];
        intArray[0] = x1;
        intArray[1] = z1;
        intArray[2] = x2;
        intArray[3] = z2;
        intArray[4] = y1;
        intArray[5] = y2;
        return intArray;
    }

    public Region clone(String worldName) {
        Region clone = new Region(name, toIntArray(), bitSet);
        clone.world = worldName;
        return clone;
    }

    @Override
    public String toString() {
        return name+": in "+world+" between ("+x1+", "+y1+", "+z1+") and ("+x2+", "+y2+", "+z2+")";
    }
}
