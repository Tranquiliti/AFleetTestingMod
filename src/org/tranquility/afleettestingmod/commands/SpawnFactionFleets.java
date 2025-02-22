package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactory;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.tranquility.afleettestingmod.AFTM_Util;

import java.util.List;
import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3.getDoctrineNumShipsMult;
import static org.tranquility.afleettestingmod.AFTM_Util.AVG_RANDOM_FLOAT;
import static org.tranquility.afleettestingmod.AFTM_Util.FleetStatData;

public class SpawnFactionFleets implements BaseCommand {
    @Override
    public BaseCommand.CommandResult runCommand(String args, BaseCommand.CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return BaseCommand.CommandResult.WRONG_CONTEXT;
        }

        String[] tmp = args.split(" ");
        if (args.isEmpty() || tmp.length == 0) {
            return BaseCommand.CommandResult.BAD_SYNTAX;
        }

        boolean clear = false;
        boolean forceFake = false;
        int offset = 0;
        if (tmp[0].charAt(0) == '-') {
            String command = tmp[0].toLowerCase();
            offset++;

            for (int i = 1; i < command.length(); i++) {
                switch (command.charAt(i)) {
                    case 'c':
                        clear = true;
                        break;
                    case 'f':
                        forceFake = true;
                        break;
                }
            }
        }

        // Command parameter but no faction
        if (tmp.length == offset) return BaseCommand.CommandResult.BAD_SYNTAX;

        String factionId = tmp[offset];

        int numFleets = 1;
        if (tmp.length > 1 + offset) try {
            numFleets = Integer.parseInt(tmp[1 + offset]);
        } catch (NumberFormatException ex) {
            Console.showMessage("Error: numOfFleets must be a number!");
            return BaseCommand.CommandResult.BAD_SYNTAX;
        }

        FleetFactory.PatrolType patrolType = FleetFactory.PatrolType.HEAVY;
        if (tmp.length > 2 + offset) {
            String patrolString = tmp[2 + offset];
            switch (patrolString) {
                case "patrolMedium":
                    patrolType = FleetFactory.PatrolType.COMBAT;
                    break;
                case "patrolSmall":
                    patrolType = FleetFactory.PatrolType.FAST;
                    break;
            }
        }

        MarketAPI bestMarket = null;
        if (!forceFake) bestMarket = getBestMarket(factionId);
        if (bestMarket == null) bestMarket = createFakeMarket(factionId);

        AFTM_Util.FleetStatData data = new FleetStatData();
        for (int i = 0; i < numFleets; i++) {
            CampaignFleetAPI fleet = createPatrol(patrolType, factionId, bestMarket);
            fleet.inflateIfNeeded(); // Inflate to apply d-mods
            fleet.forceSync();

            data.addStat(fleet);
            if (clear) {
                fleet.despawn();
            } else {
                Global.getSector().getCurrentLocation().spawnFleet(Global.getSector().getPlayerFleet(), 0, 0, fleet);
                fleet.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true, 0.3f);
            }
        }

        final float bestMarketQuality = Misc.getShipQuality(bestMarket, factionId) * 100;
        final float bestMarketFleetSize = bestMarket.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f) * 100;
        StringBuilder print = new StringBuilder(clear ? "Showing " : "Spawned ").append(numFleets).append(" ").append(factionId).append(" ").append(patrolType.getFleetType());
        print.append(" fleets, using stats from ").append(bestMarket.getName()).append(" with ship quality ").append(bestMarketQuality).append("% and fleet size ").append(bestMarketFleetSize).append("%:\n");

        data.aggregateStats();
        data.appendStats("Average fleet stats", print);

        Console.showMessage(print);
        return BaseCommand.CommandResult.SUCCESS;
    }

    // Gets the faction market with the best ship quality and fleet size
    private MarketAPI getBestMarket(String factionId) {
        List<MarketAPI> factionMarkets = Misc.getFactionMarkets(factionId);

        MarketAPI bestMarket = null;
        float bestQualityFleetSizeMult = Float.MIN_VALUE;
        for (MarketAPI market : factionMarkets) {
            float qualitySizeMult = Misc.getShipQuality(market, factionId) * market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).computeEffective(0f);
            if (qualitySizeMult > bestQualityFleetSizeMult) {
                bestQualityFleetSizeMult = qualitySizeMult;
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

        // 25% to make up for no Heavy Industry, 95% with all ship quality bonuses
        market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("fake", 0.25f + 0.95f);
        market.getStats().getDynamic().getMod(Stats.FLEET_QUALITY_MOD).modifyFlat("qualityDoctrine", market.getFaction().getDoctrine().getShipQualityContribution());

        // 382.8125% fleet size with all fleet size bonuses
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyFlat("fake", 3.828125f);
        market.getStats().getDynamic().getMod(Stats.COMBAT_FLEET_SIZE_MULT).modifyMult("shipDoctrine", getDoctrineNumShipsMult(market.getFaction().getDoctrine().getNumShips()));

        return market;
    }

    // See com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase's createPatrol() for vanilla implementation
    private static CampaignFleetAPI createPatrol(FleetFactory.PatrolType type, String factionId, MarketAPI market) {
        float combat = 0;
        float tanker = 0f;
        float freighter = 0f;
        String fleetType = type.getFleetType();

        // See com.fs.starfarer.api.impl.campaign.econ.impl.MilitaryBase's getPatrolCombatFP() for vanilla implementation
        switch (type) {
            case FAST:
                combat = Math.round(Math.round(3f + AVG_RANDOM_FLOAT * 2f) * 5f); // 20 FP
                break;
            case COMBAT:
                combat = Math.round(Math.round(6f + AVG_RANDOM_FLOAT * 3f) * 5f); // 40 FP
                tanker = Math.round(AVG_RANDOM_FLOAT * 5f); // 3 FP
                break;
            case HEAVY:
                combat = Math.round(Math.round(10f + AVG_RANDOM_FLOAT * 5f) * 5f); // 65 FP
                tanker = Math.round(AVG_RANDOM_FLOAT * 10f); // 5 FP
                freighter = Math.round(AVG_RANDOM_FLOAT * 10f); // 5 FP
                break;
        }

        FleetParamsV3 params = new FleetParamsV3(market, null, factionId, null, fleetType, combat, // combatPts
                freighter, // freighterPts
                tanker, // tankerPts
                0f, // transportPts
                0f, // linerPts
                0f, // utilityPts
                0f // qualityMod
        );
        params.random = new Random();
        CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);

        String postId = Ranks.POST_PATROL_COMMANDER;
        String rankId = Ranks.SPACE_COMMANDER;
        switch (type) {
            case FAST:
                rankId = Ranks.SPACE_LIEUTENANT;
                break;
            case COMBAT:
                rankId = Ranks.SPACE_COMMANDER;
                break;
            case HEAVY:
                rankId = Ranks.SPACE_CAPTAIN;
                break;
        }

        fleet.getCommander().setPostId(postId);
        fleet.getCommander().setRankId(rankId);

        return fleet;
    }
}