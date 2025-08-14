package data.missions.afleettestingmod_station_tester;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetGoal;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.AICoreOfficerPluginImpl;
import com.fs.starfarer.api.impl.campaign.ids.Commodities;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Skills;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import org.lwjgl.input.Keyboard;
import org.tranquility.afleettestingmod.AFTMUtil;

import java.util.List;

/**
 * Code adapted from Dark.Revenant's <a href="https://fractalsoftworks.com/forum/index.php?topic=8007.0">Interstellar Imperium</a> Station Tester mission
 */
@SuppressWarnings("unused")
public class MissionDefinition implements MissionDefinitionPlugin {
    private static List<String> STATIONS;
    private static int stationIndex;
    private static byte stationCoreType;
    private static List<String> FACTIONS;
    private static AFTMUtil.TesterFleetParams enemyParams;
    private static boolean balanceFleets, speedUp, officers, autofit;

    private void init() {
        if (enemyParams != null) enemyParams.reset();
        else enemyParams = new AFTMUtil.TesterFleetParams();

        stationIndex = 0;
        stationCoreType = 0;
        balanceFleets = true;
        speedUp = true;
        officers = false;
        autofit = false;
    }

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        if (STATIONS == null) { // Initializing mission
            STATIONS = AFTMUtil.getMissionStations();
            FACTIONS = AFTMUtil.getMissionFactions();
            init();
        }

        boolean shiftEnabled = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));

        if (shiftEnabled && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
            init();
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            enemyParams.setRefreshFleet();
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
            if (shiftEnabled) {
                stationIndex--;
                if (stationIndex < 0) stationIndex = STATIONS.size() - 1;
            } else enemyParams.incrementIndex(-1, FACTIONS);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
            if (shiftEnabled) {
                stationIndex++;
                if (stationIndex >= STATIONS.size()) stationIndex = 0;
            } else enemyParams.incrementIndex(1, FACTIONS);
        }
        if (shiftEnabled && Keyboard.isKeyDown(Keyboard.KEY_W)) {
            stationCoreType++;
            if (stationCoreType > 2) stationCoreType = 0;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            enemyParams.incrementFP(-AFTMUtil.MISSION_FP_STEP);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            enemyParams.incrementFP(AFTMUtil.MISSION_FP_STEP);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_Z)) {
            enemyParams.incrementQuality(-AFTMUtil.MISSION_QUALITY_STEP);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_C)) {
            enemyParams.incrementQuality(AFTMUtil.MISSION_QUALITY_STEP);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_B)) {
            balanceFleets = !balanceFleets;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_T)) {
            speedUp = !speedUp;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_O)) {
            officers = !officers;
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_F)) {
            autofit = !autofit;
        }

        // Generate player station
        FleetMemberAPI playerStation = Global.getFactory().createFleetMember(FleetMemberType.SHIP, STATIONS.get(stationIndex));
        if (stationCoreType > 0) {
            PersonAPI commander = new AICoreOfficerPluginImpl().createPerson(stationCoreType == 2 ? Commodities.OMEGA_CORE : Commodities.ALPHA_CORE, Factions.NEUTRAL, null);
            if (stationCoreType == 2) {
                commander.getStats().setLevel(14);
                commander.getStats().setSkillLevel(Skills.BALLISTIC_MASTERY, 2);
                commander.getStats().setSkillLevel(Skills.SYSTEMS_EXPERTISE, 2);
                commander.getStats().setSkillLevel(Skills.MISSILE_SPECIALIZATION, 2);
                commander.getStats().setSkillLevel(Skills.ORDNANCE_EXPERTISE, 2);
                commander.getStats().setSkillLevel(Skills.POLARIZED_ARMOR, 2);
                commander.getStats().setSkillLevel(Skills.OMEGA_ECM, 0);
            }
            playerStation.setCaptain(commander);
        }
        playerStation.setShipName("ISS Last Stand");
        playerStation.getRepairTracker().setCR(1f); // Stations always have 100% CR

        api.initFleet(FleetSide.PLAYER, null, FleetGoal.ATTACK, true);
        api.addFleetMember(FleetSide.PLAYER, playerStation);
        api.setFleetTagline(FleetSide.PLAYER, STATIONS.get(stationIndex));

        AFTMUtil.initMissionFleet(api, FleetSide.ENEMY, enemyParams, FACTIONS, balanceFleets, officers, autofit);

        // Values taken from BattleCreationPluginImpl.java
        float width = 18000f, height = 18000f; // Default size of battle map with no objectives
        api.initMap(-width * 0.5f, width * 0.5f, -height * 0.5f, height * 0.5f);

        // Battle of Chicomoztoc
        api.addPlanet(0f, -512f, 300f, "barren-desert", 0f, true);

        String optionBrief = "";
        if (balanceFleets) optionBrief += "Fleet Balancer, ";
        if (speedUp) {
            api.addPlugin(AFTMUtil.createSpeedUpPlugin());
            optionBrief += "1-100x Speed-Up, ";
        }
        if (officers) optionBrief += "Officers, ";
        if (autofit) optionBrief += "Autofit, ";
        if (stationCoreType == 1) optionBrief += "Alpha Core Station, ";
        if (stationCoreType == 2) optionBrief += "Level 14 Station, ";
        if (!optionBrief.isEmpty())
            api.addBriefingItem("Enabled: " + optionBrief.substring(0, optionBrief.length() - 2));

        api.getContext().aiRetreatAllowed = false;
        api.getContext().enemyDeployAll = true;
        api.getContext().fightToTheLast = true;
        api.getContext().setStandoffRange(6000f);
    }
}