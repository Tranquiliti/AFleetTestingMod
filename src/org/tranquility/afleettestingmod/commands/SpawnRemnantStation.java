package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.AICoreOfficerPlugin;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.fleet.FleetMemberType;
import com.fs.starfarer.api.impl.campaign.fleets.FleetFactoryV3;
import com.fs.starfarer.api.impl.campaign.ids.*;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantOfficerGeneratorPlugin;
import com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantStationFleetManager;
import com.fs.starfarer.api.util.Misc;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.lwjgl.util.vector.Vector2f;

import java.util.Random;

import static com.fs.starfarer.api.impl.campaign.procgen.themes.RemnantThemeGenerator.addRemnantStationInteractionConfig;

public class SpawnRemnantStation implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        String[] tmp = args.split(" ");
        int pts = 128; // 16 * 8
        if (!args.isEmpty() && tmp.length > 0) try {
            pts = Integer.parseInt(tmp[0]);
        } catch (NumberFormatException ex) {
            Console.showMessage("Error: pts must be a whole number!");
            return CommandResult.ERROR;
        }

        int numFleets = 10;
        if (tmp.length > 1) try {
            numFleets = Integer.parseInt(tmp[1]);
        } catch (NumberFormatException ex) {
            Console.showMessage("Error: numFleets must be a whole number!");
            return CommandResult.ERROR;
        }

        // Setting rep to Inhospitable so player doesn't immediately get attacked
        Global.getSector().getPlayerFaction().setRelationship(Factions.REMNANTS, RepLevel.INHOSPITABLE);
        pts = pts / 8; // RemnantStationFleetManager will multiply the final combat pts by 8 to get the real combat FP value
        spawnStation(pts, numFleets);

        Console.showMessage(new StringBuilder("Fully-operational Nexus online, spawning up to ").append(numFleets).append(" fleets with initial total ship FP of ").append(pts * 8));
        return CommandResult.SUCCESS;
    }

    private void spawnStation(int pts, int maxFleets) {
        StarSystemAPI system = Global.getSector().getPlayerFleet().getStarSystem();

        CampaignFleetAPI fleet = FleetFactoryV3.createEmptyFleet(Factions.REMNANTS, "battlestation", null);
        FleetMemberAPI member = Global.getFactory().createFleetMember(FleetMemberType.SHIP, "remnant_station2_Standard");
        fleet.getFleetData().addFleetMember(member);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_AGGRESSIVE, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_NO_JUMP, true);
        fleet.getMemoryWithoutUpdate().set(MemFlags.MEMORY_KEY_MAKE_ALLOW_DISENGAGE, true);
        fleet.addTag(Tags.NEUTRINO_HIGH);
        fleet.setStationMode(true);
        addRemnantStationInteractionConfig(fleet);
        system.addEntity(fleet);
        fleet.clearAbilities();
        fleet.addAbility(Abilities.TRANSPONDER);
        fleet.getAbility(Abilities.TRANSPONDER).activate();
        fleet.getDetectedRangeMod().modifyFlat("gen", 1000.0F);
        fleet.setAI(null);

        Vector2f v = Global.getSector().getPlayerFleet().getLocation();
        fleet.setLocation(v.x, v.y);

        String coreId = Commodities.ALPHA_CORE;
        Random random = new Random();
        AICoreOfficerPlugin plugin = Misc.getAICoreOfficerPlugin(coreId);
        PersonAPI commander = plugin.createPerson(coreId, fleet.getFaction().getId(), random);
        fleet.setCommander(commander);
        fleet.getFlagship().setCaptain(commander);
        RemnantOfficerGeneratorPlugin.integrateAndAdaptCoreForAIFleet(fleet.getFlagship());
        RemnantOfficerGeneratorPlugin.addCommanderSkills(commander, fleet, null, 3, random);

        member.getRepairTracker().setCR(member.getRepairTracker().getMaxCR());

        RemnantStationFleetManager activeFleets = new RemnantStationFleetManager(fleet, 1.0F, maxFleets, maxFleets, 15.0F, pts, pts);
        system.addScript(activeFleets);
    }
}