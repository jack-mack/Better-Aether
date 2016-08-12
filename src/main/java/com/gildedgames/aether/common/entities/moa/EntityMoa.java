package com.gildedgames.aether.common.entities.moa;

import com.gildedgames.aether.common.entities.ai.moa.*;
import com.gildedgames.aether.common.entities.living.enemies.EntityCockatrice;
import com.gildedgames.aether.common.entities.util.AnimalGender;
import com.gildedgames.aether.common.entities.util.EntityGroup;
import com.gildedgames.aether.common.entities.util.EntityGroupMember;
import com.gildedgames.aether.common.items.ItemsAether;
import com.gildedgames.aether.common.items.miscellaneous.ItemMoaEgg;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.*;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.datasync.DataParameter;
import net.minecraft.network.datasync.DataSerializers;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.oredict.OreDictionary;

public class EntityMoa extends EntityAnimal implements EntityGroupMember
{

	private MoaGenetics genetics;

	private static final DataParameter<Integer> MAX_JUMPS = new DataParameter<>(16, DataSerializers.VARINT);

	private static final DataParameter<Integer> REMAINING_JUMPS = new DataParameter<>(17, DataSerializers.VARINT);

	private static final DataParameter<Boolean> EGG_STOLEN = new DataParameter<>(18, DataSerializers.BOOLEAN);

	private static final DataParameter<Boolean> RAISED_BY_PLAYER = new DataParameter<>(19, DataSerializers.BOOLEAN);

	private static final DataParameter<Boolean> GENDER = new DataParameter<>(20, DataSerializers.BOOLEAN);

	private static final DataParameter<Integer> GENETIC_SEED = new DataParameter<>(21, DataSerializers.VARINT);

	private static final DataParameter<Integer> MOTHER_GENETIC_SEED = new DataParameter<>(22, DataSerializers.VARINT);

	private static final DataParameter<Integer> FATHER_GENETIC_SEED = new DataParameter<>(23, DataSerializers.VARINT);

	private static final DataParameter<Boolean> HAS_PARENTS = new DataParameter<>(24, DataSerializers.BOOLEAN);

	private static final DataParameter<Boolean> SHOULD_RESET_GENETICS = new DataParameter<>(25, DataSerializers.BOOLEAN);

	public float wingRotation, destPos, prevDestPos, prevWingRotation;

	public int ticksUntilFlap;

	private EntityGroup pack;

	private MoaNest familyNest;

	private boolean addedBreedingTask;

	public EntityMoa(World world)
	{
		super(world);

		this.initAI();

		this.familyNest = new MoaNest(world);

		this.setGeneticSeed(MoaGenetics.getRandomGeneticSeed(world));

		this.setSize(1.0F, 2.0F);
		this.stepHeight = 1.0F;
	}

	public EntityMoa(World world, int geneticSeed)
	{
		this(world);

		this.setGeneticSeed(geneticSeed);

		this.setFatherSeed(geneticSeed);
		this.setMotherSeed(geneticSeed);
	}

	public EntityMoa(World world, MoaNest familyNest)
	{
		this(world, familyNest.familyGeneticSeed);

		this.familyNest = familyNest;
		this.initAI();
	}

	public EntityMoa(World world, MoaNest familyNest, int fatherSeed, int motherSeed)
	{
		this(world, familyNest);

		this.setFatherSeed(fatherSeed);
		this.setMotherSeed(motherSeed);
	}

	private void initAI()
	{
		this.tasks.addTask(0, new EntityAISwimming(this));
		this.tasks.addTask(1, new AIPanicPack(this, 0.35F));
		this.tasks.addTask(2, new EntityAIWander(this, 0.30F));
		this.tasks.addTask(4, new EntityAIWatchClosest(this, EntityPlayer.class, 6.0F));
		this.tasks.addTask(5, new EntityAILookIdle(this));
		this.tasks.addTask(6, new EntityAIMate(this, 0.25F));
		this.tasks.addTask(8, new AIAnimalPack(this, 0.25F));
		this.tasks.addTask(10, new AIStayNearNest(this, 8, 0.25F));
		this.tasks.addTask(12, new AIAvoidEntityAsChild(this, EntityPlayer.class, 5.0F, 0.3D, 0.3D));
		this.tasks.addTask(13, new EntityAITempt(this, 0.25F, Items.WHEAT, false));
		this.tasks.addTask(14, new EntityAIAttackMelee(this, 0.7D, true));
		this.targetTasks.addTask(1, new AIProtectPack(this));
	}

	@Override
	protected void entityInit()
	{
		super.entityInit();

		this.dataManager.register(EntityMoa.MAX_JUMPS, 0);
		this.dataManager.register(EntityMoa.REMAINING_JUMPS, 0);
		this.dataManager.register(EntityMoa.EGG_STOLEN, Boolean.FALSE);
		this.dataManager.register(EntityMoa.RAISED_BY_PLAYER, Boolean.FALSE);
		this.dataManager.register(EntityMoa.GENDER, Boolean.FALSE);
		this.dataManager.register(EntityMoa.GENETIC_SEED, 0);
		this.dataManager.register(EntityMoa.MOTHER_GENETIC_SEED, 0);
		this.dataManager.register(EntityMoa.FATHER_GENETIC_SEED, 0);
		this.dataManager.register(EntityMoa.HAS_PARENTS, Boolean.FALSE);
		this.dataManager.register(EntityMoa.SHOULD_RESET_GENETICS, Boolean.FALSE);
	}

	@Override
	protected void applyEntityAttributes()
	{
		super.applyEntityAttributes();

		this.getEntityAttribute(SharedMonsterAttributes.MAX_HEALTH).setBaseValue(10.0D);
		this.getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(1.0D);
	}

	public void initGenetics()
	{
		if (this.hasParents())
		{
			MoaGenetics fatherGenetics = MoaGenetics.getFromSeed(this.worldObj, this.getFatherSeed());
			MoaGenetics motherGenetics = MoaGenetics.getFromSeed(this.worldObj, this.getMotherSeed());

			this.genetics = MoaGenetics.getMixedResult(this.worldObj, fatherGenetics, motherGenetics);
		}
		else
		{
			this.genetics = MoaGenetics.getFromSeed(this.worldObj, this.getGeneticSeed());
		}

		this.setMaxJumps(this.genetics.jumps);
	}

	@Override
	public void onUpdate()
	{
		super.onUpdate();

		if (this.genetics == null || this.shouldResetGenetics())
		{
			this.initGenetics();
		}

		if (this.hasParents() && this.genetics == null)
		{
			this.initGenetics();
		}

		if (!this.addedBreedingTask || this.shouldResetGenetics())
		{
			this.tasks.addTask(9, new AIMoaPackBreeding(this, this.getGeneticSeed(), this.getGeneticSeed(), 0.25F));
			this.addedBreedingTask = true;
			this.setResetGenetics(false);
		}

		if (this.isJumping)
		{
			this.motionY += 0.05F;
		}

		if (this.pack == null)
		{
			this.pack = this.familyNest.pack;
		}

		this.updateWingRotation();

		this.fallSlowly();
	}

	private void fallSlowly()
	{
		this.fallDistance = 0;

		if (!this.onGround && this.motionY < 0.0D)
		{
			this.motionY *= 0.63749999999999996D;
		}
	}

	public void updateWingRotation()
	{
		if (!this.onGround)
		{
			if (this.ticksUntilFlap == 0)
			{
				//this.worldObj.playSoundAtEntity(this, "mob.bat.takeoff", 0.15f, MathHelper.clamp_float(this.rand.nextFloat(), 0.7f, 1.0f) + MathHelper.clamp_float(this.rand.nextFloat(), 0f, 0.3f));

				this.ticksUntilFlap = 11;
			}
			else
			{
				this.ticksUntilFlap--;
			}
		}

		this.prevWingRotation = this.wingRotation;
		this.prevDestPos = this.destPos;

		this.destPos += 0.2D;
		this.destPos = Math.min(1.0F, Math.max(0.01F, this.destPos));

		if (this.onGround)
		{
			this.destPos = 0.0F;
		}

		this.wingRotation += 0.533F;
	}

	@Override
	public boolean attackEntityAsMob(Entity entity)
	{
		entity.motionY = 0.8F;
		entity.motionZ = this.getLookVec().zCoord;
		entity.motionX = this.getLookVec().xCoord;
		entity.velocityChanged = true;

		entity.attackEntityFrom(DamageSource.causeMobDamage(this), this.isGroupLeader() ? 3 : 2);

		return super.attackEntityAsMob(entity);
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound nbt)
	{
		super.writeEntityToNBT(nbt);

		nbt.setInteger("geneticSeed", this.getGeneticSeed());

		nbt.setBoolean("hasParents", this.hasParents());
		nbt.setBoolean("refreshGenetics", this.shouldResetGenetics());
		nbt.setBoolean("playerGrown", this.isRaisedByPlayer());

		if (this.hasParents())
		{
			nbt.setInteger("fatherSeed", this.getFatherSeed());
			nbt.setInteger("motherSeed", this.getMotherSeed());
		}

		if (this.getGender() != null)
		{
			nbt.setString("creatureGender", this.getGender().name());
		}

		nbt.setInteger("remainingJumps", this.getRemainingJumps());

		this.familyNest.writeToNBT(nbt);
	}

	@Override
	public void readEntityFromNBT(NBTTagCompound nbt)
	{
		super.readEntityFromNBT(nbt);

		this.setGeneticSeed(nbt.getInteger("geneticSeed"));

		if (nbt.getBoolean("hasParents"))
		{
			this.setFatherSeed(nbt.getInteger("fatherSeed"));
			this.setMotherSeed(nbt.getInteger("motherSeed"));
		}

		this.setResetGenetics(nbt.getBoolean("refreshGenetics"));

		this.setRaisedByPlayer(nbt.getBoolean("playerGrown"));

		if (nbt.getString("creatureGender") != null)
		{
			this.setGender(AnimalGender.get(nbt.getString("creatureGender")));
		}

		this.setRemainingJumps(nbt.getInteger("remainingJumps"));

		this.familyNest.readFromNBT(nbt);
	}

	@Override
	public boolean isGroupLeader()
	{
		return this.getGender() == AnimalGender.MALE;
	}

	@Override
	public EntityGroup getGroup()
	{
		return this.familyNest != null ? this.familyNest.pack : null;
	}

	@Override
	public boolean isProtective()
	{
		return this.isGroupLeader() || this.hasEggBeenStolen();
	}

	public boolean hasEggBeenStolen()
	{
		return this.dataManager.get(EntityMoa.EGG_STOLEN);
	}

	public void setEggStolen(boolean flag)
	{
		this.dataManager.set(EntityMoa.EGG_STOLEN, flag);
	}

	public boolean shouldResetGenetics()
	{
		return this.dataManager.get(EntityMoa.SHOULD_RESET_GENETICS);
	}

	public void resetGenetics()
	{
		this.setResetGenetics(true);
	}

	private void setResetGenetics(boolean flag)
	{
		this.dataManager.set(EntityMoa.SHOULD_RESET_GENETICS, flag);
	}

	public boolean isRaisedByPlayer()
	{
		return this.dataManager.get(EntityMoa.RAISED_BY_PLAYER);
	}

	public void setRaisedByPlayer(boolean flag)
	{
		this.dataManager.set(EntityMoa.RAISED_BY_PLAYER, flag);
	}

	public AnimalGender getGender()
	{
		return this.dataManager.get(EntityMoa.GENDER) ? AnimalGender.MALE : AnimalGender.FEMALE;
	}

	public void setGender(AnimalGender gender)
	{
		this.dataManager.set(EntityMoa.GENDER, gender == AnimalGender.MALE ? true : false);
	}

	public int getGeneticSeed()
	{
		return this.dataManager.get(EntityMoa.GENETIC_SEED);
	}

	public void setGeneticSeed(int seed)
	{
		this.dataManager.set(EntityMoa.GENETIC_SEED, seed);
	}

	public int getFatherSeed()
	{
		return this.dataManager.get(EntityMoa.FATHER_GENETIC_SEED);
	}

	public void setFatherSeed(int seed)
	{
		this.dataManager.set(EntityMoa.FATHER_GENETIC_SEED, seed);

		this.setHasParents(true);

		this.resetGenetics();
	}

	public int getMotherSeed()
	{
		return this.dataManager.get(EntityMoa.MOTHER_GENETIC_SEED);
	}

	public void setMotherSeed(int seed)
	{
		this.dataManager.set(EntityMoa.MOTHER_GENETIC_SEED, seed);

		this.setHasParents(true);

		this.resetGenetics();
	}

	public boolean hasParents()
	{
		return this.dataManager.get(EntityMoa.HAS_PARENTS);
	}

	private void setHasParents(boolean flag)
	{
		this.dataManager.set(EntityMoa.HAS_PARENTS, flag);
	}

	public int getRemainingJumps()
	{
		return this.dataManager.get(EntityMoa.REMAINING_JUMPS);
	}

	private void setRemainingJumps(int jumps)
	{
		this.dataManager.set(EntityMoa.REMAINING_JUMPS, jumps);
	}

	public int getMaxJumps()
	{
		return this.dataManager.get(EntityMoa.MAX_JUMPS);
	}

	public void setMaxJumps(int maxJumps)
	{
		this.dataManager.set(EntityMoa.MAX_JUMPS, maxJumps);
	}

	public MoaGenetics getGenetics()
	{
		return this.genetics;
	}

	public MoaNest getFamilyNest()
	{
		return this.familyNest;
	}

	public EntityGroup getAnimalPack()
	{
		return this.pack;
	}

	public void setAnimalPack(EntityGroup pack)
	{
		this.pack = pack;
		this.pack.onAnimalJoin(this);
	}

	@Override
	public void onDeath(DamageSource damageSource)
	{
		if (this.pack != null)
		{
			this.pack.onAnimalDeath(this);
		}

		super.onDeath(damageSource);
	}

	@Override
	public void setRevengeTarget(EntityLivingBase entity)
	{
		super.setRevengeTarget(entity);

		if (this.pack != null && entity != null)
		{
			this.pack.addOrRenewAggressor(entity);
		}
	}

	@Override
	public EntityAgeable createChild(EntityAgeable matingAnimal)
	{
		if (matingAnimal instanceof EntityMoa)
		{
			EntityMoa mate = (EntityMoa) matingAnimal;

			this.tasks.addTask(7, new AIMoaLayEgg(this, this.getGeneticSeed(), mate.getGeneticSeed(), 0.35F));

			this.setGrowingAge(6000);
			mate.setGrowingAge(6000);
			this.resetInLove();

			this.setAttackTarget(null);
			mate.setAttackTarget(null);

			mate.resetInLove();
		}

		return null;
	}

	@Override
	public ItemStack getPickedResult(RayTraceResult target)
	{
		ItemStack moaEgg = new ItemStack(ItemsAether.moa_egg);

		MoaGenetics genetics = MoaGenetics.getMixedResult(this.worldObj, this.getFatherSeed(), this.getMotherSeed());
		NBTTagCompound nbtTag = ItemMoaEgg.getNBTFromGenetics(genetics);
		nbtTag.setInteger("geneticSeed", this.getGeneticSeed());

		nbtTag.setInteger("fatherGeneticSeed", this.getFatherSeed());
		nbtTag.setInteger("motherGeneticSeed", this.getMotherSeed());

		moaEgg.setTagCompound(nbtTag);

		return moaEgg;
	}

}
