package de.iani.ast;

import de.iani.ast.PlayerArmorStandEditData.EditState;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitRunnable;

public class ArmorStandListener implements Listener {

    private ArmorStandTools plugin;

    private ArmorStand lastArmorStand;

    public ArmorStandListener(ArmorStandTools plugin) {
        this.plugin = plugin;
        plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {

            @Override
            public void run() {
                lastArmorStand = null;
            }
        }, 1, 1);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.stopEditing(event.getPlayer(), true);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onArmorStandInteract(PlayerInteractAtEntityEvent event) {
        Entity e = event.getRightClicked();
        if (e instanceof ArmorStand) {
            ArmorStand armorStand = (ArmorStand) e;
            if (event.getPlayer().isSneaking()) {
                if (plugin.startEditing(event.getPlayer(), armorStand)) {
                    event.setCancelled(true);
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClose(InventoryCloseEvent event) {
        HumanEntity e = event.getPlayer();
        if (e instanceof Player) {
            Player player = (Player) e;
            plugin.stopEditing(player, false);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryDrag(InventoryDragEvent event) {
        HumanEntity e = event.getWhoClicked();
        if (e instanceof Player) {
            Player player = (Player) e;
            PlayerArmorStandEditData data = plugin.getEditData(player);
            if (data == null) {
                return;
            }
            int numInTop = event.getView().getTopInventory().getSize();
            if (event.getRawSlots().size() == 1) {
                int slot = event.getRawSlots().iterator().next();
                if (slot >= 0 && slot < numInTop) {
                    ItemStack newInSlot = event.getNewItems().values().iterator().next();
                    data.onInventoryDrag(slot, newInSlot, event);
                }
            } else {
                for (Integer slot : event.getRawSlots()) {
                    if (slot >= 0 && slot < numInTop) {
                        event.setCancelled(true);
                        return;
                    }
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        HumanEntity e = event.getWhoClicked();
        if (e instanceof Player) {
            Player player = (Player) e;
            PlayerArmorStandEditData data = plugin.getEditData(player);
            if (data == null) {
                return;
            }
            if (data.getArmorStand() == lastArmorStand) {
                event.setCancelled(true);
                return;
            }
            lastArmorStand = data.getArmorStand();
            if (event.getAction() == InventoryAction.HOTBAR_SWAP || event.getAction() == InventoryAction.COLLECT_TO_CURSOR) {
                event.setCancelled(true);
                return;
            }
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(data.getInventory())) {
                data.onInventoryClicked(event.getSlot(), event);
            } else if (event.getAction() == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerArmorStandEditData data = plugin.getEditData(player);
        if (data == null) {
            return;
        }
        data.onPlayerMove(event.getTo());
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerTeleport(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        PlayerArmorStandEditData data = plugin.getEditData(player);
        if (data == null) {
            return;
        }
        data.onPlayerMove(event.getTo());
    }

    private boolean onPlayerInteract(Player player) {
        PlayerArmorStandEditData data = plugin.getEditData(player);
        if (data == null || data.getEditState() != EditState.RotationWithoutWindow) {
            return false;

        }
        if (plugin.getServer().getCurrentTick() - data.getFreeMoveStartTick() > plugin.getPluginConfig().getFreeEditTimeoutTicks()) {
            Messages.send(player, plugin.getPluginConfig().message("free-edit-ended"));
            plugin.stopEditing(player, true);
        }
        return true;
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.PHYSICAL && onPlayerInteract(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (onPlayerInteract(event.getPlayer())) {
            event.setCancelled(true);
        }
        PlayerInventory inv = event.getPlayer().getInventory();
        ItemStack item = event.getHand() == EquipmentSlot.HAND ? inv.getItemInMainHand() : inv.getItemInOffHand();
        if (item != null && item.getType() == Material.NAME_TAG) {
            UUID playerId = event.getPlayer().getUniqueId();
            new BukkitRunnable() {
                @Override
                public void run() {
                    Player p = plugin.getServer().getPlayer(playerId);
                    if (p != null) {
                        p.updateInventory();
                    }
                }
            }.runTask(plugin);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (onPlayerInteract(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = false, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (damager instanceof Player && onPlayerInteract((Player) damager)) {
            event.setCancelled(true);
        }
    }
}
