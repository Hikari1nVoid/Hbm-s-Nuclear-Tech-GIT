package com.hbm.world.feature;

import java.util.Random;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockStalagmite;
import com.hbm.inventory.RecipesCommon.MetaBlock;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.gen.NoiseGeneratorPerlin;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;

public class OreCave {

	private NoiseGeneratorPerlin noise;
	private MetaBlock ore;
	/** The number that is being deducted flat from the result of the perlin noise before all other processing. Increase this to make strata rarer. */
	private double threshold = 2D;
	/** The mulitplier for the remaining bit after the threshold has been deducted. Increase to make strata wavier. */
	private int rangeMult = 3;
	/** The maximum range after multiplying - anything above this will be subtracted from (maxRange * 2) to yield the proper range. Increase this to make strata thicker. */
	private int maxRange = 4;
	/** The y-level around which the stratum is centered. */
	private int yLevel = 30;
	
	public OreCave(Block ore) {
		this(ore, 0);
	}
	
	public OreCave(Block ore, int meta) {
		this.ore = new MetaBlock(ore, meta);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	public OreCave setThreshold(double threshold) {
		this.threshold = threshold;
		return this;
	}
	
	public OreCave setRangeMult(int rangeMult) {
		this.rangeMult = rangeMult;
		return this;
	}
	
	public OreCave setMaxRange(int maxRange) {
		this.maxRange = maxRange;
		return this;
	}
	
	public OreCave setYLevel(int yLevel) {
		this.yLevel = yLevel;
		return this;
	}

	@SubscribeEvent
	public void onDecorate(DecorateBiomeEvent.Pre event) {
		
		if(this.noise == null) {
			this.noise = new NoiseGeneratorPerlin(new Random(event.world.getSeed() + (ore.getID() * 31) + yLevel), 2);
		}
		
		World world = event.world;
		
		if(world.provider.dimensionId != 0)
			return;
		
		int cX = event.chunkX;
		int cZ = event.chunkZ;
		
		double scale = 0.01D;
		
		for(int x = cX; x < cX + 16; x++) {
			for(int z = cZ; z < cZ + 16; z++) {
				
				double n = noise.func_151601_a(x * scale, z * scale);
				
				if(n > threshold) {
					int range = (int)((n - threshold) * rangeMult);
					
					if(range > maxRange)
						range = (maxRange * 2) - range;
					
					if(range < 0)
						continue;
					
					for(int y = yLevel - range; y <= yLevel + range; y++) {
						Block genTarget = world.getBlock(x, y, z);
						
						if(genTarget.isNormalCube() && (genTarget.getMaterial() == Material.rock || genTarget.getMaterial() == Material.ground)) {
							
							boolean shouldGen = false;
							
							for(ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
								Block neighbor = world.getBlock(MathHelper.clamp_int(x + dir.offsetX, cX, cX + 16), y + dir.offsetY, MathHelper.clamp_int(z + dir.offsetZ, cZ, cZ + 16));
								if(neighbor.getMaterial() == Material.air || neighbor instanceof BlockStalagmite) {
									shouldGen = true;
									break;
								}
							}
							if(shouldGen) world.setBlock(x, y, z, ore.block, ore.meta, 2);
						} else {
							
							if((genTarget.getMaterial() == Material.air || !genTarget.isNormalCube()) && event.rand.nextInt(5) == 0) {
								
								if(ModBlocks.stalactite.canPlaceBlockAt(world, x, y, z)) {
									world.setBlock(x, y, z, ModBlocks.stalactite, ore.meta, 2);
								} else {
									if(ModBlocks.stalagmite.canPlaceBlockAt(world, x, y, z)) {
										world.setBlock(x, y, z, ModBlocks.stalagmite, ore.meta, 2);
									}
								}
							}
						}
					}
				}
			}
		}
	}
}
