package com.strangeone101.holoitems.abilities;

import com.strangeone101.holoitems.CustomItem;
import com.strangeone101.holoitems.ItemAbility;
import com.strangeone101.holoitems.items.Items;
import com.strangeone101.holoitems.items.PhoenixTotem;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.lang.Math;

public class PhoenixTotemActiveAbility extends ItemAbility {

    public static final int ABILITY_INITIAL_TIME = 0;
    public static final int ABILITY_CONCLUSION_TIME = 120;
    public static final int EFFECT_DURATION_FIRE_RES = 1200; // 60s
    public static final int EFFECT_AMP_FIRE_RES = 1;
    public static final int EFFECT_DURATION_ABSORPTION = 600; // 30s
    public static final int EFFECT_AMP_ABSORPTION = 4;
    public static final int EFFECT_FIRE_TICKS = 200; // 10s
    public static final float EFFECT_EXPLOSION_INIT = 4F;
    public static final float EFFECT_EXPLOSION_FINAL = 6F;
    public static final int EFFECT_EXPLOSION_AMOUNT = 4;
    public static final int EFFECT_EXPLOSION_RADIUS = 3;
    public static final int ANIM_PARTICLE_COUNT = 6;
    public static final int ANIM_PROGRESS_MAX = ABILITY_CONCLUSION_TIME;
    public static final int ANIM_RADIAN_FACTOR = 5;
    public static final int ANIM_RADIUS_FACTOR = 9;
    public static final int EGG_RADIUS = 2;
    public static final int EGG_OVAL_FACTOR = 4;
    public static final int EGG_RADIUS_OVAL = EGG_RADIUS * EGG_OVAL_FACTOR;
    public static final int EGG_GRANULARITY = 64;
    public static final Material EGG_MATERIAL = Material.RED_STAINED_GLASS;
    public static final Material EGG_MATERIAL_ALT = Material.MAGMA_BLOCK;
    public static final int PLATFORM_RADIUS_MAX = 5;
    public static final int PLATFORM_GRANULARITY = 14;
    public static final Material PLATFORM_MATERIAL = Material.OBSIDIAN;
    public static final Material PLATFORM_MATERIAL_ALT = Material.CRYING_OBSIDIAN;



    private int abilityProgressTime; // time since start of ability (s)
    private Location epicenter = null;

    public PhoenixTotemActiveAbility(Player player, ItemStack stack, Inventory inventory, int slot) {
        super(player, stack, inventory, slot);

        // ability already active or on cooldown
        PhoenixTotem item = (PhoenixTotem) getItem();
        if (ItemAbility.isAbilityActive(player, this.getClass())
            || !item.isCharged(getStack().getItemMeta())) return;

        // recreate the item so it doesn't get 'used up'
        // need to also re-enable the passive charging
        ItemStack refreshedItem = Items.PHOENIX_TOTEM.buildStack(player);
        ((PlayerInventory) inventory).setItemInOffHand(refreshedItem);
        new PhoenixTotemChargeAbility(player, refreshedItem, inventory, 0);

        abilityProgressTime = 0;
        start();
    }

    @Override
    public void tick() {
        // initial effects:
        // - player is encased within spherical 'egg'
        // - player gains regen, fire res, and extra hearts buffs
        //
        // endured intermediate effects:
        // - a weak heatwave damages nearby entities continuously
        // - damage increases as the egg nears singularity
        // - damage nullified by fire resistance
        // - red fire particles gravitate towards the egg
        //
        // concluding effects
        // - spherical egg is removed
        // - a non-destructive explosion damages nearby entities
        // - fire is lit around the area
        // - player gains strength and movement speed buffs

        Player player = getPlayer();
        World world = player.getWorld();

        if (abilityProgressTime == ABILITY_INITIAL_TIME) {
            // intial effects

            // set the epicenter that'll be referenced by later phases of the
            // ability
            epicenter = player.getLocation();

            // add buffs to the player
            PotionEffect effectFireRes =
                new PotionEffect(PotionEffectType.FIRE_RESISTANCE,
                                 EFFECT_DURATION_FIRE_RES,
                                 EFFECT_AMP_FIRE_RES);
            PotionEffect effectAbsorption =
                new PotionEffect(PotionEffectType.ABSORPTION,
                                 EFFECT_DURATION_ABSORPTION,
                                 EFFECT_AMP_ABSORPTION);
            player.setHealth(player.getMaxHealth() / 2);
            player.addPotionEffect(effectFireRes);
            player.addPotionEffect(effectAbsorption);

            // spawn the egg encasing the playedr
            spawnEgg(player.getLocation());

            // push other entities away
            world.createExplosion(epicenter, EFFECT_EXPLOSION_INIT, false, false);

            // for that extra suspense
            player.playSound(player.getLocation(), Sound.ENTITY_ENDER_DRAGON_DEATH, 1, 2);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&cYou rise from the &6&oashes &4..."));
        }
        else if (abilityProgressTime < ABILITY_CONCLUSION_TIME) {
            // endured intermediate effects

            // TODO: add DOT to nearby entities
            spawnParticles(abilityProgressTime, epicenter);
            for (Entity entity : world.getNearbyEntities(epicenter, 8, 8, 8)) {
                entity.setFireTicks(EFFECT_FIRE_TICKS);
            }
        }
        else {
            // concluding effects

            // spawn a platform under the epicenter and explood
            spawnPlatform(epicenter);
            world.createExplosion(epicenter, EFFECT_EXPLOSION_FINAL, true, true);

        }

        // increment the time and end the ability if expired
        abilityProgressTime += 1;
        if (abilityProgressTime > ABILITY_CONCLUSION_TIME) {
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', "&6Rebirth once again!"));
            remove();
        }
    }

    private void spawnParticles(int progress, Location epicenter) {
        double radianOffset = 2 * Math.PI / ANIM_PARTICLE_COUNT;
        for (int i = 0; i < ANIM_PARTICLE_COUNT; i++) {
            double radians = (double) progress / ANIM_RADIAN_FACTOR + i * radianOffset;

            double dirX = Math.cos(radians);
            double dirY = 0;
            double dirZ = Math.sin(radians);

            Vector offset = new Vector(dirX, dirY, dirZ)
                .multiply((double) (ANIM_PROGRESS_MAX - progress) / ANIM_RADIUS_FACTOR);
            Location locSpawn = epicenter.clone().add(offset).add(0, 1, 0);
            locSpawn.getWorld().spawnParticle(Particle.FLAME, locSpawn, 3, 0.25, 0.25, 0.25, 0);
        }

        for (int i = 0; i < 2; i++) {
            double dirX = 2 * Math.random() - 1;
            double dirY = 2 * Math.random() - 1;
            double dirZ = 2 * Math.random() - 1;

            epicenter.getWorld().spawnParticle(Particle.FLAME, epicenter, 0, dirX, dirY, dirZ, 1);
        }
    }

    public static void spawnEgg(Location epicenter) {
        // cache the squared radius for later calculations
        double radiusSquared = Math.pow(EGG_RADIUS, 2);
        for (int levelY = EGG_RADIUS_OVAL; levelY >= -EGG_RADIUS_OVAL; levelY--) {
            // get the radius of the current 'slice' of the egg
            double lateralRadius = Math.sqrt(radiusSquared - (Math.pow(levelY, 2) / EGG_OVAL_FACTOR));

            // draw out the lateral slice of the egg
            for (int i = 0; i < EGG_GRANULARITY; i++) {
                // get the angle in radians
                double radians = 2 * Math.PI * i / EGG_GRANULARITY;

                // multiply by the lateralRadius to bring the vector to the correct length
                double dirX = Math.cos(radians) * lateralRadius;
                double dirY = levelY;
                double dirZ = Math.sin(radians) * lateralRadius;

                // create the location object
                Vector spawnLoc = new Vector(dirX, dirY, dirZ);
                Location locationTarget = epicenter.clone().add(spawnLoc);
                Block block = epicenter.getWorld().getBlockAt(locationTarget);

                // only place the block if it's an empty space
                if (block.getType() == Material.AIR) {
                    if (Math.random() < 0.2)
                        block.setType(EGG_MATERIAL_ALT);
                    else
                        block.setType(EGG_MATERIAL);
                }
            }
        }

    }

    public static void spawnPlatform(Location epicenter) {
        for (int radius = 0; radius < PLATFORM_RADIUS_MAX; radius++) {
            // draw out a layer of the filled circle
            for (int i = 0; i < PLATFORM_GRANULARITY; i++) {
                // get the angle in radians
                double radians = 2 * Math.PI * i / PLATFORM_GRANULARITY;

                // multiply by the radius to bring the vector to the correct length
                // NOTE: dirY is -1 since we want the platform to be below the
                // epicenter
                double dirX = Math.cos(radians) * radius;
                double dirY = -1;
                double dirZ = Math.sin(radians) * radius;

                // create the location object
                Vector spawnLoc = new Vector(dirX, dirY, dirZ);
                Location locationTarget = epicenter.clone().add(spawnLoc);
                Block block = epicenter.getWorld().getBlockAt(locationTarget);

                // spawn the new block
                if (Math.random() < 0.2)
                    block.setType(PLATFORM_MATERIAL_ALT);
                else
                    block.setType(PLATFORM_MATERIAL);
            }
        }

    }

    public static void spawnExplosions(Location epicenter) {
        for (int i = 0; i < EFFECT_EXPLOSION_AMOUNT; i++) {
            double radians = 2 * Math.PI * i / EFFECT_EXPLOSION_AMOUNT;

            // multiply by the radius to bring the vector to the correct length
            // NOTE: dirY is -1 since we want the platform to be below the
            // epicenter
            double dirX = Math.cos(radians) * EFFECT_EXPLOSION_RADIUS;
            double dirY = 0;
            double dirZ = Math.sin(radians) * EFFECT_EXPLOSION_RADIUS;

            // create the location object
            Vector spawnLoc = new Vector(dirX, dirY, dirZ);
            Location locationTarget = epicenter.clone().add(spawnLoc);

            // spawn the new block
            locationTarget.getWorld().createExplosion(locationTarget, EFFECT_EXPLOSION_INIT, true, true);
        }
    }

    @Override
    public long getCooldownLength() { return 0; }

    @Override
    public CustomItem getItem() {
        return Items.PHOENIX_TOTEM;
    }
}
