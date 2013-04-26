package JSoup;


import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.net.*;
import java.io.*;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * based on http://java.sun.com/developer/technicalArticles/ThirdParty/WebCrawler/
 * @author nicolas
 *
 */

public class LinksCrawler implements Runnable {
	
	final static String className = LinksCrawler.class.getName();
	final static Logger log = Logger.getLogger(className);
	
	
	public static final String DISALLOW = "Disallow:";
	public static final int    SEARCH_LIMIT = 1000;

	
	Vector linksToTry;
	LinkedBlockingQueue<String> visitedPages;
	Vector visitedPages_v;// same as visitedPages (but this one is not shared, whereas visitedPages serves as monitor of the linksCrawler and the contentReader)
	Vector barUrls;
	
	private String startingUrl;//e.g. http://www.olympic.org
	
	private String robotsTxt; // the robots.txt file



	
	

	public LinksCrawler(String url, LinkedBlockingQueue<String> visitedPages) {
		
		startingUrl = url;
		this.visitedPages = visitedPages;
	}

	public void run() {
		// initialize search data structures
		 linksToTry = new Vector();
		 visitedPages_v = new Vector();
		 barUrls = new Vector();

			
			
		// set default for URL access
		URLConnection.setDefaultAllowUserInteraction(false);
		
	
		
		String root = null;
		try {
			URL baseUrl = new URL(startingUrl);
			linksToTry.addElement(startingUrl);
			root = baseUrl.getHost();
			//read robots.txt
			readRobotsTxt(root);
			System.out.println("****searching in root " + root);
			log.debug("****searching in root " + root);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
			System.exit(0);
		}

		

		/* ***************************
		 * While vectorToSearch.size > 0  
		 * *********************** **/
		String currentURL_S=null;
		while ((linksToTry.size() > 0 && visitedPages_v.size() <= SEARCH_LIMIT - 1) ) {
			// get the first element from the to be searched list
			currentURL_S = (String) linksToTry.elementAt(0);
			linksToTry.removeElementAt(0);

			System.out.println("**** SEARCHING in ***** " + currentURL_S);
			log.debug("****searching in " + currentURL_S);
			System.out.println("vectorToSearch is " + linksToTry);
			log.debug("vectorToSearch is " + linksToTry);

			URL currentURL=null;
			try { 
				currentURL = new URL(currentURL_S);
			} catch (MalformedURLException e) {
				System.out.println("ERROR: invalid URL " + currentURL_S);
				log.error("ERROR: invalid URL " + currentURL_S);
				barUrls.add(currentURL_S);
				continue;
			}

			
			// can only search http: protocol URLs
			if (! (currentURL.getProtocol().startsWith("http") || currentURL.getProtocol().startsWith("https"))) {
				barUrls.add(currentURL_S);
				continue;
			}

			if (!robotSafe(currentURL_S)){
				barUrls.add(currentURL_S);
				break;
			}

			StringBuffer sb = new StringBuffer();
			URLConnection urlConnection=null;
			InputStream urlStream=null;
			try {
				urlConnection = currentURL.openConnection();
				urlConnection.setConnectTimeout(12000);
				urlConnection.setReadTimeout(12000);

				urlStream = urlConnection.getInputStream();
				String type 
				// = urlConnection.guessContentTypeFromStream(urlStream);
				= urlConnection.getContentType();
				if (type == null){
					barUrls.add(currentURL_S);
					continue;
				}
				if (!type.startsWith("text/html")) {
					barUrls.add(currentURL_S);
					continue;
				}

				
				byte b[] = new byte[1000];
				int numRead = urlStream.read(b);
				
				
				while (numRead!=-1){
					sb.append(new String(b,0,numRead));
					numRead = urlStream.read(b);
				}
				//System.out.println("Content is " + content);
				log.debug("finished reading content of " + currentURL_S);
				
			} catch (IOException e) {
				System.out.println("ERROR: couldn't open URL " + currentURL + ". Exception = " + e); e.printStackTrace();
				log.error("ERROR: couldn't open URL " + currentURL + ". Exception = " + e); 
				barUrls.add(currentURL_S);
				continue;
			}
			finally{
				System.out.println("In Finally ");
				log.debug("In Finally ");
				try {
					if (urlStream!=null){
						System.out.println("In Finally, closing urlStream");
						log.debug("In Finally, closing urlStream");
						urlStream.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			try {
				visitedPages.put(currentURL_S);
				visitedPages_v.add(currentURL_S);
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			}

			
			/* So now, we know this page is valid.
			 * Loof for links inside it:
			 */
			Document doc = Jsoup.parse(sb.toString());
			Elements links = doc.select("[href]");
			String link_text=null;
			for (Element link : links){
				link_text = link.attr("href");
				link_text = StringUtils.deleteWhitespace(link_text);
				
				URL urlLink = null;
				
					
				//don't take links to positions in the page
				if (link_text.startsWith("#"))
					continue;
				
				if (link_text.startsWith("javascript:"))
					continue;
				
				if (link_text.startsWith("mailto:"))
					continue;
				
				if (link_text.endsWith(".css") || link_text.endsWith(".pdf")
						|| link_text.endsWith(".jpg")
						|| link_text.endsWith(".gif")
						|| link_text.endsWith(".png")
						|| link_text.endsWith(".mp3")
						|| link_text.endsWith(".")
						|| link_text.contains(".swf?"))
					continue;

				
				
				
				
				
				if (link_text.startsWith("/")){
					try {
						urlLink = new URL(currentURL, link_text);
						link_text = urlLink.toString();
						System.out.println("Link starts with /, new link is " + link_text);
						log.debug("Link starts with /, new link is " + link_text);
					} catch (MalformedURLException e) {
						continue;
					}
				}
				else if (link_text.trim().startsWith("http")){
					//don't take external links (on other hosts)
					if (!link_text.trim().startsWith("http://"+root)){
						continue;
					}
					
				}else{ // should start with an alpha-numeric thing
					System.out.println("should start with an alpha-numeric thing : link_text = currentURL_S  + link_text = " + currentURL_S + " ; "+ link_text);
					log.debug("should start with an alpha-numeric thing link_text = currentURL_S  + link_text = " + currentURL_S + " ; "+ link_text);
					link_text = currentURL_S  + link_text;
				}
				
				/* don't cross over language */
				if (link_text.startsWith("http://www.olympic.org/fr"))
					continue;
				
				
				if ( ! ( visitedPages_v.contains(link_text) || linksToTry.contains(link_text) || barUrls.contains(link_text) ) ){
				  linksToTry.add(link_text);
				  System.out.println("Adding " + link_text);
				  log.debug("Adding " + link_text);
				}
				


			}/* End of: Looking for links inside the page.
			 */

	
		}/* End of: While vectorToSearch.size > 0 etc
		  */

		
			System.out.println("vectorToSearch.size =  " + linksToTry.size());
			log.debug("vectorToSearch.size =  " + linksToTry.size());
			System.out.println("vectorSearched.size =  " + visitedPages_v.size());
			log.debug("vectorSearched.size =  " + visitedPages_v.size());
			
			System.out.println("The list of links (visited) is :"); 
			log.debug("The list of links (visited) is :");
			printVector(visitedPages_v);
			
			//Send 'poison' (EOF) message to the contentReader:
			try {
				visitedPages.put("DONE");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}



	private void readRobotsTxt(String root) {

		String robotsTxtUrl = "http://" + root + "/robots.txt";
		URL urlRobot=null;
		try { 
			urlRobot = new URL(robotsTxtUrl);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		StringBuffer sb = new StringBuffer();
		try {
			InputStream urlRobotStream = urlRobot.openStream();

			// read in entire file
			System.out.println("Reading robots.txt. urlRobot is " + urlRobot);
			log.debug("Reading robots.txt. urlRobot is " + urlRobot);
			byte b[] = new byte[1000];
			int numRead = urlRobotStream.read(b);
			while (numRead != -1) {
				sb.append(new String(b, 0, numRead));
				numRead = urlRobotStream.read(b);
			}
			urlRobotStream.close();
		} catch (IOException e) {
		}
		
		robotsTxt = sb.toString();
		
	}

	
	
	boolean robotSafe(String url) {
		
		if (robotsTxt==null){
			return false;
		}
		

		// assume that this robots.txt refers to us and 
		// search for "Disallow:" commands.
		int index = 0;
		while ((index = robotsTxt.indexOf(DISALLOW, index)) != -1) {
			index += DISALLOW.length();
			String strPath = robotsTxt.substring(index);
			StringTokenizer st = new StringTokenizer(strPath);

			if (!st.hasMoreTokens())
				break;

			String strBadPath = st.nextToken();

			// if the URL starts with a disallowed path, it is not safe
			if (url.indexOf(strBadPath) == 0)
				return false;
		}

		return true;
	}
	
	
	private void printVector(Vector visitedPages_v) {
		Iterator it = visitedPages_v.iterator();
		while (it.hasNext()){
			String page = (String) it.next();
			System.out.println(page);
			log.debug(page);
		}
	}
	
	/**
	 * Retrieves the url of the 'doc' in another language, based on the way 
	 * that switching between languages is encoded in the html.
	 * 
	 * For example, given 
	 * "<a href='/fr/' title='Français'>Français</a>" as langSwitcher,
	 * it will retrieve '/fr/', which is the (relative) path pointing to the french translation
	 * of the current page.
	 * 
	 * It is very likely that the language switching links are always encoded in the same way on
	 * all the pages of a given website.
	 * 
	 * @param doc the HTML page in the origin language
	 * @param langSwitcher : the "switching language" tag, taken from the HTML of the origin language.
	 * @return the url of the page in the desired other language. It might well be a relative url. It
	 * is the responsibility of the caller to interpret it and construct the complete url.
	 */
	public static String getCorrespondingLanguageLink(Document doc, String attributeName, String attributeValue) {
		Element link = doc.select("a[title=Fran\u00E7ais]").first();
		return link.attr("abs:href");
	}

	

}