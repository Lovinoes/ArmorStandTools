package de.iani.ast;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.world.World;
import com.sk89q.worldguard.LocalPlayer;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.Flags;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class WorldGuardProtectionListener implements Listener {
    private final ArmorStandTools plugin;
    private final WorldGuardPlugin worldGuard;

    public WorldGuardProtectionListener(ArmorStandTools plugin) {
        this.plugin = plugin;
        worldGuard = (WorldGuardPlugin) plugin.getServer().getPluginManager().getPlugin("WorldGuard");
    }

    @EventHandler
    public void onArmorStandTeleport(ArmorStandTeleportEvent event) {
        ApplicableRegionSet set = WorldGuard.getInstance().getPlatform().getRegionContainer().createQuery().getApplicableRegions(BukkitAdapter.adapt(event.getLocation()));
        if (!canBuild(event.getPlayer(), set)) {
            Messages.sendError(event.getPlayer(), plugin.getPluginConfig().message("no-build-permission"));
            event.setCancelled(true);
        }
    }

    private boolean canBuild(Player bplayer, ApplicableRegionSet set) {
        LocalPlayer player = worldGuard.wrapPlayer(bplayer);
        return hasBypass(player, player.getWorld()) || set.testState(player, Flags.BUILD);
    }

    private boolean hasBypass(LocalPlayer wgPlayer, World world) {
        return WorldGuard.getInstance().getPlatform().getSessionManager().hasBypass(wgPlayer, world);
    }
}
