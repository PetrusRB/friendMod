package com.petrus.friend.entity;

import java.util.List;
import java.util.UUID;

import com.petrus.friend.goals.BotFollowGoal;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

/**
 * FriendBot — IA melhorada, por-entidade owner, persistência em NBT.
 */
public class FriendBot extends PathfinderMob {
  private enum BotState {
    IDLE, FOLLOWING, DEFENDING, MINING, BUILDING_HOUSE, ESCAPING, REQUEST_HELP
  }

  @SuppressWarnings("unused")
  private BotState state = BotState.IDLE;

  // owner por-entidade (antes era static)
  private UUID ownerID = null;

  // inventario
  private SimpleContainer inventory = new SimpleContainer(36);

  // cooldowns / timers (ticks)
  private int tickCooldown = 0;
  private int buildCooldown = 0;
  private int helpRequestCooldown = 0;
  private int attackCooldown = 0; // evita spam de ataques

  // Constantes configuráveis
  private static final int ORE_SEARCH_RADIUS = 12;
  private static final int ESCAPE_HELP_COOLDOWN = 20 * 60; // 60s
  private static final int BUILD_COOLDOWN_TICKS = 20 * 60 * 5; // 5 min

  public FriendBot(EntityType<? extends FriendBot> type, Level world) {
    super(type, world);
    this.setNoAi(false);
  }

  @Override
  protected void registerGoals() {
    super.registerGoals();
    // prioridade: nadar > atacar > seguir dono > vaguear > olhar > abrir portas
    this.goalSelector.addGoal(0, new FloatGoal(this));
    this.goalSelector.addGoal(5, new BotFollowGoal(this, 1.0D, 3.0F, 12.0F));
    this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.3D, true));
    this.goalSelector.addGoal(3, new RandomStrollGoal(this, 0.8D));
    this.goalSelector.addGoal(4, new LookAtPlayerGoal(this, Player.class, 8.0F));
    this.goalSelector.addGoal(5, new OpenDoorGoal(this, true));

    // target goals
    this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    // ataca nearest Monster (zumbis, esqueletos, etc.)
    this.targetSelector.addGoal(1,
        new NearestAttackableTargetGoal<Monster>(this, Monster.class, 10, true, false, mob -> true));
  }

  @Override
  public void tick() {
    super.tick();

    if (this.level().isClientSide)
      return;

    // cooldowns
    if (tickCooldown > 0)
      tickCooldown--;
    if (buildCooldown > 0)
      buildCooldown--;
    if (helpRequestCooldown > 0)
      helpRequestCooldown--;
    if (attackCooldown > 0)
      attackCooldown--;

    ServerLevel serverLevel = (ServerLevel) this.level();
    Player owner = getOwnerPlayer();

    try {
      // prioridade alta: se preso, tentar escapar / pedir ajuda
      if (processHoleDetection(serverLevel))
        return;

      // prioridade média: criar uma casa se estiver sozinho.
      if (processBuildHouse(serverLevel, owner))
        return;

      // minerar se tiver chance
      if (processFindOre(serverLevel))
        return;
    } catch (Exception e) {
      e.printStackTrace();
      // se der ruim, remover a entidade com segurança
      this.discard();
      return;
    }

    // estado default é controlado pelos goals
    state = BotState.IDLE;
  }

  // -------------------------
  // Processos (retorna true se processou)
  // -------------------------
  private boolean processHoleDetection(ServerLevel level) {
    // tentar escapar se preso
    if (isInHole(this.level(), this.blockPosition())) {
      boolean escaped = tryEscapeHole(level);
      if (!escaped && helpRequestCooldown == 0) {
        requestHelp(level, getOwnerPlayer());
        helpRequestCooldown = ESCAPE_HELP_COOLDOWN;
      }
      state = BotState.ESCAPING;
      return true;
    }
    return false;
  }

  private boolean processFindOre(ServerLevel level) {
    BlockPos orePos = findNearbyOre(level);
    if (orePos != null && buildCooldown == 0) {
      mineAt(level, orePos);
      state = BotState.MINING;
      return true;
    }
    return false;
  }

  private boolean processBuildHouse(ServerLevel level, Player owner) {
    boolean alone = owner == null || level.players().isEmpty() ||
        (owner != null && level.players().size() == 1 && level.players().get(0).equals(owner));
    if (alone && buildCooldown == 0) {
      buildSimpleHouse(level, this.blockPosition().offset(2, 0, 0));
      buildCooldown = BUILD_COOLDOWN_TICKS;
      state = BotState.BUILDING_HOUSE;
      return true;
    }
    return false;
  }

  // -------------------------
  // Owner API (por-entidade)
  // -------------------------
  public boolean setOwnerID(UUID uuid) {
    if (uuid == null)
      return false;
    if (uuid.equals(this.ownerID))
      return false;
    this.ownerID = uuid;

    // tenta setar nome customizado
    if (this.level() instanceof ServerLevel sl) {
      Player p = sl.getPlayerByUUID(uuid);
      String ownerName = (p != null) ? p.getName().getString() : uuid.toString().substring(0, 8);
      this.setCustomName(Component.literal("Friend de " + ownerName));
      this.setCustomNameVisible(true);
    } else {
      this.setCustomName(Component.literal("Friend"));
    }
    return true;
  }

  public UUID getOwnerID() {
    return this.ownerID;
  }

  public Player getOwnerPlayer() {
    if (ownerID == null)
      return null; // evita crash
    if (!(this.level() instanceof ServerLevel level))
      return null;
    return level.getPlayerByUUID(ownerID);
  }

  // -------------------------
  // Mineração
  // -------------------------
  private void storeOreInChest(ServerLevel level, BlockPos chestPos, ItemStack oreStack) {
    BlockEntity be = level.getBlockEntity(chestPos);
    if (be instanceof ChestBlockEntity chest) {
      for (int slot = 0; slot < chest.getContainerSize(); slot++) {
        ItemStack stackInSlot = chest.getItem(slot);
        if (stackInSlot.isEmpty()) {
          chest.setItem(slot, oreStack.copy());
          chest.setChanged();
          break;
        } else if (ItemStack.isSameItem(stackInSlot, oreStack)
            && stackInSlot.getCount() + oreStack.getCount() <= stackInSlot.getMaxStackSize()) {
          stackInSlot.grow(oreStack.getCount());
          chest.setChanged();
          break;
        }
      }
    }
  }

  private BlockPos findNearbyOre(ServerLevel level) {
    int r = ORE_SEARCH_RADIUS;
    BlockPos me = this.blockPosition();
    for (int y = -6; y <= 6; y++) {
      for (int x = -r; x <= r; x++) {
        for (int z = -r; z <= r; z++) {
          BlockPos pos = me.offset(x, y, z);
          BlockState bs = level.getBlockState(pos);
          if (isOre(bs))
            return pos;
        }
      }
    }
    return null;
  }

  private boolean isOre(BlockState bs) {
    return bs.is(Blocks.COAL_ORE) || bs.is(Blocks.IRON_ORE) || bs.is(Blocks.DEEPSLATE_IRON_ORE)
        || bs.is(Blocks.DIAMOND_ORE) || bs.is(Blocks.DEEPSLATE_DIAMOND_ORE) || bs.is(Blocks.EMERALD_ORE)
        || bs.is(Blocks.DEEPSLATE_COAL_ORE) || bs.is(Blocks.GOLD_ORE) || bs.is(Blocks.DEEPSLATE_GOLD_ORE);
  }

  private List<ItemStack> getDrops(ServerLevel level, BlockPos pos) {
    BlockState state = level.getBlockState(pos);
    ResourceLocation lootTableId = state.getBlock().getLootTable();

    LootParams.Builder lootParamsBuilder = new LootParams.Builder(level)
        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
        .withParameter(LootContextParams.TOOL, ItemStack.EMPTY);

    LootParams lootParams = lootParamsBuilder.create(LootContextParamSets.BLOCK);

    LootTable lootTable = level.getServer().getLootData().getLootTable(lootTableId);
    return lootTable.getRandomItems(lootParams);
  }

  private void mineAt(ServerLevel level, BlockPos orePos) {
    if (orePos == null)
      return;
    this.getNavigation().moveTo(orePos.getX() + 0.5, orePos.getY(), orePos.getZ() + 0.5, 1.0D);
    if (this.position().distanceTo(Vec3.atCenterOf(orePos)) < 2.5) {
      BlockState blockState = level.getBlockState(orePos);
      if (!blockState.isAir()) {
        List<ItemStack> drops = getDrops(level, orePos);
        level.destroyBlock(orePos, false, this);

        BlockPos chestPos = findChestInHouse(level, this.blockPosition());
        if (chestPos == null) {
          createChestWithSign(level, this.blockPosition().offset(1, 0, 0));
          chestPos = this.blockPosition().offset(1, 0, 0);
        }
        for (ItemStack drop : drops)
          storeOreInChest(level, chestPos, drop);
      }
    }
  }

  // -------------------------
  // Construir casa, escape, helpers, etc (mantidos)
  // -------------------------
  private void createChestWithSign(ServerLevel level, BlockPos pos) {
    BlockPos chestPos = pos;
    if (!level.getBlockState(chestPos).canBeReplaced())
      chestPos = chestPos.above();
    level.setBlock(chestPos, Blocks.CHEST.defaultBlockState(), 3);

    BlockPos signPos = chestPos.north();
    level.setBlock(signPos, Blocks.OAK_SIGN.defaultBlockState(), 3);

    BlockEntity signEntity = level.getBlockEntity(signPos);
    if (signEntity instanceof SignBlockEntity sign) {
      SignText signText = new SignText();
      signText.setMessage(0, Component.literal("Meus minérios"));
      signText.setMessage(1, Component.literal(DyeColor.RED + "NÃO MEXA!!"));
      sign.setText(signText, false);
      sign.setChanged();
    }
  }

  private boolean isAreaFree(ServerLevel level, BlockPos origin, int width, int height, int depth) {
    int countSolid = 0;
    int total = width * height * depth;
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        for (int z = 0; z < depth; z++) {
          BlockPos pos = origin.offset(x, y, z);
          if (!level.getBlockState(pos).isAir())
            countSolid++;
        }
      }
    }
    return countSolid < total / 4;
  }

  private void buildSimpleHouse(ServerLevel level, BlockPos origin) {
    int w = 5, h = 4, d = 5;
    BlockPos base = origin.immutable();
    // Floor
    for (int x = 0; x < w; x++)
      for (int z = 0; z < d; z++)
        level.setBlock(base.offset(x, 0, z), Blocks.STONE.defaultBlockState(), 3);

    for (int y = 1; y <= h; y++) {
      for (int x = 0; x < w; x++) {
        level.setBlock(base.offset(x, y, 0), Blocks.OAK_PLANKS.defaultBlockState(), 3);
        level.setBlock(base.offset(x, y, d - 1), Blocks.OAK_PLANKS.defaultBlockState(), 3);
      }
      for (int z = 0; z < d; z++) {
        level.setBlock(base.offset(0, y, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
        level.setBlock(base.offset(w - 1, y, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);
      }
    }

    for (int x = -1; x <= w; x++)
      for (int z = -1; z <= d; z++)
        level.setBlock(base.offset(x, h + 1, z), Blocks.OAK_PLANKS.defaultBlockState(), 3);

    BlockPos doorPos = base.offset(w / 2, 1, 0);
    level.setBlock(doorPos, Blocks.OAK_DOOR.defaultBlockState(), 3);
    level.setBlock(doorPos.above(), Blocks.OAK_DOOR.defaultBlockState(), 3);

    level.setBlock(base, Blocks.CRAFTING_TABLE.defaultBlockState(), 3);
    level.setBlock(base.offset(1, 0, 0), Blocks.FURNACE.defaultBlockState(), 3);
    level.setBlock(base.offset(2, 0, 0), Blocks.CHEST.defaultBlockState(), 3);
    level.setBlock(base.offset(0, 0, 1), Blocks.RED_BED.defaultBlockState(), 3);
    level.setBlock(base.offset(1, 2, 1), Blocks.TORCH.defaultBlockState(), 3);

    if (level.getServer() != null)
      level.getServer().getPlayerList().broadcastSystemMessage(
          Component.literal("§e" + this.getName().getString() + " construiu uma casinha."), false);
  }

  private boolean isInHole(Level level, BlockPos pos) {
    boolean walls = true;
    for (Direction d : Direction.Plane.HORIZONTAL) {
      if (!level.getBlockState(pos.relative(d)).canOcclude()) {
        walls = false;
        break;
      }
    }
    boolean ceiling = level.getBlockState(pos.above()).canOcclude();
    return walls && ceiling;
  }

  private boolean tryEscapeHole(ServerLevel level) {
    if (this.onGround())
      this.jumpFromGround();
    BlockPos p = this.blockPosition();
    for (Direction d : Direction.Plane.HORIZONTAL) {
      BlockPos test = p.relative(d);
      if (level.getBlockState(test).isAir() && level.getBlockState(test.above()).isAir()) {
        this.getNavigation().moveTo(test.getX() + 0.5, test.getY(), test.getZ() + 0.5, 1.0D);
        return true;
      }
    }
    return false;
  }

  private void requestHelp(Level level, Player owner) {
    String msg = this.getName().getString() + ": preciso de ajuda, estou preso!";
    if (owner != null) {
      owner.sendSystemMessage(Component.literal(msg));
    } else {
      if (level.getServer() != null)
        level.getServer().getPlayerList().broadcastSystemMessage(Component.literal(msg), false);
    }
  }

  private BlockPos findChestInHouse(ServerLevel level, BlockPos origin) {
    int searchRadius = 7;
    for (int dx = -searchRadius; dx <= searchRadius; dx++)
      for (int dy = -1; dy <= 3; dy++)
        for (int dz = -searchRadius; dz <= searchRadius; dz++) {
          BlockPos pos = origin.offset(dx, dy, dz);
          BlockEntity be = level.getBlockEntity(pos);
          if (be instanceof ChestBlockEntity)
            return pos;
        }
    return null;
  }

  private Player findNearestPlayer(Level level, double radius) {
    List<Player> list = level.getEntitiesOfClass(Player.class, this.getBoundingBox().inflate(radius),
        p -> !p.equals(this));
    if (list.isEmpty())
      return null;
    Player best = null;
    double bestD = Double.MAX_VALUE;
    for (Player p : list) {
      double d = p.distanceToSqr(this);
      if (d < bestD) {
        bestD = d;
        best = p;
      }
    }
    return best;
  }

  private <T extends net.minecraft.world.entity.LivingEntity> T findNearestEntity(Level level, Class<T> clazz,
      double radius) {
    List<T> list = level.getEntitiesOfClass(clazz, this.getBoundingBox().inflate(radius), e -> !e.equals(this));
    if (list.isEmpty())
      return null;
    T best = null;
    double bestD = Double.MAX_VALUE;
    for (T e : list) {
      double d = e.distanceToSqr(this);
      if (d < bestD) {
        bestD = d;
        best = e;
      }
    }
    return best;
  }

  // -------------------------
  // Data/Inventory (persist owner por-entidade)
  // -------------------------
  public void initInventory() {
    this.addItemToInventory(new ItemStack(Items.NETHERITE_AXE));
    this.addItemToInventory(new ItemStack(Items.NETHERITE_PICKAXE));
    this.addItemToInventory(new ItemStack(Items.NETHERITE_SWORD));
    this.addItemToInventory(new ItemStack(Items.COOKED_BEEF, 3 * 64));
  }

  public boolean addItemToInventory(ItemStack stack) {
    for (int i = 0; i < inventory.getContainerSize(); i++) {
      ItemStack slotStack = inventory.getItem(i);
      if (slotStack.isEmpty()) {
        inventory.setItem(i, stack.copy());
        return true;
      } else if (ItemStack.isSameItem(slotStack, stack)
          && slotStack.getCount() + stack.getCount() <= slotStack.getMaxStackSize()) {
        slotStack.grow(stack.getCount());
        return true;
      }
    }
    return false;
  }

  @Override
  public void addAdditionalSaveData(CompoundTag compound) {
    super.addAdditionalSaveData(compound);
    compound.put("Inventory", inventory.createTag());
    if (this.getOwnerID() != null)
      compound.putUUID("OwnerUUID", this.getOwnerID());
  }

  @Override
  public void readAdditionalSaveData(CompoundTag compound) {
    super.readAdditionalSaveData(compound);
    try {
      inventory.fromTag(compound.getList("Inventory", 10));
    } catch (Exception ignore) {
    }
    if (compound.hasUUID("OwnerUUID")) {
      this.setOwnerID(compound.getUUID("OwnerUUID"));
    }
  }

  // -------------------------
  // Atributos
  // -------------------------
  public static AttributeSupplier.Builder createAttributes() {
    return PathfinderMob.createMobAttributes()
        .add(Attributes.MAX_HEALTH, 20)
        .add(Attributes.ARMOR, 0)
        .add(Attributes.MOVEMENT_SPEED, 0.3)
        .add(Attributes.FOLLOW_RANGE, 16)
        .add(Attributes.KNOCKBACK_RESISTANCE, 0.5)
        .add(Attributes.ATTACK_DAMAGE, 3);
  }

  @Override
  public MobType getMobType() {
    return MobType.UNDEFINED;
  }
}
