package data.console.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.Iterator;

public class AddPresetOfficer implements BaseCommand {
    @Override
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        if (args.isEmpty()) return CommandResult.BAD_SYNTAX;

        try {
            JSONObject officerSettings = Global.getSettings().getMergedJSON("data/config/presetOfficers.json").optJSONObject(args);
            if (officerSettings == null) {
                Console.showMessage("Error: preset officer ID not found!");
                return CommandResult.ERROR;
            }

            String faction = officerSettings.optString("faction", Factions.PLAYER);
            int level = officerSettings.optInt("level", 1);
            String personality = officerSettings.optString("personality", Personalities.STEADY);

            PersonAPI officer = Global.getSector().getFaction(faction).createRandomPerson();
            officer.getStats().setSkipRefresh(true);
            officer.getStats().setLevel(level);
            officer.setPersonality(personality);
            officer.setRankId(Ranks.SPACE_LIEUTENANT);
            officer.setPostId(Ranks.POST_OFFICER);

            JSONObject skills = officerSettings.optJSONObject("skills");
            if (skills != null) for (Iterator<String> iter = skills.keys(); iter.hasNext(); ) {
                String skillId = (String) iter.next();
                officer.getStats().setSkillLevel(skillId, skills.getInt(skillId));
            }
            officer.getStats().setSkipRefresh(false);

            if (officerSettings.optBoolean("isSleeper"))
                officer.getMemoryWithoutUpdate().set(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER, true);

            Global.getSector().getPlayerFleet().getFleetData().addOfficer(officer);
        } catch (Exception e) {
            Console.showMessage(e);
            return CommandResult.ERROR;
        }

        Console.showMessage("Officer " + args + " created successfully!");
        return CommandResult.SUCCESS;
    }
}
