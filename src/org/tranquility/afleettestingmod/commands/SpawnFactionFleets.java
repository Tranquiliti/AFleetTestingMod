package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.tranquility.afleettestingmod.AFTMUtil;

import java.util.List;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantSeededFleetManager.initRemnantFleetProperties;

public class SpawnFactionFleets implements BaseCommandWithSuggestion {
    private static final List<String> MODIFIERS = List.of("-", "-a", "-c", "-f", "-i", "-o", "-r", "-t", "-v");
    private static final List<String> PATROLS = List.of("patrolSmall", "patrolMedium", "patrolLarge");

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String[] tmp = args.split(" ");
        if (args.isEmpty() || tmp.length < 2) {
            return CommandResult.BAD_SYNTAX;
        }

        boolean disableAI = false;
        boolean clear = false;
        boolean forceFake = false;
        boolean ignoreMarketFleetSizeMult = false;
        boolean withOfficers = true;
        boolean useRemnantProperties = false;
        boolean testMode = false;
        boolean verbose = false;
        if (tmp[0].charAt(0) == '-') {
            String command = tmp[0].toLowerCase();

            for (int i = 1; i < command.length(); i++) {
                switch (command.charAt(i)) {
                    case 'a':
                        disableAI = true;
                        break;
                    case 'c':
                        clear = true;
                        break;
                    case 'f':
                        forceFake = true;
                        break;
                    case 'i':
                        ignoreMarketFleetSizeMult = true;
                        break;
                    case 'o':
                        withOfficers = false;
                        break;
                    case 'r':
                        useRemnantProperties = true;
                        break;
                    case 't':
                        testMode = true;
                        break;
                    case 'v':
                        verbose = true;
                        break;
                }
            }
        }

        String[] factionParam = tmp[1].split("/");
        String factionId = factionParam[0];
        if (Global.getSector().getFaction(factionId) == null) {
            Console.showMessage("Error: no faction found with faction id \"%s\"!".formatted(factionId));
            return CommandResult.ERROR;
        }

        String factionOverride = null;
        if (factionParam.length > 1) factionOverride = factionParam[1];
        if (factionOverride != null && Global.getSector().getFaction(factionOverride) == null) {
            Console.showMessage("Error: no overriding faction found with faction id \"%s\"!".formatted(factionOverride));
            return CommandResult.ERROR;
        }

        int numFleets = 1;
        if (tmp.length > 2) try {
            numFleets = Integer.parseInt(tmp[2]);
        } catch (NumberFormatException ex) {
            Console.showMessage("Error: numFleets must be a whole number!");
            return CommandResult.ERROR;
        }

        float combat = 0;
        String patrolType = FleetTypes.PATROL_LARGE;
        if (tmp.length > 3) {
            String patrolString = tmp[3];
            try {
                combat = Float.parseFloat(patrolString);
                patrolType = FleetTypes.TASK_FORCE;
            } catch (NumberFormatException e) {
                switch (patrolString) {
                    case FleetTypes.PATROL_SMALL:
                    case FleetTypes.PATROL_MEDIUM:
                    case FleetTypes.PATROL_LARGE:
                        patrolType = patrolString;
                        break;
                    default:
                        Console.showMessage("Error: %s is not a valid patrol type or floating-point number!".formatted(patrolString));
                        return CommandResult.ERROR;
                }
            }
        }

        Float qualityOverride = null;
        if (tmp.length > 4) try {
            qualityOverride = Float.parseFloat(tmp[4]);
        } catch (NumberFormatException ex) {
            Console.showMessage("Error: qualityOverride must be a floating-point number!");
            return CommandResult.ERROR;
        }

        MarketAPI bestMarket = null;
        if (!forceFake) bestMarket = getBestMarket(factionId);
        if (bestMarket == null) bestMarket = createFakeMarket(factionId);

        if (testMode) {
            List<CampaignFleetAPI> nearbyFleets = AFTMUtil.getNearbyFleets();
            FactionAPI spawnedFaction = Global.getSector().getFaction(factionOverride == null ? factionId : factionOverride);
            // Assuming player fleet is always the nearest fleet, so we get the next nearest
            if (!nearbyFleets.isEmpty() && nearbyFleets.size() > 1) {
                FactionAPI nearestFaction = nearbyFleets.get(1).getFaction();
                if (!nearestFaction.equals(spawnedFaction)) {
                    nearestFaction.setRelationship(Factions.PLAYER, RepLevel.INHOSPITABLE);
                    spawnedFaction.setRelationship(nearestFaction.getId(), RepLevel.VENGEFUL);
                }
            }
            spawnedFaction.setRelationship(Factions.PLAYER, RepLevel.COOPERATIVE);
        }

        AFTMUtil.FleetStatData statData = verbose ? new AFTMUtil.FleetStatData() : null;
        for (int i = 0; i < numFleets; i++) {
            CampaignFleetAPI fleet = createPatrol(bestMarket, factionId, qualityOverride, patrolType, combat, ignoreMarketFleetSizeMult, withOfficers);
            fleet.inflateIfNeeded(); // Inflate to apply d-mods
            fleet.forceSync();

            if (factionOverride != null) fleet.setFaction(factionOverride, true);

            if (verbose) statData.addStat(fleet);

            if (clear) fleet.despawn();
            else {
                Global.getSector().getCurrentLocation().spawnFleet(Global.getSector().getPlayerFleet(), 0f, 0f, fleet);
                if (!testMode) fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true, 0.2f);
                if (useRemnantProperties) initRemnantFleetProperties(null, fleet, false);
                if (disableAI) {
                    fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
                    fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
                    fleet.setAI(null);
                    fleet.setNullAIActionText("dormant");
                }
            }
        }

        StringBuilder print = new StringBuilder(clear ? "Showing " : "Spawned ").append(numFleets).append(" ").append(factionId);
        print.append(factionOverride != null ? " (" + factionOverride + ") " : " ").append(patrolType).append(" fleets, using stats from ");
        print.append(bestMarket.getName()).append(" with ship quality ").append((qualityOverride == null ? Misc.getShipQuality(bestMarket, factionId) : qualityOverride) * 100f).append("%");
        if (!ignoreMarketFleetSizeMult)
            print.append(" and fleet size ").append(bestMarket.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f) * 100f).append("%");
        print.append(verbose ? ":" : ".").append("\n");

        if (verbose) {
            statData.aggregateStats();
            statData.appendStats("Average " + Global.getSector().getFaction(factionId).getDisplayName() + " fleet", print);
        }

        Console.showMessage(print);
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        return switch (parameter) {
            case 0 -> MODIFIERS;
            case 1 -> Global.getSector().getAllFactions().stream().map(FactionAPI::getId).toList();
            case 3 -> PATROLS;
            default -> List.of();
        };
    }

    // Gets the faction market with the best ship quality and fleet size multiplier
    private MarketAPI getBestMarket(String factionId) {
        List<MarketAPI> factionMarkets = Misc.getFactionMarkets(factionId);

        MarketAPI bestMarket = null;
        float bestMultiplier = Float.MIN_VALUE;
        for (MarketAPI market : factionMarkets) {
            float multiplier = Misc.getShipQuality(market, factionId) * market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f);
            if (multiplier > bestMultiplier) {
                bestMultiplier = multiplier;
                bestMarket = market;
            }
        }

        return bestMarket;
    }

    // See com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3's createFleet() for vanilla implementation
    private MarketAPI createFakeMarket(String factionId) {
        MarketAPI market = Global.getFactory().createMarket("fake", "fake", 5);
        market.getStability().modifyFlat("fake", 10000);
        market.setFactionId(factionId);
        SectorEntityToken token = Global.getSector().getHyperspace().createToken(0, 0);
        market.setPrimaryEntity(token);

        // 95% with all ship quality bonuses
        // Pristine Nanoforge (+50%), Orbital Works (+20%), 10 stability (+25%)
        market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("AFTM_fake", 0.95f - market.getShipQualityFactor());
        market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("AFTM_qualityDoctrine", market.getFaction().getDoctrine().getShipQualityContribution());

        // 382.8125% fleet size with all fleet size bonuses
        // Hypercognition admin (+20%), size 6 colony (+125%), Cryoarithmetic Engine (+100%), 10 stability (x1.25), Alpha Core on High Command (x1.25)
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat("AFTM_fake", 3.828125f);
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult("AFTM_shipDoctrine", FleetFactoryV3.getDoctrineNumShipsMult(market.getFaction().getDoctrine().getNumShips()));

        return market;
    }

    // See com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase's createPatrol() for vanilla implementation
    private static CampaignFleetAPI createPatrol(MarketAPI market, String factionId, Float qualityOverride, String fleetType, float combat, boolean ignoreMarketFleetSizeMult, boolean withOfficers) {
        float tanker = 0f;
        float freighter = 0f;

        Random random = new Random(); // Keeping FP random since it significantly affects the final result
        if (combat <= 0f) {
            // See com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase's getPatrolCombatFP() for vanilla implementation
            switch (fleetType) {
                case FleetTypes.PATROL_SMALL:
                    combat = Math.round(Math.round(3f + random.nextFloat() * 2f) * 5f); // ~20 FP
                    break;
                case FleetTypes.PATROL_MEDIUM:
                    combat = Math.round(Math.round(6f + random.nextFloat() * 3f) * 5f); // ~37.5 FP
                    tanker = Math.round(random.nextFloat() * 5f); // ~2.5 FP
                    break;
                case FleetTypes.PATROL_LARGE:
                    combat = Math.round(Math.round(10f + random.nextFloat() * 5f) * 5f); // ~62.5 FP
                    tanker = Math.round(random.nextFloat() * 10f); // ~5 FP
                    freighter = Math.round(random.nextFloat() * 10f); // ~5 FP
                    break;
            }
        }

        FleetParamsV3 params = new FleetParamsV3(market, null, factionId, qualityOverride, fleetType, combat, // combatPts
                freighter, // freighterPts
                tanker, // tankerPts
                0f, // transportPts
                0f, // linerPts
                0f, // utilityPts
                0f // qualityMod
        );
        params.random = random;
        params.ignoreMarketFleetSizeMult = ignoreMarketFleetSizeMult;
        params.withOfficers = withOfficers;
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        String rankId = switch (fleetType) {
            case FleetTypes.PATROL_SMALL -> Ranks.SPACE_LIEUTENANT;
            case FleetTypes.PATROL_MEDIUM -> Ranks.SPACE_COMMANDER;
            default -> Ranks.SPACE_CAPTAIN;
        };

        fleet.getCommander().setPostId(Ranks.POST_PATROL_COMMANDER);
        fleet.getCommander().setRankId(rankId);

        return fleet;
    }
}