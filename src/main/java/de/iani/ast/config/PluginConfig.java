package de.iani.ast.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.attribute.AttributeModifier.Operation;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;

// loads config.yml and turns its entries into components/itemstacks, so the plugin
// can be re-themed by editing the config instead of the code
public class PluginConfig {

    // used for zero-effect attribute modifiers on menu items, just so there's something
    // for hideDefaultTooltipStats() to hide
    private static final NamespacedKey HIDE_ARMOR_TOOLTIP_KEY = NamespacedKey.fromString("armorstandtools:justhide");

    private final JavaPlugin plugin;
    private final MiniMessage miniMessage = MiniMessage.miniMessage();
    private FileConfiguration config;

    public PluginConfig(JavaPlugin plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        this.config = plugin.getConfig();
    }

    // settings.*

    public String getDefaultArmorStandName() {
        return config.getString("settings.default-armorstand-name", "Armor Stand");
    }

    public double getMaxFreeEditDistance() {
        return config.getDouble("settings.max-free-edit-distance", 15);
    }

    public double getDefaultSizeLimit() {
        return config.getDouble("settings.default-size-limit", 4);
    }

    public double getUnlimitedSizeLimit() {
        return config.getDouble("settings.unlimited-size-limit", 16);
    }

    public int getFreeEditTimeoutTicks() {
        return config.getInt("settings.free-edit-timeout-ticks", 5);
    }

    // builds the inventory title from settings.gui-title, a MiniMessage string with
    // <name> and <prefix> tags that insert already-built components
    public Component guiTitle(Component armorStandName) {
        String template = config.getString("settings.gui-title", "<name> Editor");
        TagResolver resolver = TagResolver.builder()
                .resolver(TagResolver.resolver("name", Tag.inserting(armorStandName)))
                .resolver(TagResolver.resolver("prefix", Tag.inserting(prefix())))
                .build();
        return miniMessage.deserialize(template, resolver);
    }

    // messages.*

    private String rawMessage(String key) {
        String value = config.getString("messages." + key);
        return value != null ? value : "";
    }

    private Component render(String template, Map<String, String> placeholders) {
        String text = template;
        if (placeholders != null) {
            for (Map.Entry<String, String> entry : placeholders.entrySet()) {
                text = text.replace("{" + entry.getKey() + "}", entry.getValue());
            }
        }
        return miniMessage.deserialize(text).decoration(TextDecoration.ITALIC, false);
    }

    public Component message(String key) {
        return render(rawMessage(key), null);
    }

    public Component message(String key, Map<String, String> placeholders) {
        return render(rawMessage(key), placeholders);
    }

    // like message(), but embeds an already-built component via a tag, e.g.
    // "Baseplate is now <status>" with placeholderTag "status"
    public Component messageWithComponent(String key, String placeholderTag, Component value) {
        TagResolver resolver = TagResolver.resolver(placeholderTag, Tag.inserting(value));
        return miniMessage.deserialize(rawMessage(key), resolver).decoration(TextDecoration.ITALIC, false);
    }

    public Component prefix() {
        return message("prefix");
    }

    public Component defaultColor(Component message) {
        return applyColorIfUnset(message, "default-color");
    }

    public Component successColor(Component message) {
        return applyColorIfUnset(message, "success-color");
    }

    public Component errorColor(Component message) {
        return applyColorIfUnset(message, "error-color");
    }

    private Component applyColorIfUnset(Component message, String colorKey) {
        if (message.color() != null) {
            return message;
        }
        // plain color name ("gold") or hex code ("#AABBCC"), not a MiniMessage tag,
        // since this only colorizes an already-built component
        String colorValue = rawMessage(colorKey);
        TextColor color = NamedTextColor.NAMES.value(colorValue.toLowerCase(Locale.ROOT));
        if (color == null) {
            color = TextColor.fromHexString(colorValue);
        }
        return color != null ? message.color(color) : message;
    }

    public Component activation(boolean active) {
        return message(active ? "active" : "inactive");
    }

    // items.*

    private ConfigurationSection itemSection(String key) {
        return config.getConfigurationSection("items." + key);
    }

    public Material getItemMaterial(String key, Material fallback) {
        ConfigurationSection section = itemSection(key);
        if (section == null) {
            return fallback;
        }
        String materialName = section.getString("material");
        if (materialName == null) {
            return fallback;
        }
        Material material = Material.matchMaterial(materialName);
        return material != null ? material : fallback;
    }

    private Component getItemName(String key) {
        ConfigurationSection section = itemSection(key);
        String name = section != null ? section.getString("name") : null;
        if (name == null) {
            return Component.empty();
        }
        return miniMessage.deserialize(name).decoration(TextDecoration.ITALIC, false);
    }

    private List<Component> getItemLore(String key) {
        ConfigurationSection section = itemSection(key);
        List<String> lore = section != null ? section.getStringList("lore") : List.of();
        List<Component> result = new ArrayList<>();
        for (String line : lore) {
            result.add(miniMessage.deserialize(line).decoration(TextDecoration.ITALIC, false));
        }
        return result;
    }

    private boolean getItemGlint(String key) {
        ConfigurationSection section = itemSection(key);
        return section != null && section.getBoolean("glint", false);
    }

    private void applyGlint(ItemMeta meta, String key) {
        if (getItemGlint(key)) {
            meta.setEnchantmentGlintOverride(true);
        }
    }

    // hides minecraft's default tooltip lines ("+2 armor", attack damage, etc.) on every
    // menu item. these items are never actually worn/wielded, so slapping harmless
    // zero-effect modifiers on everything is fine even where they don't apply.
    // an item needs at least one modifier for HIDE_ATTRIBUTES to have something to hide,
    // which is why the zero modifiers get added first
    private void hideDefaultTooltipStats(ItemMeta meta) {
        meta.addAttributeModifier(Attribute.ARMOR, new AttributeModifier(HIDE_ARMOR_TOOLTIP_KEY, 0, Operation.ADD_SCALAR));
        meta.addAttributeModifier(Attribute.ARMOR_TOUGHNESS, new AttributeModifier(HIDE_ARMOR_TOOLTIP_KEY, 0, Operation.ADD_SCALAR));
        meta.addAttributeModifier(Attribute.KNOCKBACK_RESISTANCE, new AttributeModifier(HIDE_ARMOR_TOOLTIP_KEY, 0, Operation.ADD_SCALAR));
        meta.addAttributeModifier(Attribute.ATTACK_DAMAGE, new AttributeModifier(HIDE_ARMOR_TOOLTIP_KEY, 0, Operation.ADD_SCALAR));
        meta.addAttributeModifier(Attribute.ATTACK_SPEED, new AttributeModifier(HIDE_ARMOR_TOOLTIP_KEY, 0, Operation.ADD_SCALAR));
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_DYE);
    }

    // builds a menu item with material/name/lore all read from config.yml for this key
    public ItemStack buildItem(String key, Material fallbackMaterial) {
        ItemStack stack = new ItemStack(getItemMaterial(key, fallbackMaterial));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(getItemName(key));
        List<Component> lore = getItemLore(key);
        if (!lore.isEmpty()) {
            meta.lore(lore);
        }
        applyGlint(meta, key);
        hideDefaultTooltipStats(meta);
        stack.setItemMeta(meta);
        return stack;
    }

    // like buildItem(), but the name is computed at runtime (e.g. "x = 12.3°").
    // any extra lore passed in is shown before the static lore from config
    public ItemStack buildDynamicItem(String key, Material fallbackMaterial, Component name, Component... lore) {
        ItemStack stack = new ItemStack(getItemMaterial(key, fallbackMaterial));
        ItemMeta meta = stack.getItemMeta();
        meta.displayName(name.decoration(TextDecoration.ITALIC, false));
        List<Component> loreComponents = new ArrayList<>();
        for (Component line : lore) {
            loreComponents.add(line.decoration(TextDecoration.ITALIC, false));
        }
        loreComponents.addAll(getItemLore(key));
        if (!loreComponents.isEmpty()) {
            meta.lore(loreComponents);
        }
        applyGlint(meta, key);
        hideDefaultTooltipStats(meta);
        stack.setItemMeta(meta);
        return stack;
    }

}
