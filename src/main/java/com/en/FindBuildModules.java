package com.en;

import com.en.table.BuildModules;
import org.jspecify.annotations.Nullable;
import org.openrewrite.*;
import org.openrewrite.groovy.GroovyIsoVisitor;
import org.openrewrite.groovy.tree.G;
import org.openrewrite.java.tree.J;

import java.util.regex.Pattern;

public class FindBuildModules extends Recipe {
    transient BuildModules buildModules = new BuildModules(this);

    @Override
    public @NlsRewrite.DisplayName String getDisplayName() {
        return "Find which branches run for which modules";
    }

    @Override
    public @NlsRewrite.Description String getDescription() {
        return "Itemizing the modules that exist in Jenkinsfiles and the branches that trigger them.";
    }

    @Override
    public TreeVisitor<?, ExecutionContext> getVisitor() {
        return Preconditions.check(new FindSourceFiles("**/Jenkinsfile"), new GroovyIsoVisitor<ExecutionContext>() {
            @Override
            public G.MapLiteral visitMapLiteral(G.MapLiteral mapLiteral, ExecutionContext ctx) {
                String moduleType = getAttribute(mapLiteral, String.class, "moduleType");
                String branchPattern = getAttribute(mapLiteral, String.class, "branchPattern");
                Boolean isProductionDeployment = getAttribute(mapLiteral, Boolean.class, "isProductionDeployment");

                if (moduleType != null && branchPattern != null) {
                    Pattern compiledBranchPattern = Pattern.compile(branchPattern);
                    buildModules.insertRow(ctx, new BuildModules.Row(
                            moduleType,
                            compiledBranchPattern.matcher("feature").matches(),
                            compiledBranchPattern.matcher("release").matches(),
                            compiledBranchPattern.matcher("hotfix").matches(),
                            Boolean.TRUE.equals(isProductionDeployment)
                    ));
                }

                return super.visitMapLiteral(mapLiteral, ctx);
            }

            private @Nullable <V> V getAttribute(G.MapLiteral mapLiteral, Class<V> type, String attribute) {
                for (G.MapEntry element : mapLiteral.getElements()) {
                    if (element.getKey().printTrimmed(getCursor()).equals(attribute)) {
                        if (element.getValue() instanceof J.Literal) {
                            Object value = ((J.Literal) element.getValue()).getValue();
                            if (value != null && type.isAssignableFrom(value.getClass())) {
                                //noinspection unchecked
                                return (V) value;
                            }
                        }
                    }
                }
                return null;
            }
        });
    }
}
