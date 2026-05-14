package org.tranquility.afleettestingmod;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseEveryFrameCombatPlugin;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShieldAPI;
import com.fs.starfarer.api.combat.ShipHullSpecAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.BattleAutoresolverPluginImpl;
import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONArray;
import org.json.JSONException;
import org.lwjgl.util.vector.Vector2f;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for A Fleet Testing Mod
 */
public final class AFTMUtil {
    public static final byte MISSION_FP_STEP = 5;
    public static final byte MISSION_QUALITY_STEP = 5;

    private static final float MAX_SPEED_UP_MULT = 100f;
    // Recommended to avoid AI bugs like this:
    // https://fractalsoftworks.com/forum/index.php?topic=32491.msg474622#msg474622
    private static final float CAP_TO_FPS = 60f;

    private static final float AVG_RANDOM_FLOAT = 0.5f;

    /**
     * Gets a list of all fleets in the same location as the player fleet, sorted by distance to the player fleet.
     *
     * @return A sorted List of fleets in the player fleet's location
     */
    public static List<CampaignFleetAPI> getNearbyFleets() {
        List<CampaignFleetAPI> fleetList = Misc.getNearbyFleets(Global.getSector().getPlayerFleet(), Float.MAX_VALUE);
        fleetList.sort((fleet1, fleet2) -> {
            if (fleet1 == fleet2) return 0;
            Vector2f pLoc = Global.getSector().getPlayerFleet().getLocation();
            return Float.compare(Misc.getDistance(pLoc, fleet1.getLocation()), Misc.getDistance(pLoc, fleet2.getLocation()));
        });

        return fleetList;
    }

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
                if (Global.getCombatEngine().isPaused()) return;

                // Provided by Dark.Revenant
                int roundedFrameTimeMsec = (int) Math.ceil(1000f * Global.getCombatEngine().getElapsedInLastFrame() + 1);
                float scaledFPS = 1000f / roundedFrameTimeMsec;
                float unscaledFPS = Global.getCombatEngine().getTimeMult().getModifiedValue() * scaledFPS;
                float cappedTimeMult = Math.max(1f, unscaledFPS / CAP_TO_FPS);
                float newTimeMult = Math.min(cappedTimeMult, MAX_SPEED_UP_MULT);
                Global.getCombatEngine().getTimeMult().modifyMult("afleettestingmod_speedUp", newTimeMult);
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
        String faction = factions.get(params.getFactionIndex());
        CampaignFleetAPI fleet = params.initFleet(faction, balanceFleets, officers, autofit);

        api.initFleet(side, null, FleetGoal.ATTACK, true);
        for (FleetMemberAPI member : fleet.getFleetData().getMembersInPriorityOrder())
            api.addFleetMember(side, member);

        api.setFleetTagline(side, "%s (%d FP [Target: %d]) (%d%% ship quality)".formatted(faction, fleet.getFleetPoints(), params.getTargetFleetPoints(), params.getFleetQuality()));
    }
}