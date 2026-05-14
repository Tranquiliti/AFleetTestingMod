package org.tranquility.afleettestingmod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;

import java.util.List;
import java.util.Random;

// For missions
public class TesterFleetParams {
    private static final int DEFAULT_FP = 160;
    private static final int DEFAULT_QUALITY_PERCENT = 120;  // 120% is the minimum required to guarantee no random ship d-mods in vanilla

    private Random rand;
    private int factionIndex;
    private int targetFleetPoints;
    private int fleetQuality;
    private int bestDistance;
    private long bestFleetSeed;
    private boolean refreshFleet;

    public TesterFleetParams() {
        rand = new Random();
        targetFleetPoints = DEFAULT_FP;
        fleetQuality = DEFAULT_QUALITY_PERCENT;
        bestDistance = Integer.MAX_VALUE;
        refreshFleet = true;
    }

    public void reset() {
        rand = new Random();
        factionIndex = 0;
        targetFleetPoints = DEFAULT_FP;
        fleetQuality = DEFAULT_QUALITY_PERCENT;
        refreshFleet = true;
    }

    public void setRefreshFleet() {
        refreshFleet = true;
    }

    public int getFactionIndex() {
        return factionIndex;
    }

    public int getTargetFleetPoints() {
        return targetFleetPoints;
    }

    public int getFleetQuality() {
        return fleetQuality;
    }

    public void incrementIndex(int i, List<String> factionList) {
        factionIndex += i;
        if (factionIndex < 0) factionIndex = factionList.size() - 1;
        else if (factionIndex >= factionList.size()) factionIndex = 0;
        refreshFleet = true;
    }

    public void incrementFP(int i) {
        targetFleetPoints = Math.max(10, targetFleetPoints + i);
        refreshFleet = true;
    }

    public void incrementQuality(int i) {
        fleetQuality = Math.max(-50, fleetQuality + i); // -50% is the minimum possible in vanilla
    }

    public CampaignFleetAPI initFleet(String factionId, boolean balanceFleets, boolean withOfficers, boolean autofit) {
        CampaignFleetAPI bestFleet = null;
        if (refreshFleet) bestDistance = Integer.MAX_VALUE;
        for (int repetitions = balanceFleets ? 1000 : 1; repetitions > 0; repetitions--) {
            FleetParamsV3 params = new FleetParamsV3(null, factionId, fleetQuality / 100f, FleetTypes.PATROL_LARGE, targetFleetPoints, 0f, 0f, 0f, 0f, 0f, 0f);
            params.withOfficers = withOfficers;
            params.ignoreMarketFleetSizeMult = true;
            params.modeOverride = FactionAPI.ShipPickMode.PRIORITY_THEN_ALL;
            params.forceAllowPhaseShipsEtc = true;

            long fleetSeed = refreshFleet ? rand.nextLong() : bestFleetSeed;
            params.random = new Random(fleetSeed);

            CampaignFleetAPI fleet = FleetFactoryV3.createFleet(params);
            if (!refreshFleet) bestFleet = fleet;

            int distance = Math.abs(fleet.getFleetPoints() - targetFleetPoints);
            if (distance < bestDistance) {
                bestDistance = distance;
                bestFleetSeed = fleetSeed;
                bestFleet = fleet;
            }
            if (distance == 0) break;
        }

        if (bestFleet != null) {
            // Inflator is required to enable ship quality and autofit changes
            DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
            p.seed = bestFleetSeed;
            p.quality = fleetQuality / 100f;
            if (!autofit) p.rProb = 0f; // Set autofit randomize probability to 0
            p.factionId = factionId;
            new DefaultFleetInflater(p).inflate(bestFleet);

            // Note for factions affected by an implemented GenerateFleetOfficersPlugin:
            // The plugin only takes effect if a campaign save was loaded at any point during a game session
            // So, these factions don't get AI cores or custom officers if a campaign save hasn't been loaded yet
            if (withOfficers) {
                PersonAPI dummy = Global.getSettings().createPerson();
                dummy.setStats(bestFleet.getCommanderStats()); // Use real commander's stats to keep fleetwide skills active
                bestFleet.setCommander(dummy); // Mainly to prevent player from controlling the flagship
            }
        }
        refreshFleet = false;

        return bestFleet;
    }
}