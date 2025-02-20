package com.en.table;

import lombok.Value;
import org.openrewrite.Column;
import org.openrewrite.DataTable;
import org.openrewrite.Recipe;

public class BuildModules extends DataTable<BuildModules.Row> {
    public BuildModules(Recipe recipe) {
        super(recipe,
                "Deployment phases",
                "The branches that are involved in each SDLC phase.");
    }

    @Value
    public static class Row {
        @Column(displayName = "Module type",
                description = "The name of the SDLC stage.")
        String moduleType;

        @Column(displayName = "Runs for `feature` branch",
                description = "Whether a `feature` branch runs this module.")
        boolean runsForFeature;

        @Column(displayName = "Runs for `release` branch",
                description = "Whether a `release` branch runs this module.")
        boolean runsForRelease;

        @Column(displayName = "Runs for `hotfix` branch",
                description = "Whether a `hotfix` branch runs this module.")
        boolean runsForHotfix;

        @Column(displayName = "Production deployment",
                description = "Is listed as a production deployment.")
        boolean productionDeployment;
    }
}
