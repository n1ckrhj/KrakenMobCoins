package me.aglerr.krakenmobcoins.utils;

import com.cryptomorin.xseries.SkullUtils;
import com.cryptomorin.xseries.XMaterial;
import com.cryptomorin.xseries.XSound;
import com.cryptomorin.xseries.messages.ActionBar;
import com.cryptomorin.xseries.messages.Titles;
import de.tr7zw.changeme.nbtapi.NBTItem;
import me.aglerr.krakenmobcoins.MobCoins;
import me.aglerr.krakenmobcoins.configs.ConfigMessages;
import me.aglerr.krakenmobcoins.shops.CategoryInventory;
import me.aglerr.krakenmobcoins.shops.NormalShopInventory;
import me.aglerr.krakenmobcoins.shops.RotatingShopInventory;
import me.aglerr.krakenmobcoins.shops.items.ShopItems;
import me.swanis.mobcoins.MobCoinsAPI;
import me.swanis.mobcoins.profile.Profile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.scheduler.BukkitRunnable;

import javax.rmi.CORBA.Util;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Utils {

    private final MobCoins plugin;
    public Utils(final MobCoins plugin){
        this.plugin = plugin;
    }

    public String color(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public List<String> color(List<String> strings) {
        return strings.stream().map(this::color).collect(Collectors.toList());
    }

    public void sendConsoleMessage(String string) {
        System.out.println(color("[KrakenMobCoins] " + string));
    }

    public String getPrefix(){
        return color(ConfigMessages.PREFIX.toString());
    }

    public boolean isInt(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean isDouble(String str){
        try{
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }

    public DecimalFormat getDFormat(){
        DecimalFormat df = new DecimalFormat("###,###,###,###,###.##");
        return df;
    }

    public ItemStack getMobCoinItem(double amount){
        FileConfiguration config = MobCoins.getInstance().getConfig();
        Utils utils = MobCoins.getInstance().getUtils();

        String material = config.getString("mobcoinItem.material");
        String name = config.getString("mobcoinItem.name");
        List<String> lore = new ArrayList<>();
        for(String line : config.getStringList("mobcoinItem.lore")){
            lore.add(line.replace("%coins%", this.getDFormat().format(amount)));
        }
        
        ItemStack stack = null;
        if(material.contains(";")){
            String[] split = material.split(";");
            if(split[0].equals("head")){
                
                String texture = split[1];
                stack = XMaterial.PLAYER_HEAD.parseItem();
                SkullMeta sm = (SkullMeta) stack.getItemMeta();
                SkullUtils.applySkin(sm, texture);
                sm.setDisplayName(utils.color(name));
                sm.setLore(utils.color(lore));
                stack.setItemMeta(sm);
                
            }
            
        } else {
            
            stack = XMaterial.matchXMaterial(material).get().parseItem();
            ItemMeta im = stack.getItemMeta();
            im.setDisplayName(utils.color(name));
            im.setLore(utils.color(lore));
            stack.setItemMeta(im);
        }

        NBTItem nbtItem = new NBTItem(stack);
        nbtItem.setDouble("amount", amount);
        nbtItem.setString("info", "krakenmobcoins");

        return nbtItem.getItem();
    }

    public boolean hasOffhand() {
        if (Bukkit.getVersion().contains("1.9") ||
                Bukkit.getVersion().contains("1.10") ||
                Bukkit.getVersion().contains("1.11") ||
                Bukkit.getVersion().contains("1.12") ||
                Bukkit.getVersion().contains("1.13") ||
                Bukkit.getVersion().contains("1.14") ||
                Bukkit.getVersion().contains("1.15") ||
                Bukkit.getVersion().contains("1.16") ||
                Bukkit.getVersion().contains("1.17")){
            return true;
        } else {
            return false;
        }

    }

    public int[] getRemainingTime(long remaining) {
        int days = (int)(remaining / 86400000L);
        int hours = (int)(remaining % 86400000L) / 3600000;
        int minutes = (int)(remaining % 3600000L / 60000L);
        int seconds = (int)(remaining % 60000L / 1000L);
        return new int[] { days, hours, minutes, seconds };
    }

    public String getFormattedString(long remaining) {
        int[] remainingTime = getRemainingTime(remaining);
        String string = "";
        for (int i = 0; i < remainingTime.length; i++) {
            if (remainingTime[i] != 0) {
                String s = "";
                if (i == 0)
                    s = "d";
                if (i == 1)
                    s = "h";
                if (i == 2)
                    s = "m";
                if (i == 3)
                    s = "s";
                if (string.length() == 0) {
                    string = remainingTime[i] + s;
                } else {
                    string = string + " " + remainingTime[i] + s;
                }
            }
        }
        return string;
    }

    public void resetStock(){
        MobCoins.getInstance().getLimitManager().getConfiguration().set("items", new ArrayList<>());
        MobCoins.getInstance().getLimitManager().saveData();
        plugin.getItemStockManager().clearStock();
    }

    public void resetLimit(){
        MobCoins.getInstance().getLimit().clear();
    }

    public void refreshNormalItems(){

        FileConfiguration config = MobCoins.getInstance().getConfig();
        if(config.getBoolean("options.shuffleRotating")){
            Collections.shuffle(MobCoins.getInstance().getNormalItems());
        } else {

            List<Integer> normalSlots = config.getIntegerList("rotatingShop.normalItemSlots");
            List<ShopItems> removed = new ArrayList<>();

            if(MobCoins.getInstance().getNormalItems().size() > normalSlots.size()){
                for(int x = 0; x < normalSlots.size(); x++){
                    removed.add(MobCoins.getInstance().getNormalItems().get(0));
                    MobCoins.getInstance().getNormalItems().remove(0);
                }

            }

            if(!removed.isEmpty()){
                for(ShopItems items : removed){
                    MobCoins.getInstance().getNormalItems().add(items);
                }
            }

        }


    }

    public void refreshSpecialItems(){

        FileConfiguration config = MobCoins.getInstance().getConfig();
        if(config.getBoolean("options.shuffleRotating")){
            Collections.shuffle(MobCoins.getInstance().getSpecialItems());
        }

        List<Integer> specialSlots = config.getIntegerList("rotatingShop.specialItemSlots");
        List<ShopItems> removed = new ArrayList<>();

        if(MobCoins.getInstance().getNormalItems().size() > specialSlots.size()){
            for(int x = 0; x < specialSlots.size(); x++){
                removed.add(MobCoins.getInstance().getSpecialItems().get(0));
                MobCoins.getInstance().getSpecialItems().remove(0);
            }

        }

        if(!removed.isEmpty()){
            for(ShopItems items : removed){
                MobCoins.getInstance().getSpecialItems().add(items);
            }
        }

    }

    public void openShopMenu(Player player){
        FileConfiguration config = MobCoins.getInstance().getConfig();
        if(config.getBoolean("rotatingShop.enabled")){
            String title = color(config.getString("rotatingShop.title"));
            int size = config.getInt("rotatingShop.size");

            new RotatingShopInventory(size, title, player, plugin).open(player);
        } else {
            String title = color(config.getString("normalShop.title"));
            int size = config.getInt("normalShop.size");

            new CategoryInventory(size, title, player).open(player);
        }

    }

    public void openCategory(String category, Player player){
        FileConfiguration config = MobCoins.getInstance().getConfig();
        String finalCategory = category + ".yml";
        if(MobCoins.getInstance().getCategories().containsKey(finalCategory)){
            FileConfiguration configuration = MobCoins.getInstance().getCategories().get(finalCategory);
            String title = color(configuration.getString("title"));
            int size = configuration.getInt("size");

            new NormalShopInventory(size, title, finalCategory, player, plugin).open(player);

        } else {

         player.sendMessage(color(config.getString("messages.categoryNotExist"))
         .replace("%prefix%", getPrefix())
         .replace("%category%", category));

        }
    }

    public void exampleShop(File file){

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        config.set("title", "Shop Menu");
        config.set("size", 36);

        // Vote Key
        List<String> lore = new ArrayList<>();
        lore.add("&7You can use this key on /crates");
        lore.add("");
        lore.add(" &7Price: &610.25 coins");
        lore.add(" &7Limit: &6%limit% / %maxLimit%");
        lore.add("");
        lore.add("&a&lCLICK TO PURCHASE!");

        List<String> commands = new ArrayList<>();
        commands.add("give %player% TRIPWIRE_HOOK 1");
        commands.add("broadcast &a&l%player% just bought 1x Vote Key!");

        config.set("items.voteKey.type", "shop");
        config.set("items.voteKey.material", "TRIPWIRE_HOOK");
        config.set("items.voteKey.amount", 1);
        config.set("items.voteKey.slot", 10);
        config.set("items.voteKey.price", 10.25);
        config.set("items.voteKey.limit", 3);
        config.set("items.voteKey.useStock", true);
        config.set("items.voteKey.stock", 5);
        config.set("items.voteKey.glow", false);
        config.set("items.voteKey.name", "&aVote Key");
        config.set("items.voteKey.lore", lore);
        config.set("items.voteKey.commands", commands);


        // Border
        List<Integer> slots = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 17, 18, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35);

        config.set("items.borderItem.type", "dummy");
        config.set("items.borderItem.material", "BLACK_STAINED_GLASS_PANE");
        config.set("items.borderItem.amount", 1);
        config.set("items.borderItem.name", "&f");
        config.set("items.borderItem.slots", slots);

        // Back Button
        List<String> list = new ArrayList();
        list.add("&7Click to go back to categories menu!");

        config.set("items.backButton.type", "back");
        config.set("items.backButton.material", "BARRIER");
        config.set("items.backButton.amount", 1);
        config.set("items.backButton.slot", 31);
        config.set("items.backButton.name", "&cBack");
        config.set("items.backButton.lore", list);


        try{
            config.save(file);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public int getBooster(Player player){
        for(PermissionAttachmentInfo perm : player.getEffectivePermissions()){
            if(perm.getPermission().startsWith("krakenmobcoins.booster.")){
                String permission = perm.getPermission().replace(".", ";");
                String[] split = permission.split(";");
                return Integer.parseInt(split[2]);
            }
        }
        return 0;
    }

    public void sendSound(Player player){
        FileConfiguration config = MobCoins.getInstance().getConfig();
        if(config.getBoolean("receivedMobCoins.sound.enabled")){
            String name = config.getString("receivedMobCoins.sound.name").toUpperCase();
            float volume = (float) config.getDouble("receivedMobCoins.sound.volume");
            float pitch = (float) config.getDouble("receivedMobCoins.sound.pitch");
            player.playSound(player.getLocation(), XSound.matchXSound(name).get().parseSound(), volume, pitch);
        }
    }

    public void sendMessage(Player player, double amount){
        FileConfiguration config = MobCoins.getInstance().getConfig();
        if(config.getBoolean("receivedMobCoins.message.enabled")){
            String message = config.getString("receivedMobCoins.message.message");
            player.sendMessage(color(message)
                    .replace("%prefix%", getPrefix())
                    .replace("%amount%", this.getDFormat().format(amount)));
        }
    }

    public void sendTitle(Player player, double amount){
        FileConfiguration config = MobCoins.getInstance().getConfig();
        if(config.getBoolean("receivedMobCoins.title.enabled")){
            String title = config.getString("receivedMobCoins.title.titles.title").replace("%amount%", this.getDFormat().format(amount));
            String subtitle = config.getString("receivedMobCoins.title.titles.subtitle").replace("%amount%", this.getDFormat().format(amount));
            int fadeIn = config.getInt("receivedMobCoins.title.titles.fadeIn");
            int stay = config.getInt("receivedMobCoins.title.titles.stay");
            int fadeOut = config.getInt("receivedMobCoins.title.titles.fadeOut");

            Titles.sendTitle(player, fadeIn, stay, fadeOut, color(title), color(subtitle));

        }
    }

    public void sendActionBar(Player player, double amount){
        FileConfiguration config = MobCoins.getInstance().getConfig();
        if(config.getBoolean("receivedMobCoins.actionBar.enabled")){
            String message = config.getString("receivedMobCoins.actionBar.message").replace("%amount%", this.getDFormat().format(amount));
            int duration = config.getInt("receivedMobCoins.actionBar.duration") * 20;

            ActionBar.sendActionBar(MobCoins.getInstance(), player, color(message), duration);

        }
    }

}
