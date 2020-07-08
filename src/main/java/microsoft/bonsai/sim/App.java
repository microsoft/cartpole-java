package microsoft.bonsai.sim;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import com.microsoft.rest.RestClient;
import com.microsoft.rest.ServiceResponseBuilder;

import microsoft.bonsai.simulatorapi.*;
import microsoft.bonsai.client.*;
import microsoft.bonsai.simulatorapi.implementation.*;
import microsoft.bonsai.simulatorapi.models.*;
import com.microsoft.rest.serializer.JacksonAdapter;

import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.api.Response;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;


import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;


public class App {
    public static void main(String[] args) throws Exception {
        
        if (args.length == 0) {
            trainAndAssess();
        } else if (args[0] == "predict") {
            runPrediction();
        }
       
    }

    private static void trainAndAssess() throws InterruptedException,IllegalArgumentException,Exception {
        int sequenceId = 1;
        String workspaceName = getWorkspace();
        String accessKey = getAccessKey();
      
        BonsaiClientConfig bcConfig = new BonsaiClientConfig(workspaceName,true);
        
        BonsaiClient client = new BonsaiClient(bcConfig);

        CartPoleModel model = new CartPoleModel();

        // object that indicates if we have registered successfully
        Object registered = null;
        String sessionId = "";

        // we need to add an authorization header for the access key, so we need to
        
        while (true) {
            // go through the registration process
            if (registered == null) {
                Sessions sessions = client.sessions();

                SimulatorInterface sim_interface = new SimulatorInterface();
                sim_interface.withName("Cartpole-Java");
                sim_interface.withTimeout(60.0);
                sim_interface.withCapabilities(null);

                // minimum required
                sim_interface.withSimulatorContext(bcConfig.simulatorContext);

                // create only returns an object, so we need to check what type of object
                Object registrationResponse = sessions.create(workspaceName, sim_interface);

                // if we get an error during registration
                if (registrationResponse.getClass() == ProblemDetails.class) {
                    ProblemDetails details = (ProblemDetails) registrationResponse;

                    System.out.println(java.time.LocalDateTime.now() + " - ProblemDetails - " + details.title());
                }
                // successful registration
                else if (registrationResponse.getClass() == SimulatorSessionResponse.class) {
                    registered = registrationResponse;

                    SimulatorSessionResponse sessionResponse = (SimulatorSessionResponse) registrationResponse;

                    // this is required
                    sessionId = sessionResponse.sessionId();
                }
                System.out.println(java.time.LocalDateTime.now() + " - registered session " + sessionId);

            } else // now we are registered
            {
                System.out.println(java.time.LocalDateTime.now() + " - advancing " + sequenceId);

                // build the SimulatorState object
                SimulatorState simState = new SimulatorState();
                simState.withSequenceId(sequenceId); // required
                simState.withSessionId(sessionId); // required
                simState.withState(model.getState()); // required
                simState.withHalted(model.halted()); // required

                // advance only returns an object, so we need to check what type of object
                Object response = client.sessions().advance(workspaceName, sessionId, simState);

                // if we get an error during advance
                if (response.getClass() == ProblemDetails.class) {
                    ProblemDetails details = (ProblemDetails) response;

                    System.out.println(java.time.LocalDateTime.now() + " - ProblemDetails - " + details.title());
                }
                // succesful advance
                else if (response.getClass() == Event.class) {

                    Event event = (Event) response;
                    System.out.println(java.time.LocalDateTime.now() + " - received event: " + event.type());
                    sequenceId = event.sequenceId(); // get the sequence from the result

                    // now check the type of event and handle accordingly

                    if (event.type() == EventTypesEventType.EPISODE_START) {
                        // ignored event in Cartpole
                        CartPoleConfig config = new CartPoleConfig();
                        // model.start(event.episodeStart().config());

                    } else if (event.type() == EventTypesEventType.EPISODE_STEP) {
                        CartPoleAction action = new CartPoleAction();

                        // action() returns an Object with a class value of LinkedHashMap
                        LinkedHashMap map = (LinkedHashMap) event.episodeStep().action();

                        // get the value of the action
                        Object theCommand = map.get("command");

                        // sometimes that value is an integer -- somtimes its a double. in either case,
                        // CartPoleAction.command is a double
                        if (theCommand.getClass() == Integer.class)
                            action.command = ((Integer) theCommand).doubleValue();
                        else if (theCommand.getClass() == Double.class)
                            action.command = ((Double) theCommand).doubleValue();

                        // move the model forward
                        model.step(action);
                    } else if (event.type() == EventTypesEventType.EPISODE_FINISH) {
                        System.out.println("Episode Finish");
                    } else if (event.type() == EventTypesEventType.IDLE) {
                        Thread.sleep(event.idle().callbackTime().longValue() * 1000);
                    } else if (event.type() == EventTypesEventType.UNREGISTER) {
                        client.sessions().delete(workspaceName, sessionId);
                    }
                }
            }
        }
    }

    private static void runPrediction() throws Exception
    {
        CartPoleModel model = new CartPoleModel();
        String predictionurl = "http://cp-java.azurewebsites.net/v1/prediction";

        HttpClient httpClient = new HttpClient();
        httpClient.start();

        while(true)
        {
        
            ObjectMapper mapper = new ObjectMapper();
            String json = mapper.writeValueAsString(model.getState());
            
            System.out.println(json);

            Request request = httpClient.newRequest(predictionurl)
                                        .content(new StringContentProvider(json), "application/json")
                                        .method(HttpMethod.GET);// HTTP GET w/ body



            ContentResponse response = request.send();

            String jsonResponse = response.getContentAsString();

            System.out.println(jsonResponse);
            
            JavaType type = mapper.getTypeFactory().constructType(CartPoleAction.class);
            CartPoleAction action = mapper.readValue(jsonResponse, type);

            model.step(action);
    
        }
    }

    
    private static String getWorkspace() {

		if (System.getenv("SIM_WORKSPACE") != null) {
			return System.getenv("SIM_WORKSPACE");
		}

		return "<WORKSPACE>";
	}

    private static String getAccessKey() {

		if (System.getenv("SIM_ACCESS_KEY") != null) {
			return System.getenv("SIM_ACCESS_KEY");
		}

		return "<ACCESS_KEY>";
	}
    
}
