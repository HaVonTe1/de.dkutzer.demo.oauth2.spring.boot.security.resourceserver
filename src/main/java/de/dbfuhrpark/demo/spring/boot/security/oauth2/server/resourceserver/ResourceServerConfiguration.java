package de.dbfuhrpark.demo.spring.boot.security.oauth2.server.resourceserver;

import java.util.Collection;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.security.oauth2.provider.token.DefaultAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

@Configuration
@EnableResourceServer
//No @EnableWebSecurity necessary
public class ResourceServerConfiguration extends ResourceServerConfigurerAdapter {

    @Value("${security.token-check-uri}")
    private String uri_token_check;

    @Value("${security.resource-id}")
    private String resourceId;

    @Value("${security.client-id}")
    private String clientId;

    @Value("${security.client-secret}")
    private String clientSecret;


    @Override
    public void configure(HttpSecurity http) throws Exception {
        http.
            authorizeRequests()
            .anyRequest().authenticated()
            .and().formLogin().disable();//we dont want a redirect in case the request comes without a token
    }

    /*
    Spring Securities default behaviour is to expect the  authorization server to be embedded in the service itself.
    We have to tell him to validate the incoming token against a remote service.
    Luckly Keycloak has an endpoint for this.
     */
    @Override
    public void configure(ResourceServerSecurityConfigurer resources) throws Exception {
        resources.resourceId(resourceId);

        RemoteTokenServices tokenServices = new RemoteTokenServices();
        tokenServices.setCheckTokenEndpointUrl(this.uri_token_check);
        tokenServices.setClientId(clientId);
        tokenServices.setClientSecret(clientSecret);
        tokenServices.setAccessTokenConverter(new KeycloakAccessTokenConverter());
        resources.tokenServices(tokenServices);

    }
    //This is necesarry because the roles/authorities are packed in a claim "realm_access->roles".
    // Spring Security expects it in "authorities".
    /*
    BUT there is an altenative:
    see https://stackoverflow.com/questions/47069345/how-to-use-spring-security-remotetokenservice-with-keycloak/47077307#47077307

    Via keycloak admin console you can create a token mapper of type "User Realm Role" with claim name "authorities" for the client.
    Then the access token contains the role names in this attribute and no custom DefaultAccessTokenConverter is needed.
     */
    private class KeycloakAccessTokenConverter extends DefaultAccessTokenConverter {

        @Override
        public OAuth2Authentication extractAuthentication(Map<String, ?> map) {
            OAuth2Authentication oAuth2Authentication = super.extractAuthentication(map);
            Collection<GrantedAuthority> authorities = (Collection<GrantedAuthority>) oAuth2Authentication.getOAuth2Request().getAuthorities();
            if (map.containsKey("realm_access")) {
                Map<String, Object> realm_access = (Map<String, Object>) map.get("realm_access");
                if(realm_access.containsKey("roles")) {
                    ((Collection<String>) realm_access.get("roles")).forEach(r -> authorities.add(new SimpleGrantedAuthority(r)));
                }
            }
            return new OAuth2Authentication(oAuth2Authentication.getOAuth2Request(),oAuth2Authentication.getUserAuthentication());
        }
    }

}
