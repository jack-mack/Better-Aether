package com.gildedgames.aether.common.entities.ai.sheepuff;

import com.gildedgames.aether.common.blocks.BlocksAether;
import com.google.common.base.Predicate;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.block.state.pattern.BlockStateMatcher;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class EntityAIEatAetherGrass extends EntityAIBase
{
	private static final Predicate<IBlockState> IS_TALL_GRASS = BlockStateMatcher.forBlock(BlocksAether.tall_aether_grass);
	private final EntityLiving grassEaterEntity;
	private final World entityWorld;
	int eatingGrassTimer;
	public EntityAIEatAetherGrass(EntityLiving grassEaterEntityIn)
	{
		this.grassEaterEntity = grassEaterEntityIn;
		this.entityWorld = grassEaterEntityIn.worldObj;
		this.setMutexBits(7);
	}
	public boolean shouldExecute()
	{
		if (this.grassEaterEntity.getRNG().nextInt(this.grassEaterEntity.isChild() ? 50 : 1000) != 0)
		{
			return false;
		}
		else
		{
			BlockPos blockpos = new BlockPos(this.grassEaterEntity.posX, this.grassEaterEntity.posY, this.grassEaterEntity.posZ);
			return IS_TALL_GRASS.apply(this.entityWorld.getBlockState(blockpos)) ? true : this.entityWorld.getBlockState(blockpos.down()).getBlock() == BlocksAether.aether_grass;
		}
	}
	public void startExecuting()
	{
		this.eatingGrassTimer = 40;
		this.entityWorld.setEntityState(this.grassEaterEntity, (byte)10);
		this.grassEaterEntity.getNavigator().clearPathEntity();
	}
	public void resetTask()
	{
		this.eatingGrassTimer = 0;
	}
	public boolean continueExecuting()
	{
		return this.eatingGrassTimer > 0;
	}
	public int getEatingGrassTimer()
	{
		return this.eatingGrassTimer;
	}
	public void updateTask()
	{
		this.eatingGrassTimer = Math.max(0, this.eatingGrassTimer - 1);

		if (this.eatingGrassTimer == 4)
		{
			BlockPos blockpos = new BlockPos(this.grassEaterEntity.posX, this.grassEaterEntity.posY, this.grassEaterEntity.posZ);

			if (IS_TALL_GRASS.apply(this.entityWorld.getBlockState(blockpos)))
			{
				if (this.entityWorld.getGameRules().getBoolean("mobGriefing"))
				{
					this.entityWorld.destroyBlock(blockpos, false);
				}

				this.grassEaterEntity.eatGrassBonus();
			}
			else
			{
				BlockPos blockpos1 = blockpos.down();

				if (this.entityWorld.getBlockState(blockpos1).getBlock() == BlocksAether.aether_grass)
				{
					if (this.entityWorld.getGameRules().getBoolean("mobGriefing"))
					{
						this.entityWorld.playEvent(2001, blockpos1, Block.getIdFromBlock(BlocksAether.aether_grass));
						this.entityWorld.setBlockState(blockpos1, BlocksAether.aether_dirt.getDefaultState(), 2);
					}

					this.grassEaterEntity.eatGrassBonus();
				}
			}
		}
	}
}