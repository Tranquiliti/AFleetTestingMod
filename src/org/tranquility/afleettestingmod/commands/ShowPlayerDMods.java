package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
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

        // Too much effort (and not much gain) to change TreeMap to HashMap while keeping output sorted
        StringBuilder print = new StringBuilder();
        TreeMap<String, TreeMap<String, Integer>> hullsDMods = new TreeMap<>();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            String hullId = member.getHullSpec().getHullId();
            if (!hullsDMods.containsKey(hullId)) hullsDMods.put(hullId, new TreeMap<String, Integer>());

            print.append(member.getShipName()).append(" (").append(hullId).append("): ");
            for (String permaMod : member.getVariant().getPermaMods()) {
                HullModSpecAPI modSpec = Global.getSettings().getHullModSpec(permaMod);
                if (modSpec.hasTag(Tags.HULLMOD_DMOD)) {
                    String modDisplay = modSpec.getDisplayName();
                    TreeMap<String, Integer> modCount = hullsDMods.get(hullId);
                    if (modCount.containsKey(modDisplay)) modCount.put(modDisplay, modCount.get(modDisplay) + 1);
                    else modCount.put(modDisplay, 1);

                    print.append(modDisplay).append(", ");
                }
            }
            print.delete(print.length() - 2, print.length()).append("\n");
        }

        for (String hull : hullsDMods.keySet()) {
            print.append("----- D-mod distribution for ").append(hull).append(" -----\n");
            TreeMap<String, Integer> modCount = hullsDMods.get(hull);
            for (String display : modCount.keySet())
                print.append(display).append(": ").append(modCount.get(display)).append("\n");
        }

        Console.showMessage(print);
        return CommandResult.SUCCESS;
    }
}