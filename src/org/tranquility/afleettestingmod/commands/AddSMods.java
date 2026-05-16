package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.lazywizard.console.BaseCommandWithSuggestion;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class AddSMods implements BaseCommandWithSuggestion {
    private static final List<String> HULLMOD_IDS = Global.getSettings().getAllHullModSpecs().stream().map(HullModSpecAPI::getId).toList();

    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty()) return CommandResult.BAD_SYNTAX;
        String[] tmp = args.split(" ");

        if (tmp.length < 2) {
            Console.showMessage("Error: No hullmod id specified!");
            return CommandResult.BAD_SYNTAX;
        }

        try {
            Global.getSettings().getHullSpec(tmp[0]);
        } catch (RuntimeException e) {
            Console.showMessage("Error: hull id \"%s\" does not exist!".formatted(tmp[0]));
            return CommandResult.ERROR;
        }

        // First verify that all specified hullmod ids are correct
        for (int i = 1; i < tmp.length; i++)
            if (Global.getSettings().getHullModSpec(tmp[i]) == null) {
                Console.showMessage("Error: hullmod id \"%s\" does not exist!".formatted(tmp[i]));
                return CommandResult.ERROR;
            }

        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())
            if (member.getHullId().equals(tmp[0])) for (int i = 1; i < tmp.length; i++)
                member.getVariant().addPermaMod(tmp[i], true);

        Console.showMessage("Applied s-mods to all ships with hull id \"%s\".".formatted(tmp[0]));
        return CommandResult.SUCCESS;
    }

    @Override
    public List<String> getSuggestions(int parameter, List<String> previous, CommandContext context) {
        if (parameter == 0) {
            if (!context.isInCampaign()) return List.of();

            Set<String> playerShips = new LinkedHashSet<>();
            for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy())
                playerShips.add(member.getHullId());

            return playerShips.stream().toList();
        } else return HULLMOD_IDS;
    }
}