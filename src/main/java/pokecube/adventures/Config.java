package pokecube.adventures;

import java.util.Map.Entry;

import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.config.ModConfig.Type;
import pokecube.adventures.blocks.afa.AfaTile;
import pokecube.adventures.blocks.daycare.DaycareTile;
import pokecube.adventures.blocks.genetics.helper.BaseGeneticsTile;
import pokecube.adventures.blocks.genetics.helper.ClonerHelper;
import pokecube.adventures.blocks.genetics.helper.ClonerHelper.DNAPack;
import pokecube.adventures.blocks.genetics.helper.recipe.RecipeClone;
import pokecube.adventures.blocks.warppad.WarppadTile;
import pokecube.adventures.utils.EnergyHandler;
import pokecube.core.database.Database;
import pokecube.core.entity.pokemobs.genetics.genes.SpeciesGene;
import pokecube.core.entity.pokemobs.genetics.genes.SpeciesGene.SpeciesInfo;
import pokecube.core.handlers.ItemGenerator;
import pokecube.core.items.ItemFossil;
import thut.api.entity.genetics.Alleles;
import thut.core.common.config.Config.ConfigData;
import thut.core.common.config.Configure;

public class Config extends ConfigData
{
    public static final Config instance = new Config();

    private static final String MACHINE = "machine";
    private static final String TRAINER = "trainers";
    private static final String BAG     = "bag";

    @Configure(category = Config.TRAINER, comment = "If true, anything that is an INPC will be made into a trainer")
    public boolean npcsAreTrainers = true;

    @Configure(category = Config.TRAINER, comment = "This is the time, in ticks, which a trainer will go on cooldown for, for a player, after winning or losing a battle")
    public int trainerCooldown     = 5000;
    @Configure(category = Config.TRAINER, comment = "How far trainers will check for targets to battle")
    public int trainerSightRange   = 8;
    @Configure(category = Config.TRAINER, comment = "This is how long the trainer will be on cooldown after a battle, for targets other than the one they were battling")
    public int trainerBattleDelay  = 50;
    @Configure(category = Config.TRAINER, comment = "This is the delay between the trainer deciding to send out a pokemob, and actually doing so")
    public int trainerSendOutDelay = 50;
    @Configure(category = Config.TRAINER, comment = "Trainers will look for targets every this many ticks")
    public int trainerAgroRate     = 20;

    @Configure(category = Config.TRAINER, comment = "If true, trainer's pokemobs will gain exp as they battle")
    public boolean trainerslevel           = true;
    @Configure(category = Config.TRAINER, comment = "If true, trainers will spawn naturally")
    public boolean trainerSpawn            = true;
    @Configure(category = Config.TRAINER, comment = "This determines how sparsely trainers spawn, there will only be trainerDensity trainers spawn every this far, excluding special spawns like villages")
    public int     trainerBox              = 256;
    @Configure(category = Config.TRAINER, comment = "how many trainers spawn in trainerBox")
    public double  trainerDensity          = 2;
    @Configure(category = Config.TRAINER, comment = "If false, pokemobs will not be able to hurt NPCs")
    public boolean pokemobsHarmNPCs        = false;
    @Configure(category = Config.TRAINER, comment = "If true, trainers will occasionally battle each other")
    public boolean trainersBattleEachOther = true;
    @Configure(category = Config.TRAINER, comment = "If true, trainers will occasionally battle wild pokemobs")
    public boolean trainersBattlePokemobs  = true;
    @Configure(category = Config.TRAINER, comment = "if the trainer does not see its target for this many ticks, it will give up the battle")
    public int     trainerDeAgressTicks    = 100;
    @Configure(category = Config.TRAINER, comment = "If true, trainers will occasionally mate to produce more trainers")
    public boolean trainersMate            = true;
    @Configure(category = Config.TRAINER, comment = "if true, trainers that are non agressive (ie on cooldown or bribed) will offer item trades")
    public boolean trainersTradeItems      = true;
    @Configure(category = Config.TRAINER, comment = "if true, trainers which are non agressive, might offer their mobs as trades for your mobs")
    public boolean trainersTradeMobs       = true;
    @Configure(category = Config.TRAINER, comment = "if true, trainers with no pokemobs will despawn")
    public boolean cullNoMobs              = false;
    @Configure(category = Config.TRAINER, comment = "if true, if there is no player within aiPauseDistance, the trainer will not tick")
    public boolean trainerAIPause          = true;
    @Configure(category = Config.TRAINER, comment = "see trainerAIPause")
    public int     aiPauseDistance         = 64;
    @Configure(category = Config.TRAINER, comment = "This is how far trainers will check to see if there are too many nearby to battle")
    public int     trainer_crowding_radius = 16;
    @Configure(category = Config.TRAINER, comment = "If there are more than this many trainers within trainer_crowding_radius, then the trainers will not agress players")
    public int     trainer_crowding_number = 5;

    @Configure(category = Config.TRAINER, comment = "this is the default reward for trainers")
    public String trainer_defeat_reward = "{\"values\":{\"id\":\"minecraft:emerald\",\"n\":\"1\"}}";

    // Energy Sihpon related options
    @Configure(category = Config.MACHINE, comment = "This it the maximum forge energy per tick from an energy siphon")
    public int maxOutput = 256;

    // Energy Sihpon related options
    @Configure(category = Config.MACHINE, comment = "this is how often the siphon will search for new pokemobs to pull from")
    public int siphonUpdateRate = 100;
    @Configure(category = Config.MACHINE, comment = "This scales the amount of hunger produced by pulling energy out of a pokemob")
    public int energyHungerCost = 5;

    @Configure(category = Config.MACHINE, comment = "if true, energy siphons can be linked to recieving blocks with the device linker")
    public boolean wirelessSiphons = true;

    @Configure(category = Config.MACHINE, comment = "How much energy you get of a pokemob, a is the max of spatk and atk, and x is the level of the pokemob")
    public String powerFunction = "a*x/10";

    // Cloning related options
    @Configure(type = Type.CLIENT, category = Config.MACHINE, comment = "If true, there are additional tooltips by default in the genetics blocks")
    public boolean expandedDNATooltips      = true;
    @Configure(category = Config.MACHINE, comment = "Amount of forge energy needed to revive a fossil")
    public int     fossilReanimateCost      = 10000;
    @Configure(category = Config.MACHINE, comment = "If true, automatically registers DNA extraction recipes for fossils")
    public boolean autoAddFossilDNA         = true;
    @Configure(category = Config.MACHINE, comment = "can be used to scale the energy cost of genetics machines, x is the original energy input")
    public String  clonerEfficiencyFunction = "x";

    // Options related to warp pad
    @Configure(category = Config.MACHINE, comment = "energy cost of warppad use, dw is 0 for the same dimension, varies otherwise")
    public String  warpPadCostFunction = "(dx)*(dx) + (dy)*(dy) + (dz)*(dz) + (5*dw)^4";
    @Configure(category = Config.MACHINE, comment = "if false, warp pads will not require energy")
    public boolean warpPadEnergy       = true;
    @Configure(category = Config.MACHINE, comment = "warp pads can charge up to this much stored energy")
    public int     warpPadMaxEnergy    = 10000000;

    // Options related to daycare block.
    @Configure(category = Config.MACHINE, comment = "this is how much daycare power is produced per item of fuel consumed")
    public int     dayCarePowerPerFuel = 256;
    @Configure(category = Config.MACHINE, comment = "daycares will only run every this many ticks")
    public int     dayCareTickRate     = 20;
    @Configure(category = Config.MACHINE, comment = "amount of daycare power requrired to give exp to a pokemob, variables are: x - pokemob's current exp, l - pokemob's current lvl, n - pokemob's needed exp from current level to next")
    public String  dayCarePowerPerExp  = "0.5";
    @Configure(category = Config.MACHINE, comment = "amount of exp given per daycare tick, variables are: x - pokemob's current exp, l - pokemob's current lvl, n - pokemob's needed exp from current level to next")
    public String  dayCareExpFunction  = "n/10";
    @Configure(category = Config.MACHINE, comment = "If true, daycares will also speed up the breeding time")
    public boolean dayCareBreedSpeedup = true;
    @Configure(category = Config.MACHINE, comment = "How many effective ticks are added for breeding time per daycare tick")
    public int     dayCareBreedAmount  = 10;

    @Configure(category = Config.MACHINE, comment = "Effective cost of a lvl 100 pokemob, this is for applying breeding tick stuff, even though lvl 100 can't gain exp")
    public int dayCareLvl100EffectiveLevel = 30;

    // AFA related options
    @Configure(category = Config.MACHINE, comment = "How likely an AFA is to result in shiny mobs nearby if it has a shiny charm in it")
    public int    afaShinyRate         = 4096;
    @Configure(category = Config.MACHINE, comment = "energy cost of the AFA for running in ability mode, l is the level of the mob, d is the range of the AFA")
    public String afaCostFunction      = "(d^3)/(10 + 5*l)";
    @Configure(category = Config.MACHINE, comment = "energy cost of the AFA when running in shiny mode, d is the range of the AFA")
    public String afaCostFunctionShiny = "(d^3)/10";
    @Configure(category = Config.MACHINE, comment = "Maximum energy in the AFA, this effectively sets a max range for it to affect, based on the costs")
    public int    afaMaxEnergy         = 3200;
    @Configure(category = Config.MACHINE, comment = "afa ticks every this many ticks")
    public int    afaTickRate          = 5;

    @Configure(category = Config.BAG, type = Type.SERVER, comment = "If true, the bag can hold any item")
    public boolean bagsHoldEverything  = false;
    @Configure(category = Config.BAG, type = Type.SERVER, comment = "if true, the bag can hold filled pokecubes")
    public boolean bagsHoldFilledCubes = false;
    @Configure(category = Config.BAG, type = Type.SERVER, comment = "the number of pages the bag has")
    public int     bagPages            = 32;

    public boolean loaded = false;

    public Config()
    {
        super(PokecubeAdv.MODID);
    }

    @Override
    public void onUpdated()
    {
        if (!this.loaded) return;

        EnergyHandler.initParser();
        BaseGeneticsTile.initParser(this.clonerEfficiencyFunction);
        WarppadTile.initParser(this.warpPadCostFunction);
        DaycareTile.initParser(this.dayCarePowerPerExp, this.dayCareExpFunction);
        AfaTile.initParser(this.afaCostFunction, this.afaCostFunctionShiny);
        this.dayCareTickRate = Math.max(1, this.dayCareTickRate);
        this.afaTickRate = Math.max(1, this.afaTickRate);
        this.trainerAgroRate = Math.max(1, this.trainerAgroRate);
        RecipeClone.ENERGYCOST = this.fossilReanimateCost;

        if (this.autoAddFossilDNA) for (final Entry<String, ItemFossil> fossil : ItemGenerator.fossils.entrySet())
        {
            final String name = fossil.getKey();
            final ItemStack stack = new ItemStack(fossil.getValue());
            final SpeciesGene gene = new SpeciesGene();
            final SpeciesInfo info = gene.getValue();
            info.entry = Database.getEntry(name);
            final Alleles genes = new Alleles(gene, gene);
            ClonerHelper.registerDNA(new DNAPack(name, genes, 1), stack);
        }
    }

}
