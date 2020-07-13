/**
   Authors:
   Rawan Alkhalaf - 1605159
   Dana AlAhdal   - 1607540
 */

package osp.Threads;

import osp.IFLModules.*;
import osp.Hardware.*;

/**    
       The timer interrupt handler.  This class is called upon to
       handle timer interrupts.

       @OSPProject Threads
*/

public class TimerInterruptHandler extends IflTimerInterruptHandler
{
    /**
       This basically only needs to reset the times and dispatch
       another process.
       
       Date of Last Modification: 07/03/2020

       @OSPProject Threads
    */
	
    public void do_handleInterrupt()
    {
    	HTimer.set(0); // reset timer
        ThreadCB.dispatch(); // calls dispatch from ThreadCB
    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
