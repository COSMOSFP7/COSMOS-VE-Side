/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ntua.cosmos.hackathonplanneraas;

import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.ontology.DatatypeProperty;
import com.hp.hpl.jena.ontology.ObjectProperty;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.ontology.OntResource;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.util.FileManager;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.mindswap.pellet.jena.PelletReasonerFactory;

/**
 *
 * @author PANAGIOTIS BOURELOS NTUA PhD Candidate
 */
public final class Planner {
    private static volatile Planner thisInstance;
    private static String dataRetrieval;//temperature from any sensor
    private static String dataRetrievalWholeObject;//whole data reading
    
    public static synchronized void setDataRetrieval(String in){
        dataRetrieval = in;
    }
    
    public static synchronized String getDataRetrieval(){
        return dataRetrieval;
    }
    
    public static synchronized void setDataRetrievalWholeObject(String in){
        dataRetrievalWholeObject = in;
    }
    
    public static synchronized String getDataRetrievalWholeObject(){
        return dataRetrievalWholeObject;
    }
    
    /**
      * Get the only instance of this class.
      *
      * @return the single instance.
      */
    public static Planner getInstance() {
        if (thisInstance == null) {
            synchronized (Planner.class) {
                if (thisInstance == null) {
                    thisInstance = new Planner();
                }
            }
        }
        return thisInstance;
    }
    @Deprecated
    public JSONObject searchEventSolution(JSONObject obj){
        StoredPaths pan = new StoredPaths();
        JSONObject returned = new JSONObject();
        long since = 0;
        long ts = 0;
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        String originalString = "";
        double similarity = 0.0, current = 0.0;
        if(!obj.isEmpty()){
            Set keys = obj.keySet();
            Iterator iter = keys.iterator();
            for( ; iter.hasNext() ; ){
                String temporary = String.valueOf(iter.next());
                if(temporary.startsWith("has"))
                    names.add(temporary);
            }

            names.stream().forEach((name) -> {
                values.add(String.valueOf(obj.get(name)));
            });
        }
        originalString = values.stream().map((value) -> value).reduce(originalString, String::concat);
        //Begin the initialisation process.
        OntModelSpec s = new OntModelSpec( PelletReasonerFactory.THE_SPEC );
        OntDocumentManager dm = OntDocumentManager.getInstance();
        dm.setFileManager( FileManager.get() );
        s.setDocumentManager(dm);
        OntModel m = ModelFactory.createOntologyModel(s,null);
        InputStream in = FileManager.get().open(StoredPaths.casebasepath);
        if (in == null) {
            throw new IllegalArgumentException("File: " + StoredPaths.casebasepath + " not found");
        }
        // read the file
        m.read(in, null);
        //begin building query string.
        String queryString = pan.prefixrdf+pan.prefixowl+pan.prefixxsd+pan.prefixrdfs+
        pan.prefixCasePath;
        queryString += "\nSELECT DISTINCT ";
        for (int i=0; i<names.size();i++){
            queryString += "?param"+i+" ";
        }
        queryString += "?message ?handle ?URI WHERE {";
        for (int i=0; i<names.size();i++){
            queryString += "?event base:"+names.get(i)+" ?param"+i+" . ";
        }
        queryString += "?event base:isSolvedBy ?solution . ?solution base:exposesMessage ?message . ?solution base:eventHandledBy ?handle . ?solution base:URI ?URI}";
        try
        {
            String testString = "";
            Query query = QueryFactory.create(queryString);
            QueryExecution qe = QueryExecutionFactory.create(query, m);
            ResultSet results =  qe.execSelect();
            for ( ; results.hasNext() ; )
            {
                QuerySolution soln = results.nextSolution() ;
                // Access variables: soln.get("x");
                Literal lit;
                for(int i=0;i<names.size();i++){
                    lit = soln.getLiteral("param"+i);// Get a result variable by name.
                    String temporary = String.valueOf(lit).substring(0, String.valueOf(lit).indexOf("^^"));
                    testString += temporary;
                }
                String longer = testString, shorter = originalString;
                if (testString.length() < originalString.length()) { // longer should always have greater length
                  longer = originalString; shorter = testString;
                }
                int longerLength = longer.length();
                System.out.println("Similarity between:"+originalString+" and "+testString+" is:");
                current = (longerLength - StringUtils.getLevenshteinDistance(longer, shorter))/(double) longerLength;
                System.out.println(current+" out of 1.0.");
                if(similarity < current){
                    similarity = current;
                    returned.clear();
                    returned.put("message", soln.getLiteral("message").getString());
                    returned.put("URI", soln.getLiteral("URI").getString());
                    returned.put("handle", soln.getLiteral("handle").getString());
                }
            }
        }catch(Exception e){
            System.out.println("Search is interrupted by an error.");
        }
        m.close();
        return returned;
    }
    
    /**
     * Method to search for Cases similar with those of the input.
     * @param w A List of doubles containing weights for Case problem parameters. If null, no special weight given to parameters.
     * @param prob A string arraylist with the values of the problem parameters.
     * @param params A string arraylist with the names of problem parameters as they appear in the ontology.
     * @param solutionat A string arraylist with the names of the solution parameters for the expected case.
     * @param thr A double containing the similarity threshold for Cases retrieval.
     * @param shareable A parameter indicating whether the result is destined for sharing purposes or not.
     * @return An arraylist with solution attribute values as well as the URI of the
     * recommended service and any message that may be contained.
     */
    public ArrayList<String> searchSimilarCase(List<Double> w, ArrayList<String> prob, ArrayList<String> params, ArrayList<String> solutionat, double thr, boolean shareable){
        System.out.println("****************************************************");
        System.out.println("We now begin the actual search for a similar problem");
        System.out.println("****************************************************\n\n");
        StoredPaths pan = new StoredPaths();
        if(w==null){
            w = new ArrayList<>();
            double fractal = 1.0/params.size();
            for (String param : params) {
                w.add(fractal);
            }
        }
        double prevsim = 0.0;
        boolean hasresult = false;
        ArrayList<String> solution = new ArrayList<>();
        ArrayList<String> tempbest = new ArrayList<>();
        String URI = "None", message = "None";
        int[] answer = new int[solutionat.size()];
        //Begin the initialisation process.
        OntModelSpec s = new OntModelSpec( PelletReasonerFactory.THE_SPEC );
        OntDocumentManager dm = OntDocumentManager.getInstance();
        dm.setFileManager( FileManager.get() );
        s.setDocumentManager(dm);
        OntModel m = ModelFactory.createOntologyModel(s,null);
        InputStream in = FileManager.get().open(StoredPaths.casebasepath);
        if (in == null) {
            throw new IllegalArgumentException("File: " + StoredPaths.casebasepath + " not found");
        }
        // read the file
        m.read(in, null);
        //begin building query string.
        String queryString =pan.prefixrdf+pan.prefixowl+pan.prefixxsd+pan.prefixrdfs+
        pan.prefixCasePath;
        //add answer params to query string select
        queryString += "\nselect distinct ";
        for(int z=0;z<params.size();z++){
            queryString += "?param" + z + " ";
        }
        for(int z=0;z<solutionat.size();z++){
            queryString += "?answer" + z + " ";
        }
        //add URI and message params to query string select
        queryString += "?URI ?message where{";//expected return values, must all exist in ontology even if blank
        //add problem params and values to query
        for(int z=0;z<params.size();z++){
            if (w.get(z)>=0.1)
                queryString += "?prob base:"+params.get(z)+" ?param" + z + " . ";
            else
                queryString += "OPTIONAL{?prob base:"+params.get(z)+" ?param" + z + "} . ";
        }
        if (shareable)
            queryString += "?prob base:isShareable true . ";
        //connect problem and solution
        queryString += "?solution base:solves ?prob . ";
        //add solution params to query body
        for (int z=0;z<solutionat.size();z++){
            queryString += "?solution base:" + solutionat.get(z) + " ?answer" + z + " . ";
        }
        //add params for uri and message
        queryString += "?solution base:URI ?URI . ?solution base:exposesMessage ?message}";
        System.out.println("********************************************************************");
        System.out.println("This is the dynamicaly generated query String for problem retrieval.");
        System.out.println(queryString);//print query string for checking purposes.
        System.out.println("********************************************************************\n\n");
        try
        {
            Query query = QueryFactory.create(queryString);
            QueryExecution qe = QueryExecutionFactory.create(query, m);
            ResultSet results =  qe.execSelect();
            for ( ; results.hasNext() ; )
            {
                double similarity = 0.0;
                QuerySolution soln = results.nextSolution() ;
                // Access variables: soln.get("x");
                Literal lit;
                ArrayList<Literal> literlistparam = new ArrayList<>();
                ArrayList<Literal> literlistsol = new ArrayList<>();
                for (int z=0;z<params.size();z++){
                    lit = soln.getLiteral("param"+z);// Get a result variable by name.
                    literlistparam.add(lit);
                }
                List<Double> answers = new ArrayList<>();
                List<Double> a = new ArrayList<>();
                List<Double> b = new ArrayList<>();
                double length = 0.0;
                for (int z = 0; z<literlistparam.size(); z++){
                    a.add(Double.valueOf(prob.get(z)));
                    if (!literlistparam.get(z).getString().equalsIgnoreCase("")){
                        b.add(Double.valueOf(literlistparam.get(z).getString()));
                    }
                    else
                        b.add(0.0);
                }
                for (int z = 0; z<literlistparam.size(); z++){
                    length += a.get(z) + b.get(z);
                }
                
                System.out.println("********************************");
                System.out.println("Problem parameter similarities: ");
                for (int z = 0; z<literlistparam.size(); z++){
                    answers.add(1.0 - Math.abs(a.get(z) - b.get(z))/length);
                    System.out.println(answers.get(z));
                }
                System.out.println("********************************");
                
                for(int z=0;z<answers.size();z++){
                    similarity += w.get(z)*answers.get(z);
                }
                System.out.println("Total similarity from previous parameters: "+similarity);
                if (similarity>prevsim && similarity>=thr){
                    hasresult = true;
                    for (int z=0;z<solutionat.size();z++){
                        lit = soln.getLiteral("answer"+z);// Get a result variable by name.
                        literlistsol.add(lit);
                    }
                    Literal lit2 = soln.getLiteral("URI") ;
                    Literal lit3 = soln.getLiteral("message") ;
                    
                    tempbest.clear();
                    for(int z=0;z<solutionat.size();z++){
                        if(literlistsol.get(z).isLiteral())
                            tempbest.add(literlistsol.get(z).getString());
                    }
                    if(lit2.isLiteral())
                        URI = lit2.getLexicalForm();
                    if(lit3.isLiteral()){
                        if(!lit3.getString().equalsIgnoreCase(""))
                            message = lit3.getString();
                    }
                    else{
                        message = "None";
                    }
                }
                if(similarity>prevsim)
                    prevsim = similarity;
            }
            qe.close();
        }
        catch(NumberFormatException e){
            System.out.println("Query not valid.");
        }
        m.close();
        if (!hasresult)
            //solution.add("0");
            solution.add("NOANSWER");
        else
            solution.addAll(tempbest);
        solution.add(String.valueOf(prevsim));
        solution.add(URI);
        solution.add(message);
        return solution;
    }
    /**
     * 
     * @param params
     * @param thr
     * @return 
     */
    public boolean compareProbParams(ArrayList<String> params, double thr){
        //query for all problem ATTRIBUTES of each problem in localstore.        
        System.out.println("***************************************");
        System.out.println("At this point we are checking if a similar problem "
                + "structure is present in the Case Base.");
        System.out.println("***************************************\n\n");
        StoredPaths pan = new StoredPaths();
        boolean isworthchecking = false;
        double similarity;
        ArrayList<ArrayList<String>> contents = new ArrayList<>();
        ArrayList<String> tempparam = new ArrayList<>();
        ArrayList<String> namelist = new ArrayList<>();
        String tempname = "";
        for ( int i = 0;  i < params.size(); i++){
            String tempName = params.get(i);
            if(tempName.equals("ttl")){
                params.remove(i);
            }
        }
        //Begin the initialisation process.
        OntModelSpec s = new OntModelSpec(OntModelSpec.OWL_DL_MEM);
        OntDocumentManager dm = OntDocumentManager.getInstance();
        dm.setFileManager( FileManager.get() );
        s.setDocumentManager(dm);
        OntModel m = ModelFactory.createOntologyModel(s,null);
        InputStream in = FileManager.get().open(StoredPaths.casebasepath);
        if (in == null) {
            throw new IllegalArgumentException("File: " + StoredPaths.casebasepath + " not found");
        }
        // read the file
        m.read(in, null);
        //begin building query string.
        String queryString =pan.prefixrdf+pan.prefixowl+pan.prefixxsd+pan.prefixrdfs+
        pan.prefixCasePath;
        queryString += "\nSELECT distinct ?prob ?param WHERE { ?prob ?param ?value . ?prob base:hasCaseType \"problem\"^^xsd:string . filter isLiteral(?value) . ?param a owl:DatatypeProperty . filter not exists{  { filter regex(str(?value), \"problem\" )} union {?prob ?param true} union {?prob ?param false} } }";
        System.out.println("***************************************");
        System.out.println("Query String used: ");
        System.out.println(queryString);
        System.out.println("***************************************\n\n");
        try
        {
            Query query = QueryFactory.create(queryString);
            QueryExecution qe = QueryExecutionFactory.create(query, m);
            ResultSet results =  qe.execSelect();
            int i = 0;
            for ( ; results.hasNext() ; )
            {
                QuerySolution soln = results.nextSolution() ;
                // Access variables: soln.get("x");
                Resource res;
                Resource res2;
                res = soln.getResource("prob");// Get a result variable by name.
                if(!res.getURI().equalsIgnoreCase(tempname)){
                    tempname = res.getURI();
                    namelist.add(res.getURI());
                    i++;
                    if (!tempparam.isEmpty())
                        contents.add(new ArrayList<>(tempparam));
                    tempparam.clear();
                }
                res2 = soln.getResource("param");
                tempparam.add(res2.getURI().substring(res2.getURI().indexOf('#')+1));
            }
            qe.close();
            contents.add(new ArrayList<>(tempparam));
        }
        catch(NumberFormatException e){
            System.out.println("Query not valid.");
        }
        m.close();
        ArrayList<String> union = new ArrayList<>();
        ArrayList<String> intersection = new ArrayList<>();
        int i = 0;
        for (ArrayList<String> content : contents) {
            intersection.addAll(params);
            intersection.retainAll(content);
            union.addAll(content);
            similarity = intersection.size()/union.size();
            if (similarity>=thr){
                isworthchecking = true;
                System.out.println("The Case is worth checking because of this "+similarity+" metric "
                        + "in problem identified as "+namelist.get(i)+".");
            }
            i++;
            intersection.clear();
            union.clear();
        }
        return isworthchecking;
    }
    
    public ArrayList<ArrayList<String>> searchIotService(String domain){
        ArrayList<ArrayList<String>> solution = new ArrayList<>(); 
        int i = 0;
        String servicename;
        String prevser = "";
        String prevURI = "";
        String URI;
        ArrayList<String> paramname = new ArrayList<>();
        ArrayList<String> respname = new ArrayList<>();
        StoredPaths pan = new StoredPaths();
        OntModel m = createModel(StoredPaths.servicepath);
        //begin building query string.
        String queryString = pan.prefixrdf+pan.prefixowl+pan.prefixxsd+pan.prefixrdfs+
        pan.prefixServicePath;
        queryString += "\nselect distinct ?service ?URI ?namep ?namer "
                + "where{?service base:isServiceOf base:"+domain+" . "
                + "?service base:usesComponent ?comp . ?comp base:hasRESTParameter ?param . "
                + "?comp base:hasRESTResponce ?resp . ?comp base:accessURI ?URI . "
                + "?param base:hasName ?namep . ?resp base:hasName ?namer} order by ?service ?namer";
        System.out.println(queryString);
        try
        {
            Query query = QueryFactory.create(queryString);
            QueryExecution qe = QueryExecutionFactory.create(query, m);
            ResultSet results =  qe.execSelect();
            for ( ; results.hasNext() ; )
            {
                QuerySolution soln = results.nextSolution() ;
                // Access variables: soln.get("x");
                Resource res = soln.getResource("service");
                Literal lit = soln.getLiteral("URI");
                Literal lit2 = soln.getLiteral("namep");
                Literal lit3 = soln.getLiteral("namer");
                servicename = res.getURI().substring(res.getURI().indexOf('#')+1);
                URI = lit.getString();
                
                if (prevser.equalsIgnoreCase("") && prevURI.equalsIgnoreCase("")){
                    prevser = servicename;
                    prevURI = URI;
                    respname.add(lit3.getString());
                    respname.add("^");
                    paramname.add(lit2.getString());
                    paramname.add("^");
                }
                if (!servicename.equalsIgnoreCase(prevser)){
                    solution.add(new ArrayList<>(Arrays.asList(prevser,"^",prevURI)));
                    solution.add(new ArrayList<>(Arrays.asList("&")));
                    paramname.remove(paramname.size()-1);
                    solution.add(new ArrayList<>(paramname));
                    solution.add(new ArrayList<>(Arrays.asList("&")));
                    respname.remove(respname.size()-1);
                    solution.add(new ArrayList<>(respname));
                    solution.add(new ArrayList<>(Arrays.asList("&")));
                    prevser = servicename;
                    prevURI = URI;
                    paramname.clear();
                    respname.clear();
                }
                if(!paramname.contains(lit2.getString())){
                    paramname.add(lit2.getString());
                    paramname.add("^");
                }
                if (!lit3.getString().equalsIgnoreCase(respname.get(i))){
                    respname.add(lit3.getString());
                    respname.add("^");
                    //paramname.clear();
                    i++;
                    i++;
                }
            }
            solution.add(new ArrayList<>(Arrays.asList(prevser,"^",prevURI)));
            solution.add(new ArrayList<>(Arrays.asList("&")));
            paramname.remove(paramname.size()-1);
            solution.add(new ArrayList<>(paramname));
            solution.add(new ArrayList<>(Arrays.asList("&")));
            respname.remove(respname.size()-1);
            solution.add(new ArrayList<>(respname));
            //solution.add(new ArrayList<>(Arrays.asList("&")));
            qe.close();
        }
        catch(NumberFormatException e){
            System.out.println("Query not valid.");
        }
        m.close();
        return solution;
    }
    
    /**
     * Method to create a Jena model from an owl file, physically map the imported files
     * and then apply the Pellet reasoner on the model.
     * 
     * @param path a string parameter indicating the physical location of the owl file.
     * 
     * @return a Jena OntModel variable
     */
    public static OntModel createModel(String path){
        OntModelSpec s = new OntModelSpec( PelletReasonerFactory.THE_SPEC );
        OntDocumentManager dm = OntDocumentManager.getInstance();
        dm.setFileManager( FileManager.get() );
        s.setDocumentManager(dm);
        OntModel m = ModelFactory.createOntologyModel(s,null);
        InputStream in = FileManager.get().open(path);
        if (in == null) {
            throw new IllegalArgumentException("File: " + path + " not found");
        }
        m.read(in, null);
        return m;
    }
    
    public void storeCase(ArrayList<String> problemparam, ArrayList<String> problemval, ArrayList<String> solutionparam, ArrayList<String> solutionval){
        int count = 0;
        StoredPaths pan = new StoredPaths();
        //Begin the initialisation process.
        OntModelSpec s = new OntModelSpec(PelletReasonerFactory.THE_SPEC);
        OntDocumentManager dm = OntDocumentManager.getInstance();
        dm.setFileManager( FileManager.get() );
        s.setDocumentManager(dm);
        OntModel m = ModelFactory.createOntologyModel(s,null);
        //
        InputStream in = FileManager.get().open(StoredPaths.casebasepath);
        if (in == null) {
            throw new IllegalArgumentException("File: " + StoredPaths.casebasepath + " not found");
        }
        // read the file
        m.read(in, null);
        
        OntResource instance = m.getOntResource(pan.casebase+StoredPaths.myname);
        OntResource problem = m.getOntResource(pan.casebase+"problem"+String.valueOf(count));
        while (problem!=null){
            count++;
            problem = m.getOntResource(pan.casebase+"problem"+String.valueOf(count));
        }
        problem = m.createOntResource(pan.casebase+"problem"+String.valueOf(count));
        OntResource solution = m.createOntResource(pan.casebase+"solution_of_problem"+String.valueOf(count));
        DatatypeProperty isShareable = m.getDatatypeProperty(pan.casebase+"isShareable");
        DatatypeProperty hasCaseType = m.getDatatypeProperty(pan.casebase+"hasCaseType");
        ObjectProperty solves = m.getObjectProperty(pan.casebase+"solves");
        ObjectProperty includesInCaseBase = m.getObjectProperty(pan.casebase+"includesInCaseBase");
        if(problemval.size()==problemparam.size()&&solutionparam.size()==solutionval.size()){
            for(int i = 0; i<problemval.size(); i++){
                DatatypeProperty property = m.createDatatypeProperty(pan.casebase+problemparam.get(i));
                problem.addLiteral(property,problemval.get(i));
                property = m.createDatatypeProperty(pan.casebase+solutionparam.get(i));
                if(solutionparam.get(i).equalsIgnoreCase("URI"))
                    m.add(solution, property, solutionval.get(i), XSDDatatype.XSDanyURI);
                else
                    solution.addLiteral(property,solutionval.get(i));
            }
            problem.addLiteral(hasCaseType, "problem");
            m.add(problem, isShareable, "true", XSDDatatype.XSDboolean);
            solution.addLiteral(hasCaseType, "solution");
            solution.addProperty(solves, problem);
            instance.addProperty(includesInCaseBase, problem);
            instance.addProperty(includesInCaseBase, solution);
        }else{
            System.out.println("ERROR! Could not store the new Case.");
        }
        try{
            try (FileOutputStream out = new FileOutputStream(StoredPaths.casebasepath, false)) {
                m.write( out, "RDF/XML");
            }
            m.close();
        }catch(IOException fe){
            System.out.println("File not found.");
        }
    }
    
    public boolean isDouble(String input)
    {
        try
        {
           Double.parseDouble(input);
           return true;
        }
        catch( Exception e)
        {
           return false;
        }
    }
    @Deprecated
    public String retrieveValueOfProb(ArrayList<String> info, ArrayList<String> solutionAttributes, String valueSearched) {
        //to be refined!!!!!!!!!!!!!!!! NOT ONLY CONSUMPTION BUT OTHERS LIKE URI AND MESSAGE
        StoredPaths pan = new StoredPaths();
        int size = 0;
        String probVar = "";
        ArrayList<String> solutionValue = new ArrayList<>();
        for(String in:info){
            if(info.indexOf(in) == info.size() - 4){
                break;
            }else{
                solutionValue.add(info.get(info.indexOf(in)));
            }
        }
        //Begin the initialisation process.
        OntModelSpec s = new OntModelSpec( PelletReasonerFactory.THE_SPEC );
        OntDocumentManager dm = OntDocumentManager.getInstance();
        dm.setFileManager( FileManager.get() );
        s.setDocumentManager(dm);
        OntModel m = ModelFactory.createOntologyModel(s,null);
        InputStream in = FileManager.get().open(StoredPaths.casebasepath);
        if (in == null) {
            throw new IllegalArgumentException("File: " + StoredPaths.casebasepath + " not found");
        }
        // read the file
        m.read(in, null);
        //begin building query string.
        String queryString = pan.prefixrdf+pan.prefixowl+pan.prefixxsd+pan.prefixrdfs+pan.prefixCasePath;
        queryString += "\nSELECT distinct ?sol ?prob ?returned WHERE { ?sol base:hasCaseType \"solution\"^^xsd:string .";
        for(int i = 0; i < solutionValue.size(); i++){
            queryString += " ?param"+i+" a owl:DatatypeProperty .";
        }
        for(int i = 0; i < solutionValue.size(); i++){
            queryString += " ?sol ?param"+i+" ?value"+i+" .";
        }
        queryString += " ?sol base:solves ?prob . ?prob base:"+valueSearched+" ?returned .";
        for(int i = 0; i < solutionValue.size(); i++){
            queryString += " filter regex(str(?param"+i+"), \"^http://www.localstore.casebase.com/CaseBaseOntology#"+solutionAttributes.get(i)+"$\") ";
            queryString += " filter regex(str(?value"+i+"), \"^"+solutionValue.get(i)+"$\") ";
        }
        queryString += "}";
        //!!!ATTENTION exact regex string match requires ^ and $ to match the ENTIRE word, else it acts as if searching anything that contains
        //The above query checks for problems containing the datatypes hasTin, hasTout and hasTdes!
        //the real way to make it agnostic is to pass each solution variable and use regexes for each one value
        //PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>
        //PREFIX owl: <http://www.w3.org/2002/07/owl#>
        //PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>
        //PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        //PREFIX base: <http://www.localstore.casebase.com/CaseBaseOntology#>
        //
        //SELECT distinct ?sol ?prob ?returned WHERE { 
        //	?sol base:hasCaseType "solution"^^xsd:string . 
        //	?param0 a owl:DatatypeProperty .
        //	?param1 a owl:DatatypeProperty .
        //	?sol ?param0 ?value0 . 
        //	?sol ?param1 ?value1 .
        //	?sol base:solves ?prob . 
        //	?prob base:hasTdes ?returned  .
        //	filter regex(str(?param0), "^http://www.localstore.casebase.com/CaseBaseOntology#hasConsumption$" ) .  
        //	filter regex(str(?value0), "^630$") .
        //	filter regex(str(?param1), "^http://www.localstore.casebase.com/CaseBaseOntology#exposesMessage$" ) .  
        //	filter regex(str(?value1), "^message to differ$" )
        //}
        System.out.println("***************************************");
        System.out.println("Query String used: ");
        System.out.println(queryString);
        System.out.println("***************************************\n\n");
        try
        {
            Query query = QueryFactory.create(queryString);
            QueryExecution qe = QueryExecutionFactory.create(query, m);
            ResultSet results =  qe.execSelect();
            size = results.getRowNumber();
            for ( ; results.hasNext() ; )
            {
                
                QuerySolution soln = results.nextSolution();
                // Access variables: soln.get("x");
                Literal lit;
                lit = soln.getLiteral("returned");
                probVar = lit.getString();
            }
            qe.close();
        }
        catch(NumberFormatException e){
            System.out.println("Query not valid.");
        }
        m.close();
        System.out.println("Variable value returned is: "+probVar);
        return probVar;    
    }
    
    @Deprecated
    public void searchSimilarEvent(String decoded) throws ParseException {
        JSONObject event = new JSONObject();
        JSONParser parser = new JSONParser();
        String body = "";
        event = (JSONObject) parser.parse(decoded);
        JSONObject SimilarEvent;
        SimilarEvent = searchEventSolution(event);
        System.out.println(SimilarEvent.toJSONString());
        if (String.valueOf(SimilarEvent.get("handle")).contains("internal")){
            body = getPOSTBody(event);
            callRemoteSolution(String.valueOf(SimilarEvent.get("URI")).replace("\\/", "/"),body);
        }
    }
    @Deprecated
    private String getPOSTBody(JSONObject obj) {
        String body = "{}";
        StoredPaths pan = new StoredPaths();
        JSONObject returned = new JSONObject();
        ArrayList<String> names = new ArrayList<>();
        ArrayList<String> values = new ArrayList<>();
        String originalString = "";
        double similarity = 0.0, current = 0.0;
        if(!obj.isEmpty()){
            Set keys = obj.keySet();
            Iterator iter = keys.iterator();
            for( ; iter.hasNext() ; ){
                String temporary = String.valueOf(iter.next());
                if(temporary.startsWith("has"))
                    names.add(temporary);
            }
            names.stream().forEach((name) -> {
                values.add(String.valueOf(obj.get(name)));
            });
        }
        originalString = values.stream().map((value) -> value).reduce(originalString, String::concat);
        //Begin the initialisation process.
        OntModelSpec s = new OntModelSpec( PelletReasonerFactory.THE_SPEC );
        //s.setReasoner(PelletReasonerFactory.theInstance().create());
        OntDocumentManager dm = OntDocumentManager.getInstance();
        dm.setFileManager( FileManager.get() );
        s.setDocumentManager(dm);
        OntModel m = ModelFactory.createOntologyModel(s,null);
        //OntModel m = ModelFactory.createOntologyModel( PelletReasonerFactory.THE_SPEC );
        InputStream in = FileManager.get().open(StoredPaths.casebasepath);
        if (in == null) {
            throw new IllegalArgumentException("File: " + StoredPaths.casebasepath + " not found");
        }
        // read the file
        m.read(in, null);
        //begin building query string.
        String queryString = pan.prefixrdf+pan.prefixowl+pan.prefixxsd+pan.prefixrdfs+
        pan.prefixCasePath;
        queryString += "\nSELECT DISTINCT ";
        for (int i=0; i<names.size();i++){
            queryString += "?param"+i+" ";
        }
        queryString += "?message ?handle ?URI ?body WHERE {";
        for (int i=0; i<names.size();i++){
            queryString += "?event base:"+names.get(i)+" ?param"+i+" . ";
        }
        queryString += "?event base:isSolvedBy ?solution . ?solution base:exposesMessage ?message . ?solution base:eventHandledBy ?handle . ?solution base:URI ?URI . ?solution base:hasPOSTBody ?body}";
        try
        {
            String testString = "";
            Query query = QueryFactory.create(queryString);
            //System.out.println(String.valueOf(query));
            QueryExecution qe = QueryExecutionFactory.create(query, m);
            ResultSet results =  qe.execSelect();
            for ( ; results.hasNext() ; )
            {
                QuerySolution soln = results.nextSolution() ;
                // Access variables: soln.get("x");
                Literal lit;
                for(int i=0;i<names.size();i++){
                    lit = soln.getLiteral("param"+i);// Get a result variable by name.
                    String temporary = String.valueOf(lit).substring(0, String.valueOf(lit).indexOf("^^"));
                    testString += temporary;
                }
                String longer = testString, shorter = originalString;
                if (testString.length() < originalString.length()) { // longer should always have greater length
                  longer = originalString; shorter = testString;
                }
                int longerLength = longer.length();
                current = (longerLength - StringUtils.getLevenshteinDistance(longer, shorter))/(double) longerLength;
                if(similarity < current){
                    similarity = current;
                    returned.clear();
                    returned.put("message", soln.getLiteral("message").getString());
                    returned.put("URI", soln.getLiteral("URI").getString());
                    returned.put("handle", soln.getLiteral("handle").getString());
                    body = soln.getLiteral("body").getString();
                }
            }
        }catch(Exception e){
            System.out.println("Search is interrupted by an error.");
        }
        m.close();
        return body;
    }
    @Deprecated
    private String callRemoteSolution(String urlPoint, String urlParameters) {
        String results = "Almost there";
        try {
            int i=0;
            int answer = 0;
            ArrayList<String> sol = new ArrayList<>();
            JSONObject obj = new JSONObject();
            //set connection URL, open the connection and write the URL body to be passed
            String request = urlPoint;
            URL url = new URL(request);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            //JSON sending code prep
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestMethod("POST");
            BufferedReader reader;
            String temp;
            //JSON write body
            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                writer.write(urlParameters);
                writer.flush();
                String line;
                int responseCode = conn.getResponseCode();
                System.out.println("\nSending 'POST' request to URL : " + url);
                System.out.println("With a POST body of : " + urlParameters);
                System.out.println("Response Code : " + responseCode);
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                //start creating the responce string
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }   temp = response.toString();
            }
            reader.close();
            results = temp;
        }catch (MalformedURLException ex) {
            Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
        }catch (ProtocolException ex) {
            Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
        }catch (IOException ex) {
            Logger.getLogger(Planner.class.getName()).log(Level.SEVERE, null, ex);
        }
        return results;
    }
    @Deprecated
    public void createStructureOfCase(JSONObject input) throws ParseException, BadCaseStructureException{
        StoredPaths pan = new StoredPaths();
        //Begin the initialisation process.
        OntModelSpec s = new OntModelSpec(PelletReasonerFactory.THE_SPEC);
        OntDocumentManager dm = OntDocumentManager.getInstance();
        dm.setFileManager( FileManager.get() );
        s.setDocumentManager(dm);
        OntModel m = ModelFactory.createOntologyModel(s,null);
        //
        InputStream in = FileManager.get().open(StoredPaths.casebasepath);
        if (in == null) {
            throw new IllegalArgumentException("File: " + StoredPaths.casebasepath + " not found");
        }
        // read the file
        m.read(in, null);
        JSONParser par = new JSONParser();
        String problemnames = String.valueOf(((JSONObject)par.parse(String.valueOf(input.get("payload")))).get("probattribs"));
        String solutionnames = String.valueOf(((JSONObject)par.parse(String.valueOf(input.get("payload")))).get("solattribs"));
        String[] problems = problemnames.split("#");
        String[] solutions = solutionnames.split("#");
        DatatypeProperty data;
        for(String problem:problems){
            if(problem.startsWith("has"))
                data = m.createDatatypeProperty(pan.casebase+problem);
            else
                throw new BadCaseStructureException("Could not Create Case Structure");
        }
        for(String solution:solutions){
            if(solution.startsWith("has"))
                data = m.createDatatypeProperty(pan.casebase+solution);
            else
                throw new BadCaseStructureException("Could not Create Case Structure");
        }
        try{
            try (FileOutputStream out = new FileOutputStream(StoredPaths.casebasepath, false)) {
                m.write( out, "RDF/XML");
            }
            m.close();
        }catch(IOException fe){
            System.out.println("File not found.");
        }
    }
    @Deprecated
    public class BadCaseStructureException extends Exception {
        public BadCaseStructureException(){}
        public BadCaseStructureException(String message){super(message);}
    }
}
