package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.TreeMap;

public class ShowFleetComposition implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        TreeMap<String, TreeMap<String, Integer>> factions = new TreeMap<String, TreeMap<String, Integer>>();
        for (CampaignFleetAPI fleet : Global.getSector().getPlayerFleet().getContainingLocation().getFleets()) {
            String factionName = fleet.getFaction().getDisplayName();
            if (!factions.containsKey(factionName)) factions.put(factionName, new TreeMap<String, Integer>());

            TreeMap<String, Integer> fleetComp = (TreeMap<String, Integer>) factions.get(factionName);
            for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
                String hullName = member.getHullSpec().getHullName();
                if (!fleetComp.containsKey(hullName)) fleetComp.put(hullName, 1);
                else fleetComp.put(hullName, (Integer) fleetComp.get(hullName) + 1);
            }
        }

        if (factions.isEmpty()) {
            Console.showMessage("Error: no fleet found in current location!");
            return CommandResult.ERROR;
        }

        StringBuilder print = new StringBuilder();
        for (String factionId : factions.keySet()) {
            print.append("--- ").append(factionId).append(" ---\n");
            TreeMap<String, Integer> factionComp = (TreeMap<String, Integer>) factions.get(factionId);
            int totalHulls = 0;
            for (String hullId : factionComp.keySet()) {
                int hullCount = (Integer) factionComp.get(hullId);
                print.append(hullId).append(": ").append(hullCount).append("\n");
                totalHulls += hullCount;
            }
            print.append("Total number of ships: ").append(totalHulls).append("\n");
        }

        Console.showMessage(print);
        return CommandResult.SUCCESS;
    }
}
