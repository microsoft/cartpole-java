package microsoft.bonsai.client;

/**
 * Manages configuration for connecting to the Bonsai platform
 */
public class BonsaiClientConfig {
    
     /**
     * The Bonsai accessKey.
     */
    String accessKey;

     /**
     * If output statements are enabled for console logging
     */
    Boolean enableLogging;

     /**
     * The connection context details
     */
    //required, but the system will handle it
    public String simulatorContext = "";

    /**
     * The url of the server
     */
    public String server = "https://api.bons.ai/";

    /**
     * The unique Bonsai workspace
     */
    public String workspace;

    /**
     * Initializes an instance of BonsaiClientConfig class using workspace and accessKey.
     *
     * @param workspace the user's Bonsai workspace ID
     * @param accessKey the user's access key
     * @param enableLogging output data for logging
     */
    public BonsaiClientConfig(String workspace, String accessKey, Boolean enableLogging) throws IllegalArgumentException, Exception {
        setBonsaiClientConfig(workspace,accessKey, enableLogging, false);
    }

     /**
      * Initializes an instance of BonsaiClientConfig class using Azure Active
      * Directory credentials.
      *
      * @param workspace     the user's Bonsai workspace ID
      * @param enableLogging output data for logging
      * @throws Exception
      * @throws IllegalArgumentException
      */
     public BonsaiClientConfig(String workspace, Boolean enableLogging) throws IllegalArgumentException, Exception {
        setBonsaiClientConfig(workspace,null, enableLogging, true);
    }

     /**
     * Initializes an instance of BonsaiClientConfig class using Azure Active Directory credentials.
     *
     * @param workspace the user's Bonsai workspace ID
     * @param accessKey the user's access key
     * @param enableLogging output data for logging
     * @param useAAD indicates if the configuration is going to use Azure Active Directory
     */
    private void setBonsaiClientConfig(String workspace, String accessKey, Boolean enableLogging, Boolean useAAD) throws IllegalArgumentException,Exception {

        String WORKSPACE_ENV = "SIM_WORKSPACE";
        String ACCESS_KEY_ENV = "SIM_ACCESS_KEY";
        String SIM_CONTEXT_ENV = "SIM_CONTEXT";
        String SERVER_ENV = "SIM_API_HOST";

        if (System.getenv(WORKSPACE_ENV) != null) {
			this.workspace = System.getenv(WORKSPACE_ENV);
        }
        else
        {
            this.workspace = workspace;
        }

        if(useAAD)
        {
            AADAuth auth = new AADAuth();
            String token = auth.getTokenFromCache();

            if(token.isEmpty())
                token = auth.deviceCodeLogin(enableLogging);

            this.accessKey = "Bearer " + token;
        }
        else   
        {
            if (System.getenv(ACCESS_KEY_ENV) != null) 
                this.accessKey = System.getenv(ACCESS_KEY_ENV);
            else
                this.accessKey = accessKey;
        }

        if(this.accessKey == null && !useAAD)
            throw new IllegalArgumentException("Must pass an accessKey value or useAAD should be true");

        if(this.accessKey.isEmpty()  && !useAAD)
            throw new IllegalArgumentException("Must pass an accessKey value or useAAD should be true");
        
        if (System.getenv(SIM_CONTEXT_ENV) != null) {
			this.simulatorContext = System.getenv(SIM_CONTEXT_ENV);
        }
        
        if (System.getenv(SERVER_ENV) != null) {
			this.server = System.getenv(SERVER_ENV);
        }
       
        this.enableLogging = enableLogging;
        
        
    }
}