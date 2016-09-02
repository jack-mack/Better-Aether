package com.gildedgames.aether.common.network.packets;

import com.gildedgames.aether.api.capabilites.AetherCapabilities;
import com.gildedgames.aether.api.player.IPlayerAetherCapability;
import com.gildedgames.aether.common.capabilities.player.PlayerAetherImpl;
import com.gildedgames.util.core.io.MessageHandlerClient;
import com.gildedgames.util.core.io.MessageHandlerServer;
import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import static com.gildedgames.aether.common.network.packets.AetherMovementPacket.Action.EXTRA_JUMP;

public class MidAirJumpsChangedPacket implements IMessage
{

	private int midAirJumpsAllowed;

	public MidAirJumpsChangedPacket()
	{

	}

	public MidAirJumpsChangedPacket(int midAirJumpsAllowed)
	{
		this.midAirJumpsAllowed = midAirJumpsAllowed;
	}

	@Override
	public void fromBytes(ByteBuf buf)
	{
		this.midAirJumpsAllowed = buf.readInt();
	}

	@Override
	public void toBytes(ByteBuf buf)
	{
		buf.writeInt(this.midAirJumpsAllowed);
	}

	public static class Handler extends MessageHandlerClient<MidAirJumpsChangedPacket, MidAirJumpsChangedPacket>
	{
		@Override
		public MidAirJumpsChangedPacket onMessage(MidAirJumpsChangedPacket message, EntityPlayer player)
		{
			PlayerAetherImpl playerAether = PlayerAetherImpl.getPlayer(player);

			playerAether.getAbilitiesModule().setMidAirJumpsAllowed(message.midAirJumpsAllowed);

			return null;
		}

	}
}