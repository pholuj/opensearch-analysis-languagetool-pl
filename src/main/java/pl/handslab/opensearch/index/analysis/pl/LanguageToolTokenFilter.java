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
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.lucene.util.AttributeSource;
import org.languagetool.AnalyzedSentence;
import org.languagetool.AnalyzedToken;
import org.languagetool.AnalyzedTokenReadings;
import org.languagetool.JLanguageTool;

public final class LanguageToolTokenFilter extends TokenFilter {
  static final String POS_TYPE = "pos";
  static final String LEMMA_TYPE = "lemma";
  static final String WORD_TYPE = "word";

  private final JLanguageTool languageTool;
  private final Stack<PosStackData> posStack;
  private final CharTermAttribute termAtt;
  private final OffsetAttribute offsetAtt;
  private final PositionIncrementAttribute posIncrAtt;
  private final TypeAttribute typeAtt;
  private final StringBuilder collectedInput = new StringBuilder();

  private AttributeSource.State current;
  private Iterator<AnalyzedTokenReadings> tokenIter;

  LanguageToolTokenFilter(TokenStream input, JLanguageTool languageTool) {
    super(input);
    this.languageTool = languageTool;
    posStack = new Stack<>();
    termAtt = addAttribute(CharTermAttribute.class);
    offsetAtt = addAttribute(OffsetAttribute.class);
    posIncrAtt = addAttribute(PositionIncrementAttribute.class);
    typeAtt = addAttribute(TypeAttribute.class);
  }

  private class PosStackData {
    public final String value;
    public final String type;

    PosStackData(String value, String type) {
      this.value = value;
      this.type = type;
    }
  }

  @Override
  public boolean incrementToken() throws IOException {

    if (posStack.size() > 0) {
      PosStackData pop = posStack.pop();
      restoreState(current);
      termAtt.append(pop.value);
      posIncrAtt.setPositionIncrement(0);
      typeAtt.setType(pop.type);
      return true;
    }

    if (tokenIter == null || !tokenIter.hasNext()) {
      // there are no remaining tokens from the current sentence... are there more
      // sentences?
      if (input.incrementToken()) {
        // a new sentence is available: process it.
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
        /*
         * it should not be possible to have a sentence with 0 words, check just in
         * case. returning EOS isn't the best either, but it's the behavior of the
         * original code.
         */
        if (!tokenIter.hasNext()) {
          return false;
        }
      } else {
        return false; // no more sentences, end of stream!
      }
    }

    // It must clear attributes, as it is creating new tokens.
    clearAttributes();
    AnalyzedTokenReadings tr = tokenIter.next();

    // add POS tag for sentence start.
    if (tr.isSentenceStart()) {
      // TODO: would be needed so negated tokens can match on something (see
      // testNegatedMatchAtSentenceStart())
      // but breaks other cases:
      termAtt.append("SENT_START");

      String posTag = tr.getAnalyzedToken(0).getPOSTag();
      String lemma = tr.getAnalyzedToken(0).getLemma();
      termAtt.append(posTag);
      if (lemma != null) {
        termAtt.append(lemma);
        typeAtt.setType(LEMMA_TYPE);
      } else {
        typeAtt.setType(POS_TYPE);
      }
      return true;
    }

    // by pass the white spaces.
    if (tr.isWhitespace()) {
      return this.incrementToken();
    }

    offsetAtt.setOffset(tr.getStartPos(), tr.getEndPos());

    for (AnalyzedToken token : tr) {
      if (token.getPOSTag() != null) {
        posStack.push(new PosStackData(token.getPOSTag(), POS_TYPE));
      }
      if (token.getLemma() != null) {
        // chances are good this is the same for all loop iterations, store it anyway...
        posStack.push(new PosStackData(token.getLemma(), LEMMA_TYPE));
      }
    }

    if (0 == posStack.size()) {
      // no lemmas, add original word as base lemma
      posStack.push(new PosStackData(tr.getAnalyzedToken(0).getToken().toLowerCase(), LEMMA_TYPE));
    }

    current = captureState();
    termAtt.append(tr.getAnalyzedToken(0).getToken());
    typeAtt.setType(WORD_TYPE);

    return true;
  }
}