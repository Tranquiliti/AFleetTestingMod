package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.TreeMap;

public class ShowPlayerDMods implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        StringBuilder print = new StringBuilder();
        TreeMap<String, TreeMap<String, Integer>> shipModCount = new TreeMap<String, TreeMap<String, Integer>>();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            String hullName = member.getHullSpec().getHullName();
            if (!shipModCount.containsKey(hullName)) shipModCount.put(hullName, new TreeMap<String, Integer>());

            print.append(member.getShipName()).append(" (").append(hullName).append("): ");
            for (String permaMod : member.getVariant().getPermaMods()) {
                HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(permaMod);
                if (modSpec.hasTag(Tags.HULLMOD_DMOD)) {
                    String display = modSpec.getDisplayName();
                    TreeMap<String, Integer> modCount = (TreeMap<String, Integer>) shipModCount.get(hullName);
                    if (!modCount.containsKey(display)) modCount.put(display, 1);
                    else modCount.put(display, (Integer) modCount.get(display) + 1);

                    print.append(display).append(", ");
                }
            }
            print.delete(print.length() - 2, print.length()).append("\n");
        }

        for (String hull : shipModCount.keySet()) {
            print.append("--- D-Mod distribution for ").append(hull).append(" ---\n");
            TreeMap<String, Integer> modCount = (TreeMap<String, Integer>) shipModCount.get(hull);
            for (String display : modCount.keySet())
                print.append(display).append(": ").append(modCount.get(display)).append("\n");
        }

        Console.showMessage(print);
        return CommandResult.SUCCESS;
    }
}
