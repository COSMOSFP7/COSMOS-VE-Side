/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.ntua.cosmos.hackathonplanneraas;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author PANAGIOTIS BOURELOS NTUA PhD Candidate
 */
public class StoredPaths {
    public String friendbase = "http://www.localstore.friends.com/FriendOntology#";
    public String casebase = "http://www.localstore.casebase.com/CaseBaseOntology#";
    public static String dirpath;
    public static String friendlistpath;
    public static String friendlistpath1;
    public static String casebasepath;
    public static String servicepath;
    
    public void createPaths() {
 
	Properties prop = new Properties();
	InputStream input = null;
 
	try {
            input = new FileInputStream(System.getProperty("user.home")+"/config.properties");
            // load a properties file
            prop.load(input);
            dirpath = prop.getProperty("dirpath");
            friendlistpath = prop.getProperty("friendlistpath");
            friendlistpath1 = prop.getProperty("friendlistpath1");
            casebasepath = prop.getProperty("casebasepath");
            servicepath = prop.getProperty("servicepath");
	} catch (IOException ex) {
	} finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {

                }
            }
	}
    }
    
    public void storePaths(){
	Properties prop = new Properties();
	OutputStream output = null;
 
	try {
            output = new FileOutputStream(System.getProperty("user.home")+"/config.properties");
            prop.setProperty("dirpath", System.getProperty("user.home")+"/localstore");
            prop.setProperty("friendlistpath", System.getProperty("user.home")+"/localstore/friendlist.owl");
            prop.setProperty("friendlistpath1", System.getProperty("user.home")+"/localstore/friendlist1.owl");
            prop.setProperty("casebasepath", System.getProperty("user.home")+"/localstore/casebase.owl");
            prop.setProperty("servicepath", System.getProperty("user.home")+"/localstore/VE.owl");
            prop.store(output, null);
	} catch (IOException ex) {
	} finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {

                }
            }
	}
    }
    
    public void storeDataForRanking(String questioned, String answered, String reqid){
	Properties prop = new Properties();
	OutputStream output = null;
 
	try {
            output = new FileOutputStream(dirpath+"/temp.metrics");
            prop.setProperty("Questioned", questioned);
            prop.setProperty("Answered", answered);
            prop.setProperty("reqid", reqid);
            prop.store(output, null);
	} catch (IOException ex) {
	} finally {
            if (output != null) {
                try {
                    output.close();
                } catch (IOException e) {
                }
            }
	}
    }
    
    public static void reqIDStore(String reqid){
	try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(dirpath+"/reqs.txt", true)))) {
            out.println(reqid);
        }catch (IOException e) {
            //exception handling
        }
    }
    
    public static boolean reqIDFind(String reqid){
        boolean found = false;
	try(BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dirpath+"/reqs.txt")))){
            String line;
            while ((line = br.readLine()) != null)
            {
                if (line.equals(reqid))
                {
                    found = true;
                }
            }
        }catch (IOException e){
            //exception handling
        }
        return found;
    }
    
    public static final String myname = "Flat1";
    
    public String prefixrdf = "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>";
    public String prefixowl = "PREFIX owl: <http://www.w3.org/2002/07/owl#>";
    public String prefixxsd = "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>";
    public String prefixrdfs = "PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>";
    public String prefixCasePath = "PREFIX base: <http://www.localstore.casebase.com/CaseBaseOntology#>";
    public String prefixFriendPath = "PREFIX base: <http://www.localstore.friends.com/FriendOntology#>";
    public String prefixServicePath = "PREFIX base: <http://www.cosmos.ntua.gr/MasterCOSMOSOnt#>";
    
    /**
     * Method to update the path of the localstore for this VE.
     * 
     * @param newpath a string variable with the new full path to the owl file
     */
    public static void setCaseLocalPath(String newpath){
        casebasepath = newpath;
    }

    /**
     * Prints the path of the localstore.
     */
    public static void printLocalPath(){
        System.out.println(friendlistpath);
        System.out.println(casebasepath);
        System.out.println(servicepath);
    }
    
    public static String returnPath(File fs){
        String path = "";
        try {
            path = fs.getCanonicalPath();
        } catch (IOException ex) {
            Logger.getLogger(StoredPaths.class.getName()).log(Level.SEVERE, null, ex);
        }
        return path;
    }
}
