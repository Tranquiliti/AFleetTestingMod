package org.tranquility.afleettestingmod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflater;
import com.fs.starfarer.api.impl.campaign.fleets.DefaultFleetInflaterParams;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.fleets.FleetParamsV3;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.util.*;

/**
 * Utility class for A Fleet Testing Mod
 */
public final class AFTM_Util {
    public static final byte MISSION_FP_STEP = 5;
    public static final byte MISSION_QUALITY_STEP = 5;
    private static final int DEFAULT_FP = 160;
    private static final int DEFAULT_QUALITY_PERCENT = 120;  // 120% is the minimum required to guarantee no random ship D-Mods in vanilla

    private static final float AVG_RANDOM_FLOAT = 0.5f;

    // See com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl's computeDataForFleet() for vanilla implementation
    public static float computeDataForFleet(CampaignFleetAPI fleet) {
        BattleAutoresolverPluginImpl.FleetAutoresolveData fleetData = new BattleAutoresolverPluginImpl.FleetAutoresolveData();
        fleetData.fleet = fleet;

        fleetData.fightingStrength = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            BattleAutoresolverPluginImpl.FleetMemberAutoresolveData data = computeDataForMember(member);
            if (data.combatReady) fleetData.fightingStrength += data.strength;
        }

        return fleetData.fightingStrength;
    }

    public static BattleAutoresolverPluginImpl.FleetMemberAutoresolveData computeDataForMember(FleetMemberAPI member) {
        BattleAutoresolverPluginImpl.FleetMemberAutoresolveData data = new BattleAutoresolverPluginImpl.FleetMemberAutoresolveData();

        data.member = member;
        ShipHullSpecAPI hullSpec = data.member.getHullSpec();
        if ((member.isCivilian()) || !member.canBeDeployedForCombat()) {
            data.strength = 0.25f;
            if (hullSpec.getShieldType() != ShieldAPI.ShieldType.NONE) {
                data.shieldRatio = 0.5f;
            }
            data.combatReady = false;
            return data;
        }

        data.combatReady = true;

        MutableShipStatsAPI stats = data.member.getStats();

        float normalizedHullStr = stats.getHullBonus().computeEffective(hullSpec.getHitpoints()) + stats.getArmorBonus().computeEffective(hullSpec.getArmorRating()) * 10f;

        float normalizedShieldStr = stats.getFluxCapacity().getModifiedValue() + stats.getFluxDissipation().getModifiedValue() * 10f;


        if (hullSpec.getShieldType() == ShieldAPI.ShieldType.NONE) {
            normalizedShieldStr = 0;
        } else {
            float shieldFluxPerDamage = hullSpec.getBaseShieldFluxPerDamageAbsorbed();
            shieldFluxPerDamage *= stats.getShieldAbsorptionMult().getModifiedValue() * stats.getShieldDamageTakenMult().getModifiedValue();

            if (shieldFluxPerDamage < 0.1f) shieldFluxPerDamage = 0.1f;
            float shieldMult = 1f / shieldFluxPerDamage;
            normalizedShieldStr *= shieldMult;
        }

        if (normalizedHullStr < 1) normalizedHullStr = 1;
        if (normalizedShieldStr < 1) normalizedShieldStr = 1;

        data.shieldRatio = normalizedShieldStr / (normalizedShieldStr + normalizedHullStr);
        if (member.isStation()) {
            data.shieldRatio = 0.5f;
        }

        float strength = Misc.getMemberStrength(member, true, true, true);

        strength *= 0.85f + 0.3f * AVG_RANDOM_FLOAT; // No randomness

        data.strength = Math.max(strength, 0.25f);

        return data;
    }

    // See com.fs.starfarer.api.impl.campaign.FleetEncounterContext's gainXP() for vanilla implementation
    public static float getBaseXP(CampaignFleetAPI fleet) {
        float fpTotal = 0;
        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            float fp = member.getFleetPointCost();
            fp *= 1f + member.getCaptain().getStats().getLevel() / 5f;
            fpTotal += fp;
        }

        float xp = fpTotal * 250;
        xp *= 2f;

        xp *= Global.getSettings().getFloat("xpGainMult");

        return xp;
    }

    public static BaseEveryFrameCombatPlugin createSpeedUpPlugin() {
        return new BaseEveryFrameCombatPlugin() {
            @Override
            public void advance(float amount, List<InputEventAPI> events) {
                if (!Global.getCombatEngine().isPaused())
                    Global.getCombatEngine().getTimeMult().modifyMult("afleettestingmod", Math.max(1f, 1f / (Global.getCombatEngine().getElapsedInLastFrame() * 30f)));
            }
        };
    }

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
        for (FleetMemberAPI member : fleet.getFleetData().getMembersInPriorityOrder())
            api.addFleetMember(side, member);

        api.setFleetTagline(side, String.format("%s (%d FP [Target: %d]) (%d%% ship quality)", faction, fleet.getFleetPoints(), params.targetFleetPoints, params.fleetQuality));
    }

    // Aggregates fleet composition data
    public static class FleetCompositionData {
        private final HashMap<String, Integer> fleetComposition = new HashMap<>();
        private int numHulls;

        public void addMembers(CampaignFleetAPI fleet) {
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                String hullId = member.getHullSpec().getDParentHullId(); // To avoid marking (D) hulls as separate
                if (hullId == null) hullId = member.getHullId();
                if (fleetComposition.containsKey(hullId))
                    fleetComposition.put(hullId, fleetComposition.get(hullId) + 1);
                else fleetComposition.put(hullId, 1);
                numHulls++;
            }
        }

        public void appendComposition(String name, StringBuilder print) {
            print.append("----- ").append(name).append(" -----\n");
            Object[] sortedSet = fleetComposition.keySet().toArray();
            Arrays.sort(sortedSet); // Sort by hull ID
            for (Object memberId : sortedSet) {
                int hullCount = fleetComposition.get((String) memberId);
                print.append(memberId).append(": ").append(hullCount).append(" (").append(hullCount / (float) numHulls * 100f).append("%)\n");
            }
            print.append("Total number of ships: ").append(numHulls).append("\n");
        }
    }

    // Aggregates stat data from fleets
    public static class FleetStatData {
        // All floats since they can be divided to get the average
        private float baseDP = 0;
        private float realDP = 0;
        private float avgMaxCR = 0;
        private float numOfficers = 0;
        private float avgNumDMods = 0;
        private float fleetFP = 0;
        private float numShips = 0;
        private float numFrigates = 0;
        private float numDestroyers = 0;
        private float numCruisers = 0;
        private float numCapitals = 0;
        private float numFlightDecks = 0;
        private float baseXP = 0;
        private float effectiveStrength = 0;
        private float autoResolveStrength = 0;

        private final HashMap<Integer, Float> officers = new HashMap<>();
        private final HashMap<String, Float> wings = new HashMap<>();

        private float numMembers = 0;
        private float numFleets = 0;

        public void addStat(CampaignFleetAPI fleet) {
            if (fleet == null) return;
            numFleets++;

            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                numMembers++;
                baseDP += member.getUnmodifiedDeploymentPointsCost();
                realDP += member.getDeploymentPointsCost();
                avgMaxCR += member.getRepairTracker().getMaxCR();
                if (!member.getCaptain().isDefault()) {
                    numOfficers++;
                    int level = member.getCaptain().getStats().getLevel();
                    if (officers.containsKey(level)) officers.put(level, officers.get(level) + 1f);
                    else officers.put(level, 1f);
                }
                avgNumDMods += DModManager.getNumDMods(member.getVariant());
                numFlightDecks += member.getNumFlightDecks();
                for (String id : member.getVariant().getWings()) {
                    if (wings.containsKey(id)) wings.put(id, wings.get(id) + 1f);
                    else wings.put(id, 1f);
                }
            }

            fleetFP += fleet.getFleetPoints();
            numShips += fleet.getNumShips();
            numFrigates += fleet.getNumFrigates();
            numDestroyers += fleet.getNumDestroyers();
            numCruisers += fleet.getNumCruisers();
            numCapitals += fleet.getNumCapitals();
            baseXP += getBaseXP(fleet);
            effectiveStrength += fleet.getEffectiveStrength();
            autoResolveStrength += computeDataForFleet(fleet);
        }

        // Averages out the stats using numFleets and numMembers
        // Does not average out the counts in the officers and wings HashMaps
        public void aggregateStats() {
            if (numFleets == 0 || numMembers == 0) return;

            baseDP /= numFleets;
            realDP /= numFleets;
            avgMaxCR /= numMembers;
            numOfficers /= numFleets;
            avgNumDMods /= numMembers;
            numFlightDecks /= numFleets;
            fleetFP /= numFleets;
            numShips /= numFleets;
            numFrigates /= numFleets;
            numDestroyers /= numFleets;
            numCruisers /= numFleets;
            numCapitals /= numFleets;
            baseXP /= numFleets;
            effectiveStrength /= numFleets;
            autoResolveStrength /= numFleets;
        }

        public void appendStats(String name, StringBuilder print) {
            print.append("----- ").append(name).append(" -----");
            print.append("\nTotal base DP: ").append(baseDP);
            print.append("\nTotal effective DP: ").append(realDP);
            print.append("\nAverage ship max CR: ").append(avgMaxCR * 100).append("%");
            print.append("\nTotal officers: ").append(numOfficers);
            appendOfficers(print);
            print.append("\nAverage ship d-mod count: ").append(avgNumDMods);
            print.append("\nTotal flight decks: ").append(numFlightDecks);
            appendWings(print);
            print.append("\nTotal ship FP: ").append(fleetFP);
            print.append("\nTotal number of ships: ").append(numShips);
            print.append("\nTotal frigates/destroyers/cruisers/capitals: ").append(numFrigates).append(" / ").append(numDestroyers).append(" / ").append(numCruisers).append(" / ").append(numCapitals);
            print.append("\nTotal base XP: ").append(baseXP);
            print.append("\nEffective strength: ").append(effectiveStrength);
            print.append("\nAuto-resolve strength: ").append(autoResolveStrength).append("\n");
        }

        private void appendOfficers(StringBuilder print) {
            if (officers.isEmpty()) return;
            Object[] sortedSet = officers.keySet().toArray();
            Arrays.sort(sortedSet);
            print.append("\n  {\"");
            for (Object obj : sortedSet) {
                Integer level = (Integer) obj;
                print.append(level).append("\": ").append(officers.get(level) / numFleets).append(", \"");
            }
            print.delete(print.length() - 3, print.length()).append("}");
        }

        private void appendWings(StringBuilder print) {
            if (wings.isEmpty()) return;
            Object[] sortedSet = wings.keySet().toArray();
            Arrays.sort(sortedSet);
            print.append("\n  {\"");
            for (Object obj : sortedSet) {
                String id = (String) obj;
                print.append(id).append("\": ").append(wings.get(id) / numFleets).append(", \"");
            }
            print.delete(print.length() - 3, print.length()).append("}");
        }
    }

    // For missions
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
                p.allWeapons = !autofit; // FleetParamsV3 has it default to 'null'
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
}