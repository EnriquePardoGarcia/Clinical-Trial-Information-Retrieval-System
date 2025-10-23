package es.udc.fi.irudc.c2425.ClinicalTrials;

import java.util.ArrayList;
import java.util.List;

public class ClinicalTrial {
    private String nctId;
    private String orgStudyId;
    private String secondaryId;
    private String briefTitle;
    private String officialTitle;
    private String leadSponsor;
    private String briefSummary;
    private String detailedDescription; // NUEVO
    private String overallStatus;
    private String startDate;
    private String completionDate;
    private String studyType;
    private String phase;
    private String primaryOutcome;
    private List<String> conditions;
    private List<String> interventions;
    private String criteria;
    private String minimumAge;
    private String maximumAge;
    private int minAgeInt; // NUEVO
    private int maxAgeInt; // NUEVO
    private String gender; // NUEVO
    private String location;

    public ClinicalTrial() {
        this.nctId = "N/A";
        this.orgStudyId = "N/A";
        this.secondaryId = "N/A";
        this.briefTitle = "N/A";
        this.officialTitle = "N/A";
        this.leadSponsor = "N/A";
        this.briefSummary = "N/A";
        this.detailedDescription = "N/A"; // NUEVO
        this.overallStatus = "N/A";
        this.startDate = "N/A";
        this.completionDate = "N/A";
        this.studyType = "N/A";
        this.phase = "N/A";
        this.primaryOutcome = "N/A";
        this.conditions = new ArrayList<>();
        this.interventions = new ArrayList<>();
        this.criteria = "N/A";
        this.minimumAge = "N/A";
        this.maximumAge = "N/A";
        this.minAgeInt = -1; // NUEVO
        this.maxAgeInt = -1; // NUEVO
        this.gender = "N/A"; // NUEVO
        this.location = "N/A";
    }

    public String getNctId() { return nctId; }
    public void setNctId(String nctId) { this.nctId = nctId; }

    public String getOrgStudyId() { return orgStudyId; }
    public void setOrgStudyId(String orgStudyId) { this.orgStudyId = orgStudyId; }

    public String getSecondaryId() { return secondaryId; }
    public void setSecondaryId(String secondaryId) { this.secondaryId = secondaryId; }

    public String getBriefTitle() { return briefTitle; }
    public void setBriefTitle(String briefTitle) { this.briefTitle = briefTitle; }

    public String getOfficialTitle() { return officialTitle; }
    public void setOfficialTitle(String officialTitle) { this.officialTitle = officialTitle; }

    public String getLeadSponsor() { return leadSponsor; }
    public void setLeadSponsor(String leadSponsor) { this.leadSponsor = leadSponsor; }

    public String getBriefSummary() { return briefSummary; }
    public void setBriefSummary(String briefSummary) { this.briefSummary = briefSummary; }

    public String getDetailedDescription() { return detailedDescription; }
    public void setDetailedDescription(String detailedDescription) { this.detailedDescription = detailedDescription; }

    public String getOverallStatus() { return overallStatus; }
    public void setOverallStatus(String overallStatus) { this.overallStatus = overallStatus; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getCompletionDate() { return completionDate; }
    public void setCompletionDate(String completionDate) { this.completionDate = completionDate; }

    public String getStudyType() { return studyType; }
    public void setStudyType(String studyType) { this.studyType = studyType; }

    public String getPhase() { return phase; }
    public void setPhase(String phase) { this.phase = phase; }

    public String getPrimaryOutcome() { return primaryOutcome; }
    public void setPrimaryOutcome(String primaryOutcome) { this.primaryOutcome = primaryOutcome; }

    public List<String> getConditions() { return conditions; }
    public void setConditions(List<String> conditions) { this.conditions = conditions; }

    public void addCondition(String condition) { this.conditions.add(condition); }

    public List<String> getInterventions() { return interventions; }
    public void setInterventions(List<String> interventions) { this.interventions = interventions; }

    public void addIntervention(String intervention) { this.interventions.add(intervention); }

    public String getcriteria() { return criteria; }
    public void setcriteria(String criteria) { this.criteria = criteria; }

    public String getMinimumAge() { return minimumAge; }
    public void setMinimumAge(String minimumAge) { this.minimumAge = minimumAge; }

    public String getMaximumAge() { return maximumAge; }
    public void setMaximumAge(String maximumAge) { this.maximumAge = maximumAge; }

    public int getMinAgeInt() { return minAgeInt; }
    public void setMinAgeInt(int minAgeInt) { this.minAgeInt = minAgeInt; }

    public int getMaxAgeInt() { return maxAgeInt; }
    public void setMaxAgeInt(int maxAgeInt) { this.maxAgeInt = maxAgeInt; }

    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public void printValues() {
        System.out.println("======================================");
        System.out.println("        DATOS DEL ENSAYO CLÍNICO      ");
        System.out.println("======================================");

        System.out.println("NCT ID:           " + nctId);
        System.out.println("Org Study ID:     " + orgStudyId);
        System.out.println("Secondary ID:     " + (secondaryId.equals("N/A") ? "N/A" : secondaryId));
        System.out.println("\nBrief Title:      " + briefTitle);
        System.out.println("Official Title:   " + officialTitle);
        System.out.println("\nLead Sponsor:     " + leadSponsor);
        System.out.println("\nBrief Summary:    " + briefSummary);
        System.out.println("\nStatus:           " + overallStatus);
        System.out.println("Start Date:       " + startDate);
        System.out.println("Completion Date:  " + completionDate);
        System.out.println("\nStudy Type:       " + studyType);
        System.out.println("Phase:            " + (phase.equals("N/A") ? "N/A" : phase));
        System.out.println("Primary Outcome:  " + primaryOutcome);
        System.out.println("\nConditions:       " + (conditions.isEmpty() ? "N/A" : String.join(", ", conditions)));
        System.out.println("Interventions:    " + (interventions.isEmpty() ? "N/A" : String.join(", ", interventions)));
        System.out.println("\nCriteria: " + criteria);
        System.out.println("Minimum Age:      " + minimumAge);
        System.out.println("Maximum Age:      " + maximumAge);
        System.out.println("Gender:           " + gender);
        System.out.println("Location:         " + location);
        System.out.println("======================================\n");
    }
}
