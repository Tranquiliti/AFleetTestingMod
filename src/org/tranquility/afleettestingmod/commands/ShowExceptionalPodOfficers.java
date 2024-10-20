package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.characters.MutableCharacterStatsAPI.SkillLevelAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.SleeperPodsSpecial.SleeperPodsSpecialData;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import static com.fs.starfarer.api.impl.campaign.procgen.themes.SalvageSpecialAssigner.MAX_EXCEPTIONAL_PODS_OFFICERS;

public class ShowExceptionalPodOfficers implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        StringBuilder print = new StringBuilder();
        for (StarSystemAPI system : Global.getSector().getStarSystems())
            findExceptionalPodOfficers(system, print);

        // Officer pods can be found in hyperspace, so also check that
        findExceptionalPodOfficers(Global.getSector().getHyperspace(), print);

        if (print.length() == 0) print.append("No exceptional pod officers found!\n");

        int numAlreadyCreated = Global.getSector().getMemoryWithoutUpdate().getInt("$SleeperPodsSpecialCreator_exceptionalCount");
        if (numAlreadyCreated < MAX_EXCEPTIONAL_PODS_OFFICERS)
            print.append(MAX_EXCEPTIONAL_PODS_OFFICERS - numAlreadyCreated).append(" more may spawn from newly-generated salvage - check hyperspace shipwrecks frequently!");
        else print.append("No more exceptional pod officers can spawn in this sector!");

        Console.showMessage(print.toString());
        return CommandResult.SUCCESS;
    }

    private void findExceptionalPodOfficers(LocationAPI loc, StringBuilder print) {
        for (SectorEntityToken entity : loc.getAllEntities()) {
            Object specialData = entity.getMemoryWithoutUpdate().get(MemFlags.SALVAGE_SPECIAL_DATA);
            if (specialData instanceof SleeperPodsSpecialData) {
                PersonAPI officer = ((SleeperPodsSpecialData) specialData).officer;
                if (officer != null && officer.getMemoryWithoutUpdate().getBoolean(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER)) {
                    print.append(String.format("Exceptional pod officer %s found within %s in %s:\n", officer.getName().getFullName(), entity.getFullName(), loc.getName()));
                    for (SkillLevelAPI skill : officer.getStats().getSkillsCopy())
                        if (skill.getSkill().isCombatOfficerSkill())
                            print.append('\t').append(skill.getSkill().getName()).append(skill.getLevel() > 1f ? " (Elite)\n" : "\n");
                }
            }
        }
    }
}