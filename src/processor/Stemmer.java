package src.processor;

import org.tartarus.snowball.ext.PorterStemmer;

public class Stemmer {
    private static final PorterStemmer stemmer = new PorterStemmer();

    public static String stem(String word) {
        stemmer.setCurrent(word.toLowerCase());
        stemmer.stem();
        return stemmer.getCurrent();
    }
}
