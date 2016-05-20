/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ntua.cosmos.hackathonplanneraas;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 *
 * @author PANAGIOTIS BOURELOS NTUA PhD Candidate
 */
public class InterfacesOfPlanner/* extends HttpServlet*/{
    private static final int ARITHMETIC_CASE = 1;
    private static final int TEXTUAL_CASE = 2;
    
    /**
     * Type specific interface of the Planner as a Service paradigm
     * which calculates arithmetic similarity
     * Receives a JSON message containing the Case which is documented as such (ex.):
     * {
     *      "payload": {
     *          "message_type": 1 or 2 (implemented 1),
     *          "problem_attributes": "hasOne#hasTwo#hasThree",
     *          "problem_values": "wow#such_blah#much_talk",
     *          "solution_attributes": "hasFour#hasFive#hasSix",
     *          "forSharing":"false",
     *          "weights":"0.5#0.4#0.1",
     *          "threshold":"1.0"
     *      }
     * }
     * problem_attributes and solution_attributes are the names of 
     * localstore-used datatype properties divided by #
     * same for values
     */
    public class ArithmeticCaseSimilarity extends HttpServlet{
        //use searchSimilarity
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
            ArrayList<String> search_result;
            BufferedReader br = request.getReader();
            // Turn the POSTed body into a string.
            int b;
            StringBuilder buf = new StringBuilder(512);
            while ((b = br.read()) != -1) {
                buf.append((char) b);
            }
            br.close();
            String body = buf.toString();
            //Handle JSON string and parse it to object
            JSONObject obj = new JSONObject(), returned_obj = new JSONObject();
            JSONParser parse = new JSONParser();
            try {
                obj = (JSONObject) parse.parse(body);
            } catch (ParseException ex) {
                Logger.getLogger(InterfacesOfPlanner.class.getName()).log(Level.SEVERE, null, ex);
                response.setContentType("application/json;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println(ex.getMessage());
                return;
            }
            //new ArrayList<Element>(Arrays.asList(array))
            String[] split_test = String.valueOf(obj.get("problem_attributes")).split("#");
            List<String> asList_test = Arrays.asList(split_test);
            ArrayList<String> probattr = new ArrayList<>(asList_test);
            ArrayList<String> soluatrr = new ArrayList<>(Arrays.asList(String.valueOf(obj.get("solution_attributes")).split("#")));
            ArrayList<String> probvals = new ArrayList<>(Arrays.asList(String.valueOf(obj.get("problem_values")).split("#")));
            String[] stringWeights = String.valueOf(obj.get("weights")).split("#");
            List<Double> weights = new ArrayList<Double>() {{for (String tempLongString : stringWeights) add(Double.valueOf(tempLongString));}};//cool code line for turning a string array into a double list
            double thr = Double.valueOf(String.valueOf(obj.get("threshold")));
            if(Planner.getInstance().compareProbParams(probattr, 1.0)){
                //Planner function call
                search_result = Planner.getInstance().searchSimilarCase(weights, probvals, probattr, soluatrr, thr, true);
                //End Planner function call
                if(!search_result.get(0).equals("NOANSWER")){
                    int length = search_result.indexOf(search_result.get(search_result.size() - 4));
                    //BUILD THE ANSWERING JSON
                    returned_obj.clear();
                    String temp_answ = "";
                    for(int z = 0; z <= length; z++)
                        temp_answ += search_result.get(z)+"#";//string seperated by #
                    temp_answ = temp_answ.substring(0, temp_answ.length() - 1);//removing extra # from the end
                    returned_obj.put("answer_values", temp_answ);
                    returned_obj.put("similarity",search_result.get(search_result.size() - 3));
                    returned_obj.put("URI",search_result.get(search_result.size() - 2));
                    returned_obj.put("message",search_result.get(search_result.size() - 1));
                    returned_obj.put("friendthatanswered","N/A");
                    response.setContentType("application/json;charset=utf-8");
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.getWriter().println(returned_obj);
                }else{
                    //No EXP SHARING for this instance will return empty JSON
                }
            }
        }
    }
    
    /**
     * Main interface of the Planner as a Service paradigm
     * It will be the deciding factor in any case retrieval choices to be made
     * Subroutines will be implemented as independent REST-exposed services
     * Receives a JSON message containing the Case which is documented as such (ex.):
     * {
     *      "payload": {
     *          "message_type": 1 or 2 (implemented 1),
     *          "problem_attributes": "hasOne#hasTwo#hasThree",
     *          "problem_values": "wow#such_blah#much_talk",
     *          "solution_attributes": "hasFour#hasFive#hasSix",
     *          "forSharing":"false",
     *          "weights":"0.5#0.4#0.1",
     *          "threshold":"1.0"
     *      }
     * }
     * problem_attributes and solution_attributes are the names of 
     * localstore-used datatype properties divided by #
     * same for values
     */
    public class RetrieveSimilarCase extends HttpServlet{
        //main entry point into PaaS Services
        @Override
        protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException{
            BufferedReader br = request.getReader();
            // Turn the POSTed body into a string.
            int b;
            StringBuilder buf = new StringBuilder(512);
            while ((b = br.read()) != -1) {
                buf.append((char) b);
            }
            br.close();
            String body = buf.toString();
            //Handle JSON string and parse it to object
            JSONObject obj = new JSONObject(), returned_obj = new JSONObject();
            JSONParser parse = new JSONParser();
            try {
                obj = (JSONObject) parse.parse(body);
            } catch (ParseException ex) {
                Logger.getLogger(InterfacesOfPlanner.class.getName()).log(Level.SEVERE, null, ex);
                response.setContentType("application/json;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().println(ex.getMessage());
                return;
            }
            //Forward JSON to appropriate services
            try{
                obj = (JSONObject) obj.get("payload");
                int type = Integer.valueOf(String.valueOf(obj.get("message_type")));
                if (type == ARITHMETIC_CASE){
                    returned_obj = customPOST(obj,"http://localhost:3030/planner/similarity/arithmetic");
                }else if(type == TEXTUAL_CASE){
                    //Not Valid, empty JSON
                }else{
                    //custom similarity service
                    //Not Valid, empty JSON
                }
            }catch(NumberFormatException | IOException ex){
                //
            }finally{
                response.setContentType("application/json;charset=utf-8");
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().println(returned_obj);
            }
        }
    }

    /**
     * The custom agnostic POST caller used for interVE service communication through JSON
     * @param obj JSON object to be send
     * @param URL target of the call
     * @return a JSONObject containing the answer values as well as similarity,
     * URI, message and answerer of the Solution
     * @throws MalformedURLException when the URL is invalid
     * @throws ProtocolException when there is a problem with the TCP protocol
     * @throws IOException when data is not valid
     * @throws java.net.ConnectException
     */
    @SuppressWarnings("ConvertToTryWithResources")
    public JSONObject customPOST(JSONObject obj, String URL) throws MalformedURLException, ProtocolException, IOException, ConnectException{
        System.out.println("****************************************************\n");
        System.out.println("Initiating custom POST call function to Service of Planner.");
        System.out.println("****************************************************\n\n");
        JSONObject sol = new JSONObject(), answerObject = new JSONObject();
        JSONParser parse = new JSONParser();
        int answer = 0;
        //set connection URL, open the connection and write the URL body to be passed
        String request = URL;
        URL url = new URL(request);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        //JSON sending code prep
        conn.setDoOutput(true);
        conn.setDoInput(true);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setRequestProperty("Accept", "application/json");
        conn.setRequestMethod("POST");
        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        //JSON write body
        obj.writeJSONString(writer);
        writer.flush();
        String line;
        int responseCode = conn.getResponseCode();
	System.out.println("\nSending 'POST' request to URL : " + url);
        System.out.println("With a POST body of : " + obj);
	System.out.println("Response Code : " + responseCode);
        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        //start creating the responce string
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        String temp = response.toString();
        
        try {
            answerObject = (JSONObject) parse.parse(temp);
        } catch (ParseException | ClassCastException ex) {
            String[] divided = String.valueOf(obj.get("solution_atrributes")).split("#");
            for (int z = 0; z < divided.length; z++) {
                sol.put("answer_param"+z,"#");
            }
            sol.put("similarity","0.0");
            sol.put("hasURI","None");
            sol.put("hasMessage","None");
            sol.put("VE","None");
            writer.close();
            reader.close();
            return sol;
        }
        Set<String> keySet = answerObject.keySet();
        Iterator<String> iterator = keySet.iterator();
        while(iterator.hasNext()){
            String handleread = iterator.next();
            if(handleread.contains("answer_param"))
                answer++;
        }
        for(int z = 0; z < answer; z++)
            sol.put( "answer_param"+z, (String) answerObject.get("answer_param"+z));
        sol.put( "similarity", (String) answerObject.get("similarity"));
        sol.put( "hasURI", (String) answerObject.get("URI"));
        sol.put( "hasMessage", (String) answerObject.get("message"));
        sol.put( "VE", (String) answerObject.get("friendthatanswered"));
        writer.close();
        reader.close();
        return sol;
    }
}

//MESSAGE STRUCTURE of new interfaces
/*
{
    "payload": {
        "message_type": specific_number,
        "problem_attributes": "hasOne#hasTwo#hasThree",
        "problem_values": "wow#such_blah#much_talk",
        "solution_attributes": "hasFour#hasFive#hasSix",
        "hasMessage":"true",(?) might be useless
        "hasURI":"true"(?) might be useless
    }
}

Actual Case For Testing
{
    "payload": {
            "message_type": "1",
            "problem_attributes": "hasTin#hasTdes#hasTout",
            "problem_values": "14#20#12",
            "solution_attributes": "hasConsumption",
            "forSharing": "false",
            "weights": "0.5#0.3#0.2",
            "threshold": "1.0"
    }
}
*/