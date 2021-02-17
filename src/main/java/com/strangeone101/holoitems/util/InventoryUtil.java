package com.strangeone101.holoitems.util;

import com.strangeone101.holoitems.CustomItem;
import com.strangeone101.holoitems.HoloItemsPlugin;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

public class InventoryUtil {
    public static int getSlot(ItemStack stack, Inventory inv) {
        ItemStack[] stacks = inv.getContents();
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stackOther = stacks[i];
            if (stack.equals(stackOther)) return i;
        }
        return -1;
    }

    public static int getSlotCustomItem(CustomItem item, Inventory inv) {
        String targetId = item.getInternalName();
        ItemStack[] stacks = inv.getContents();
        for (int i = 0; i < stacks.length; i++) {
            ItemStack stackOther = stacks[i];
            String otherId = getCustomItemId(stackOther);

            if (otherId != null && otherId.equals(targetId)) return i;
        }
        return -1;
    }

    public static boolean isCustomItem(ItemStack stack) {
        return stack != null &&
            stack.hasItemMeta() &&
            stack.getItemMeta()
            .getPersistentDataContainer()
            .has(HoloItemsPlugin.getKeys().CUSTOM_ITEM_ID,
                 PersistentDataType.STRING);
    }

    public static String getCustomItemId(ItemStack stack) {
        if (isCustomItem(stack)) {
            return stack.getItemMeta()
                .getPersistentDataContainer()
                .get(HoloItemsPlugin.getKeys().CUSTOM_ITEM_ID,
                     PersistentDataType.STRING);
        }
        else return null;
    }
}
