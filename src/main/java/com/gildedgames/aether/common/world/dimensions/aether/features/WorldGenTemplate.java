package com.gildedgames.aether.common.world.dimensions.aether.features;

import com.gildedgames.aether.common.util.structure.TemplatePrimer;
import com.gildedgames.aether.common.world.gen.IWorldGen;
import com.google.common.collect.Lists;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.Mirror;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenerator;
import net.minecraft.world.gen.structure.StructureBoundingBox;
import net.minecraft.world.gen.structure.template.BlockRotationProcessor;
import net.minecraft.world.gen.structure.template.ITemplateProcessor;
import net.minecraft.world.gen.structure.template.PlacementSettings;
import net.minecraft.world.gen.structure.template.Template;

import java.util.List;
import java.util.Random;

public class WorldGenTemplate extends WorldGenerator implements IWorldGen
{

	public static final Rotation[] ROTATIONS = Rotation.values();

	private Template template;

	private List<PlacementCondition> placementConditions = Lists.newArrayList();

	private CenterOffsetProcessor centerOffsetProcessor;

	private boolean randomRotation = true, checkAreaLoaded = true;

	public WorldGenTemplate(Template template)
	{
		this.template = template;

		this.placementConditions = Lists.newArrayList();
	}

	public WorldGenTemplate(Template template, PlacementCondition condition, PlacementCondition... placementConditions)
	{
		this.template = template;

		this.placementConditions = Lists.newArrayList(placementConditions);

		this.placementConditions.add(condition);
	}

	public void setCheckAreaLoaded(boolean flag)
	{
		this.checkAreaLoaded = flag;
	}

	public void setRandomRotation(boolean flag)
	{
		this.randomRotation = flag;
	}

	public CenterOffsetProcessor getCenterOffsetProcessor()
	{
		return this.centerOffsetProcessor;
	}

	public void setCenterOffsetProcessor(CenterOffsetProcessor centerOffsetProcessor)
	{
		this.centerOffsetProcessor = centerOffsetProcessor;
	}

	public BlockPos getCenteredPos(BlockPos pos, PlacementSettings settings)
	{
		BlockPos size = this.template.transformedSize(settings.getRotation());

		switch (settings.getRotation())
		{
		case NONE:
		default:
			pos = pos.add(-(size.getX() / 2.0) + 1, 0, -(size.getZ() / 2.0) + 1);
			break;
		case CLOCKWISE_90:
			pos = pos.add(size.getX() / 2.0, 0, -(size.getZ() / 2.0) + 1);
			break;
		case COUNTERCLOCKWISE_90:
			pos = pos.add(-(size.getX() / 2.0) + 1, 0, (size.getZ() / 2.0));
			break;
		case CLOCKWISE_180:
			pos = pos.add((size.getX() / 2.0), 0, (size.getZ() / 2.0));
			break;
		}

		if (this.getCenterOffsetProcessor() != null)
		{
			pos = pos.add(this.getCenterOffsetProcessor().getOffset(settings.getRotation()));
		}

		return pos;
	}

	@Override
	public boolean generate(World world, Random rand, BlockPos pos, boolean centered)
	{
		Rotation rotation = this.randomRotation ? ROTATIONS[rand.nextInt(ROTATIONS.length)] : ROTATIONS[0];

		PlacementSettings settings = new PlacementSettings().setMirror(Mirror.NONE).setRotation(rotation).setIgnoreEntities(false).setChunk(null).setReplacedBlock(null).setIgnoreStructureBlock(false);

		if (centered)
		{
			pos = this.getCenteredPos(pos, settings);
		}

		boolean result = this.placeTemplateWithCheck(world, pos, settings);

		if (result)
		{
			this.postGenerate(world, rand, pos, rotation);
		}

		return result;
	}

	public Template getTemplate()
	{
		return this.template;
	}

	private StructureBoundingBox getBoundingBoxFromTemplate(BlockPos pos, PlacementSettings settings)
	{
		Rotation rotation = settings.getRotation();
		BlockPos blockpos = this.template.transformedSize(rotation);
		StructureBoundingBox bb = new StructureBoundingBox(0, 0, 0, blockpos.getX(), blockpos.getY() - 1, blockpos.getZ());

		switch (rotation)
		{
		case NONE:
		default:
			break;
		case CLOCKWISE_90:
			bb.offset(-blockpos.getX(), 0, 0);
			break;
		case COUNTERCLOCKWISE_90:
			bb.offset(0, 0, -blockpos.getZ());
			break;
		case CLOCKWISE_180:
			bb.offset(-blockpos.getX(), 0, -blockpos.getZ());
		}

		bb.offset(pos.getX(), pos.getY(), pos.getZ());

		return bb;
	}

	public boolean canGenerate(World world, BlockPos pos, PlacementSettings settings)
	{
		return this.canGenerate(world, pos, settings, false, this.checkAreaLoaded);
	}

	public boolean canGenerate(World world, BlockPos pos, PlacementSettings settings, boolean centered)
	{
		return this.canGenerate(world, pos, settings, centered, this.checkAreaLoaded);
	}

	public boolean canGenerate(World world, BlockPos pos, PlacementSettings settings, boolean centered, boolean checkAreaLoaded)
	{
		if (centered)
		{
			pos = this.getCenteredPos(pos, settings);
		}

		final StructureBoundingBox bb = this.getBoundingBoxFromTemplate(pos, settings);

		if ((checkAreaLoaded && !world.isAreaLoaded(bb)) || bb.maxY > world.getActualHeight())
		{
			return false;
		}

		List<Template.BlockInfo> info = TemplatePrimer.getBlocks(this.template);

		List<Template.BlockInfo> infoTransformed = TemplatePrimer.getBlocks(info, pos, settings, this.template);

		for (Template.BlockInfo block : infoTransformed)
		{
			for (PlacementCondition condition : this.placementConditions)
			{
				if (!condition.canPlace(this.template, world, pos, block))
				{
					return false;
				}
			}
		}

		for (PlacementCondition condition : this.placementConditions)
		{
			if (!condition.canPlaceCheckAll(this.template, world, pos, infoTransformed))
			{
				return false;
			}
		}

		return true;
	}

	@Override
	public final boolean generate(World world, Random rand, BlockPos pos)
	{
		return this.generate(world, rand, pos, false);
	}

	protected void postGenerate(World world, Random rand, BlockPos pos, Rotation rotation)
	{

	}

	public boolean placeTemplateWithCheck(World world, BlockPos pos, PlacementSettings settings)
	{
		return this.placeTemplateWithCheck(world, pos, settings, false);
	}

	public boolean placeTemplateWithCheck(World world, BlockPos pos, PlacementSettings settings, boolean centered)
	{
		if (this.canGenerate(world, pos, settings))
		{
			this.placeTemplateWithoutCheck(world, pos, settings, centered);

			return true;
		}

		return false;
	}

	public void placeTemplateWithoutCheck(World world, BlockPos pos, PlacementSettings settings)
	{
		this.placeTemplateWithoutCheck(world, pos, settings, false);
	}

	public void placeTemplateWithoutCheck(World world, BlockPos pos, PlacementSettings settings, boolean centered)
	{
		if (centered)
		{
			pos = this.getCenteredPos(pos, settings);
		}

		ITemplateProcessor processor = new BlockRotationProcessor(pos, settings);
		TemplatePrimer.populateAll(this.template, world, pos, processor, settings);
	}

	public static boolean canGrowInto(Block block)
	{
		Material material = block.getDefaultState().getMaterial();

		return material == Material.AIR || material == Material.LEAVES || material == Material.PLANTS;
	}

	public static boolean isReplaceable(World world, BlockPos pos)
	{
		IBlockState state = world.getBlockState(pos);

		return state.getBlock().isAir(state, world, pos) || state.getBlock().isLeaves(state, world, pos) || WorldGenTemplate.canGrowInto(state.getBlock());
	}

	public interface PlacementCondition
	{

		boolean canPlace(Template template, World world, BlockPos placedAt, Template.BlockInfo block);

		/** Should return true by default **/
		boolean canPlaceCheckAll(Template template, World world, BlockPos placedAt, List<Template.BlockInfo> blocks);

	}

	public interface CenterOffsetProcessor
	{

		BlockPos getOffset(Rotation rotation);

	}

}
