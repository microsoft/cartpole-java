package com.microsoft.bonsai.simulatorapi.samples.cartpole;

import java.util.LinkedHashMap;

import com.microsoft.bonsai.client.*;
import com.microsoft.bonsai.generated.Sessions;
import com.microsoft.bonsai.generated.models.*;


import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpMethod;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;


/**
 * Demonstrates calling Cartpole example for training, assessment, and prediction
 */
public class App {
    public static void main(String[] args) throws Exception {
        
        if (args.length == 0) {
            trainAndAssess();
        } else if (args[0] == "predict") {
            runPrediction(args[1]);
        }
       
    }

    /**
     * peforms training and assessment
     * @throws InterruptedException
     * @throws IllegalArgumentException
     * @throws Exception
     */
    private static void trainAndAssess() throws InterruptedException,IllegalArgumentException,Exception {
        int sequenceId = 1;
        String workspaceName = getWorkspace();
        String accessKey = getAccessKey();
      
        BonsaiClientConfig bcConfig = new BonsaiClientConfig(workspaceName,accessKey);
        
        BonsaiClient client = new BonsaiClient(bcConfig);

        //the cartpole model
        Model model = new Model();

        // object that indicates if we have registered successfully
        Object registered = null;
        String sessionId = "";

        while (true) {
            // go through the registration process
            if (registered == null) {
                Sessions sessions = client.sessions();

                SimulatorInterface  sim_interface = new SimulatorInterface();
             
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

                    if (event.type() == EventType.EPISODE_START) {
                        
                        Config config = new Config();

                        // use event.episodeStart().config() to obtain values (not used in Cartpole)

                       model.start(config);

                    } else if (event.type() == EventType.EPISODE_STEP) {
                        Action action = new Action();

                        // action() returns an Object with a class value of LinkedHashMap
                        LinkedHashMap map = (LinkedHashMap) event.episodeStep().action();

                        // get the value of the action
                        Object theCommand = map.get("command");

                        // sometimes that value is an integer -- somtimes its a double. in either case,
                        // Action.command is a double
                        if (theCommand.getClass() == Integer.class)
                            action.command = ((Integer) theCommand).doubleValue();
                        else if (theCommand.getClass() == Double.class)
                            action.command = ((Double) theCommand).doubleValue();

                        // move the model forward
                        model.step(action);
                    } else if (event.type() == EventType.EPISODE_FINISH) {
                        System.out.println("Episode Finish");
                    } else if (event.type() == EventType.IDLE) {
                        Thread.sleep(event.idle().callbackTime().longValue() * 1000);
                    } else if (event.type() == EventType.UNREGISTER) {
                        client.sessions().delete(workspaceName, sessionId);
                    }
                }
            }
        }
    }

    /** calls an exported brain for predictions (different control loop) */
    private static void runPrediction(String predictionurl) throws Exception
    {
        Model model = new Model();
        
        HttpClient httpClient = new HttpClient();
        httpClient.start();
        ObjectMapper mapper = new ObjectMapper();

        while(true)
        {
            // get the state from the model
            String json = mapper.writeValueAsString(model.getState());
            
            //see the request
            System.out.println(json);

            Request request = httpClient.newRequest(predictionurl)
                                        .content(new StringContentProvider(json), "application/json")
                                        .method(HttpMethod.GET);// HTTP GET w/ body



            ContentResponse response = request.send();

            String jsonResponse = response.getContentAsString();

            //see the response
            System.out.println(jsonResponse);
            
            JavaType type = mapper.getTypeFactory().constructType(Action.class);
            Action action = mapper.readValue(jsonResponse, type);

            // pass the response action to the model
            model.step(action);
    
        }
    }

    
    private static String getWorkspace() {

		if (System.getenv("SIM_WORKSPACE") != null) {
			return System.getenv("SIM_WORKSPACE");
		}

        //fill in your Bonsai workspace ID
		return "<WORKSPACE>";
	}

    private static String getAccessKey() {

		if (System.getenv("SIM_ACCESS_KEY") != null) {
			return System.getenv("SIM_ACCESS_KEY");
		}

         //fill in your Bonsai access key
		return "<ACCESS_KEY>";
	}
    
}
