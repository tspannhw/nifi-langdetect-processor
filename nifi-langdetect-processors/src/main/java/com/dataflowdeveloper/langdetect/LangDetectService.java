/**
 * 
 */
package com.dataflowdeveloper.langdetect;

import java.io.File;
import java.io.IOException;

import org.apache.tika.langdetect.Lingo24LangDetector;
import org.apache.tika.langdetect.OptimaizeLangDetector;
import org.apache.tika.language.detect.LanguageDetector;
import org.apache.tika.language.detect.LanguageResult;

import opennlp.tools.langdetect.Language;
import opennlp.tools.langdetect.LanguageDetectorME;
import opennlp.tools.langdetect.LanguageDetectorModel;


/**
 * @author tspann
 *
 */
public class LangDetectService {

	// Public Vars
	public static final String CURRENT_DIR = "src/main/resources/META-INF/input";
	public static final String CURRENT_FILE = "/en-ner-person.bin"; // CURRENT_DIR + "/input/en-ner-person.bin";
	public static final String CURRENT_TOKEN_FILE = "/en-token.bin"; // CURRENT_DIR + "/input/en-token.bin";
	public static final String CURRENT_ORG_FILE = "/en-ner-organization.bin";
	public static final String CURRENT_LOCATION_FILE = "/en-ner-location.bin";
	public static final String CURRENT_DATE_FILE = "/en-ner-date.bin";
	public static final String LANG_DETECT_MODEL = "/langdetect-183.bin";

	/**
	 * https://github.com/apache/tika/blob/master/tika-example/src/main/java/org/apache/tika/example/LanguageDetectorExample.java
	 * optional web services Google Translate or Microsoft Translate
	 * 
	 * com.optimaize.langdetect.LanguageDetectorImpl version
	 * @param modelDirectory
	 * @param file
	 * @param sentence
	 * @return language
	 */
	public String langdetectTikaOptimaize(String sentence) {

		LanguageDetector detector = null;
		try {
			detector = new OptimaizeLangDetector().loadModels();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		LanguageResult result = detector.detect(sentence);
//		System.out.println("Confidence:"+ result.getConfidence());
//		System.out.println("Raw:"+ result.getRawScore());
//		
		return result.getLanguage();
	}

	/** Text Detector
	 * TextLangDetector
	 * Language Detection using MIT Lincoln Lab’s Text.jl library https://github.com/trevorlewis/TextREST.jl Please run the TextREST.jl server before using this.

	 */
	
	/**
	 * https://developer.lingo24.com/premium-machine-translation-api
	 * 
	 * @param sentence
	 * @return
	 */
	public String langdetectTikaTextLangDetector(String sentence) {

		LanguageDetector detector = null;
		try {
			detector = new Lingo24LangDetector().loadModels();
		} catch (IOException e) {
			e.printStackTrace();
			return "";
		}
		LanguageResult result = detector.detect(sentence);
		return result.getLanguage();
	}
	
	/**
	 * open nlp
	 * uses:   com.optimaize.langdetect.LanguageDetectorImpl
	 * 
	 * @param modelDirectory
	 * @param sentence
	 * @return language
	 */
	public String langdetectOpenNLP(String modelDirectory, String sentence) {

		String predictedLanguage = null;

		try {
			// load the trained model bin file
			File modelFile = new File(modelDirectory + LANG_DETECT_MODEL);

			// load trained model
			LanguageDetectorModel trainedModel = new LanguageDetectorModel(modelFile);

			// load the detector
			LanguageDetectorME languageDetector = new LanguageDetectorME(trainedModel);

			Language[] languages = languageDetector.predictLanguages(sentence);

			if (languages != null) {
				for (Language language : languages) {
					System.out.println("Language:" + language.getConfidence() + "," + language.getLang());
				}
				if (languages.length > 0) {
					predictedLanguage = languages[languages.length-1].getLang();
					System.out.println("Language:" + languages[languages.length-1].getConfidence() + "," + languages[languages.length-1].getLang());
				}
//				System.out.println("Languages: " + languages.length);
			}
		} catch (Exception e) {
			e.printStackTrace();

			return "";
		}

		return predictedLanguage;
	}
}