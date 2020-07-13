/**
   Authors:
   Rawan Alkhalaf - 1605159
   Dana AlAhdal   - 1607540
 */

package osp.Threads;

import osp.Utilities.*;
import osp.IFLModules.*;
import osp.Tasks.*;
import osp.Hardware.*;
import osp.Devices.*;
import osp.Memory.*;
import osp.Resources.*;

/**
   This class is responsible for actions related to threads, including
   creating, killing, dispatching, resuming, and suspending threads.

   @OSPProject Threads 
*/

public class ThreadCB extends IflThreadCB 
{
    /**
       The thread constructor. Must call 
       	   super();
       as its first statement.
		
	   Date of Last Modification: 02/03/2020
	   
       @OSPProject Threads
    */
	
    public ThreadCB()
    {
        super();
    }

    /**
       This method will be called once at the beginning of the
       simulation. The student can set up static variables here.
       
       Date of Last Modification: 02/03/2020
       
       @OSPProject Threads
    */
    
    static GenericList readyQueue;
    
    public static void init()
    {
        readyQueue = new GenericList();
    }

    /** 
        Sets up a new thread and adds it to the given task. 
        The method must set the ready status and attempt to 
        add thread to task. If the latter fails because there
        are already too many threads in this task, so does 
        this method, otherwise, the thread is appended 
        to the ready queue and dispatch() is called.

		The priority of the thread can be set using the 
		getPriority/setPriority methods. However, OSP itself
		doesn't care what the actual value of the priority is. 
		These methods are just provided in case priority
		scheduling is required.

		@return thread or null
	
		Date of Last Modification: 02/03/2020

        @OSPProject Threads
    */
    
    static public ThreadCB do_create(TaskCB task)
    {
    	// 1. If the given task is null, call dispatcher and return null
        // 2. If the task already have the maximum number of threads, call dispatcher and return null
    	if (task == null || task.getThreadCount() == MaxThreadsPerTask ) { 
        	dispatch(); 
        	return null;
        }
        
        ThreadCB thread = new ThreadCB(); // 3. Create the Thread
        thread.setPriority(task.getPriority()); // 4. Set up the thread priority same as the priority of task
        thread.setStatus(ThreadReady); // 5. Set up the thread status to ThreadReady
        thread.setTask(task); // 6. Associate the task with the thread
       
        // 7. Associate the thread with the task; if this fails then call dispatch and return null
        if(task.addThread(thread) == FAILURE) {
        	dispatch();
        	return null;	
        }
        
        // 8. Append the new thread to the ready queue
        readyQueue.append(thread); 
        
        // 9. Call dispatch and return thread.
    	dispatch();
    	return thread;
    }

    /** 
		Kills the specified thread. 

		The status must be set to ThreadKill, the thread must
		be removed from the task's list of threads and its pending
		IORBs must be purged from all device queues.
        
		If some thread was on the ready queue, it must removed,
		if the thread was running, the processor becomes idle, 
		and dispatch() must be called to resume a waiting thread.
	
		Date of Last Modification: 02/03/2020
	
		@OSPProject Threads
    */
    public void do_kill()
    {
    	// 1. If thread status is ThreadReady then remove it from the ready queue
    	if(getStatus() == ThreadReady) {
    		readyQueue.remove(this);
    	}
    	
    	else if(getStatus() == ThreadRunning) {
    		if (getTask().getCurrentThread() == this) { // 2. verify that it is the current thread
    			MMU.setPTBR(null); // 3. set the PTBR to null
    			getTask().setCurrentThread(null); // 3. remove the threadís association with its task
    		}
    	}
    	
    	setStatus(ThreadKill); // 4. Set the thread status to ThreadKill
    	getTask().removeThread(this);
    	
    	// 5. Loop through the device table to purge any IORB associated with this thread
    	for(int i=0; i<Device.getTableSize();i++) {
    		Device.get(i).cancelPendingIO(this);
    	}
    	
    	ResourceCB.giveupResources(this); // 6. Release all resources
    	
    	dispatch(); // 7. Call dispatch
    	
    	// 8. Check if the task has any threads left. If not, then kill the task.
    	if (getTask().getThreadCount() == 0) {
    		getTask().kill();
    	}
    }

    /** 
        Suspends the thread that is currently on the processor 
        on the specified event. 

        Note that the thread being suspended doesn't need to be
        running. It can also be waiting for completion of a pagefault
        and be suspended on the IORB that is bringing the page in.
	
		Thread's status must be changed to ThreadWaiting or higher,
        the processor set to idle, the thread must be in the right
        waiting queue, and dispatch() must be called to give CPU
        control to some other thread.

		@param event - event on which to suspend this thread.
		
		Date of Last Modification: 04/03/2020
		
        @OSPProject Threads
    */
    
    public void do_suspend(Event event)
    {
    	if (getStatus() == ThreadRunning) {
    		if (MMU.getPTBR().getTask().getCurrentThread() == this) { // 1. verify it is the current thread
    			setStatus(ThreadWaiting); // 2. changing its status to ThreadWaiting
    			MMU.setPTBR(null); // 2. setting the PTBR to null
    			getTask().setCurrentThread(null); // 2. letting its task know that it is not the current thread anymore
    			
    		}
    	}
    	
    	// 3. If it is already waiting then increment its waiting status
    	else if (getStatus() >= ThreadWaiting) {
    		setStatus(getStatus()+1); 
    	}
    	
    	else
    		setStatus(ThreadWaiting);
    	
    	// 4. Make sure its not in the ready queue
    	if (readyQueue.contains(this)) {
    		readyQueue.remove(this);
    	}
    	
    	event.addThread(this); // 5. Add this thread to the event queue
    	dispatch(); // 6. Call dispatch
    }

    /** 
        Resumes the thread.
        
		Only a thread with the status ThreadWaiting or higher
		can be resumed.  The status must be set to ThreadReady or
		decremented, respectively.
		
		A ready thread should be placed on the ready queue.
		
		Date of Last Modification: 04/03/2020
		
		@OSPProject Threads
    */
    
    public void do_resume()
    {
    	if (getStatus() < ThreadWaiting) {
    		MyOut.print(this, "Attempt to resume " + this + ", which wasnít waiting");
    		return; 
    	}
    	
    	MyOut.print(this, "Resuming " + this);
    	
    	// resumes the thread from ThreadWaiting to ThreadReady
    	if (getStatus() == ThreadWaiting) 
    		setStatus(ThreadReady); 
    	
    	// decrement the thread's waiting level until its equal to ThreadWaiting	 			
    	else if (getStatus() > ThreadWaiting) 
    		setStatus(getStatus() - 1);
    	
    	// Puts the thread on the ready queue if its ready
    	if (getStatus() == ThreadReady)
    		readyQueue.append(this);
    	
    	dispatch(); // call dispatch
    }

    /** 
        Selects a thread from the run queue and dispatches it. 

        If there is just one thread ready to run, reschedule the thread 
        currently on the processor.

        In addition to setting the correct thread status it must
        update the PTBR.
	
		@return SUCCESS or FAILURE
		
		Date of Last Modification: 07/03/2020
		
        @OSPProject Threads
    */
    
    public static int do_dispatch()
    {
    	ThreadCB thread; // declare variable of type ThreadCB
    	
    	try {
    		
    		// 1. If there is currently a running thread and its status is ThreadRunning then just return SUCCESS
    		if (MMU.getPTBR().getTask().getCurrentThread().getStatus()==ThreadRunning)
    			return SUCCESS;
    	}
    	
    	catch(Exception e) {
    		
    		if (readyQueue.isEmpty()) { // 3. If the ready queue is empty then set the PTBR to null and return FAILURE
    			MMU.setPTBR(null);
    			return FAILURE;	
    		}
    		
    		else {
    			thread = (ThreadCB) readyQueue.removeHead(); // 2. If there is no currently running thread, select the thread at the head of the ready queue
    			thread.setStatus(ThreadRunning);  // 4. set the thread status to ThreadRunning
    			MMU.setPTBR(thread.getTask().getPageTable()); // 4. set the PTBR to point to the threadís page table
    	    	thread.getTask().setCurrentThread(thread); // 4. set the thread as the current thread of its task
    	    	HTimer.set(0);  // 4. set the interrupt timer to 0 (zero)
    		}
    		
    	}
    	
    	return SUCCESS; // 5. Return SUCCESS
    }

    /**
       Called by OSP after printing an error message. The student can
       insert code here to print various tables and data structures in
       their state just after the error happened.  The body can be
       left empty, if this feature is not used.

       @OSPProject Threads
    */
    
    public static void atError()
    {
        // your code goes here

    }

    /** Called by OSP after printing a warning message. The student
        can insert code here to print various tables and data
        structures in their state just after the warning happened.
        The body can be left empty, if this feature is not used.
       
        @OSPProject Threads
     */
    public static void atWarning()
    {
        // your code goes here

    }


    /*
       Feel free to add methods/fields to improve the readability of your code
    */

}

/*
      Feel free to add local classes to improve the readability of your code
*/
