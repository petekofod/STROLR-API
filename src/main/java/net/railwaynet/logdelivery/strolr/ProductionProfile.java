package net.railwaynet.logdelivery.strolr;

import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;

@Configuration
public class ProductionProfile {

    @Bean
    @Profile("production")
    WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> containerCustomizer(
            @Value("${keystore.file}") Resource keystoreFile,
            @Value("${keystore.pass}") String keystorePass,
            @Value("${truststore.file}") Resource truststoreFile,
            @Value("${truststore.pass}") String truststorePass,
            @Value("${server.port}") String serverPort) throws Exception {

        String absoluteKeystoreFile = keystoreFile.getFile().getAbsolutePath();
        String absoluteTruststoreFile = truststoreFile.getFile().getAbsolutePath();

        return (ConfigurableServletWebServerFactory container) -> {
            TomcatServletWebServerFactory tomcat = (TomcatServletWebServerFactory) container;
            tomcat.addConnectorCustomizers(
                    (connector) -> {
                        connector.setPort(Integer.parseInt(serverPort));
                        connector.setSecure(true);
                        connector.setScheme("https");

                        Http11NioProtocol proto = (Http11NioProtocol) connector.getProtocolHandler();
                        proto.setSSLEnabled(true);
                        proto.setKeystoreFile(absoluteKeystoreFile);
                        proto.setKeystorePass(keystorePass);
                        proto.setTruststoreFile(absoluteTruststoreFile);
                        proto.setTruststorePass(truststorePass);
                        proto.setKeystoreType("PKCS12");
                        proto.setKeyAlias("1");
                    }
            );

        };
    }
}
