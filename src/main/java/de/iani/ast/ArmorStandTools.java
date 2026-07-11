package de.iani.ast;

import com.google.common.base.Objects;
import de.iani.ast.config.PluginConfig;
import de.iani.ast.events.PlayerStartEditingArmorStandEvent;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

public class ArmorStandTools extends JavaPlugin {

    private HashMap<UUID, PlayerArmorStandEditData> armorStandEdits;
    private PluginConfig pluginConfig;

    @Override
    public void onEnable() {
        pluginConfig = new PluginConfig(this);
        Messages.init(pluginConfig);

        armorStandEdits = new HashMap<>();
        getServer().getPluginManager().registerEvents(new ArmorStandListener(this), this);

        if (getServer().getPluginManager().getPlugin("WorldGuard") != null) {
            getServer().getPluginManager().registerEvents(new WorldGuardProtectionListener(this), this);
        }
    }

    public PluginConfig getPluginConfig() {
        return pluginConfig;
    }

    public boolean startEditing(Player player, ArmorStand armorstand) {
        if (!new PlayerStartEditingArmorStandEvent(player, armorstand).callEvent()) {
            return false;
        }
        if (!armorStandEdits.containsKey(player.getUniqueId())) {
            PlayerArmorStandEditData data = new PlayerArmorStandEditData(this, player, armorstand);
            armorStandEdits.put(player.getUniqueId(), data);
        }
        return true;
    }

    public void stopEditing(Player player, boolean stopIfFreeMove) {
        PlayerArmorStandEditData old = armorStandEdits.get(player.getUniqueId());
        if (old != null) {
            if (stopIfFreeMove || old.getEditState() != PlayerArmorStandEditData.EditState.RotationWithoutWindow) {
                armorStandEdits.remove(player.getUniqueId());
                old.getInventory().clear();
                player.closeInventory();
            }
        }
    }

    public PlayerArmorStandEditData getEditData(Player player) {
        return armorStandEdits.isEmpty() ? null : armorStandEdits.get(player.getUniqueId());
    }

    public static boolean itemStackEquals(ItemStack stack1, ItemStack stack2) {
        if (stack1 != null && stack1.getType() == Material.AIR) {
            stack1 = null;
        }
        if (stack2 != null && stack2.getType() == Material.AIR) {
            stack2 = null;
        }
        return Objects.equal(stack1, stack2);
    }
}
