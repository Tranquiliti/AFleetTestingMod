package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.FleetTypes;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager;
import com.fs.starfarer.api.impl.combat.threat.ThreatFleetBehaviorScript;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager.*;

public class SpawnThreatFleets implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        LocationAPI spawnLoc = Global.getSector().getPlayerFleet().getContainingLocation();
        if (!(spawnLoc instanceof StarSystemAPI)) {
            Console.showMessage("Error: This command can only be used if the player is in a star system.");
            return CommandResult.WRONG_CONTEXT;
        }
        StarSystemAPI system = (StarSystemAPI) Global.getSector().getPlayerFleet().getContainingLocation();

        String[] tmp = args.split(" ");

        float depth = Misc.getAbyssalDepth(system.getLocation(), true);
        if (!args.isEmpty() && tmp.length > 0) try {
            depth = Float.parseFloat(tmp[0]);
        } catch (NumberFormatException ex) {
            Console.showMessage("Error: depths must be a floating-point number!");
            return CommandResult.ERROR;
        }

        int numFleets = 1;
        if (tmp.length > 1) try {
            numFleets = Integer.parseInt(tmp[1]);
        } catch (NumberFormatException ex) {
            Console.showMessage("Error: numFleets must be a whole number!");
            return CommandResult.ERROR;
        }

        for (int i = 0; i < numFleets; i++) {
            CampaignFleetAPI f = spawnThreatFleet(system, depth);
            Global.getSector().getCurrentLocation().spawnFleet(Global.getSector().getPlayerFleet(), 0f, 0f, f);
            f.getMemoryWithoutUpdate().set(MemFlags.FLEET_IGNORES_OTHER_FLEETS, true, 0.2f);
        }

        Console.showMessage(String.format("Spawned %d Threat fleets with abyssal depth of %f!", numFleets, depth));
        return CommandResult.SUCCESS;
    }

    // See com.fs.starfarer.api.impl.combat.threat.DisposableThreatFleetManager for vanilla implementation
    private CampaignFleetAPI spawnThreatFleet(StarSystemAPI system, float depth) {
        int numSecond = 0;
        int numThird = 0;
        for (CampaignFleetAPI fleet : system.getFleets()) {
            if (fleet.getFaction().getId().equals(Factions.THREAT)) {
                String type = Misc.getFleetType(fleet);
                if (type == null) continue;

                switch (type) {
                    case FleetTypes.PATROL_SMALL -> {
                    }
                    case FleetTypes.PATROL_MEDIUM -> numSecond++;
                    case FleetTypes.PATROL_LARGE -> numThird++;
                }
            }
        }

        // this is not entriely accruate because depths don't correspond 100% with first/second/third strike
        // that's fine, though
        int maxSecond = 1;
        if (depth >= DEPTH_2 && (float) Math.random() < 0.5f) maxSecond = 2;

        if (numThird > 0) {
            depth = Math.min(depth, DEPTH_2 - 0.1f);
        }
        if (numSecond > maxSecond) {
            if ((float) Math.random() < 0.5f) {
                depth = Math.min(depth, DEPTH_0 - 0.1f);
            } else {
                depth = Math.min(depth, DEPTH_1 - 0.1f);
            }
        }

        WeightedRandomPicker<DisposableThreatFleetManager.FabricatorEscortStrength> picker = new WeightedRandomPicker<>();
        DisposableThreatFleetManager.FabricatorEscortStrength strength;
        int fabricators = 0;
        if (depth < DEPTH_0) {
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 3f);
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 10f);
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 1f);
            strength = picker.pick();
        } else if (depth < DEPTH_1) {
            fabricators = 1;
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.NONE, 1f);
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 10f);
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 5f);
            strength = picker.pick();
        } else if (depth < DEPTH_2) {
            fabricators = 2;
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 10f);
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 5f);
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 5f);
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MAXIMUM, 5f);
            strength = picker.pick();
            if (strength == DisposableThreatFleetManager.FabricatorEscortStrength.MAXIMUM) {
                fabricators = 1;
            }
        } else {
            fabricators = 2;
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.LOW, 10f);
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM, 5f);
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.HIGH, 5f);
            picker.add(DisposableThreatFleetManager.FabricatorEscortStrength.MAXIMUM, 5f);
            strength = picker.pick();
            if (strength == DisposableThreatFleetManager.FabricatorEscortStrength.LOW || strength == DisposableThreatFleetManager.FabricatorEscortStrength.MEDIUM) {
                fabricators = 3;
            }
        }

        CampaignFleetAPI f = createThreatFleet(fabricators, 0, 0, strength, null);
        system.addEntity(f);
        f.addScript(new ThreatFleetBehaviorScript(f, system));

        return f;
    }
}