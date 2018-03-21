package org.codehaus.mojo.wagon;
import java.io.File;

import org.apache.maven.plugin.logging.SystemStreamLog;
import org.apache.maven.wagon.authentication.AuthenticationInfo;
import org.apache.maven.wagon.proxy.ProxyInfo;
import org.apache.maven.wagon.proxy.ProxyInfoProvider;
import org.apache.maven.wagon.repository.Repository;
import org.codehaus.mojo.wagon.shared.DefaultWagonDownload;
import org.codehaus.mojo.wagon.shared.WagonFileSet;
import org.codehaus.mojo.wagon.shared.http.LightweightHttpWagon;
import org.junit.Ignore;
import org.junit.Test;

public class TestDirectoryScanner {
	@Test
	@Ignore
	public void test() throws Exception {
		SystemStreamLog logger = new SystemStreamLog();
		
		DefaultWagonDownload downloader = new DefaultWagonDownload();
		
		WagonFileSet srcFileSet = new WagonFileSet();
		srcFileSet.setDownloadDirectory( new File("/tmp") );
        
        String[] includes = { "**" };
        srcFileSet.setIncludes(includes);
        
        String[] excludes = { ".*/**", "archetype-catalog.xml*" };
        srcFileSet.setExcludes( excludes );
        
        Repository repository = new Repository(
        		"repository", 
        		"http://10.1.21.177:8082/nexus/service/rest/repository/browse/maven-staging-migration-tool-1.29.7");
        
        AuthenticationInfo authInfo = new AuthenticationInfo();
        authInfo.setUserName("admin");
        authInfo.setPassword("admin123");

        LightweightHttpWagon wagon = new LightweightHttpWagon("http://10.1.21.177:8082/nexus/repository/maven-staging-migration-tool-1.29.7");
        
        wagon.connect(repository, authInfo, new ProxyInfoProvider() {
            public ProxyInfo getProxyInfo( String protocol ) {
                return null;
            }
        });

        downloader.download( wagon, srcFileSet, logger );
	}
}
