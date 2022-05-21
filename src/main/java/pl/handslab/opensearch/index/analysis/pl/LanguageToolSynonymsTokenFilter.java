/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
/**
 * A filter that allows to use synonyms filter on extracted lemmsa.
 * Positions are incremented with each lemma to let filter working,.
 * Keep in mind that position will not be related with "word" in input.
 * 
 * Based on LangagetoolTokenFilter by Tao Lin from Langagetool project.
 * 
 * @author PPH
 */
package pl.handslab.opensearch.index.analysis.pl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Stack;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;

public final class LanguageToolSynonymsTokenFilter extends TokenFilter {
	static final String LEMMA_TYPE = "lemma";

	private final JLanguageTool languageTool;
	private final Stack<String> posStack;
	private final CharTermAttribute termAtt;
	private final OffsetAttribute offsetAtt;
	private final TypeAttribute typeAtt;
	private final StringBuilder collectedInput = new StringBuilder();

	private AttributeSource.State current;
	private Iterator<AnalyzedTokenReadings> tokenIter;

	LanguageToolSynonymsTokenFilter(TokenStream input, JLanguageTool languageTool) {
		super(input);
		this.languageTool = languageTool;
		posStack = new Stack<>();
		termAtt = addAttribute(CharTermAttribute.class);
		offsetAtt = addAttribute(OffsetAttribute.class);
		typeAtt = addAttribute(TypeAttribute.class);
	}
	
	@Override
	public boolean incrementToken() throws IOException {
		if (posStack.size() > 0) {
			return this.parseItemFromStack();
		}

		if (tokenIter == null || !tokenIter.hasNext()) {
			if (input.incrementToken()) {
				String sentenceStr = termAtt.toString();
				collectedInput.append(sentenceStr);
				if (sentenceStr.length() >= 255) {
					// Long sentences get split, so keep collecting data to avoid errors
					// later. See https://github.com/languagetool-org/languagetool/issues/364
					return true;
				} else {
					sentenceStr = collectedInput.toString();
					collectedInput.setLength(0);
				}
				AnalyzedSentence sentence = languageTool.getAnalyzedSentence(sentenceStr);
				List<AnalyzedTokenReadings> tokenBuffer = Arrays.asList(sentence.getTokens());

				tokenIter = tokenBuffer.iterator();
				if (!tokenIter.hasNext()) {
					return false;
				}
			} else {
				return false;
			}
		}

		clearAttributes();
		AnalyzedTokenReadings tr = tokenIter.next();

		if (tr.isSentenceStart() || tr.isWhitespace() || tr.isNonWord()) {
			return this.incrementToken();
		}

		offsetAtt.setOffset(tr.getStartPos(), tr.getEndPos());

		for (AnalyzedToken token : tr) {
			if (token.getLemma() != null && -1 == posStack.indexOf(token.getLemma())) {
				String lowerCaseLemma = token.getLemma().toLowerCase();
				posStack.push(lowerCaseLemma);
				this.addLemmasBasedOnWordToStack(lowerCaseLemma);
			}
		}

		current = captureState();

		typeAtt.setType(LEMMA_TYPE);
		if (!posStack.empty()) {	
			termAtt.append(posStack.pop());
		} else {
			termAtt.append(tr.getAnalyzedToken(0).getToken().toLowerCase());
		}
		return true;
	}
	
	private boolean parseItemFromStack() {
		String pop = posStack.pop();
		restoreState(current);
		termAtt.append(pop);
		typeAtt.setType(LEMMA_TYPE);
		return true;
	}

	private void addLemmasBasedOnWordToStack(String word) throws IOException {
		AnalyzedSentence sentence = languageTool.getAnalyzedSentence(word);
		List<AnalyzedTokenReadings> tokenBuffer = Arrays.asList(sentence.getTokens());
		Iterator<AnalyzedTokenReadings> tokenIter = tokenBuffer.iterator();
		if (!tokenIter.hasNext()) {
			return;
		}

		while (tokenIter.hasNext()) {
			AnalyzedTokenReadings tr = tokenIter.next();
			for (AnalyzedToken token : tr) {
				if (token.getLemma() != null && -1 == posStack.indexOf(token.getLemma())) {
					posStack.push(token.getLemma().toLowerCase());
				}
			}
		}
	}
}