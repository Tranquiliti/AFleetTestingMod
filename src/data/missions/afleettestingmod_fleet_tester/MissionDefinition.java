package data.missions.afleettestingmod_fleet_tester;

import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.mission.FleetSide;
import com.fs.starfarer.api.mission.MissionDefinitionAPI;
import com.fs.starfarer.api.mission.MissionDefinitionPlugin;
import org.lwjgl.input.Keyboard;
import org.tranquility.afleettestingmod.AFTM_Util;

import java.util.List;

/**
 * Code adapted from Dark.Revenant's <a href="https://fractalsoftworks.com/forum/index.php?topic=8007.0">Interstellar Imperium</a> Station Tester mission
 */
@SuppressWarnings("unused")
public class MissionDefinition implements MissionDefinitionPlugin {
    private static List<String> FACTIONS;
    private static AFTM_Util.TesterFleetParams playerParams;
    private static AFTM_Util.TesterFleetParams enemyParams;
    private static boolean balanceFleets, speedUp, officers, autofit;
    private static byte objectiveType;

    private void init() {
        if (playerParams != null) playerParams.reset();
        else playerParams = new AFTM_Util.TesterFleetParams();
        if (enemyParams != null) enemyParams.reset();
        else enemyParams = new AFTM_Util.TesterFleetParams();

        balanceFleets = true;
        speedUp = true;
        officers = false;
        autofit = false;
        objectiveType = 0;
    }

    @Override
    public void defineMission(MissionDefinitionAPI api) {
        if (FACTIONS == null) { // Initializing mission
            FACTIONS = AFTM_Util.getMissionFactions();
            init();
        }

        boolean shiftEnabled = (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT));

        if (shiftEnabled && (Keyboard.isKeyDown(Keyboard.KEY_LCONTROL) || Keyboard.isKeyDown(Keyboard.KEY_RCONTROL))) {
            init();
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_SPACE)) {
            playerParams.setRefreshFleet();
            enemyParams.setRefreshFleet();
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_Q)) {
            if (shiftEnabled) playerParams.incrementIndex(-1, FACTIONS);
            else enemyParams.incrementIndex(-1, FACTIONS);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_E)) {
            if (shiftEnabled) playerParams.incrementIndex(1, FACTIONS);
            else enemyParams.incrementIndex(1, FACTIONS);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_A)) {
            if (shiftEnabled) playerParams.incrementFP(-AFTM_Util.MISSION_FP_STEP);
            else enemyParams.incrementFP(-AFTM_Util.MISSION_FP_STEP);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_D)) {
            if (shiftEnabled) playerParams.incrementFP(AFTM_Util.MISSION_FP_STEP);
            else enemyParams.incrementFP(AFTM_Util.MISSION_FP_STEP);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_Z)) {
            if (shiftEnabled) playerParams.incrementQuality(-AFTM_Util.MISSION_QUALITY_STEP);
            else enemyParams.incrementQuality(-AFTM_Util.MISSION_QUALITY_STEP);
        }
        if (Keyboard.isKeyDown(Keyboard.KEY_C)) {
            if (shiftEnabled) playerParams.incrementQuality(AFTM_Util.MISSION_QUALITY_STEP);
            else enemyParams.incrementQuality(AFTM_Util.MISSION_QUALITY_STEP);
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
        if (Keyboard.isKeyDown(Keyboard.KEY_M)) {
            objectiveType++;
            if (objectiveType == 5) objectiveType = 0;
        }

        AFTM_Util.initMissionFleet(api, FleetSide.PLAYER, playerParams, FACTIONS, balanceFleets, officers, autofit);

        AFTM_Util.initMissionFleet(api, FleetSide.ENEMY, enemyParams, FACTIONS, balanceFleets, officers, autofit);

        // Values taken from BattleCreationPluginImpl.java
        float width = objectiveType == 0 ? 18000f : 24000f;
        float height = 18000f;
        api.initMap(-width * 0.5f, width * 0.5f, -height * 0.5f, height * 0.5f);

        String optionBrief = "";
        if (balanceFleets) optionBrief += "Fleet Balancer, ";
        if (speedUp) {
            api.addPlugin(AFTM_Util.createSpeedUpPlugin());
            optionBrief += "1-100x Speed-Up, ";
        }
        if (officers) optionBrief += "Officers, ";
        if (autofit) optionBrief += "Autofit, ";
        switch (objectiveType) {
            case 1:
                api.addObjective(-5000f, -3000f, Tags.COMM_RELAY);
                api.addObjective(5000f, -3000f, Tags.SENSOR_ARRAY);
                api.addObjective(-5000f, 3000f, Tags.SENSOR_ARRAY);
                api.addObjective(5000f, 3000f, Tags.COMM_RELAY);
                optionBrief += "Objectives (Square), ";
                break;
            case 2:
                api.addObjective(0f, -3000f, Tags.COMM_RELAY);
                api.addObjective(-5000f, 0f, Tags.SENSOR_ARRAY);
                api.addObjective(5000f, 0f, Tags.SENSOR_ARRAY);
                api.addObjective(0f, 3000f, Tags.COMM_RELAY);
                optionBrief += "Objectives (Diamond), ";
                break;
            case 3:
                api.addObjective(-5000f, -3000f, Tags.COMM_RELAY);
                api.addObjective(2000f, -1200f, Tags.SENSOR_ARRAY);
                api.addObjective(-2000f, 1200f, Tags.SENSOR_ARRAY);
                api.addObjective(5000f, 3000f, Tags.COMM_RELAY);
                optionBrief += "Objectives (Slash), ";
                break;
            case 4:
                api.addObjective(5000f, -3000f, Tags.COMM_RELAY);
                api.addObjective(-2000f, -1200f, Tags.SENSOR_ARRAY);
                api.addObjective(2000f, 1200f, Tags.SENSOR_ARRAY);
                api.addObjective(-5000f, 3000f, Tags.COMM_RELAY);
                optionBrief += "Objectives (Backslash), ";
                break;
        }
        if (!optionBrief.isEmpty())
            api.addBriefingItem("Enabled: " + optionBrief.substring(0, optionBrief.length() - 2));

        api.getContext().aiRetreatAllowed = false;
        api.getContext().enemyDeployAll = true;
        api.getContext().fightToTheLast = true;
        api.getContext().setStandoffRange(objectiveType == 0 ? 6000f : height - 4500f);
    }
}