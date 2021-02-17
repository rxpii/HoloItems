package com.strangeone101.holoitems.items;

import com.strangeone101.holoitems.CustomItem;
import com.strangeone101.holoitems.HoloItemsPlugin;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public class PhoenixTotem extends CustomItem {
    public static final int CHARGE_INITIAL = 0;
    public static final int CHARGE_MAX = 100;
    public static final int CHARGE_ACTIVATE_THRESH = 100;
    public static final int CHARGE_RATE = 3;
    public static final int CHARGE_SKY_LIGHT_THRESH = 15; // sky light level needed to charge
    public static final int CHARGE_BLOCK_LIGHT_THRESH = 13; // block light level needed to charge
    public static final byte TRUE = 1;
    public static final byte FALSE = 0;

    public PhoenixTotem() {
        super("phoenix_totem", Material.TOTEM_OF_UNDYING);

        // holds the current charge of the totem
        this.addVariable("charge", container -> {
            NamespacedKey keyCharge = HoloItemsPlugin.getKeys().PHOENIX_TOTEM_CHARGE;

            if (container.has(keyCharge, PersistentDataType.INTEGER)) {
                int charge = container.get(keyCharge, PersistentDataType.INTEGER);
                return Integer.toString(charge);
            }
            else {
                return "(Error: charge undefined.)";
            }
        });

        // tracks whether the totem is currently charging or not
        this.addVariable("charge_state", container -> {
            NamespacedKey keyCharge = HoloItemsPlugin.getKeys().PHOENIX_TOTEM_CHARGE_STATE;

            if (container.has(keyCharge, PersistentDataType.BYTE)) {
                byte chargeState = container.get(keyCharge, PersistentDataType.BYTE);
                // not charging
                if (chargeState == FALSE)
                    return ChatColor.DARK_GRAY + "The totem pulses weakly...";
                // charging
                else
                    return ChatColor.YELLOW + "The totem basks in the sunlight.";
            }
            else {
                return "(Error: charge_state undefined.)";
            }
        });

        this.setDisplayName(ChatColor.GOLD + "Phoenix Totem")
            .addLore(ChatColor.GRAY + "Rebirth upon death!")
            .addLore("")
            .addLore("{charge_state}")
            .addLore(ChatColor.DARK_GRAY + "Charge: " + ChatColor.YELLOW + "{charge}");
    }

    @Override
    public ItemStack buildStack(Player player) {
        ItemStack stack = super.buildStack(player);
        ItemMeta meta = stack.getItemMeta();

        setCharge(meta, CHARGE_INITIAL);
        setChargeState(meta, TRUE);
        stack.setItemMeta(meta);

        return updateStack(stack, player);
    }

    public int getCharge(ItemMeta meta) {
        return meta.getPersistentDataContainer()
            .get(HoloItemsPlugin.getKeys().PHOENIX_TOTEM_CHARGE,
                 PersistentDataType.INTEGER);
    }

    public void setCharge(ItemMeta meta, int chargeNew) {
        meta.getPersistentDataContainer()
            .set(HoloItemsPlugin.getKeys().PHOENIX_TOTEM_CHARGE,
                 PersistentDataType.INTEGER,
                 chargeNew);
    }

    public int getChargeState(ItemMeta meta) {
        return meta.getPersistentDataContainer()
            .get(HoloItemsPlugin.getKeys().PHOENIX_TOTEM_CHARGE_STATE,
                 PersistentDataType.BYTE);
    }

    public void setChargeState(ItemMeta meta, byte chargeStateNew) {
        meta.getPersistentDataContainer()
            .set(HoloItemsPlugin.getKeys().PHOENIX_TOTEM_CHARGE_STATE,
                 PersistentDataType.BYTE,
                 chargeStateNew);
    }

    public boolean isCharged(ItemMeta meta) {
        return getCharge(meta) >= CHARGE_ACTIVATE_THRESH;
    }
}
