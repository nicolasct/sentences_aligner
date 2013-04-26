package JSoup;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * Reads the content of 2 aligned web pages, 
 * e.g. from http://www.olympic.org/ (English) and
 * http://www.olympic.org/fr/ (French translation),
 *  and writes aligned sentences and phrases in a file (bicorpus).
 *  
 * For that, it launches 2 threads: one for an EnglishReader (reading the
 * English page) and one for a FrenchReader.
 * These names are just exemplary; it should work for any pair of languages.
 *
 */
public class ContentCrawler implements Runnable {
	
	final static String className = ContentCrawler.class.getName();
	final static Logger log = Logger.getLogger(className);


	private LinkedBlockingQueue<String> pagesToRead;
	
	

	
	public ContentCrawler(LinkedBlockingQueue<String> webSite_urls) {
		this.pagesToRead = webSite_urls;
	}

	public void run () {
		
		String currentUrl=null;
		
		try {
			
			while ((currentUrl = pagesToRead.take()) != null){   // && !currentUrl.equals("DONE")){ (don't use that! it stops far too early)
				
				System.out.println(this.getClass() + " currentUrl = " + currentUrl);log.debug(this.getClass() + " currentUrl = " + currentUrl);
			
				String fileName = "bi_corpus.txt_"+ (currentUrl.replaceAll(":|/|\\s|\\\\|\\?|\\*|\"|<|>|\\|", "_"));
				if (fileName.length() > 100)
					fileName = fileName.substring(0,100)+".txt";
				
				File file = new File(fileName);
				SynchronousContent synchronousContent = new SynchronousContent(file);
				
				Thread t1 = new Thread(new EnglishReader(synchronousContent, currentUrl));
				Thread t2 = new Thread(new FrenchReader(synchronousContent));
				
				t1.start();
				try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				t2.start();
				
				try {
					t1.join();
					t2.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			
			}
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

	}

}
