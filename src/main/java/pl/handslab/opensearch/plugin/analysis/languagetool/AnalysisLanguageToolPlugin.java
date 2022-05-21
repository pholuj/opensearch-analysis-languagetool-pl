package pl.handslab.opensearch.plugin.analysis.languagetool;

import org.opensearch.index.analysis.TokenFilterFactory;
import org.opensearch.indices.analysis.AnalysisModule.AnalysisProvider;
import org.opensearch.plugins.AnalysisPlugin;
import org.opensearch.plugins.Plugin;

import pl.handslab.opensearch.index.analysis.pl.LanguageToolSynonymsTokenFilterFactory;
import pl.handslab.opensearch.index.analysis.pl.LanguageToolTokenFilterFactory;

import java.util.Map;
import java.util.HashMap;

public class AnalysisLanguageToolPlugin extends Plugin implements AnalysisPlugin {
    public static final String FILTER_NAME = "languagetool_pl_stem";
    public static final String FILTER_SYNONYMS_NAME = "languagetool_synonyms_pl_stem";

    @Override
    public Map<String, AnalysisProvider<TokenFilterFactory>> getTokenFilters() {
        HashMap<String, AnalysisProvider<TokenFilterFactory>> out = new HashMap<String, AnalysisProvider<TokenFilterFactory>>();
        out.put(FILTER_NAME, LanguageToolTokenFilterFactory::new);
        out.put(FILTER_SYNONYMS_NAME, LanguageToolSynonymsTokenFilterFactory::new);
        return out;
    }
}
