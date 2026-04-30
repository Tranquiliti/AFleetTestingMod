package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClearAllSMods implements BaseCommandWithSuggestion {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        boolean onlyOneShipType = false;
        if (!args.isEmpty()) try {
            Global.getSettings().getHullSpec(args);
            onlyOneShipType = true;
        } catch (RuntimeException e) {
            Console.showMessage(new StringBuilder().append("Error: hull id \"").append(args).append("\" does not exist!"));
            return CommandResult.ERROR;
        }

        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())
            if (!onlyOneShipType || member.getHullId().equals(args))
                for (String sMod : member.getVariant().getSMods().stream().toList())
                    member.getVariant().removePermaMod(sMod);

        if (onlyOneShipType)
            Console.showMessage(new StringBuilder().append("Applied s-mods to all ships with hull id \"").append(args).append("\""));
        else Console.showMessage("Cleared all s-mods from all ships!");
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        if (!context.isInCampaign() || parameter != 0) return List.of();

        Set<String> playerShips = new LinkedHashSet<>();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())
            playerShips.add(member.getHullId());

        return playerShips.stream().toList();
    }
}