package com.strangeone101.holoitems.abilities;

import com.strangeone101.holoitems.CustomItem;
import com.strangeone101.holoitems.HoloItemsPlugin;
import com.strangeone101.holoitems.ItemAbility;
import com.strangeone101.holoitems.items.Items;
import com.strangeone101.holoitems.items.PhoenixTotem;
import com.strangeone101.holoitems.util.InventoryUtil;

import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.lang.Math;

public class PhoenixTotemChargeAbility extends ItemAbility {
    public static final long UPDATE_DELAY = 1000; // 1s

    private long nextUpdate = 0;
    public PhoenixTotemChargeAbility(Player player, ItemStack stack, Inventory inventory, int slot) {
        super(player, stack, inventory, slot);

        // override existing charging ability if needed
        // this could happen if the current totem gets its active used,
        // spawning in a new one
        if (ItemAbility.isAbilityActive(player, this.getClass())) {
            remove();
        }

        // start charging the totem!
        start();
    }

    @Override
    public void tick() {
        // don't update every tick
        long timeCur = System.currentTimeMillis();
        if (timeCur < nextUpdate) return;
        nextUpdate = timeCur + UPDATE_DELAY;

        Player player = getPlayer();
        PhoenixTotem item = (PhoenixTotem) getItem();
        ItemStack stack = getStack();
        ItemMeta meta = stack.getItemMeta();
        Inventory inv = getInventory();

        // ensure that the item is in the inventory
        // else disable charging and return
        int slot = InventoryUtil.getSlot(stack, inv);
        if (slot == -1) {
            remove();
            return;
        }

        // check that the player is exposed to a sky light level sufficient to
        // charge the totem
        Block playerBlock = player.getLocation().getBlock().getRelative(0, 1, 0);
        int skyLightLevel = playerBlock.getLightFromSky();

        if (isDay() && skyLightLevel >= PhoenixTotem.CHARGE_SKY_LIGHT_THRESH) {
            // the totem is charging

            // every tick, charge the totem
            int chargeCurrent = item.getCharge(meta);
            int chargeNew = Math.min(chargeCurrent + PhoenixTotem.CHARGE_RATE, PhoenixTotem.CHARGE_MAX);
            item.setCharge(meta, chargeNew);
            item.setChargeState(meta, PhoenixTotem.TRUE);
        }
        // the totem is not charging
        else {
            item.setChargeState(meta, PhoenixTotem.FALSE);
        }

        if (item.isCharged(meta)) {
            // make it glow!
            addGlow(meta);
        }
        else {
            removeGlow(meta);
        }

        stack.setItemMeta(meta);
        item.updateStack(stack, player);
        inv.setItem(slot, stack);
    }

    private void addGlow(ItemMeta meta) {
        // hide the enchants that we apply to make the item glow
        meta.addEnchant(Enchantment.CHANNELING, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
    }

    private void removeGlow(ItemMeta meta) {
        meta.removeEnchant(Enchantment.CHANNELING);
    }

    public boolean isDay() {
        long time = getPlayer().getWorld().getTime();

        return time < 12300 || time > 23850;
    }

    @Override
    public long getCooldownLength() { return 0; }

    @Override
    public CustomItem getItem() {
        return Items.PHOENIX_TOTEM;
    }

}
