package example.fuzzer;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.DomNode;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlForm;
import com.gargoylesoftware.htmlunit.html.HtmlInput;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlSubmitInput;

public class BasicFuzzer {

	// Map of page url to inputs (query params and form fields)
	private static Map<String, PageInput> pagesParams = new HashMap<String, PageInput>();
	private static final String currentPage = Properties.bodgeit;
	
	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException{
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
		discoverLinks(webClient, currentPage);
		System.out.println("Done finding links");
		//doFormPost(webClient);
		discoverPages(webClient, currentPage);
		System.out.println("Done finding secret pages");
		webClient.closeAllWindows();
	}


	/**
	 * This code is for showing how you can get all the links on a given page, and visit a given URL
	 * @param webClient
	 * @throws IOException
	 * @throws MalformedURLException
	 * @throws URISyntaxException 
	 */
	private static void discoverLinks(WebClient webClient, String webPage) throws IOException, MalformedURLException{
		HtmlPage page = webClient.getPage(webPage);
		List<HtmlAnchor> links = page.getAnchors();
		for (HtmlAnchor link : links) {
			boolean newPage = false;
			//System.out.println("Link discovered: " + link.asText() + " @URL=" + link.getHrefAttribute());
			
			URL uri = new URL(currentPage + link.getHrefAttribute());
			
			if (!pagesParams.containsKey(uri.getPath())){
				pagesParams.put(uri.getPath(), new PageInput());
			}
			
			if(uri.getQuery() != null){
				String param = uri.getQuery().split("=")[0];
				String val = uri.getQuery().split("=")[1];
				newPage = pagesParams.get(uri.getPath()).addQueryInput(param, val);
				if(newPage){
					System.out.println("Adding page " + uri.getPath() + " with query " + param + " value " + val);
					discoverForms(webClient, webPage);
					discoverLinks(webClient, uri.toString());
					
				}
			}
			else{
				newPage = pagesParams.get(uri.getPath()).addQueryInput(null, null);
				if(newPage){
					System.out.println("Adding page " + uri.getPath() + " with query " + null);
					discoverForms(webClient, webPage);
					discoverLinks(webClient, uri.toString());
					
				}
			}
			
		}
	}
	/**
	 * This code attempts to guess any secret pages a site may have.
	 * @param webClient
	 * @param webPage
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static void discoverPages(WebClient webClient, String webPage) throws IOException, MalformedURLException{
		for (String secretURL : Properties.secretPages){
			for(String extension : Properties.pageEndings){
				try{
					HtmlPage page = webClient.getPage(webPage + "/" + secretURL + extension);
					System.out.println("URL-Discovery: Secret URL found " + webPage + secretURL + extension);
					// Some way of reporting improper data
				}
				catch (FailingHttpStatusCodeException e) {
					//Url does not work
					System.out.println("URL-Discovery: Url not valid " + webPage + secretURL + extension);
				} catch (MalformedURLException e) {
					//Invalid url in file
					System.err.println("URL-Discovery: Invalid url in secret page file " + secretURL + extension);
				} catch (IOException e) {
					//Error
					System.err.println("URL-Discovery: " + e.getMessage());
				}
			}
		}
	}

	private static void discoverForms(WebClient webClient, String webPage) throws FailingHttpStatusCodeException, MalformedURLException, IOException{
		HtmlPage page = webClient.getPage(webPage);
		String basePage = new URL(webPage).getPath();
		List<HtmlForm> forms = page.getForms();
		for (HtmlForm form : forms) {
			for (DomNode n : form.getChildren()){
				if (n instanceof HtmlInput){
    				pagesParams.get(basePage).addFormInput(n.getNodeName());
    				System.out.println("Found field " + n.getNodeName() + " on page " + webPage);
				}
			}
		}
	}
}
