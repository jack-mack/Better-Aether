package com.gildedgames.aether.common.tile_entities;

import com.gildedgames.aether.common.util.BlockPosUtil;
import com.gildedgames.util.core.nbt.NBTHelper;
import com.google.common.collect.Lists;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ITickable;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.GameType;

import java.util.List;

public class TileEntityLabyrinthEye extends TileEntitySchematicBlock implements ITickable
{
	
	private AxisAlignedBB protectedRegion;
	
	private BlockPos startOffset, endOffset;
	
	private List<EntityPlayer> protectedPlayers = Lists.newArrayList();

	public TileEntityLabyrinthEye()
	{
		super();
	}

	@Override
	public void update()
	{
		super.update();
		
		if (this.protectedRegion == null || this.getWorld().isRemote)
		{
			return;
		}
		
		List<EntityPlayer> players = this.getWorld().getEntitiesWithinAABB(EntityPlayer.class, this.protectedRegion);
		List<EntityPlayer> scheduledToRemove = Lists.newArrayList();

		for (EntityPlayer player : this.protectedPlayers)
		{
			if (!players.contains(player))
			{
				scheduledToRemove.add(player);
				
				if (!player.capabilities.isCreativeMode)
				{
					player.setGameType(GameType.SURVIVAL);
				}
					
				player.addChatComponentMessage(new TextComponentTranslation("You feel the eye's focus drift away..", new Object[0]));
			}
		}
		
		for (EntityPlayer player: players)
		{
			if (!this.protectedPlayers.contains(player))
			{
				this.protectedPlayers.add(player);
				
				if (!player.capabilities.isCreativeMode)
				{
					player.setGameType(GameType.ADVENTURE);
				}
				
				player.addChatComponentMessage(new TextComponentTranslation("You feel something watching you...", new Object[0]));
			}
		}
		
		this.protectedPlayers.removeAll(scheduledToRemove);
	}

	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound)
	{
		super.writeToNBT(compound);
		
		compound.setTag("startOffset", NBTHelper.serializeBlockPos(this.startOffset));
		compound.setTag("endOffset", NBTHelper.serializeBlockPos(this.endOffset));

		return compound;
	}

	@Override
	public void readFromNBT(NBTTagCompound compound)
	{
		super.readFromNBT(compound);
		
		this.startOffset = NBTHelper.readBlockPos(compound.getCompoundTag("startOffset"));
		this.endOffset = NBTHelper.readBlockPos(compound.getCompoundTag("endOffset"));
		
		if (this.startOffset != null && this.endOffset != null)
		{
			this.protectedRegion = BlockPosUtil.bounds(this.getPos().subtract(this.startOffset), this.getPos().subtract(this.endOffset));
		}
	}
	
	@Override
	public void onSchematicGeneration()
	{
		
	}

	@Override
	public void onMarkedForGeneration(BlockPos start, BlockPos end)
	{
		this.startOffset = this.getPos().subtract(start);
		this.endOffset = this.getPos().subtract(end);
		
		this.protectedRegion = BlockPosUtil.bounds(this.getPos().subtract(this.startOffset), this.getPos().subtract(this.endOffset));
	}

	@Override
	public boolean shouldInvalidateTEOnGen()
	{
		return false;
	}
	
}