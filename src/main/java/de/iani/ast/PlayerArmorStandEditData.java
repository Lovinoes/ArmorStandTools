package de.iani.ast;

import de.iani.ast.config.PluginConfig;
import java.text.NumberFormat;
import java.util.Map;
import java.util.function.BooleanSupplier;
import net.kyori.adventure.text.Component;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.EulerAngle;

public class PlayerArmorStandEditData {

    public static enum EditState {
        MainWindow,
        RotationWindow,
        RotationWithoutWindow
    }

    public static enum RotatablePart {
        Head,
        Body,
        LeftArm,
        RightArm,
        LeftLeg,
        RightLeg,
        Position,
        Size
    }

    // ---------------------------------------------------------------
    // General window layout
    // ---------------------------------------------------------------

    private static final int SLOT_BASEPLATE_TOGGLE = 9 * 0 + 1;
    private static final int SLOT_ARMS_TOGGLE = 9 * 1 + 1;
    private static final int SLOT_SMALL_TOGGLE = 9 * 2 + 1;
    private static final int SLOT_UNMOVEABLE_TOGGLE = 9 * 3 + 1;
    private static final int SLOT_INVISIBLE_TOGGLE = 9 * 4 + 1;
    private static final int SLOT_NAME_VISIBLE_TOGGLE = 9 * 5 + 1;

    private static final int SLOT_SIZE_OPEN = 9 * 0 + 4;
    private static final int SLOT_HEAD_ROTATION_OPEN = 9 * 1 + 4;
    private static final int SLOT_BODY_ROTATION_OPEN = 9 * 2 + 4;
    private static final int SLOT_LEFT_ARM_ROTATION_OPEN = 9 * 2 + 3;
    private static final int SLOT_RIGHT_ARM_ROTATION_OPEN = 9 * 2 + 5;
    private static final int SLOT_POSITION_OPEN = 9 * 3 + 4;

    private static final int SLOT_POSITION_BOOTS_OPEN = 9 * 4 + 4;
    private static final int SLOT_LEFT_LEG_ROTATION_OPEN = 9 * 4 + 3;
    private static final int SLOT_RIGHT_LEG_ROTATION_OPEN = 9 * 4 + 5;

    private static final int SLOT_ROTATION_BACK = 9 * 5;

    private static final EquipmentSlot[] EQUIPMENT_BY_ROW = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFF_HAND, EquipmentSlot.HAND
    };

    private final ArmorStandTools plugin;
    private final PluginConfig config;
    private final Player owner;
    private final ArmorStand armorStand;

    private final Inventory armorStandInventory;
    private final ItemStack activeIcon;
    private final ItemStack inactiveIcon;
    private EditState editState;
    private RotatablePart rotationToEdit;
    private Location initialLocation;
    private Location lastLocation;
    private int freeRotationAxis;
    private int freeMoveStartTick;

    public PlayerArmorStandEditData(ArmorStandTools plugin, Player owner, ArmorStand armorStand) {
        this.plugin = plugin;
        this.config = plugin.getPluginConfig();
        this.owner = owner;
        this.armorStand = armorStand;
        this.editState = EditState.MainWindow;
        this.activeIcon = config.buildItem("toggle-active", Material.GREEN_DYE);
        this.inactiveIcon = config.buildItem("toggle-inactive", Material.RED_DYE);

        Component name = armorStand.customName();
        if (name == null) {
            name = Component.text(config.getDefaultArmorStandName());
        }
        this.armorStandInventory = plugin.getServer().createInventory(owner, 9 * 6, config.guiTitle(name));

        editGeneral();
        owner.openInventory(armorStandInventory);
    }

    public Inventory getInventory() {
        return armorStandInventory;
    }

    public EditState getEditState() {
        return editState;
    }

    public ArmorStand getArmorStand() {
        return armorStand;
    }

    public int getFreeMoveStartTick() {
        return freeMoveStartTick;
    }

    // =================================================================
    // General window
    // =================================================================

    private void editGeneral() {
        this.editState = EditState.MainWindow;
        armorStandInventory.clear();

        armorStandInventory.setItem(9 * 0, config.buildItem("baseplate", Material.STONE_PRESSURE_PLATE));
        updateHasBasePlate();
        armorStandInventory.setItem(9 * 1, config.buildItem("arms", Material.STICK));
        updateHasArms();
        armorStandInventory.setItem(9 * 2, config.buildItem("small", Material.CLAY_BALL));
        updateIsSmall();
        armorStandInventory.setItem(9 * 3, config.buildItem("unmoveable", Material.RAIL));
        updateIsUnmoveable();
        armorStandInventory.setItem(9 * 4, config.buildItem("invisible", Material.POTION));
        updateIsInvisible();
        armorStandInventory.setItem(9 * 5, config.buildItem("name-visible", Material.NAME_TAG));
        updateNameIsVisible();

        armorStandInventory.setItem(9 * 0 + 7, config.buildItem("helmet", Material.LEATHER_HELMET));
        armorStandInventory.setItem(9 * 1 + 7, config.buildItem("chestplate", Material.LEATHER_CHESTPLATE));
        armorStandInventory.setItem(9 * 2 + 7, config.buildItem("leggings", Material.LEATHER_LEGGINGS));
        armorStandInventory.setItem(9 * 3 + 7, config.buildItem("boots", Material.LEATHER_BOOTS));
        armorStandInventory.setItem(9 * 4 + 7, config.buildItem("left-hand", Material.STICK));
        armorStandInventory.setItem(9 * 5 + 7, config.buildItem("right-hand", Material.STICK));
        updateArmorstandInventory();
        updateArmorstandInventoryLater();

        armorStandInventory.setItem(SLOT_HEAD_ROTATION_OPEN, config.buildItem("head-rotation", Material.IRON_HELMET));
        armorStandInventory.setItem(SLOT_BODY_ROTATION_OPEN, config.buildItem("body-rotation", Material.IRON_CHESTPLATE));
        armorStandInventory.setItem(SLOT_LEFT_ARM_ROTATION_OPEN, config.buildItem("left-arm-rotation", Material.STICK));
        armorStandInventory.setItem(SLOT_RIGHT_ARM_ROTATION_OPEN, config.buildItem("right-arm-rotation", Material.STICK));

        armorStandInventory.setItem(SLOT_POSITION_OPEN, config.buildItem("position", Material.IRON_LEGGINGS));
        armorStandInventory.setItem(SLOT_LEFT_LEG_ROTATION_OPEN, config.buildItem("left-leg-rotation", Material.STICK));
        armorStandInventory.setItem(SLOT_RIGHT_LEG_ROTATION_OPEN, config.buildItem("right-leg-rotation", Material.STICK));
        armorStandInventory.setItem(SLOT_POSITION_BOOTS_OPEN, config.buildItem("position-boots", Material.IRON_BOOTS));

        armorStandInventory.setItem(SLOT_SIZE_OPEN, config.buildItem("size", Material.PUFFERFISH));
    }

    private void setStatusIcon(int slot, boolean active) {
        if (editState == EditState.MainWindow) {
            armorStandInventory.setItem(slot, active ? activeIcon : inactiveIcon);
        }
    }

    private void toggle(Runnable mutate, BooleanSupplier activeAfter, Runnable updateIcon, String messageKey) {
        mutate.run();
        Messages.send(owner, config.messageWithComponent(messageKey, "status", Messages.activation(activeAfter.getAsBoolean())));
        updateIcon.run();
    }

    private void updateHasBasePlate() {
        setStatusIcon(SLOT_BASEPLATE_TOGGLE, armorStand.hasBasePlate());
    }

    private void toggleHasBasePlate() {
        toggle(() -> armorStand.setBasePlate(!armorStand.hasBasePlate()), armorStand::hasBasePlate, this::updateHasBasePlate, "baseplate-toggled");
    }

    private void updateHasArms() {
        setStatusIcon(SLOT_ARMS_TOGGLE, armorStand.hasArms());
    }

    private void toggleHasArms() {
        toggle(() -> armorStand.setArms(!armorStand.hasArms()), armorStand::hasArms, this::updateHasArms, "arms-toggled");
    }

    private void updateIsSmall() {
        setStatusIcon(SLOT_SMALL_TOGGLE, armorStand.isSmall());
    }

    private void toggleIsSmall() {
        toggle(() -> armorStand.setSmall(!armorStand.isSmall()), armorStand::isSmall, this::updateIsSmall, "small-toggled");
    }

    private void updateIsUnmoveable() {
        setStatusIcon(SLOT_UNMOVEABLE_TOGGLE, !armorStand.hasGravity());
    }

    private void toggleIsUnmoveable() {
        toggle(() -> armorStand.setGravity(!armorStand.hasGravity()), () -> !armorStand.hasGravity(), this::updateIsUnmoveable, "unmoveable-toggled");
    }

    private void updateIsInvisible() {
        setStatusIcon(SLOT_INVISIBLE_TOGGLE, !armorStand.isVisible());
    }

    private void toggleIsInvisible() {
        toggle(() -> armorStand.setVisible(!armorStand.isVisible()), () -> !armorStand.isVisible(), this::updateIsInvisible, "invisible-toggled");
    }

    private void updateNameIsVisible() {
        setStatusIcon(SLOT_NAME_VISIBLE_TOGGLE, armorStand.isCustomNameVisible());
    }

    private void toggleNameIsVisible() {
        toggle(() -> armorStand.setCustomNameVisible(!armorStand.isCustomNameVisible()), armorStand::isCustomNameVisible, this::updateNameIsVisible, "name-visible-toggled");
    }

    private void updateArmorstandInventory() {
        if (editState == EditState.MainWindow) {
            for (int row = 0; row < EQUIPMENT_BY_ROW.length; row++) {
                armorStandInventory.setItem(9 * row + 8, armorStand.getEquipment().getItem(EQUIPMENT_BY_ROW[row]));
            }
        }
    }

    private void updateArmorstandInventoryLater() {
        if (editState == EditState.MainWindow) {
            plugin.getServer().getScheduler().runTask(plugin, this::updateArmorstandInventory);
        }
    }

    // =================================================================
    // Rotation window
    // =================================================================

    private void updateRotationInventory() {
        double x = 0;
        double y = 0;
        double z = 0;
        double yaw = 0;

        EulerAngle angle = null;
        final boolean isMove = rotationToEdit == RotatablePart.Position;
        String unit = isMove ? "" : "°";
        if (rotationToEdit == RotatablePart.Head) {
            angle = armorStand.getHeadPose();
        } else if (rotationToEdit == RotatablePart.Body) {
            angle = armorStand.getBodyPose();
        } else if (rotationToEdit == RotatablePart.LeftArm) {
            angle = armorStand.getLeftArmPose();
        } else if (rotationToEdit == RotatablePart.RightArm) {
            angle = armorStand.getRightArmPose();
        } else if (rotationToEdit == RotatablePart.LeftLeg) {
            angle = armorStand.getLeftLegPose();
        } else if (rotationToEdit == RotatablePart.RightLeg) {
            angle = armorStand.getRightLegPose();
        } else if (rotationToEdit == RotatablePart.Size) {
            x = armorStand.getAttribute(Attribute.SCALE).getBaseValue();
            NumberFormat format = NumberFormat.getNumberInstance();
            Component itemName = config.message("format-scale-item", Map.of("value", format.format(x)));
            armorStandInventory.setItem(9 * 0 + 3, config.buildDynamicItem("value-display", Material.YELLOW_DYE, itemName, config.message("lore-reset-one")));
            return;
        } else if (isMove) {
            x = armorStand.getLocation().getX();
            y = armorStand.getLocation().getY();
            z = armorStand.getLocation().getZ();
            yaw = armorStand.getLocation().getYaw();
        }
        if (angle != null) {
            x = angle.getX();
            y = angle.getY();
            z = angle.getZ();
        }

        NumberFormat format = NumberFormat.getNumberInstance();
        Component nameX = config.message("format-axis", Map.of("axis", "x", "value", format.format(isMove ? x : radToDegree(x)), "unit", unit));
        Component nameY = config.message("format-axis", Map.of("axis", "y", "value", format.format(isMove ? y : radToDegree(y)), "unit", unit));
        Component nameZ = config.message("format-axis", Map.of("axis", "z", "value", format.format(isMove ? z : radToDegree(z)), "unit", unit));

        ItemStack stackx;
        ItemStack stacky;
        ItemStack stackz;
        if (rotationToEdit != RotatablePart.Position) {
            Component resetLore = config.message("lore-reset-zero");
            stackx = config.buildDynamicItem("value-display", Material.YELLOW_DYE, nameX, resetLore);
            stacky = config.buildDynamicItem("value-display", Material.YELLOW_DYE, nameY, resetLore);
            stackz = config.buildDynamicItem("value-display", Material.YELLOW_DYE, nameZ, resetLore);
        } else {
            stackx = config.buildDynamicItem("value-display", Material.YELLOW_DYE, nameX);
            stacky = config.buildDynamicItem("value-display", Material.YELLOW_DYE, nameY);
            stackz = config.buildDynamicItem("value-display", Material.YELLOW_DYE, nameZ);
        }
        armorStandInventory.setItem(9 * 0 + 3, stackx);
        armorStandInventory.setItem(9 * 1 + 3, stacky);
        armorStandInventory.setItem(9 * 2 + 3, stackz);
        if (isMove) {
            Component yawName = config.message("format-yaw", Map.of("label", "yaw", "value", format.format(yaw)));
            armorStandInventory.setItem(9 * 3 + 3, config.buildDynamicItem("value-display", Material.YELLOW_DYE, yawName, config.message("lore-reset-zero")));
        }
    }

    private void openRotationMenu(RotatablePart part) {
        editState = EditState.RotationWindow;
        rotationToEdit = part;
        armorStandInventory.clear();

        if (part == RotatablePart.Position) {
            for (int row = 0; row < 4; row++) {
                if (row < 3) {
                    setPositionRowItems(row);
                } else {
                    setRotationRowItems(row);
                }
            }
        } else if (part == RotatablePart.Size) {
            setPositionRowItems(0);
        } else {
            for (int row = 0; row < 3; row++) {
                setRotationRowItems(row);
            }
        }

        armorStandInventory.setItem(SLOT_ROTATION_BACK, config.buildItem("back-button", Material.MAGENTA_DYE));

        updateRotationInventory();
    }

    private void setPositionRowItems(int row) {
        armorStandInventory.setItem(9 * row + 0, deltaButton(false, "-1.0", "-0.001"));
        armorStandInventory.setItem(9 * row + 1, deltaButton(false, "-0.1", "-0.0001"));
        armorStandInventory.setItem(9 * row + 2, deltaButton(false, "-0.01", "-0.00001"));
        armorStandInventory.setItem(9 * row + 4, deltaButton(true, "+0.01", "+0.00001"));
        armorStandInventory.setItem(9 * row + 5, deltaButton(true, "+0.1", "+0.0001"));
        armorStandInventory.setItem(9 * row + 6, deltaButton(true, "+1.0", "+0.001"));
        armorStandInventory.setItem(9 * row + 8, config.buildItem("free-edit-button", Material.CYAN_DYE));
    }

    private void setRotationRowItems(int row) {
        armorStandInventory.setItem(9 * row + 0, deltaButton(false, "-90.0°", "-0.1°"));
        armorStandInventory.setItem(9 * row + 1, deltaButton(false, "-10.0°", "-0.01°"));
        armorStandInventory.setItem(9 * row + 2, deltaButton(false, "-1.0°", "-0.001°"));
        armorStandInventory.setItem(9 * row + 4, deltaButton(true, "+1.0°", "+0.01°"));
        armorStandInventory.setItem(9 * row + 5, deltaButton(true, "+10.0°", "+0.01°"));
        armorStandInventory.setItem(9 * row + 6, deltaButton(true, "+90.0°", "+0.1°"));
        armorStandInventory.setItem(9 * row + 8, config.buildItem("free-edit-button", Material.CYAN_DYE));
    }

    private ItemStack deltaButton(boolean positive, String label, String shiftLabel) {
        String key = positive ? "delta-positive" : "delta-negative";
        Material fallback = positive ? Material.GREEN_DYE : Material.RED_DYE;
        Component name = Component.text(label);
        Component lore = config.message("format-shift-lore", Map.of("value", shiftLabel));
        return config.buildDynamicItem(key, fallback, name, lore);
    }

    private double degreeToRad(double degree) {
        return degree * (Math.PI / 180.0);
    }

    private double radToDegree(double rad) {
        return rad * (180.0 / Math.PI);
    }

    // =================================================================
    // Click / drag routing
    // =================================================================

    private void handleModificationSlotClick(int slot, boolean shift) {
        if (editState == EditState.MainWindow) {
            handleGeneralWindowClick(slot);
        } else if (editState == EditState.RotationWindow) {
            handleRotationWindowClick(slot, shift);
        }
    }

    private void handleGeneralWindowClick(int slot) {
        if (slot == SLOT_BASEPLATE_TOGGLE) {
            toggleHasBasePlate();
        } else if (slot == SLOT_ARMS_TOGGLE) {
            toggleHasArms();
        } else if (slot == SLOT_SMALL_TOGGLE) {
            toggleIsSmall();
        } else if (slot == SLOT_UNMOVEABLE_TOGGLE) {
            toggleIsUnmoveable();
        } else if (slot == SLOT_INVISIBLE_TOGGLE) {
            toggleIsInvisible();
        } else if (slot == SLOT_NAME_VISIBLE_TOGGLE) {
            toggleNameIsVisible();
        } else if (slot == SLOT_HEAD_ROTATION_OPEN) {
            openRotationMenu(RotatablePart.Head);
        } else if (slot == SLOT_BODY_ROTATION_OPEN) {
            openRotationMenu(RotatablePart.Body);
        } else if (slot == SLOT_LEFT_ARM_ROTATION_OPEN) {
            openRotationMenu(RotatablePart.LeftArm);
        } else if (slot == SLOT_RIGHT_ARM_ROTATION_OPEN) {
            openRotationMenu(RotatablePart.RightArm);
        } else if (slot == SLOT_LEFT_LEG_ROTATION_OPEN) {
            openRotationMenu(RotatablePart.LeftLeg);
        } else if (slot == SLOT_RIGHT_LEG_ROTATION_OPEN) {
            openRotationMenu(RotatablePart.RightLeg);
        } else if (slot == SLOT_POSITION_OPEN) {
            openRotationMenu(RotatablePart.Position);
        } else if (slot == SLOT_POSITION_BOOTS_OPEN) {
            openRotationMenu(RotatablePart.Position);
        } else if (slot == SLOT_SIZE_OPEN) {
            openRotationMenu(RotatablePart.Size);
        }
    }

    private void handleRotationWindowClick(int slot, boolean shift) {
        int row = slot / 9;
        int slotInRow = slot % 9;

        int rowsAvailable = 3;
        boolean isDegreeRow = true;
        if (rotationToEdit == RotatablePart.Position) {
            rowsAvailable = 4;
            if (row < 3) {
                isDegreeRow = false;
            }
        } else if (rotationToEdit == RotatablePart.Size) {
            rowsAvailable = 1;
            isDegreeRow = false;
        }

        if (row >= 0 && row < rowsAvailable) {
            if (slotInRow >= 0 && slotInRow <= 6) {
                double delta = deltaForSlot(slotInRow, isDegreeRow, shift);
                if (delta != 0.0) {
                    applyRotationDelta(row, delta);
                }
            }
            if (slotInRow == 8) {
                startFreeEdit(row);
            }
        } else if (slot == SLOT_ROTATION_BACK) {
            editGeneral();
        }
    }

    private double deltaForSlot(int slotInRow, boolean isDegreeRow, boolean shift) {
        double delta;
        if (slotInRow == 0) {
            delta = !isDegreeRow ? -1 : degreeToRad(-90);
        } else if (slotInRow == 1) {
            delta = !isDegreeRow ? -0.1 : degreeToRad(-10);
        } else if (slotInRow == 2) {
            delta = !isDegreeRow ? -0.01 : degreeToRad(-1);
        } else if (slotInRow == 3) {

            delta = Double.NaN;
        } else if (slotInRow == 4) {
            delta = !isDegreeRow ? 0.01 : degreeToRad(1);
        } else if (slotInRow == 5) {
            delta = !isDegreeRow ? 0.1 : degreeToRad(10);
        } else if (slotInRow == 6) {
            delta = !isDegreeRow ? 1 : degreeToRad(90);
        } else {
            return 0.0;
        }
        if (shift && !Double.isNaN(delta)) {
            if (!isDegreeRow || (slotInRow != 0 && slotInRow != 6)) {
                delta *= 0.001;
            } else {
                delta = degreeToRad(slotInRow == 0 ? -0.1 : 0.1);
            }
        }
        return delta;
    }

    private void startFreeEdit(int row) {
        editState = EditState.RotationWithoutWindow;
        initialLocation = owner.getLocation();
        lastLocation = initialLocation.clone();
        freeRotationAxis = row;
        freeMoveStartTick = plugin.getServer().getCurrentTick();
        owner.closeInventory();
        Messages.send(owner, config.message("free-edit-activated"));
        Messages.send(owner, config.message("free-edit-instruction"));
        Messages.sendSuccess(owner, config.message("free-edit-confirm"));
    }

    public void onInventoryClicked(int slot, InventoryClickEvent event) {
        int col = slot % 9;
        if (editState == EditState.MainWindow && col == 8) {
            boolean cancel = true;
            if (event.getAction() == InventoryAction.PICKUP_ALL || event.getAction() == InventoryAction.SWAP_WITH_CURSOR) {
                if (placeItemInSlot(slot, event.getCursor())) {
                    cancel = false;
                }
            } else if (event.getAction() == InventoryAction.PLACE_ALL) {
                ItemStack stack = new ItemStack(event.getCursor());
                ItemStack existing = armorStandInventory.getItem(slot);
                if (existing == null || existing.isSimilar(stack)) {
                    if (existing != null) {
                        stack.setAmount(stack.getAmount() + existing.getAmount());
                    }
                    if (placeItemInSlot(slot, stack)) {
                        cancel = false;
                    }
                }
            } else if (event.getAction() == InventoryAction.PLACE_ONE) {
                ItemStack stack = new ItemStack(event.getCursor());
                ItemStack existing = armorStandInventory.getItem(slot);
                if (existing == null || existing.isSimilar(stack)) {
                    stack.setAmount(1);
                    if (existing != null) {
                        stack.setAmount(stack.getAmount() + existing.getAmount());
                    }
                    if (placeItemInSlot(slot, stack)) {
                        cancel = false;
                    }
                }
            }
            if (cancel) {
                event.setCancelled(true);
            }
        } else {
            handleModificationSlotClick(slot, event.isShiftClick());
            event.setCancelled(true);
        }
    }

    public void onInventoryDrag(int slot, ItemStack newInSlot, InventoryDragEvent event) {
        int col = slot % 9;
        if (editState == EditState.MainWindow && col == 8) {
            if (!placeItemInSlot(slot, newInSlot)) {
                event.setCancelled(true);
            }
        } else {
            handleModificationSlotClick(slot, false);
            event.setCancelled(true);
        }
    }

    private boolean placeItemInSlot(int slot, ItemStack newInSlot) {
        updateArmorstandInventoryLater();
        int row = slot / 9;
        int col = slot % 9;
        if (col == 8 && row >= 0 && row < EQUIPMENT_BY_ROW.length) {
            EquipmentSlot equipmentSlot = EQUIPMENT_BY_ROW[row];
            if (ArmorStandTools.itemStackEquals(armorStand.getEquipment().getItem(equipmentSlot), armorStandInventory.getItem(slot))) {
                armorStand.getEquipment().setItem(equipmentSlot, newInSlot);
                return true;
            }
        }
        return false;
    }

    public void onPlayerMove(Location to) {
        if (editState == EditState.RotationWithoutWindow && lastLocation != null && initialLocation != null) {
            double maxDistance = config.getMaxFreeEditDistance();
            if (to.getWorld() != initialLocation.getWorld() || to.distanceSquared(initialLocation) > maxDistance * maxDistance) {
                plugin.stopEditing(owner, true);
                Messages.sendSuccess(owner, config.message("free-edit-cancelled"));
            } else {
                double dx = to.getX() - lastLocation.getX();
                double dz = to.getZ() - lastLocation.getZ();
                double dtotal = (dx + dz) * 0.5;
                if (dtotal != 0) {
                    applyRotationDelta(freeRotationAxis, dtotal);
                    lastLocation = to.clone();
                }
            }
        }
    }

    private void applyRotationDelta(int axis, double diff) {
        NumberFormat format = NumberFormat.getNumberInstance();
        format.setMaximumFractionDigits(5);

        EulerAngle angle = null;
        if (rotationToEdit == RotatablePart.Head) {
            angle = armorStand.getHeadPose();
        } else if (rotationToEdit == RotatablePart.Body) {
            angle = armorStand.getBodyPose();
        } else if (rotationToEdit == RotatablePart.LeftArm) {
            angle = armorStand.getLeftArmPose();
        } else if (rotationToEdit == RotatablePart.RightArm) {
            angle = armorStand.getRightArmPose();
        } else if (rotationToEdit == RotatablePart.LeftLeg) {
            angle = armorStand.getLeftLegPose();
        } else if (rotationToEdit == RotatablePart.RightLeg) {
            angle = armorStand.getRightLegPose();
        } else if (rotationToEdit == RotatablePart.Size) {
            if (axis == 0) {
                AttributeInstance scaleAttribute = armorStand.getAttribute(Attribute.SCALE);
                double value = Double.isFinite(diff) ? scaleAttribute.getBaseValue() + diff : 1.0;
                if (value < 1f / 16f) {
                    value = 1f / 16f;
                }
                double max = owner.hasPermission("ArmorStandTools.unlimitedsize") ? config.getUnlimitedSizeLimit() : config.getDefaultSizeLimit();
                if (value > max) {
                    value = max;
                }
                scaleAttribute.setBaseValue(value);
                if (editState != EditState.RotationWithoutWindow) {
                    Messages.send(owner, config.message("format-scale-chat", Map.of("value", format.format(value))));
                }
            }
        } else if (rotationToEdit == RotatablePart.Position) {
            Location location = armorStand.getLocation();
            if (axis == 0 && Double.isFinite(diff)) {
                location.setX(location.getX() + diff);
                if (editState != EditState.RotationWithoutWindow) {
                    Messages.send(owner, config.message("format-axis", Map.of("axis", "X", "value", format.format(location.getX()), "unit", "")));
                }
            } else if (axis == 1 && Double.isFinite(diff)) {
                location.setY(Math.max(Math.min(location.getY() + diff, location.getWorld().getMaxHeight()), location.getWorld().getMinHeight()));
                if (editState != EditState.RotationWithoutWindow) {
                    Messages.send(owner, config.message("format-axis", Map.of("axis", "Y", "value", format.format(location.getY()), "unit", "")));
                }
            } else if (axis == 2 && Double.isFinite(diff)) {
                location.setZ(location.getZ() + diff);
                if (editState != EditState.RotationWithoutWindow) {
                    Messages.send(owner, config.message("format-axis", Map.of("axis", "Z", "value", format.format(location.getZ()), "unit", "")));
                }
            } else if (axis == 3) {
                location.setYaw(Double.isFinite(diff) ? ((float) (location.getYaw() + radToDegree(diff))) : 0.0f);
                if (editState != EditState.RotationWithoutWindow) {
                    Messages.send(owner, config.message("format-yaw", Map.of("label", "Yaw", "value", format.format(location.getYaw()))));
                }
            }
            ArmorStandTeleportEvent event = new ArmorStandTeleportEvent(armorStand, location, owner);
            plugin.getServer().getPluginManager().callEvent(event);
            if (!event.isCancelled()) {
                armorStand.teleport(location);
                if (rotationToEdit == RotatablePart.Position && axis == 1) {
                    armorStand.setGravity(false);
                }
            }
        }
        if (angle != null) {
            if (axis == 0) {
                angle = angle.setX(Double.isFinite(diff) ? (angle.getX() + diff) : 0.0f);
                if (editState != EditState.RotationWithoutWindow) {
                    Messages.send(owner, config.message("format-axis", Map.of("axis", "X", "value", format.format(radToDegree(angle.getX())), "unit", "°")));
                }
            } else if (axis == 1) {
                angle = angle.setY(Double.isFinite(diff) ? (angle.getY() + diff) : 0.0f);
                if (editState != EditState.RotationWithoutWindow) {
                    Messages.send(owner, config.message("format-axis", Map.of("axis", "Y", "value", format.format(radToDegree(angle.getY())), "unit", "°")));
                }
            } else if (axis == 2) {
                angle = angle.setZ(Double.isFinite(diff) ? (angle.getZ() + diff) : 0.0f);
                if (editState != EditState.RotationWithoutWindow) {
                    Messages.send(owner, config.message("format-axis", Map.of("axis", "Z", "value", format.format(radToDegree(angle.getZ())), "unit", "°")));
                }
            }

            if (rotationToEdit == RotatablePart.Head) {
                armorStand.setHeadPose(angle);
            } else if (rotationToEdit == RotatablePart.Body) {
                armorStand.setBodyPose(angle);
            } else if (rotationToEdit == RotatablePart.LeftArm) {
                armorStand.setLeftArmPose(angle);
            } else if (rotationToEdit == RotatablePart.RightArm) {
                armorStand.setRightArmPose(angle);
            } else if (rotationToEdit == RotatablePart.LeftLeg) {
                armorStand.setLeftLegPose(angle);
            } else if (rotationToEdit == RotatablePart.RightLeg) {
                armorStand.setRightLegPose(angle);
            }
        }

        updateRotationInventory();
    }
}
