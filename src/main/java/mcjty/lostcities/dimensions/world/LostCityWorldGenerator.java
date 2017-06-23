package mcjty.lostcities.dimensions.world;

import mcjty.lostcities.config.LostCityConfiguration;
import mcjty.lostcities.dimensions.world.lost.BuildingInfo;
import net.minecraft.block.BlockChest;
import net.minecraft.block.BlockSapling;
import net.minecraft.block.BlockVine;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.MobSpawnerBaseLogic;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityChest;
import net.minecraft.tileentity.TileEntityMobSpawner;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.datafix.fixes.EntityId;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.chunk.IChunkProvider;
import net.minecraft.world.gen.IChunkGenerator;
import net.minecraft.world.storage.loot.LootTableList;
import net.minecraftforge.fml.common.IWorldGenerator;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Random;

import static mcjty.lostcities.dimensions.world.lost.DamageArea.BLOCK_DAMAGE_CHANCE;

public class LostCityWorldGenerator implements IWorldGenerator {

    private static final EntityId FIXER = new EntityId();

    public static String fixEntityId(String id) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("id", id);
        nbt = FIXER.fixTagCompound(nbt);
        return nbt.getString("id");
    }

    @Override
    public void generate(Random random, int chunkX, int chunkZ, World world, IChunkGenerator chunkGenerator, IChunkProvider chunkProvider) {
        if (chunkGenerator instanceof LostCityChunkGenerator) {
            generateLootSpawners(random, chunkX, chunkZ, world, (LostCityChunkGenerator) chunkGenerator);
            generateVines(random, chunkX, chunkZ, world, (LostCityChunkGenerator) chunkGenerator);
            generateTrees(random, chunkX, chunkZ, world, (LostCityChunkGenerator) chunkGenerator);
        }
    }

    private void generateTrees(Random random, int chunkX, int chunkZ, World world, LostCityChunkGenerator provider) {
        int cx = chunkX * 16;
        int cz = chunkZ * 16;
        BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, provider);
        for (BlockPos cpos : info.getSaplingTodo()) {
            BlockPos pos = cpos.add(cx, 0, cz);
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == Blocks.SAPLING) {
                ((BlockSapling)Blocks.SAPLING).generateTree(world, pos, state, random);
            }
        }
        info.clearSaplingTodo();
    }

    private void generateVines(Random random, int chunkX, int chunkZ, World world, LostCityChunkGenerator provider) {
        int cx = chunkX * 16;
        int cz = chunkZ * 16;
        BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, provider);

        int bottom = Math.max(info.getCityGroundLevel() + 3, info.hasBuilding ? info.getMaxHeight() : (info.getCityGroundLevel() + 3));

        if (info.getXmin().hasBuilding) {
            if (info.getXmin().getDamageArea().getDamageFactor() < .4f) {
                for (int z = 0; z < 15; z++) {
                    for (int y = bottom; y < (info.getXmin().getMaxHeight()-6); y++) {
                        if (random.nextFloat() < provider.profile.VINE_CHANCE) {
                            if (info.getDamageArea().getDamage(0, y, z) < BLOCK_DAMAGE_CHANCE) {
                                createVineStrip(random, world, bottom, y, BlockVine.WEST, cx + 0, cz + z);
                            }
                        }
                    }
                }
            }
        }
        if (info.getXmax().hasBuilding) {
            if (info.getXmax().getDamageArea().getDamageFactor() < .4f) {
                for (int z = 0; z < 15; z++) {
                    for (int y = bottom; y < (info.getXmax().getMaxHeight()-6); y++) {
                        if (random.nextFloat() < provider.profile.VINE_CHANCE) {
                            if (info.getDamageArea().getDamage(15, y, z) < BLOCK_DAMAGE_CHANCE) {
                                createVineStrip(random, world, bottom, y, BlockVine.EAST, cx + 15, cz + z);
                            }
                        }
                    }
                }
            }
        }
        if (info.getZmin().hasBuilding) {
            if (info.getZmin().getDamageArea().getDamageFactor() < .4f) {
                for (int x = 0; x < 15; x++) {
                    for (int y = bottom; y < (info.getZmin().getMaxHeight()-6); y++) {
                        if (random.nextFloat() < provider.profile.VINE_CHANCE) {
                            if (info.getDamageArea().getDamage(x, y, 0) < BLOCK_DAMAGE_CHANCE) {
                                createVineStrip(random, world, bottom, y, BlockVine.NORTH, cx + x, cz + 0);
                            }
                        }
                    }
                }
            }
        }
        if (info.getZmax().hasBuilding) {
            if (info.getZmax().getDamageArea().getDamageFactor() < .4f) {
                for (int x = 0; x < 15; x++) {
                    for (int y = bottom; y < (info.getMaxHeight()-6); y++) {
                        if (random.nextFloat() < provider.profile.VINE_CHANCE) {
                            if (info.getDamageArea().getDamage(x, y, 15) < BLOCK_DAMAGE_CHANCE) {
                                createVineStrip(random, world, bottom, y, BlockVine.SOUTH, cx + x, cz + 15);
                            }
                        }
                    }
                }
            }
        }
    }

    private void createVineStrip(Random random, World world, int bottom, int y, PropertyBool direction, int vinex, int vinez) {
        world.setBlockState(new BlockPos(vinex, y, vinez), Blocks.VINE.getDefaultState().withProperty(direction, true));
        int yy = y-1;
        while (yy >= bottom && random.nextFloat() < .8f) {
            world.setBlockState(new BlockPos(vinex, yy, vinez), Blocks.VINE.getDefaultState().withProperty(direction, true));
            yy--;
        }
    }

    private void generateLootSpawners(Random random, int chunkX, int chunkZ, World world, LostCityChunkGenerator chunkGenerator) {
        int cx = chunkX * 16;
        int cz = chunkZ * 16;

        BuildingInfo info = BuildingInfo.getBuildingInfo(chunkX, chunkZ, chunkGenerator);

        for (Pair<BlockPos, String> pair : info.getMobSpawnerTodo()) {
            BlockPos pos = pair.getKey().add(cx, 0, cz);
            // Double check that it is still a spawner (could be destroyed by explosion)
            if (world.getBlockState(pos).getBlock() == Blocks.MOB_SPAWNER) {
                TileEntity tileentity = world.getTileEntity(pos);
                if (tileentity instanceof TileEntityMobSpawner) {
                    TileEntityMobSpawner spawner = (TileEntityMobSpawner) tileentity;
                    String id = pair.getValue();
                    if (!id.contains(":")) {
                        id = fixEntityId(id);
                    }
                    MobSpawnerBaseLogic mobspawnerbaselogic = spawner.getSpawnerBaseLogic();
                    mobspawnerbaselogic.setEntityId(new ResourceLocation(id));
                    spawner.markDirty();

                }
            }
        }
        info.clearMobSpawnerTodo();

        for (BlockPos cpos : info.getGenericTodo()) {
            BlockPos pos = cpos.add(cx, 0, cz);
            // Double check that it is still a chest (could be destroyed by explosion)
            IBlockState state = world.getBlockState(pos);
            if (state.getBlock() == Blocks.CHEST) {
                if (chunkGenerator.profile.GENERATE_LOOT) {
                    createLootChest(random, world, pos);
                }
            } else if (state.getBlock() == Blocks.GLOWSTONE) {
                world.setBlockState(pos, state, 3);
            }
        }
        info.clearGenericTodo();
    }


    private void createLootChest(Random random, World world, BlockPos pos) {
        world.setBlockState(pos, Blocks.CHEST.getDefaultState().withProperty(BlockChest.FACING, EnumFacing.SOUTH));
        TileEntity tileentity = world.getTileEntity(pos);
        if (tileentity instanceof TileEntityChest) {
            switch (random.nextInt(30)) {
                case 0:
                    ((TileEntityChest) tileentity).setLootTable(LootTableList.CHESTS_DESERT_PYRAMID, random.nextLong());
                    break;
                case 1:
                    ((TileEntityChest) tileentity).setLootTable(LootTableList.CHESTS_JUNGLE_TEMPLE, random.nextLong());
                    break;
                case 2:
                    ((TileEntityChest) tileentity).setLootTable(LootTableList.CHESTS_VILLAGE_BLACKSMITH, random.nextLong());
                    break;
                case 3:
                    ((TileEntityChest) tileentity).setLootTable(LootTableList.CHESTS_ABANDONED_MINESHAFT, random.nextLong());
                    break;
                case 4:
                    ((TileEntityChest) tileentity).setLootTable(LootTableList.CHESTS_NETHER_BRIDGE, random.nextLong());
                    break;
                default:
                    ((TileEntityChest) tileentity).setLootTable(LootTableList.CHESTS_SIMPLE_DUNGEON, random.nextLong());
                    break;
            }
        }
    }

    private void setStainedGlassIfAir(World world, int x, int y, int z, int i) {
        if (world.isAirBlock(new BlockPos(x, y, z))) {
            world.setBlockState(new BlockPos(x, y, z), Blocks.STAINED_GLASS.getStateFromMeta(i), 2);
        }
    }
}
