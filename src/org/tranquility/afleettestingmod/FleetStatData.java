package org.tranquility.afleettestingmod;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Aggregates stat data from fleets
 */
public class FleetStatData {
    private static final DecimalFormat FORMAT;

    static {
        FORMAT = new DecimalFormat("0.#######", DecimalFormatSymbols.getInstance(Locale.getDefault()));
        FORMAT.setMaximumFractionDigits(6);
    }

    // All floats since they can be divided to get the average
    private float numShips = 0;
    private float numFrigates = 0;
    private float numDestroyers = 0;
    private float numCruisers = 0;
    private float numCapitals = 0;
    private float numFlightDecks = 0;
    private float numOfficers = 0;
    private float avgNumDMods = 0;
    private float avgMaxCR = 0;
    private float baseDP = 0;
    private float realDP = 0;
    private float fleetFP = 0;
    private float baseXP = 0;
    private float effectiveStrength = 0;
    private float autoResolveStrength = 0;

    private final Map<String, Float> hulls = new HashMap<>();
    private final Map<Integer, Float> officers = new HashMap<>();
    private final Map<String, Float> wings = new HashMap<>();

    private float numFleets = 0;
    private float numMembers = 0;

    public void addStat(CampaignFleetAPI fleet) {
        if (fleet == null) return;
        numFleets++;

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            numMembers++;
            baseDP += member.getUnmodifiedDeploymentPointsCost();
            realDP += member.getDeploymentPointsCost();
            avgMaxCR += member.getRepairTracker().getMaxCR();
            String hullId = member.getHullSpec().getDParentHullId(); // To avoid marking (D) hulls as separate
            if (hullId == null) hullId = member.getHullId();
            hulls.merge(hullId, 1f, Float::sum);
            if (!member.getCaptain().isDefault()) {
                numOfficers++;
                officers.merge(member.getCaptain().getStats().getLevel(), 1f, Float::sum);
            }
            avgNumDMods += DModManager.getNumDMods(member.getVariant());
            numFlightDecks += member.getNumFlightDecks();
            for (String id : member.getVariant().getWings())
                wings.merge(id, 1f, Float::sum);
        }

        numShips += fleet.getNumShips();
        numFrigates += fleet.getNumFrigates();
        numDestroyers += fleet.getNumDestroyers();
        numCruisers += fleet.getNumCruisers();
        numCapitals += fleet.getNumCapitals();
        fleetFP += fleet.getFleetPoints();
        baseXP += AFTMUtil.getBaseXP(fleet);
        effectiveStrength += fleet.getEffectiveStrength();
        autoResolveStrength += AFTMUtil.computeDataForFleet(fleet);
    }

    /**
     * Averages out the stats using numFleets and numMembers. Does not average out the counts for the hull, officer, and wing Maps.
     */
    public void aggregateStats() {
        if (numFleets == 0 || numMembers == 0) return;

        baseDP /= numFleets;
        realDP /= numFleets;
        avgMaxCR /= numMembers;
        numOfficers /= numFleets;
        avgNumDMods /= numMembers;
        numFlightDecks /= numFleets;
        fleetFP /= numFleets;
        numShips /= numFleets;
        numFrigates /= numFleets;
        numDestroyers /= numFleets;
        numCruisers /= numFleets;
        numCapitals /= numFleets;
        baseXP /= numFleets;
        effectiveStrength /= numFleets;
        autoResolveStrength /= numFleets;
    }

    public void appendStats(String name, StringBuilder print) {
        String totalAvgStr = numFleets == 1 ? "\nTotal" : "\nAverage";
        print.append("------------------------- ").append(name).append(" -------------------------");
        print.append("\nTotal fleet count: ").append(FORMAT.format(numFleets));
        print.append(totalAvgStr).append(" ship count: ").append(FORMAT.format(numShips));
        print.append(totalAvgStr).append(" frigate/destroyer/cruiser/capital count: ").append(FORMAT.format(numFrigates)).append(" / ").append(FORMAT.format(numDestroyers)).append(" / ").append(FORMAT.format(numCruisers)).append(" / ").append(FORMAT.format(numCapitals));
        appendHulls(print);
        print.append(totalAvgStr).append(" flight deck count: ").append(FORMAT.format(numFlightDecks));
        appendWings(print);
        print.append(totalAvgStr).append(" officer count: ").append(FORMAT.format(numOfficers));
        appendOfficers(print);
        print.append("\nAverage ship d-mod count: ").append(FORMAT.format(avgNumDMods));
        print.append("\nAverage ship max CR: ").append(FORMAT.format(avgMaxCR * 100)).append("%");
        print.append(totalAvgStr).append(" base DP: ").append(FORMAT.format(baseDP));
        print.append(totalAvgStr).append(" effective DP: ").append(FORMAT.format(realDP));
        print.append(totalAvgStr).append(" ship FP: ").append(FORMAT.format(fleetFP));
        print.append(totalAvgStr).append(" base XP: ").append(FORMAT.format(baseXP));
        print.append(totalAvgStr).append(" effective strength: ").append(FORMAT.format(effectiveStrength));
        print.append(totalAvgStr).append(" auto-resolve strength: ").append(FORMAT.format(autoResolveStrength)).append("\n");
    }

    private void appendHulls(StringBuilder print) {
        if (hulls.isEmpty()) return;

        String[] hullIds = hulls.keySet().toArray(new String[0]);
        Arrays.sort(hullIds);
        print.append("\n  {\"");
        for (String id : hullIds)
            print.append(id).append("\": ").append(FORMAT.format(hulls.get(id) / numFleets)).append(", \"");
        print.delete(print.length() - 3, print.length()).append("}");
    }

    private void appendOfficers(StringBuilder print) {
        if (officers.isEmpty()) return;

        Integer[] officerLevels = officers.keySet().toArray(new Integer[0]);
        Arrays.sort(officerLevels);
        print.append("\n  {\"");
        for (int level : officerLevels)
            print.append(level).append("\": ").append(FORMAT.format(officers.get(level) / numFleets)).append(", \"");
        print.delete(print.length() - 3, print.length()).append("}");
    }

    private void appendWings(StringBuilder print) {
        if (wings.isEmpty()) return;

        String[] wingIds = wings.keySet().toArray(new String[0]);
        Arrays.sort(wingIds);
        print.append("\n  {\"");
        for (String id : wingIds)
            print.append(id).append("\": ").append(FORMAT.format(wings.get(id) / numFleets)).append(", \"");
        print.delete(print.length() - 3, print.length()).append("}");
    }
}