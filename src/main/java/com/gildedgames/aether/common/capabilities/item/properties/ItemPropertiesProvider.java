package com.gildedgames.aether.common.capabilities.item.properties;

import com.gildedgames.aether.api.AetherAPI;
import com.gildedgames.aether.api.capabilites.AetherCapabilities;
import com.gildedgames.aether.api.capabilites.items.properties.IItemPropertiesCapability;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;

public class ItemPropertiesProvider implements ICapabilityProvider
{
	private IItemPropertiesCapability properties;

	private ItemStack stack;

	public ItemPropertiesProvider(ItemStack stack)
	{
		this.stack = stack;
	}

	@Override
	public boolean hasCapability(Capability<?> capability, EnumFacing facing)
	{
		return capability == AetherCapabilities.ITEM_PROPERTIES;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getCapability(Capability<T> capability, EnumFacing facing)
	{
		if (this.hasCapability(capability, facing))
		{
			if (this.properties == null)
			{
				this.properties = new ItemPropertiesImpl(AetherAPI.equipment().getProperties(this.stack.getItem()), AetherAPI.temperature().getProperties(this.stack.getItem()));
			}

			return (T) this.properties;
		}

		return null;
	}
}
