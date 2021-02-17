package com.strangeone101.holoitems.listener;

import com.strangeone101.holoitems.CustomItem;
import com.strangeone101.holoitems.CustomItemRegistry;
import com.strangeone101.holoitems.ItemAbility;
import com.strangeone101.holoitems.HoloItemsPlugin;
import com.strangeone101.holoitems.abilities.PhoenixTotemActiveAbility;
import com.strangeone101.holoitems.abilities.PhoenixTotemChargeAbility;
import com.strangeone101.holoitems.items.Items;
import com.strangeone101.holoitems.util.InventoryUtil;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityResurrectEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class PhoenixTotemListener implements Listener {

    @EventHandler
    public void onInventoryPickup(InventoryPickupItemEvent event) {
        ItemStack stack = event.getItem().getItemStack();
        CustomItem item = CustomItemRegistry.getCustomItem(stack);

        if (item != null) {
            Player player = event.getInventory().getHolder() instanceof Player ? (Player)event.getInventory().getHolder() : null;
            item.updateStack(stack, player);
            event.getItem().setItemStack(stack);
        }
    }

    @EventHandler
    public void onEntityPickup(EntityPickupItemEvent event) {
        // ensure the entity is a player
        if (!(event.getEntity() instanceof Player)) return;

        ItemStack stack = event.getItem().getItemStack();
        CustomItem item = CustomItemRegistry.getCustomItem(stack);
        Player player = (Player) event.getEntity();

        if (item != null) {
            if (item == Items.PHOENIX_TOTEM) {
                int slot = player.getInventory().getHeldItemSlot();
                new PhoenixTotemChargeAbility(player, stack, player.getInventory(), slot);
            }
        }
    }

    @EventHandler
    public void onInventoryItemUpdate(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;
        Player player = (Player) event.getWhoClicked();

        // get the item of interest
        ItemStack stack = event.getCursor();
        if (stack.getType() == Material.AIR)
            stack = event.getCurrentItem();
        CustomItem item = CustomItemRegistry.getCustomItem(stack);

        if (item != null && item == Items.PHOENIX_TOTEM) {
            // delay checking whether the player has the totem in their inventory or not
            // if we check right away, the item may not already be moved yet, so we won't
            // find it in the player's inventory
            // NOTE: we clone the stack since we don't want it to be updated before we
            // check it later
            new BukkitRunnable() {
                private ItemStack stack;
                private Player player;

                // this is used to circumvent needing to declare the local vars as final
                private BukkitRunnable init(ItemStack stack, Player player) {
                    this.stack = stack;
                    this.player = player;
                    return this;
                }

                @Override
                public void run() {
                    PlayerInventory inv = player.getInventory();
                    int slot = InventoryUtil.getSlot(stack, inv);
                    if (slot != -1) {
                        new PhoenixTotemChargeAbility(player, stack, inv, slot);
                    }
                }

            }.init(stack.clone(), player).runTaskLater(HoloItemsPlugin.INSTANCE, 20);
        }
    }

    @EventHandler
    public void onDrop(PlayerDropItemEvent event) {
        ItemStack stack = event.getItemDrop().getItemStack();
        CustomItem item = CustomItemRegistry.getCustomItem(stack);
        Player player = event.getPlayer();

        if (item != null) {
            if (item == Items.PHOENIX_TOTEM) {
                ItemAbility ability = ItemAbility.getAbility(player, PhoenixTotemChargeAbility.class);
                if (ability != null) {
                    ability.remove();
                }
            }
        }
    }

    @EventHandler
    public void onEntityResurrect(EntityResurrectEvent event) {
        // ensure the entity is a player
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        PlayerInventory inventory = player.getInventory();
        ItemStack offHandItem = inventory.getItemInOffHand();
        String customItemId = InventoryUtil.getCustomItemId(offHandItem);

        if (customItemId != null) {
            if (customItemId.equals(Items.PHOENIX_TOTEM.getInternalName())) {
                // instantiate the active ability
                new PhoenixTotemActiveAbility(player, offHandItem, inventory, 0);
            }
        }
    }

    @EventHandler
    public void onEntityExplosionDamage(EntityDamageEvent event) {
        // ensure the entity is a player
        if (!(event.getEntity() instanceof Player)) return;

        Player player = (Player) event.getEntity();
        DamageCause cause = event.getCause();

        if (cause == DamageCause.BLOCK_EXPLOSION) {
            // nullify the damage if the player has the phoenix totem
            // active ability
            if (ItemAbility.isAbilityActive(player, PhoenixTotemActiveAbility.class)) {
                event.setCancelled(true);
            }
        }
    }
}
