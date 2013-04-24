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

public class BasicFuzzer {

	// Map of page url to inputs (query params and form fields)
	private static final Map<String, PageInput> pagesParams = new HashMap<String, PageInput>();
	private static final String currentPage = Properties.bodgeit;
	
	public static void main(String[] args) throws FailingHttpStatusCodeException, MalformedURLException, IOException{
		WebClient webClient = new WebClient();
		webClient.setJavaScriptEnabled(true);
		discoverLinks(webClient, currentPage);
		//System.out.println("Done finding links");
		//doFormPost(webClient);
		discoverPages(webClient, currentPage);
		//System.out.println("Done finding secret pages");
		
		webClient.closeAllWindows();

		for(String s : pagesParams.keySet()){
			System.out.println(pagesParams.get(s));
		}
	}


	/**
	 * This code is for showing how you can get all the links on a given page, and visit a given URL
	 * @param webClient
	 * @throws IOException
	 * @throws MalformedURLException
	 */
	private static void discoverLinks(WebClient webClient, String webPage) throws IOException, MalformedURLException{
		HtmlPage page = webClient.getPage(webPage);
		List<HtmlAnchor> links = page.getAnchors();
		
		for (HtmlAnchor link : links) {
			boolean newPage = false;
			//System.out.println("Link discovered: " + link.asText() + " @URL=" + link.getHrefAttribute());
			
			URL uri = new URL(currentPage + link.getHrefAttribute());
			
			if (!pagesParams.containsKey(uri.getPath())){
				pagesParams.put(uri.getPath(), new PageInput(uri.getPath()));
			}
			
			if(uri.getQuery() != null){
				String param = uri.getQuery().split("=")[0];
				String val = uri.getQuery().split("=")[1];
				newPage = pagesParams.get(uri.getPath()).addQueryInput(param, val);
				if(newPage){
					//System.out.println("Adding page " + uri.getPath() + " with query " + param + " value " + val);
					discoverLinks(webClient, uri.toString());
					discoverForms(webClient, webPage);
					pagesParams.get(uri.getPath()).addCookies(webClient.getCookieManager().getCookies());
					
				}
			}
			else{
				newPage = pagesParams.get(uri.getPath()).addQueryInput(null, null);
				if(newPage){
					//System.out.println("Adding page " + uri.getPath() + " with query " + null);
					discoverLinks(webClient, uri.toString());
					discoverForms(webClient, webPage);
					pagesParams.get(uri.getPath()).addCookies(webClient.getCookieManager().getCookies());
					
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
					HtmlPage page = webClient.getPage(webPage+secretURL+extension);
					System.out.println("URL-Discovery: Secret URL found " + webPage+secretURL+extension);
					if(!pagesParams.containsKey(new URL(webPage).getPath() + secretURL+extension)){
						pagesParams.put(new URL(webPage).getPath() + secretURL+extension, 
								new PageInput(new URL(webPage).getPath() + secretURL+extension));
					}
					// Some way of reporting improper data
				}
				catch (FailingHttpStatusCodeException e) {
					//Url does not work
					//System.out.println("URL-Discovery: Url not valid " + webPage + secretURL + extension);
				} catch (MalformedURLException e) {
					//Invalid url in file
					//System.err.println("URL-Discovery: Invalid url in secret page file " + secretURL + extension);
				} catch (IOException e) {
					//Error
					//System.err.println("URL-Discovery: " + e.getMessage());
				}
			}
		}
	}

	private static void discoverForms(WebClient webClient, String webPage) throws FailingHttpStatusCodeException, MalformedURLException, IOException{
		HtmlPage page = webClient.getPage(webPage);
		String basePage = new URL(webPage).getPath();

		List<HtmlInput> formInputs = new ArrayList<HtmlInput>();
		for (HtmlForm form : page.getForms()) {
			for(DomNode n : form.getChildren()){
				formInputs.addAll(getInputFields(n, webPage));
			}
		}
		
		if(!pagesParams.containsKey(basePage)){
			pagesParams.put(basePage, new PageInput(basePage));
		}
		pagesParams.get(basePage).addAllFormInput(formInputs);
	}
	
	private static List<HtmlInput> getInputFields(DomNode n, String page){
		List<HtmlInput> htmlInput = new ArrayList<HtmlInput>();
		if(n instanceof HtmlInput){
			htmlInput.add((HtmlInput) n);
			//System.out.println("Adding form element from page " + page);
		}
		if (n.hasChildNodes()){
			for(DomNode n2 : n.getChildren()){
				htmlInput.addAll(getInputFields(n2, page));
			}
		}
		
		return htmlInput;
	}
}
