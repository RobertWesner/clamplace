package io.wesner.robert.cb1060.clamplace

import org.bukkit.Material

object BlockGroup {
    val allBlocks = materialRange(1..96)

    val solid = allBlocks.filter { it !in setOf(
        Material.SAPLING,
        Material.WATER,
        Material.STATIONARY_WATER,
        Material.LAVA,
        Material.STATIONARY_LAVA,
        Material.LEAVES,
        Material.GLASS,
        Material.BED_BLOCK,
        Material.POWERED_RAIL,
        Material.DETECTOR_RAIL,
        Material.WEB,
        Material.LONG_GRASS,
        Material.DEAD_BUSH,
        Material.PISTON_EXTENSION,
        Material.PISTON_MOVING_PIECE,
        Material.YELLOW_FLOWER,
        Material.RED_ROSE,
        Material.BROWN_MUSHROOM,
        Material.RED_MUSHROOM,
        Material.STEP,
        Material.TNT,
        Material.TORCH,
        Material.FIRE,
        Material.WOOD_STAIRS,
        Material.CHEST,
        Material.REDSTONE_WIRE,
        Material.CROPS,
        Material.SOIL,
        Material.SIGN_POST,
        Material.WOODEN_DOOR,
        Material.RAILS,
        Material.COBBLESTONE_STAIRS,
        Material.WALL_SIGN,
        Material.LEVER,
        Material.STONE_PLATE,
        Material.IRON_DOOR,
        Material.WOOD_PLATE,
        Material.REDSTONE_TORCH_OFF,
        Material.REDSTONE_TORCH_ON,
        Material.STONE_BUTTON,
        Material.SNOW,
        Material.ICE,
        Material.CACTUS,
        Material.SUGAR_CANE_BLOCK,
        Material.FENCE,
        Material.PORTAL,
        Material.CAKE_BLOCK,
        Material.DIODE_BLOCK_OFF,
        Material.DIODE_BLOCK_ON,
        Material.LOCKED_CHEST,
        Material.TRAP_DOOR,
    ) }

    val interactable = setOf(
        Material.NOTE_BLOCK,
        Material.BED_BLOCK,
        Material.CHEST,
        Material.WORKBENCH,
        Material.FURNACE,
        Material.BURNING_FURNACE,
        Material.WOODEN_DOOR,
        Material.LEVER,
        Material.STONE_PLATE,
        Material.IRON_DOOR,
        Material.WOOD_PLATE,
        Material.STONE_BUTTON,
        Material.JUKEBOX,
        Material.CAKE_BLOCK,
        Material.LOCKED_CHEST,
        Material.TRAP_DOOR,
    )

    val replaceable = setOf(
        Material.AIR,
        Material.WATER,
        Material.STATIONARY_WATER,
        Material.LAVA,
        Material.STATIONARY_LAVA,
        Material.FIRE,
        Material.SNOW,
    )

    val alwaysAllowed = setOf(
        Material.FENCE,
        Material.PUMPKIN,
        Material.JACK_O_LANTERN,
    )

    val playerAngledBlocks = setOf(
        Material.DISPENSER,
        Material.PISTON_STICKY_BASE,
        Material.PISTON_BASE,
        Material.WOOD_STAIRS,
        Material.FURNACE,
        Material.COBBLESTONE_STAIRS,
        Material.PUMPKIN,
        Material.JACK_O_LANTERN,
        Material.WOODEN_DOOR,
        Material.IRON_DOOR_BLOCK,
    )

    val bypassOccupied = setOf(
        Material.LADDER,
        Material.STONE_BUTTON,
        Material.LEVER,
        Material.WATER_BUCKET,
        Material.LAVA_BUCKET,
        Material.BUCKET,
        Material.TORCH,
        Material.REDSTONE_TORCH_ON,
        Material.REDSTONE_TORCH_OFF,
        Material.SAPLING,
        Material.LONG_GRASS,
        Material.DEAD_BUSH,
        Material.YELLOW_FLOWER,
        Material.RED_ROSE,
    )

    val requireSolidToAttachTo = setOf(
        Material.TORCH,
        Material.LADDER,
        Material.LEVER,
        Material.REDSTONE_TORCH_OFF,
        Material.REDSTONE_TORCH_ON,
        Material.STONE_BUTTON,
        Material.TRAP_DOOR,
    )

    val requireBottomSupportOrAttachTo = setOf(
        Material.TORCH,
        Material.REDSTONE_TORCH_OFF,
        Material.REDSTONE_TORCH_ON,
        Material.LEVER,
    )

    val requireBottomSupport = setOf(
        Material.POWERED_RAIL,
        Material.DETECTOR_RAIL,
        Material.RAILS,
        Material.STONE_PLATE,
        Material.WOOD_PLATE,
        Material.SNOW,
        Material.BROWN_MUSHROOM,
        Material.RED_MUSHROOM,
        Material.FIRE,
        Material.REDSTONE_WIRE,
        Material.CAKE_BLOCK,
        Material.CAKE,
        Material.WOOD_DOOR,
        Material.IRON_DOOR,
        Material.REDSTONE,
        Material.DIODE_BLOCK_ON,
        Material.DIODE_BLOCK_OFF,
        Material.DIODE,
    )

    val requireBottomDirt = setOf(
        Material.SAPLING,
        Material.LONG_GRASS,
        Material.YELLOW_FLOWER,
        Material.RED_ROSE,
    )

    val step = setOf(Material.STEP, Material.DOUBLE_STEP)
    val stair = setOf(Material.COBBLESTONE_STAIRS, Material.WOOD_STAIRS)
    val doorItem = setOf(Material.WOOD_DOOR, Material.IRON_DOOR)

    private fun materialRange(range: IntRange) = range.map { Material.getMaterial(it) }.toSet()
}
