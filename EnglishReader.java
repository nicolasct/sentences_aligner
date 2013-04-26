package JSoup;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class EnglishReader implements Runnable {
	
	
	final static String className = EnglishReader.class.getName();
	final static Logger log = Logger.getLogger(className);


	private SynchronousContent synchronousContent;
	private String currentUrl;


	
	public EnglishReader(SynchronousContent synchronousContent) {
		this(synchronousContent,null);
	}


	public EnglishReader(SynchronousContent synchronousContent, String startingUrl) {
		this.synchronousContent = synchronousContent;
		this.currentUrl = startingUrl; 
		
	}


	@Override
	public void run() {
		System.out.println(this.getClass() + " running");log.debug(this.getClass() + " running");
		
		Document doc = null;
		try {
			 //doc = Jsoup.connect(currentUrl).get();
			doc = Jsoup.parse(new URL(currentUrl), 16000);
		} catch (IOException e) {
			e.printStackTrace();log.error(e + " currentUrl is " + currentUrl);
		}
		
		String lineStructure = null;
		String lineContent = null;
		if (doc != null){
		// Be nice and tell the FrenchReader the link it should read:
		String frenchLink = LinksCrawler.getCorrespondingLanguageLink(doc, "title", "Fran\u00E7ais");
		synchronousContent.writeEnglish(null, frenchLink);
		
		
		
		Class contentSelector = ContentSelector.class;
		Method[] meth_arr = contentSelector.getDeclaredMethods();
		Object[] args = new Object[1]; args[0] = doc;
		StringBuffer[] structureAndContent = null;
		
		for (Method meth : meth_arr){
				try {
					if (Modifier.isPublic(meth.getModifiers())){
						structureAndContent = (StringBuffer[]) meth.invoke(null, args);
					}
				} catch (IllegalArgumentException e1) {e1.printStackTrace();
				} catch (IllegalAccessException e1) {e1.printStackTrace();
				} catch (InvocationTargetException e1) {e1.printStackTrace();}
				
			log.debug("getText output " + structureAndContent.toString());
			
			BufferedReader brStructure = new BufferedReader(new StringReader(structureAndContent[0].toString()));
			BufferedReader brContent = new BufferedReader(new StringReader(structureAndContent[1].toString()));
			try {
				while ((lineStructure=brStructure.readLine()) != null){
					lineContent=brContent.readLine();
					System.out.println(this.getClass() + " : I continue. Writing lineContent = " + lineContent);
					log.debug(this.getClass() + " : I continue. lineContent = " + lineContent);
					log.debug(this.getClass() + " : I continue. lineStructure is = " + lineStructure);
					System.out.println(this.getClass() + " : I continue. Writing lineStructure is = " + lineStructure);
					synchronousContent.writeEnglish(lineStructure, lineContent);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

	}
		
		}// End of : if (doc != null)
		System.out.println(this.getClass() + " : I'm done. lineContent = " + lineContent + " .lineStructure was indeed: " + lineStructure);
		log.debug(this.getClass() + " : I'm done. lineContent = " + lineContent + " .lineStructure was indeed: " + lineStructure);
		synchronousContent.writeEnglish("DONE", "DONE");
	}
	
	


	}
	
	
	
		
			

