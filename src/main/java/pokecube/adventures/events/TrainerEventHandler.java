package pokecube.adventures.events;

import java.util.List;
import java.util.Random;
import java.util.function.Consumer;

import com.google.common.collect.Lists;

import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSource;
import net.minecraft.entity.Entity;
import net.minecraft.entity.INPC;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.MobEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.merchant.villager.VillagerEntity;
import net.minecraft.entity.monster.ZombieEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.ServerPlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.MerchantOffer;
import net.minecraft.item.MerchantOffers;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.util.Direction;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.server.ServerWorld;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.LivingSetAttackTargetEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.StartTracking;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import pokecube.adventures.Config;
import pokecube.adventures.PokecubeAdv;
import pokecube.adventures.ai.tasks.AIBattle;
import pokecube.adventures.ai.tasks.AIFindTarget;
import pokecube.adventures.ai.tasks.AIMate;
import pokecube.adventures.capabilities.CapabilityHasPokemobs;
import pokecube.adventures.capabilities.CapabilityHasPokemobs.DefaultPokemobs;
import pokecube.adventures.capabilities.CapabilityHasPokemobs.IHasPokemobs;
import pokecube.adventures.capabilities.CapabilityHasRewards.DefaultRewards;
import pokecube.adventures.capabilities.CapabilityHasRewards.Reward;
import pokecube.adventures.capabilities.CapabilityHasTrades.DefaultTrades;
import pokecube.adventures.capabilities.CapabilityNPCAIStates;
import pokecube.adventures.capabilities.CapabilityNPCAIStates.DefaultAIStates;
import pokecube.adventures.capabilities.CapabilityNPCAIStates.IHasNPCAIStates;
import pokecube.adventures.capabilities.CapabilityNPCMessages;
import pokecube.adventures.capabilities.CapabilityNPCMessages.DefaultMessager;
import pokecube.adventures.capabilities.CapabilityNPCMessages.IHasMessages;
import pokecube.adventures.capabilities.utils.MessageState;
import pokecube.adventures.capabilities.utils.TypeTrainer;
import pokecube.adventures.capabilities.utils.TypeTrainer.TrainerTrades;
import pokecube.adventures.entity.trainer.TrainerBase;
import pokecube.adventures.entity.trainer.TrainerNpc;
import pokecube.adventures.items.TrainerEditor;
import pokecube.adventures.network.PacketTrainer;
import pokecube.adventures.utils.DBLoader;
import pokecube.core.PokecubeCore;
import pokecube.core.ai.routes.GuardAI;
import pokecube.core.ai.routes.GuardAICapability;
import pokecube.core.ai.routes.IGuardAICapability;
import pokecube.core.database.Database;
import pokecube.core.entity.npc.NpcMob;
import pokecube.core.entity.npc.NpcType;
import pokecube.core.entity.pokemobs.EntityPokemob;
import pokecube.core.events.NpcSpawn;
import pokecube.core.events.PCEvent;
import pokecube.core.events.pokemob.InteractEvent;
import pokecube.core.events.pokemob.RecallEvent;
import pokecube.core.events.pokemob.SpawnEvent.SendOut;
import pokecube.core.handlers.events.EventsHandler;
import pokecube.core.handlers.events.SpawnHandler;
import pokecube.core.interfaces.IPokemob;
import pokecube.core.items.pokecubes.PokecubeManager;
import pokecube.core.moves.PokemobDamageSource;
import pokecube.core.moves.TerrainDamageSource;
import thut.api.entity.ai.GoalsWrapper;
import thut.api.entity.ai.IAIRunnable;
import thut.api.maths.Vector3;
import thut.api.world.mobs.data.DataSync;
import thut.core.common.network.EntityUpdate;
import thut.core.common.world.mobs.data.DataSync_Impl;
import thut.core.common.world.mobs.data.SyncHandler;
import thut.core.common.world.mobs.data.types.Data_ItemStack;
import thut.core.common.world.mobs.data.types.Data_String;

public class TrainerEventHandler
{
    private static class Provider extends GuardAICapability implements ICapabilitySerializable<CompoundNBT>
    {
        private final LazyOptional<IGuardAICapability> holder = LazyOptional.of(() -> this);

        @Override
        public void deserializeNBT(final CompoundNBT nbt)
        {
            EventsHandler.GUARDAI_CAP.getStorage().readNBT(EventsHandler.GUARDAI_CAP, this, null, nbt);
        }

        @Override
        public <T> LazyOptional<T> getCapability(final Capability<T> capability, final Direction facing)
        {
            return EventsHandler.GUARDAI_CAP.orEmpty(capability, this.holder);
        }

        @Override
        public CompoundNBT serializeNBT()
        {
            return (CompoundNBT) EventsHandler.GUARDAI_CAP.getStorage().writeNBT(EventsHandler.GUARDAI_CAP, this, null);
        }
    }

    private static class NpcOffers implements Consumer<MerchantOffers>
    {
        final NpcMob mob;

        public NpcOffers(final NpcMob mob)
        {
            this.mob = mob;

            // Check for blank name, and if so, randomize it.
            final List<String> names = mob.isMale() ? TypeTrainer.maleNames : TypeTrainer.femaleNames;
            if (!names.isEmpty() && mob.name.isEmpty()) mob.name = "pokecube." + mob.getNpcType().getName() + ".named:"
                    + names.get(new Random().nextInt(names.size()));
        }

        @Override
        public void accept(final MerchantOffers t)
        {
            final Random rand = new Random(this.mob.getUniqueID().getLeastSignificantBits());
            final String type = this.mob.getNpcType() == NpcType.PROFESSOR ? "professor" : "merchant";
            final TrainerTrades trades = TypeTrainer.tradesMap.get(type);
            if (trades != null) trades.addTrades(this.mob.getOffers(), rand);
            else this.mob.getOffers().addAll(TypeTrainer.merchant.getRecipes(rand));
        }

    }

    private static class NpcOffer implements Consumer<MerchantOffer>
    {
        final NpcMob mob;

        public NpcOffer(final NpcMob mob)
        {
            this.mob = mob;
        }

        @Override
        public void accept(final MerchantOffer t)
        {
            // TODO decide if we want anything here
            this.mob.getNpcType();
        }

    }

    static final ResourceLocation POKEMOBSCAP = new ResourceLocation(PokecubeAdv.ID, "pokemobs");
    static final ResourceLocation AICAP       = new ResourceLocation(PokecubeAdv.ID, "ai");
    static final ResourceLocation MESSAGECAP  = new ResourceLocation(PokecubeAdv.ID, "messages");
    static final ResourceLocation REWARDSCAP  = new ResourceLocation(PokecubeAdv.ID, "rewards");
    static final ResourceLocation DATASCAP    = new ResourceLocation(PokecubeAdv.ID, "data");
    static final ResourceLocation TRADESCAP   = new ResourceLocation(PokecubeAdv.ID, "trades");
    static final ResourceLocation GUARDCAP    = new ResourceLocation(PokecubeAdv.ID, "guardai");

    private static void attach_guard(final AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getCapabilities().containsKey(TrainerEventHandler.GUARDCAP)) return;
        if (event.getObject() instanceof VillagerEntity || event.getObject() instanceof TrainerNpc) event.addCapability(
                TrainerEventHandler.GUARDCAP, new Provider());
    }

    private static void attach_pokemobs(final AttachCapabilitiesEvent<Entity> event)
    {
        if (!(event.getObject() instanceof MobEntity)) return;
        if (TypeTrainer.mobTypeMapper.getType((LivingEntity) event.getObject(), false) == null) return;
        if (TrainerEventHandler.hasCap(event)) return;

        final DefaultPokemobs mobs = new DefaultPokemobs();
        final DefaultRewards rewards = new DefaultRewards();
        final MobEntity mob = (MobEntity) event.getObject();
        ItemStack stack = ItemStack.EMPTY;
        try
        {
            stack = TrainerEventHandler.fromString(Config.instance.defaultReward, event.getObject());
        }
        catch (final CommandException e)
        {
            PokecubeCore.LOGGER.warn("Error with default trainer rewards " + Config.instance.defaultReward, e);
        }
        if (!stack.isEmpty()) rewards.getRewards().add(new Reward(stack));
        final DefaultAIStates aiStates = new DefaultAIStates();
        final DefaultMessager messages = new DefaultMessager();
        mobs.init((LivingEntity) event.getObject(), aiStates, messages, rewards);
        event.addCapability(TrainerEventHandler.POKEMOBSCAP, mobs);
        event.addCapability(TrainerEventHandler.AICAP, aiStates);
        event.addCapability(TrainerEventHandler.MESSAGECAP, messages);
        event.addCapability(TrainerEventHandler.REWARDSCAP, rewards);

        if (mob instanceof TrainerBase) event.addCapability(TrainerEventHandler.TRADESCAP, new DefaultTrades());

        DataSync data = TrainerEventHandler.getData(event);
        if (data == null)
        {
            data = new DataSync_Impl();
            event.addCapability(TrainerEventHandler.DATASCAP, (DataSync_Impl) data);
        }
        mobs.datasync = data;
        mobs.holder.TYPE = data.register(new Data_String(), "");

        for (int i = 0; i < 6; i++)
            mobs.holder.POKEMOBS[i] = data.register(new Data_ItemStack(), ItemStack.EMPTY);
    }

    @SubscribeEvent
    public static void attachCaps(final AttachCapabilitiesEvent<Entity> event)
    {
        // Add capabilities for guard AI
        TrainerEventHandler.attach_guard(event);
        // Add pokemob holder caps
        TrainerEventHandler.attach_pokemobs(event);
    }

    public static ItemStack fromString(final String arg, final ICommandSource sender) throws CommandException
    {
        // TODO use ItemArgument for this somehow.
        return new ItemStack(Items.EMERALD);
    }

    private static DataSync getData(final AttachCapabilitiesEvent<Entity> event)
    {
        for (final ICapabilityProvider provider : event.getCapabilities().values())
            if (provider.getCapability(SyncHandler.CAP).isPresent()) return provider.getCapability(SyncHandler.CAP)
                    .orElse(null);
        return null;
    }

    private static boolean hasCap(final AttachCapabilitiesEvent<Entity> event)
    {
        if (event.getCapabilities().containsKey(TrainerEventHandler.POKEMOBSCAP)) return true;
        for (final ICapabilityProvider provider : event.getCapabilities().values())
            if (provider.getCapability(CapabilityHasPokemobs.HASPOKEMOBS_CAP).isPresent()) return true;
        return false;
    }

    @SubscribeEvent
    /**
     * Calls processInteract
     *
     * @param evt
     */
    public static void interactEvent(final PlayerInteractEvent.EntityInteract evt)
    {
        if (evt.getWorld().isRemote) return;
        final String ID = "LastSuccessInteractEvent";
        final long time = evt.getTarget().getPersistentData().getLong(ID);
        if (time == evt.getTarget().getEntityWorld().getGameTime()) return;
        TrainerEventHandler.processInteract(evt, evt.getTarget());
        evt.getTarget().getPersistentData().putLong(ID, evt.getTarget().getEntityWorld().getGameTime());
    }

    @SubscribeEvent
    /**
     * Calls processInteract
     *
     * @param evt
     */
    public static void interactEvent(final PlayerInteractEvent.EntityInteractSpecific evt)
    {
        if (evt.getWorld().isRemote) return;
        final String ID = "LastSuccessInteractEvent";
        final long time = evt.getTarget().getPersistentData().getLong(ID);
        if (time == evt.getTarget().getEntityWorld().getGameTime()) return;
        TrainerEventHandler.processInteract(evt, evt.getTarget());
        evt.getTarget().getPersistentData().putLong(ID, evt.getTarget().getEntityWorld().getGameTime());
    }

    /**
     * For custom item interactions with pokemobs.
     *
     * @param event
     */
    @SubscribeEvent
    public static void interactWithPokemob(final InteractEvent event)
    {
        // final PlayerEntity player = event.player;
        // final Hand hand = event.event.getHand();
        // final ItemStack held = player.getHeldItem(hand);
        // TODO trainer edit item
        // if (held.getItem() instanceof ItemTrainer)
        // {
        // PacketTrainer.sendEditOpenPacket(event.pokemob.getEntity(),
        // (ServerPlayerEntity) player);
        // event.setResult(Result.DENY);
        // }
    }

    @SubscribeEvent
    /**
     * This manages invulnerability of npcs to pokemobs, as well as managing
     * the target allocation for trainers.
     *
     * @param evt
     */
    public static void livingHurtEvent(final LivingHurtEvent evt)
    {
        final IHasPokemobs pokemobHolder = CapabilityHasPokemobs.getHasPokemobs(evt.getEntity());
        final IHasMessages messages = CapabilityNPCMessages.getMessages(evt.getEntity());

        if (evt.getEntity() instanceof INPC && !Config.instance.pokemobsHarmNPCs && (evt
                .getSource() instanceof PokemobDamageSource || evt.getSource() instanceof TerrainDamageSource)) evt
                        .setAmount(0);

        if (evt.getSource().getTrueSource() instanceof LivingEntity)
        {
            if (messages != null)
            {
                messages.sendMessage(MessageState.HURT, evt.getSource().getTrueSource(), evt.getEntity()
                        .getDisplayName(), evt.getSource().getTrueSource().getDisplayName());
                messages.doAction(MessageState.HURT, (LivingEntity) evt.getSource().getTrueSource());
            }
            if (pokemobHolder != null && pokemobHolder.getTarget() == null) pokemobHolder.setTarget((LivingEntity) evt
                    .getSource().getTrueSource());
        }
    }

    @SubscribeEvent
    /**
     * Ensures the IHasPokemobs object has synced target with the MobEntity
     * object.
     *
     * @param evt
     */
    public static void livingSetTargetEvent(final LivingSetAttackTargetEvent evt)
    {
        if (evt.getTarget() == null || !(evt.getEntity() instanceof LivingEntity)) return;
        final IHasPokemobs pokemobHolder = CapabilityHasPokemobs.getHasPokemobs(evt.getTarget());
        if (pokemobHolder != null && pokemobHolder.getTarget() == null) pokemobHolder.setTarget((LivingEntity) evt
                .getEntity());
    }

    @SubscribeEvent
    /**
     * Initializes the AI for the trainers when they join the world.
     *
     * @param event
     */
    public static void onJoinWorld(final EntityJoinWorldEvent event)
    {
        if (!(event.getEntity() instanceof LivingEntity)) return;
        TrainerEventHandler.initTrainer((LivingEntity) event.getEntity(), SpawnReason.NATURAL);
    }

    @SubscribeEvent
    public static void onNpcSpawn(final NpcSpawn event)
    {
        TrainerEventHandler.initTrainer(event.getNpcMob(), event.getReason());
    }

    private static void initTrainer(final LivingEntity npc, final SpawnReason reason)
    {
        if (npc instanceof NpcMob && !(npc instanceof TrainerBase))
        {
            ((NpcMob) npc).setInitOffers(new NpcOffers((NpcMob) npc));
            ((NpcMob) npc).setUseOffers(new NpcOffer((NpcMob) npc));
        }

        final IHasPokemobs mobs = CapabilityHasPokemobs.getHasPokemobs(npc);
        if (mobs == null || !(npc.getEntityWorld() instanceof ServerWorld)) return;
        if (npc.getPersistentData().getLong("pokeadv_join") == npc.getEntityWorld().getGameTime()) return;
        npc.getPersistentData().putLong("pokeadv_join", npc.getEntityWorld().getGameTime());

        // Wrap it as a fake vanilla AI
        if (npc instanceof MobEntity)
        {
            final List<IAIRunnable> ais = Lists.newArrayList();
            // All can battle, but only trainers will path during battle.
            ais.add(new AIBattle(npc, !(npc instanceof TrainerNpc)).setPriority(0));

            // All attack zombies.
            ais.add(new AIFindTarget(npc, ZombieEntity.class).setPriority(20));
            // Only trainers specifically target players.
            if (npc instanceof TrainerNpc)
            {
                ais.add(new AIFindTarget(npc, PlayerEntity.class).setPriority(10));
                ais.add(new AIMate(npc, ((TrainerNpc) npc).getClass()));
            }

            // 5% chance of battling a random nearby pokemob if they see it.
            if (Config.instance.trainersBattlePokemobs) ais.add(new AIFindTarget(npc, 0.05f, EntityPokemob.class)
                    .setPriority(20));

            // 1% chance of battling another of same class if seen
            if (Config.instance.trainersBattleEachOther) ais.add(new AIFindTarget(npc, 0.01f, npc.getClass())
                    .setPriority(20));
            final MobEntity living = (MobEntity) npc;
            living.goalSelector.addGoal(0, new GoalsWrapper(npc, ais.toArray(new IAIRunnable[0])));

            final IGuardAICapability guard = npc.getCapability(EventsHandler.GUARDAI_CAP).orElse(null);
            if (guard != null) living.goalSelector.addGoal(0, new GuardAI(living, guard));
        }

        final TypeTrainer newType = TypeTrainer.mobTypeMapper.getType(npc, true);
        if (newType == null) return;
        mobs.setType(newType);
        final int level = SpawnHandler.getSpawnLevel(npc.getEntityWorld(), Vector3.getNewVector().set(npc), Database
                .getEntry(1));
        TypeTrainer.getRandomTeam(mobs, npc, level, npc.getEntityWorld());
        EntityUpdate.sendEntityUpdate(npc);
    }

    /**
     * This deals with the interaction logic for trainers. It sends the
     * messages for MessageState.INTERACT, as well as applies the doAction. It
     * also handles opening the edit gui for the trainers when the player has
     * the trainer editor.
     *
     * @param evt
     * @param target
     */
    public static void processInteract(final PlayerInteractEvent evt, final Entity target)
    {
        // TODO trainer edit item.
        final IHasMessages messages = CapabilityNPCMessages.getMessages(target);
        final IHasPokemobs pokemobs = CapabilityHasPokemobs.getHasPokemobs(target);
        if (!target.isSneaking() && pokemobs != null && evt.getItemStack().getItem() instanceof TrainerEditor)
        {
            evt.setCanceled(true);
            if (evt.getPlayer() instanceof ServerPlayerEntity) PacketTrainer.sendEditOpenPacket(target,
                    (ServerPlayerEntity) evt.getPlayer());
            return;
        }
        if (messages != null)
        {
            messages.sendMessage(MessageState.INTERACT, evt.getPlayer(), target.getDisplayName(), evt.getPlayer()
                    .getDisplayName());
            messages.doAction(MessageState.INTERACT, evt.getPlayer());
        }
    }

    @SubscribeEvent
    public static void serverStarting(final FMLServerAboutToStartEvent event)
    {
        DBLoader.load();
    }

    @SubscribeEvent
    /**
     * This prevents trainer's pokemobs going to PC
     *
     * @param evt
     */
    public static void TrainerPokemobPC(final PCEvent evt)
    {
        if (evt.owner instanceof TrainerNpc) evt.setCanceled(true);
    }

    @SubscribeEvent(receiveCanceled = false)
    /**
     * This sends pokemobs back to their NPC trainers when they are recalled.
     *
     * @param evt
     */
    public static void TrainerRecallEvent(final pokecube.core.events.pokemob.RecallEvent evt)
    {
        if (evt instanceof RecallEvent.Pre) return;

        final IPokemob recalled = evt.recalled;
        final LivingEntity owner = recalled.getOwner();
        if (owner == null) return;
        final IHasPokemobs pokemobHolder = CapabilityHasPokemobs.getHasPokemobs(owner);
        if (pokemobHolder != null)
        {
            if (recalled == pokemobHolder.getOutMob()) pokemobHolder.setOutMob(null);
            pokemobHolder.addPokemob(PokecubeManager.pokemobToItem(recalled));
        }
    }

    @SubscribeEvent
    /**
     * This links the pokemob to the trainer when it is sent out.
     *
     * @param evt
     */
    public static void TrainerSendOutEvent(final SendOut.Post evt)
    {
        final IPokemob sent = evt.pokemob;
        final LivingEntity owner = sent.getOwner();
        if (owner == null || owner instanceof PlayerEntity) return;
        final IHasPokemobs pokemobHolder = CapabilityHasPokemobs.getHasPokemobs(owner);
        if (pokemobHolder != null)
        {
            if (pokemobHolder.getOutMob() != null && pokemobHolder.getOutMob() != evt.pokemob)
            {
                pokemobHolder.getOutMob().onRecall();
                pokemobHolder.setOutMob(evt.pokemob);
            }
            else pokemobHolder.setOutMob(evt.pokemob);
            final IHasNPCAIStates aiStates = CapabilityNPCAIStates.getNPCAIStates(owner);
            if (aiStates != null) aiStates.setAIState(IHasNPCAIStates.THROWING, false);
        }
    }

    @SubscribeEvent
    /**
     * This manages making of trainers invisible if they have been defeated, if
     * this is enabled for the given trainer.
     *
     * @param event
     */
    public static void TrainerWatchEvent(final StartTracking event)
    {
        if (!(event.getTarget() instanceof TrainerNpc)) return;
        final IHasPokemobs mobs = CapabilityHasPokemobs.getHasPokemobs(event.getEntity());
        if (!(mobs instanceof DefaultPokemobs)) return;
        final DefaultPokemobs pokemobs = (DefaultPokemobs) mobs;
        if (event.getPlayer() instanceof ServerPlayerEntity)
        {
            final TrainerNpc trainer = (TrainerNpc) event.getTarget();
            if (pokemobs.notifyDefeat)
            {
                final PacketTrainer packet = new PacketTrainer(PacketTrainer.MESSAGENOTIFYDEFEAT);
                packet.data.putInt("I", trainer.getEntityId());
                packet.data.putBoolean("V", pokemobs.hasDefeated(event.getPlayer()));
                PokecubeAdv.packets.sendTo(packet, (ServerPlayerEntity) event.getPlayer());
            }
        }
    }
}