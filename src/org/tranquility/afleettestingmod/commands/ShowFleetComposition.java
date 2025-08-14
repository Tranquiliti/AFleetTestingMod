package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;
import org.tranquility.afleettestingmod.AFTMUtil;

import java.util.Arrays;
import java.util.HashMap;

public class ShowFleetComposition implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        HashMap<String, AFTMUtil.FleetCompositionData> factionComps = new HashMap<>();
        for (CampaignFleetAPI fleet : Global.getSector().getPlayerFleet().getContainingLocation().getFleets()) {
            String factionId = fleet.getFaction().getId();
            if (factionComps.containsKey(factionId)) factionComps.get(factionId).addMembers(fleet);
            else factionComps.put(factionId, new AFTMUtil.FleetCompositionData());
        }

        if (factionComps.isEmpty()) {
            Console.showMessage("Error: no fleet found in current location!");
            return CommandResult.ERROR;
        }

        StringBuilder print = new StringBuilder();
        Object[] sortedSet = factionComps.keySet().toArray();
        Arrays.sort(sortedSet);
        for (Object obj : sortedSet) {
            String factionId = (String) obj;
            factionComps.get(factionId).appendComposition(factionId, print);
        }

        Console.showMessage(print);
        return CommandResult.SUCCESS;
    }
}