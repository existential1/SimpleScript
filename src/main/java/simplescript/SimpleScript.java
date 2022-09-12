package simplescript;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Scanner;

import org.apache.commons.validator.routines.UrlValidator;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

    public class SimpleScript {
        private static String link = "";
        private static String[] indexedChildlink = {};
        private static int iterations = 0, linkCount = 0, childLinkIndex = 0, timesthru = 0;
        private static HashSet<String> childLinks = new HashSet<String>();
        private static Map<String, String> linksMap = new HashMap<String, String>();

        public static void main(String[] args) throws IOException {

            if(args.length == 4) {
                if (!(processArgs(args))) {
                    outputHelpMessage();
                }
                else {
                	
                		processLink(link, iterations);
                		writeToFile(linkCount, childLinks.size(), linksMap );
                	                   
                }

            }
            else {
                outputHelpMessage();

            }

        }

        private static boolean processArgs(String[] args) {
            boolean err = false;

            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "-u":
                        link = args[++i];
                        break;
                    case "-n":
                        iterations = Integer.parseInt(args[++i]);
                        break;
                    default:
                        err = true;

                }
                if (err) {
                    break;
                }

            }
            
            return ((err) ? false : true);
        }

        private static void processLink(String link, int maxtimes) throws IOException {
        	StringBuffer sb = new StringBuffer();
        	
        	if(urlValidator(link) && isValidIterationCount(maxtimes) && isWikipediaLink(link) && linkNotVisited(link)) {
	            try {
	            	HttpResponse httpresponse = checkIfPageExists(link);
	            	if(httpresponse != null && httpresponse.getStatusLine().getStatusCode() == 200) {
	            		Scanner sc = new Scanner(httpresponse.getEntity().getContent());
	            		while(sc.hasNext()) {
	            			sb.append(sc.next());
	            			
	            		}
	            	}
	            	else {
	            		return;
	            	}
	            	
	            } catch (IOException e) {
	            	System.out.printf("Link page for %s can not be found\n", link);
	            	return;
	            }
        	}
            if(sb.toString().isEmpty() && timesthru == 0) {
            	return;
            }
            else if(!sb.toString().isEmpty()) {
 	            String result = sb.toString();
	             
	            int cnt = getLinks(link, result);
	            
	            if (timesthru++ == 0) {
	            	if(cnt > 0) {
	            		
	            		indexedChildlink = childLinks.toArray(new String[childLinks.size()]);
	            	}
	            	else {
	            		return;
	            	}
	            }
            }
            if(--iterations >= 0 && (childLinkIndex < indexedChildlink.length)) {
            	processLink(indexedChildlink[childLinkIndex++], iterations);
            }
            
        }
        
        public static int getLinks(String url, String content) {
        	StringBuffer lsb = new StringBuffer();
        	int count = 0;
        	String linkDelimiter = "<ahref=\"";
        	String[] prefixes = {"https://en.wikipedia.org",url, "https:", ""};
        	int index = 0, index2;
        	
            while (true)
            {
            	int usePrefix = -1;
            	index2 = 0;
            	index = content.indexOf(linkDelimiter, index);
            	String scrapedLink = new String(); 
                if (index != -1)
                {
                    index += linkDelimiter.length();
                    if(content.charAt(index) == '/') {
                    	usePrefix = (content.charAt(index+1) == '/') ? 2 : 0;
                    		
                    }
                    else if(content.charAt(index) == '#') {
                    	usePrefix = 1;
                    }
                    else if(content.substring(index, index+6).equals("https:")) {
                    	usePrefix = 3;
                    }
                    else {
                    	continue;
                    }
                    index2 = content.indexOf("\"", index);
                    if (index2 != -1) {
                    	scrapedLink = prefixes[usePrefix] + content.substring(index, index2);
                    	if(isWikipediaLink(scrapedLink)) {
                    		HttpResponse httpresponse = checkIfPageExists(url);
                    		if(httpresponse != null && httpresponse.getStatusLine().getStatusCode() == 200) { 
                    			lsb.append(scrapedLink + ",");
                    			childLinks.add(scrapedLink);
                    			count ++;
                    		}
                    	}
                    	index += (index2 - index);
                    	
                    }
                }
                else {
                    break;
                }
            }
        	if (count > 0 && !lsb.toString().isEmpty()) {
        		if(lsb.charAt(lsb.length() - 1) == ',') {
        			lsb.deleteCharAt(lsb.length() - 1);
        		}
        		linkCount += count;
        		linksMap.put(url,lsb.toString());
        	}
        	return count;
        }
        
        public static boolean writeToFile(int totalCount, int uniqueCount, Map<String, String> links ) {
        	StringBuffer fsb = new StringBuffer();
        	boolean success = false;
        	fsb.append("Total links: " + totalCount + ", ");
        	fsb.append("Unique links: " + uniqueCount + ", ");
        	for (Object value : links.values()) {
        	    fsb.append(value.toString() + ", ");
        	}
        	byte data[] = fsb.toString().getBytes();
            Path p = Paths.get(".\\output.csv");
 
            try (OutputStream out = new BufferedOutputStream(
              Files.newOutputStream(p))) {
              out.write(data, 0, data.length);
              success = true;
              
            } catch (IOException e) {
              System.err.println(e);
            }
        	return success;
        }
        
        public static HttpResponse checkIfPageExists(String page) {
        	CloseableHttpClient httpclient = HttpClients.createDefault();
        	HttpGet httpget = new HttpGet(page);
        	HttpResponse httpresponse = null;
			try {
				httpresponse = httpclient.execute(httpget);
				if(httpresponse.getStatusLine().getStatusCode() != 200) {
					httpresponse.setStatusCode(404);
	        	}
				
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return httpresponse;
        	        	
        }
        public static boolean urlValidator(String url)
        {
        	String[] schemes = {"http","https"};
            UrlValidator defaultValidator = new UrlValidator(schemes);
            return defaultValidator.isValid(url);
        }
        
        public static boolean isWikipediaLink(String url)
        {
        	boolean isValid = false;
        	
        	String[] validWikiForms = {"https://en.wikipedia.org/wiki/","https://en.m.wikipedia.org/wiki/"};
            if (url.indexOf(validWikiForms[0]) == 0 || url.indexOf(validWikiForms[1]) == 0) {
            	isValid = true;
            }
            
            return (isValid);
        }
        
        public static boolean isValidIterationCount (int iterations) {
        	return ((iterations > 0 && iterations < 21) ? true : false);
        }
        
        public static boolean linkNotVisited(String url) {
        	for (String key : linksMap.keySet()) {
        	    if (key.equals(url)) {
        	    	return false;
        	    }
        	}
        	
        	return true;
        }

        private static void outputHelpMessage() {
            System.out.println("Options:");
            System.out.println("-u [link to wikipedia page]");
            System.out.println("-n [no of iterations - min 1, max 20]");
            
        }



    }

