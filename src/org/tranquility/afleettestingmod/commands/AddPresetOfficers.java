package org.tranquility.afleettestingmod.commands;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Personalities;
import com.fs.starfarer.api.impl.campaign.ids.Ranks;
import com.fs.starfarer.api.util.Misc;
import org.json.JSONObject;
import org.lazywizard.console.BaseCommand;
import org.lazywizard.console.CommonStrings;
import org.lazywizard.console.Console;

import java.util.Iterator;

public class AddPresetOfficers implements BaseCommand {
    @Override
    @SuppressWarnings("unchecked")
    public CommandResult runCommand(String args, CommandContext context) {
        if (!context.isInCampaign()) {
            Console.showMessage(CommonStrings.ERROR_CAMPAIGN_ONLY);
            return CommandResult.WRONG_CONTEXT;
        }

        try {
            JSONObject presets = Global.getSettings().getMergedJSON("data/config/presetOfficers.json");

            if (args.isEmpty()) {
                StringBuilder print = new StringBuilder();
                for (Iterator<String> iter = presets.keys(); iter.hasNext(); ) {
                    String presetID = iter.next();
                    print.append(presetID).append('\n');
                }
                Console.showMessage(print.toString());
                return CommandResult.SUCCESS;
            }

            int numOfficers = 1;
            String[] tmp = args.split(" ");
            if (tmp.length > 1) try {
                numOfficers = Integer.parseInt(tmp[1]);
            } catch (NumberFormatException ex) {
                Console.showMessage("Error: numOfficers must be a whole number!");
                return CommandResult.ERROR;
            }

            for (int count = 0; count < numOfficers; count++) {
                JSONObject officerSettings = presets.optJSONObject(tmp[0]);
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
                    String skillId = iter.next();
                    officer.getStats().setSkillLevel(skillId, skills.getInt(skillId));
                }
                officer.getStats().setSkipRefresh(false);

                // See com.fs.starfarer.api.impl.campaign.events.OfficerManagerEvent's callEvent() for vanilla implementation
                if (officerSettings.optBoolean("isMercenary")) {
                    Misc.setMercenary(officer, true);
                    Misc.setMercHiredNow(officer);
                }

                // Sleeper presets are found in com.fs.starfarer.api.impl.campaign.rulecmd.salvage.special.CryopodOfficerGen.java
                if (officerSettings.optBoolean("isExceptional"))
                    officer.getMemoryWithoutUpdate().set(MemFlags.EXCEPTIONAL_SLEEPER_POD_OFFICER, true);

                Global.getSector().getPlayerFleet().getFleetData().addOfficer(officer);
            }
            Console.showMessage("Successfully created " + numOfficers + " \"" + tmp[0] + "\" officer" + (numOfficers > 1 ? "s!" : "!"));
        } catch (Exception e) {
            Console.showMessage(e);
            return CommandResult.ERROR;
        }

        return CommandResult.SUCCESS;
    }
}