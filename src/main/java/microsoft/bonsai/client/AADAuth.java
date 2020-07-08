package microsoft.bonsai.client;

import com.microsoft.aad.msal4j.*;
import com.microsoft.aad.msal4jextensions.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.function.Consumer;
import java.util.concurrent.CompletableFuture;

/**
 * Manages Azure Active Directory authentication and token caching
 */
public class AADAuth {

    private final static String CLIENT_ID = "23e69e0c-5143-40dd-90ca-a6a8cc478db5";
    private final static String AUTHORITY = "https://login.microsoftonline.com/common";
    private final static Set<String> SCOPE = Collections.singleton("api://23e69e0c-5143-40dd-90ca-a6a8cc478db5/Bonsai.Read");

    private PublicClientApplication pca;

    public AADAuth() throws Exception {
        String userHome = System.getProperty("user.home");
        Path cachePath = Paths.get(userHome);
        PersistenceSettings persistenceSettings = PersistenceSettings.builder(".bonsaicache", cachePath)
                .setMacKeychain("Microsoft.Developer.IdentityService", "MSALCache")
                .setLinuxKeyring(null,
                        "MsalTestSchema",
                        "MsalTestSecretLabel",
                        "MsalTestAttribute1Key",
                        "MsalTestAttribute1Value",
                        "MsalTestAttribute2Key",
                        "MsalTestAttribute2Value")
                .build();

        PersistenceTokenCacheAccessAspect tokenCacheAspect = new PersistenceTokenCacheAccessAspect(persistenceSettings);
        pca = PublicClientApplication.builder(CLIENT_ID)
                .authority(AUTHORITY)
                .setTokenCacheAccessAspect(tokenCacheAspect)
                .build();
    }

    /**
     * Gets a token from the cache (if available)
     */
    public String getTokenFromCache() throws Exception {

        try {
            IAuthenticationResult result;

            Set<IAccount> accountsInCache = pca.getAccounts().join();
            // Take first account in the cache. 
            IAccount account = accountsInCache.iterator().next();

            
                SilentParameters silentParameters =
                        SilentParameters
                            .builder(SCOPE, account)
                            .build();
                result = pca.acquireTokenSilently(silentParameters).join();
                return result.accessToken(); 
        } catch (Exception ex) {
            if (ex.getCause() instanceof MsalException || ex instanceof NoSuchElementException) {
                return "";
            } else {
                // Handle other exceptions accordingly
                throw ex;
            }
        }
    }

    /**
     * Performs the device login workflow
     * @param enableLogging if console logs should be shown
     */
    public String deviceCodeLogin(Boolean enableLogging) throws Exception {

        if(enableLogging)
            System.out.println("Starting device code login...");

        Consumer<DeviceCode> deviceCodeConsumer = (DeviceCode deviceCode) -> {
            System.out.println(deviceCode.message());
            // throw new RuntimeException(deviceCode.message());
        };

        DeviceCodeFlowParameters parameters = DeviceCodeFlowParameters.builder(SCOPE, deviceCodeConsumer).build();
        CompletableFuture<IAuthenticationResult> resultFuture = pca.acquireToken(parameters);

        IAuthenticationResult result = resultFuture.join();

        return result.accessToken();
    }

}
