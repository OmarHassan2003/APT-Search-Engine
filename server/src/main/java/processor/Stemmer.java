package processor;

import org.tartarus.snowball.ext.PorterStemmer;

public class Stemmer {
    
    public static String stem(String word) {
        PorterStemmer stemmer = new PorterStemmer();
        stemmer.setCurrent(word.toLowerCase());
        stemmer.stem();
        return stemmer.getCurrent();
    }
}
