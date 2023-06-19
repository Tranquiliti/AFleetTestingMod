package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.Collection;

public class AddSMods implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty()) return CommandResult.BAD_SYNTAX;

        String[] tmp = args.split(" ");

        if (Global.getSettings().getHullSpec(tmp[0]) == null) {
            Console.showMessage(String.format("Error: hull id \"%s\" does not exist!", tmp[0]));
            return CommandResult.ERROR;
        }

        if (tmp.length < 2) {
            Console.showMessage("Error: No hullmod id specified!");
            return CommandResult.BAD_SYNTAX;
        }

        // First verify that all specified hullmod ids are correct
        for (int i = 1; i < tmp.length; i++) {
            if (Global.getSettings().getHullModSpec(tmp[i]) == null) {
                Console.showMessage(String.format("Error: hullmod id \"%s\" does not exist!", tmp[i]));
                return CommandResult.ERROR;
            }
        }

        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member.getHullSpec().getHullId().equals(tmp[0])) {
                Collection<String> hullMods = member.getVariant().getHullMods();
                Collection<String> permaMods = member.getVariant().getPermaMods();
                Collection<String> sMods = member.getVariant().getSMods();
                for (int i = 1; i < tmp.length; i++) {
                    hullMods.add(tmp[i]);
                    permaMods.add(tmp[i]);
                    sMods.add(tmp[i]);
                }
            }
        }

        Console.showMessage(String.format("Applied S-Mods to all ships with hull id \"%s\"!", tmp[0]));
        return CommandResult.SUCCESS;
    }
}
