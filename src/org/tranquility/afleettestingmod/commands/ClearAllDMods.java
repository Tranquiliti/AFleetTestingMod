package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.skills.FieldRepairsScript;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class ClearAllDMods implements BaseCommandWithSuggestion {
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
            Console.showMessage("Error: hull id \"%s\" does not exist!".formatted(args));
            return CommandResult.ERROR;
        }

        List<HullModSpecAPI> dMods = DModManager.getModsWithTags(Tags.HULLMOD_DMOD);
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())
            if (!onlyOneShipType || member.getHullId().equals(args)) {
                for (HullModSpecAPI dMod : dMods) DModManager.removeDMod(member.getVariant(), dMod.getId());
                FieldRepairsScript.restoreToNonDHull(member.getVariant());
            }

        if (onlyOneShipType)
            Console.showMessage("Restored to pristine condition all ships with hull id \"%s\".".formatted(args));
        else Console.showMessage("Restored all ships to pristine condition!");
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