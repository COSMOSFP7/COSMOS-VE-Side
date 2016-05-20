/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ntua.cosmos.main;

import com.ntua.cosmos.server.ServerWithConfig;

/**
 *
 * @author PANAGIOTIS BOURELOS NTUA PhD Candidate
 */
public class VEThreadsExecution implements Runnable{
    private Thread t;
    private final String threadName;
    VEThreadsExecution(String name){
        threadName = name;
    }
    @Override
    public void run() {
        //Body of Threading
        if(this.threadName.equalsIgnoreCase("Evaluator")){
            //TODO
        }else if(this.threadName.equalsIgnoreCase("Main")){
            //Start Server and Prepare VE Services by loading them from file.
            ServerWithConfig s = new ServerWithConfig();
        }
        else if(this.threadName.equalsIgnoreCase("CEListener")){
            //TODO
        }else{
            //Not supported
        }
    }
    public void start ()
    {
        if (t == null)
        {
           t = new Thread (this, threadName);
           t.start ();
        }
    }
}
