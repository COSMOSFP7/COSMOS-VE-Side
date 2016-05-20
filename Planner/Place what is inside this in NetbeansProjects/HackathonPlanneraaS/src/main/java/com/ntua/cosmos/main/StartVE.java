/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ntua.cosmos.main;

import com.ntua.cosmos.hackathonplanneraas.StoredPaths;
import org.json.simple.parser.ParseException;

/**
 *
 * @author PANAGIOTIS BOURELOS NTUA PhD Candidate
 */
public class StartVE {
    public static void main(String args[]) throws ParseException{
        StoredPaths storage = new StoredPaths();
        storage.createPaths();
        storage.storePaths();
        VEThreadsExecution R1 = new VEThreadsExecution("Main");
        R1.start();
    }
}
