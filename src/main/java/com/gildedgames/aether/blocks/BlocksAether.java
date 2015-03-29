package com.gildedgames.aether.blocks;

import com.gildedgames.aether.Aether;
import com.gildedgames.aether.blocks.natural.BlockAetherDirt;
import com.gildedgames.aether.blocks.natural.BlockHolystone;
import com.gildedgames.aether.client.models.ModelsAether;
import com.gildedgames.aether.items.itemblocks.ItemBlockVariants;

import net.minecraft.block.Block;
import net.minecraft.item.ItemBlock;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class BlocksAether
{
	public BlockAetherDirt aether_dirt;

	public BlockHolystone holystone;

	public void preInit()
	{
		this.aether_dirt = this.registerBlock("aether_dirt", new BlockAetherDirt());

		this.holystone = this.registerBlock("holystone", ItemBlockVariants.class, new BlockHolystone());
	}

	private <T extends Block> T registerBlock(String name, Class<? extends ItemBlock> itemblock, T block)
	{
		block.setUnlocalizedName(name);
		GameRegistry.registerBlock(block, itemblock, name);

		return block;
	}

	private <T extends Block> T registerBlock(String name, T block)
	{
		block.setUnlocalizedName(name);
		GameRegistry.registerBlock(block, name);

		return block;
	}

	public void init()
	{
		if (Aether.PROXY.getModels() != null)
		{
			ModelsAether models = Aether.PROXY.getModels();

			models.registerItemRenderer(this.aether_dirt, 0);
			models.registerItemRenderer(this.holystone, BlockHolystone.HolystoneVariant.values());
		}
	}
}