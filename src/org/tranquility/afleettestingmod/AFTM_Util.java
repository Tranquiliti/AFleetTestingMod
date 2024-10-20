package org.tranquility.afleettestingmod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Utility class for A Fleet Testing Mod
 */
public final class AFTM_Util {
    public static final byte MISSION_FP_STEP = 5;
    public static final byte MISSION_QUALITY_STEP = 5;

    public static List<String> getMissionFactions() {
        try {
            JSONArray csvData = Global.getSettings().getMergedSpreadsheetDataForMod("faction_id", "data/config/afleettestingmodConfig/mission_factions.csv", "afleettestingmod");
            ArrayList<String> factions = new ArrayList<>(csvData.length());
            for (int i = 0; i < csvData.length(); i++) {
                String faction = csvData.getJSONObject(i).getString("faction_id");
                if (Global.getSettings().getFactionSpec(faction) != null) factions.add(faction);
            }
            factions.trimToSize();
            return factions;
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static List<String> getMissionStations() {
        try {
            JSONArray csvData = Global.getSettings().getMergedSpreadsheetDataForMod("station_id", "data/config/afleettestingmodConfig/mission_stations.csv", "afleettestingmod");
            ArrayList<String> stations = new ArrayList<>(csvData.length());
            for (int i = 0; i < csvData.length(); i++) {
                String station = csvData.getJSONObject(i).getString("station_id");
                if (Global.getSettings().getVariant(station) != null) stations.add(station);
            }
            stations.trimToSize();
            return stations;
        } catch (JSONException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void initMissionFleet(MissionDefinitionAPI api, FleetSide side, TesterFleetParams params, List<String> factions, boolean balanceFleets, boolean officers, boolean autofit) {
        String faction = factions.get(params.factionIndex);
        CampaignFleetAPI fleet = params.initFleet(faction, balanceFleets, officers, autofit);

        api.initFleet(side, null, FleetGoal.ATTACK, true);
        if (officers)
            fleet.setCommander(Global.getSettings().createPerson()); // Mainly to prevent player from controlling the flagship
        for (FleetMemberAPI member : fleet.getFleetData().getMembersInPriorityOrder())
            api.addFleetMember(side, member);

        api.setFleetTagline(side, String.format("%s (%d FP [Target: %d]) (%d%% ship quality)", faction, fleet.getFleetPoints(), params.targetFleetPoints, params.fleetQuality));
    }

    public static class TesterFleetParams {
        private Random rand;
        private int factionIndex;
        private int targetFleetPoints;
        private int fleetQuality;
        private int bestDistance;
        private long bestFleetSeed;
        private boolean refreshFleet;

        public TesterFleetParams() {
            rand = new Random();
            targetFleetPoints = 150;
            fleetQuality = 120; // 120% is the minimum required to guarantee no random ship D-Mods in vanilla
            bestDistance = Integer.MAX_VALUE;
            refreshFleet = true;
        }

        public void reset() {
            rand = new Random();
            factionIndex = 0;
            targetFleetPoints = 150;
            fleetQuality = 120;
            refreshFleet = true;
        }

        public void setRefreshFleet() {
            refreshFleet = true;
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

                if (fleet == null) continue;

                int distance = Math.abs(fleet.getFleetPoints() - targetFleetPoints);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestFleetSeed = fleetSeed;
                    bestFleet = fleet;
                }
                if (distance == 0) break;
            }
            refreshFleet = false;
            if (bestFleet != null) { // Inflator is required to enable ship quality and autofit changes
                DefaultFleetInflaterParams p = new DefaultFleetInflaterParams();
                p.seed = bestFleetSeed;
                p.quality = fleetQuality / 100f;
                p.allWeapons = !autofit;
                if (!autofit) p.rProb = 0f; // autofit probability
                p.factionId = factionId;
                new DefaultFleetInflater(p).inflate(bestFleet);
            }
            return bestFleet;
        }
    }
}