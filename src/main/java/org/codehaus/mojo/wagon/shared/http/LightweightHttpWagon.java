package org.codehaus.mojo.wagon.shared.http;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Iterator;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.apache.maven.wagon.InputData;
import org.apache.maven.wagon.ResourceDoesNotExistException;
import org.apache.maven.wagon.TransferFailedException;
import org.apache.maven.wagon.Wagon;
import org.apache.maven.wagon.authorization.AuthorizationException;
import org.apache.maven.wagon.resource.Resource;
import org.apache.maven.wagon.shared.http.HtmlFileListParser;

public class LightweightHttpWagon extends org.apache.maven.wagon.providers.http.LightweightHttpWagon
		implements Nexus3DownloadBaseDirectory {
	private final String sourceBaseUrl;

	public LightweightHttpWagon(final String sourceBaseUrl) {
		this.sourceBaseUrl = sourceBaseUrl;
	}
	
	public void init(Wagon wagon) {
		if (wagon instanceof org.apache.maven.wagon.providers.http.LightweightHttpWagon) {
			org.apache.maven.wagon.providers.http.LightweightHttpWagon httpWagon = (org.apache.maven.wagon.providers.http.LightweightHttpWagon) wagon;
		
			this.repository = httpWagon.getRepository();
			this.authenticationInfo = httpWagon.getAuthenticationInfo();
			this.setHttpHeaders(httpWagon.getHttpHeaders());
			this.setUseCache(httpWagon.isUseCache());
		}
	}
	
	private String buildUrl(final String repoUrl, String path) {
		path = path.replace(' ', '+');

		if (repoUrl.charAt(repoUrl.length() - 1) != '/') {
			return repoUrl + '/' + path;
		}

		return repoUrl + path;
	}

	public List getFileList(String destinationDirectory)
			throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		InputData inputData = new InputData();

		if (destinationDirectory.length() > 0 && !destinationDirectory.endsWith("/")) {
			destinationDirectory += "/";
		}

		Resource resource = new Resource(destinationDirectory);

		inputData.setResource(resource);

		fillInputData(inputData);

		InputStream is = inputData.getInputStream();

		String url = buildUrl(sourceBaseUrl != null ? sourceBaseUrl : getRepository().getUrl(), destinationDirectory);
		
		if (is == null) {
			throw new TransferFailedException(url + " - Could not open input stream for resource: '" + resource + "'");
		}

		return HtmlFileListParser.parseFileList(url, is);
	}

	public void fillInputData(InputData inputData)
			throws TransferFailedException, ResourceDoesNotExistException, AuthorizationException {
		Resource resource = inputData.getResource();
		try {
			URL url = null;
			if (resource.getName().endsWith("/") || resource.getName().equals("")) {
				url = new URL(buildUrl(getRepository().getUrl(), resource.getName()));
			} else {
				url = new URL(buildUrl(sourceBaseUrl != null ? sourceBaseUrl : getRepository().getUrl(), resource.getName()));
			}

			HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.setRequestProperty("Accept-Encoding", "gzip");
			if (!isUseCache()) {
				urlConnection.setRequestProperty("Pragma", "no-cache");
			}

			addHeaders(urlConnection);

			// TODO: handle all response codes
			int responseCode = urlConnection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_FORBIDDEN
					|| responseCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
				throw new AuthorizationException("Access denied to: " + url);
			}

			InputStream is = urlConnection.getInputStream();
			String contentEncoding = urlConnection.getHeaderField("Content-Encoding");
			boolean isGZipped = contentEncoding == null ? false : "gzip".equalsIgnoreCase(contentEncoding);
			if (isGZipped) {
				is = new GZIPInputStream(is);
			}
			inputData.setInputStream(is);
			resource.setLastModified(urlConnection.getLastModified());
			resource.setContentLength(urlConnection.getContentLength());
		} catch (MalformedURLException e) {
			throw new ResourceDoesNotExistException("Invalid repository URL", e);
		} catch (FileNotFoundException e) {
			throw new ResourceDoesNotExistException("Unable to locate resource in repository", e);
		} catch (IOException e) {
			throw new TransferFailedException("Error transferring file: " + e.getMessage(), e);
		}
	}

	private void addHeaders(URLConnection urlConnection) {
		if (getHttpHeaders() != null) {
			for (Iterator i = getHttpHeaders().keySet().iterator(); i.hasNext();) {
				String header = (String) i.next();
				urlConnection.setRequestProperty(header, getHttpHeaders().getProperty(header));
			}
		}
	}

	public String getSourceBaseUrl() {
		return sourceBaseUrl;
	}
}
