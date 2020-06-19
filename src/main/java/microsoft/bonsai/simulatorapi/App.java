package microsoft.bonsai.simulatorapi;

import java.util.LinkedHashMap;
import com.microsoft.rest.RestClient;
import microsoft.bonsai.simulatorapi.implementation.*;
import microsoft.bonsai.simulatorapi.models.*;
import com.microsoft.rest.serializer.JacksonAdapter;
import com.microsoft.rest.*;

public class App {
    public static void main(String[] args) throws Exception {
        CartPoleModel model = new CartPoleModel();
        
        int sequence_id = 1;
        String workspaceName = "<WORKSPACE>";
        String accessKey = "<ACCESS_KEY>";
        String baseUrl = "https://api.bons.ai/";
        
        // object that indicates if we have registered successfully
        Object registered = null;
        String sessionId = "";
        
        //we need to add an authorization header for the access key, so we need to build a rest client
        RestClient rc = new RestClient.Builder()
                                        .withBaseUrl(baseUrl)
                                        .withSerializerAdapter(new JacksonAdapter())        
                                        .withResponseBuilderFactory(new ServiceResponseBuilder.Factory())                                                    
                                        .build();

        // add the Authorization header 
        rc.headers().addHeader("Authorization", accessKey);
        
        // using the Swagger-generated client
        SimulatorAPIImpl client = new SimulatorAPIImpl(rc);
        
        
        while(true)
        {
            // go through the registration process
            if(registered == null)
            {
                Sessions sessions = client.sessions();
                
                SimulatorInterface sim_interface = new SimulatorInterface();
                sim_interface.withName("Cartpole-Java");
                sim_interface.withTimeout(60.0);
                sim_interface.withCapabilities(null);

                // minimum required
                sim_interface.withSimulatorContext("{}");

                //create only returns an object, so we need to check what type of object
                Object registrationResponse = sessions.create(workspaceName, sim_interface);
                
                // if we get an error during registration
                if(registrationResponse.getClass() == ProblemDetails.class)
                {
                    ProblemDetails details = (ProblemDetails)registrationResponse;
                   
                    System.out.println(java.time.LocalDateTime.now() + " - ProblemDetails - " +  details.title());
                }
                // successful registration
                else if(registrationResponse.getClass() == SimulatorSessionResponse.class)
                {
                    registered = registrationResponse;
                    
                    SimulatorSessionResponse sessionResponse = (SimulatorSessionResponse)registrationResponse;

                    //this is required
                    sessionId = sessionResponse.sessionId();
                }
                System.out.println(java.time.LocalDateTime.now() + " - registered session " + sessionId);

            }
            else // now we are registered
            {
                System.out.println(java.time.LocalDateTime.now() + " - advancing " + sequence_id);

                // build the SimulatorState object
                SimulatorState sim_state = new SimulatorState();
                sim_state.withSequenceId(sequence_id); //required
                sim_state.withSessionId(sessionId); //required
                sim_state.withState(model.getState()); //required
                sim_state.withHalted(model.halted()); //required

                //advance only returns an object, so we need to check what type of object
                Object response = client.sessions().advance(workspaceName, sessionId, sim_state);

                // if we get an error during advance
                if(response.getClass() == ProblemDetails.class)
                {
                    ProblemDetails details = (ProblemDetails)response;
                   
                    System.out.println(java.time.LocalDateTime.now() + " - ProblemDetails - " +  details.title());
                }
                // succesful advance
                else if(response.getClass() == Event.class)
                {
    
                    Event event = (Event) response;
                    System.out.println(java.time.LocalDateTime.now() + " - received event: " + event.type());
                    sequence_id = event.sequenceId(); // get the sequence from the result

                    //now check the type of event and handle accordingly

                    if(event.type() == EventTypesEventType.EPISODE_START)
                    {
                        // ignored event in Cartpole
                        CartPoleConfig config = new CartPoleConfig();
                        //model.start(event.episodeStart().config());
                        
                    }
                    else if(event.type() == EventTypesEventType.EPISODE_STEP)
                    {
                        CartPoleAction action = new CartPoleAction();
                        
                        // action() returns an Object with a class value of LinkedHashMap
                        LinkedHashMap map = (LinkedHashMap) event.episodeStep().action(); 
                        
                        // get the value of the action 
                        Object theCommand = map.get("command");

                        //sometimes that value is an integer -- somtimes its a double. in either case, CartPoleAction.command is a double
                        if(theCommand.getClass() == Integer.class)
                            action.command  = ((Integer)theCommand).doubleValue();
                        else if (theCommand.getClass() == Double.class)
                            action.command  = ((Double)theCommand).doubleValue();

                        // move the model forward
                        model.step(action);
                    }
                    else if(event.type() == EventTypesEventType.EPISODE_FINISH)
                    {
                        System.out.println("Episode Finish");
                    }
                    else if(event.type() == EventTypesEventType.IDLE)
                    {
                        Thread.sleep(event.idle().callbackTime().longValue() *  1000);
                    }
                    else if (event.type() == EventTypesEventType.UNREGISTER)
                    {
                        client.sessions().delete(workspaceName, sessionId);
                    }
                }
            }
        }
    }
}
