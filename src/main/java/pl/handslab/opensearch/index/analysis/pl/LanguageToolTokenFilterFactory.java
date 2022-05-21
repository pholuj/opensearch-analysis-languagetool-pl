package pl.handslab.opensearch.index.analysis.pl;

import org.apache.lucene.analysis.TokenStream;
import org.opensearch.common.settings.Settings;
import org.opensearch.env.Environment;
import org.opensearch.index.IndexSettings;
import org.opensearch.index.analysis.AbstractTokenFilterFactory;

import java.io.IOException;

import org.languagetool.JLanguageTool;
import org.languagetool.language.Polish;

public class LanguageToolTokenFilterFactory extends AbstractTokenFilterFactory {
    private final JLanguageTool languageTool;

    public LanguageToolTokenFilterFactory(IndexSettings indexSettings, Environment env, String name, Settings settings)
            throws IOException {
        super(indexSettings, name, settings);
        languageTool = new JLanguageTool(new Polish());
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new LanguageToolTokenFilter(tokenStream, languageTool);
    }
}
